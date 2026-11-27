package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.FeedInventoryLevelEntity
import com.example.data.local.FeedInventoryRepository
import com.example.data.local.FeedMeasurementUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val INVENTORY_VM_TAG = "FeedInventoryVM"

enum class FeedInventorySortOption(val label: String) {
    RECENT("Recently Updated"),
    NAME_ASC("Product A-Z"),
    STOCK_ASC("Lowest Stock"),
    STOCK_DESC("Highest Stock")
}

data class UnitStockTotal(
    val unitOfMeasurement: String,
    val totalQuantity: Double,
    val itemCount: Int
)

data class FeedInventoryUiState(
    val items: List<FeedInventoryLevelEntity> = emptyList(),
    val filteredItems: List<FeedInventoryLevelEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "ALL",
    val selectedUnitFilter: String = "ALL",
    val lowStockOnly: Boolean = false,
    val sortOption: FeedInventorySortOption = FeedInventorySortOption.RECENT,
    val availableCategories: List<String> = emptyList(),
    val availableUnits: List<String> = FeedMeasurementUnit.defaultSymbols,
    val unitTotals: List<UnitStockTotal> = emptyList(),
    val lowStockCount: Int = 0,
    val totalTrackedProducts: Int = 0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

private data class FeedInventoryFilterState(
    val searchQuery: String = "",
    val selectedCategory: String = "ALL",
    val selectedUnitFilter: String = "ALL",
    val lowStockOnly: Boolean = false,
    val sortOption: FeedInventorySortOption = FeedInventorySortOption.RECENT
)

private data class FeedInventoryMutationState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

class FeedInventoryViewModel(
    private val repository: FeedInventoryRepository
) : ViewModel() {

    private val filterState = MutableStateFlow(FeedInventoryFilterState())
    private val mutationState = MutableStateFlow(FeedInventoryMutationState())

    val uiState: StateFlow<FeedInventoryUiState> = combine(
        repository.inventoryLevels,
        filterState,
        mutationState
    ) { items, filters, mutation ->
        val trimmedQuery = filters.searchQuery.trim()
        val categories = items.map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val units = (FeedMeasurementUnit.defaultSymbols + items.map { it.unitOfMeasurement })
            .filter { it.isNotBlank() }
            .distinct()

        val unitTotals = items
            .groupBy { it.unitOfMeasurement }
            .entries
            .map { (unit, group) ->
                UnitStockTotal(
                    unitOfMeasurement = unit,
                    totalQuantity = group.sumOf { it.stockQuantity },
                    itemCount = group.size
                )
            }
            .sortedByDescending { it.itemCount }

        val filtered = items.filter { entity ->
            val matchesQuery = trimmedQuery.isBlank() ||
                entity.productName.contains(trimmedQuery, ignoreCase = true) ||
                entity.category.contains(trimmedQuery, ignoreCase = true) ||
                entity.section.contains(trimmedQuery, ignoreCase = true) ||
                entity.unitOfMeasurement.contains(trimmedQuery, ignoreCase = true) ||
                entity.notes.contains(trimmedQuery, ignoreCase = true)
            val matchesCategory = filters.selectedCategory == "ALL" ||
                entity.category.equals(filters.selectedCategory, ignoreCase = true)
            val matchesUnit = filters.selectedUnitFilter == "ALL" ||
                entity.unitOfMeasurement.equals(filters.selectedUnitFilter, ignoreCase = true)
            val matchesLowStock = !filters.lowStockOnly || entity.isLowStock
            matchesQuery && matchesCategory && matchesUnit && matchesLowStock
        }.let { matched ->
            when (filters.sortOption) {
                FeedInventorySortOption.RECENT -> matched.sortedWith(
                    compareByDescending<FeedInventoryLevelEntity> { it.lastUpdatedTimestamp }
                        .thenBy { it.productName.lowercase() }
                )
                FeedInventorySortOption.NAME_ASC -> matched.sortedBy { it.productName.lowercase() }
                FeedInventorySortOption.STOCK_ASC -> matched.sortedWith(
                    compareBy<FeedInventoryLevelEntity> { it.stockQuantity }
                        .thenBy { it.productName.lowercase() }
                )
                FeedInventorySortOption.STOCK_DESC -> matched.sortedWith(
                    compareByDescending<FeedInventoryLevelEntity> { it.stockQuantity }
                        .thenBy { it.productName.lowercase() }
                )
            }
        }

        FeedInventoryUiState(
            items = items,
            filteredItems = filtered,
            searchQuery = filters.searchQuery,
            selectedCategory = filters.selectedCategory,
            selectedUnitFilter = filters.selectedUnitFilter,
            lowStockOnly = filters.lowStockOnly,
            sortOption = filters.sortOption,
            availableCategories = categories,
            availableUnits = units,
            unitTotals = unitTotals,
            lowStockCount = items.count { it.isLowStock },
            totalTrackedProducts = items.size,
            isLoading = false,
            isSaving = mutation.isSaving,
            errorMessage = mutation.errorMessage,
            statusMessage = mutation.statusMessage
        )
    }.catch { error ->
        if (error is CancellationException) throw error
        Log.e(INVENTORY_VM_TAG, "Failed to observe Room feed inventory levels", error)
        emit(
            FeedInventoryUiState(
                isLoading = false,
                errorMessage = error.message ?: "Unable to read local feed inventory database."
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = FeedInventoryUiState()
    )

    fun setSearchQuery(query: String) {
        filterState.update { it.copy(searchQuery = query) }
    }

    fun setCategoryFilter(category: String) {
        filterState.update { it.copy(selectedCategory = category.ifBlank { "ALL" }) }
    }

    fun setUnitFilter(unitOfMeasurement: String) {
        filterState.update { it.copy(selectedUnitFilter = unitOfMeasurement.ifBlank { "ALL" }) }
    }

    fun setLowStockOnly(enabled: Boolean) {
        filterState.update { it.copy(lowStockOnly = enabled) }
    }

    fun setSortOption(option: FeedInventorySortOption) {
        filterState.update { it.copy(sortOption = option) }
    }

    fun clearFilters() {
        filterState.value = FeedInventoryFilterState()
    }

    fun saveFeedInventoryItem(
        productName: String,
        stockQuantity: Double,
        unitOfMeasurement: String = FeedMeasurementUnit.KILOGRAMS.symbol,
        category: String = "Finished Feed",
        section: String = "",
        reorderThreshold: Double = 500.0,
        notes: String = "",
        existingId: Long = 0L
    ) {
        viewModelScope.launch {
            mutationState.update { it.copy(isSaving = true, errorMessage = null, statusMessage = null) }
            val result = if (existingId > 0L) {
                repository.updateInventoryRecord(
                    id = existingId,
                    productName = productName,
                    stockQuantity = stockQuantity,
                    unitOfMeasurement = unitOfMeasurement,
                    category = category,
                    section = section,
                    reorderThreshold = reorderThreshold,
                    notes = notes
                ).map { it.id }
            } else {
                repository.upsertProductStock(
                    productName = productName,
                    stockQuantity = stockQuantity,
                    unitOfMeasurement = unitOfMeasurement,
                    category = category,
                    section = section,
                    reorderThreshold = reorderThreshold,
                    notes = notes
                )
            }
            result.fold(
                onSuccess = {
                    val unitLabel = FeedMeasurementUnit.normalize(unitOfMeasurement)
                    mutationState.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = "Saved ${productName.trim()} ($stockQuantity $unitLabel) to Room inventory."
                        )
                    }
                },
                onFailure = { error ->
                    mutationState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to save feed inventory record."
                        )
                    }
                }
            )
        }
    }

    fun adjustStock(productName: String, deltaQuantity: Double) {
        viewModelScope.launch {
            mutationState.update { it.copy(isSaving = true, errorMessage = null, statusMessage = null) }
            repository.adjustStockQuantity(
                itemName = productName,
                deltaQuantity = deltaQuantity
            ).fold(
                onSuccess = { updated ->
                    mutationState.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = "${updated.productName} stock updated to ${updated.stockQuantity} ${updated.unitOfMeasurement}."
                        )
                    }
                },
                onFailure = { error ->
                    mutationState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to adjust stock quantity."
                        )
                    }
                }
            )
        }
    }

    fun setStockQuantity(productName: String, newQuantity: Double) {
        viewModelScope.launch {
            mutationState.update { it.copy(isSaving = true, errorMessage = null, statusMessage = null) }
            val existing = repository.getByItemName(productName)
            repository.upsertProductStock(
                productName = productName,
                stockQuantity = newQuantity,
                unitOfMeasurement = existing?.unitOfMeasurement ?: FeedMeasurementUnit.KILOGRAMS.symbol,
                category = existing?.category ?: "Finished Feed",
                section = existing?.section.orEmpty(),
                reorderThreshold = existing?.reorderThreshold,
                notes = existing?.notes
            ).fold(
                onSuccess = {
                    val unitLabel = existing?.unitOfMeasurement ?: FeedMeasurementUnit.KILOGRAMS.symbol
                    mutationState.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = "Updated ${productName.trim()} to $newQuantity $unitLabel."
                        )
                    }
                },
                onFailure = { error ->
                    mutationState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to set stock quantity."
                        )
                    }
                }
            )
        }
    }

    fun changeUnitOfMeasurement(productName: String, newUnitOfMeasurement: String) {
        viewModelScope.launch {
            mutationState.update { it.copy(isSaving = true, errorMessage = null, statusMessage = null) }
            repository.updateUnitOfMeasurement(
                productName = productName,
                unitOfMeasurement = newUnitOfMeasurement
            ).fold(
                onSuccess = { updated ->
                    mutationState.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = "Changed ${updated.productName} unit to ${updated.unitOfMeasurement}."
                        )
                    }
                },
                onFailure = { error ->
                    mutationState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to update unit of measurement."
                        )
                    }
                }
            )
        }
    }

    fun deleteItem(item: FeedInventoryLevelEntity) {
        viewModelScope.launch {
            mutationState.update { it.copy(isSaving = true, errorMessage = null, statusMessage = null) }
            repository.deleteItem(item).fold(
                onSuccess = {
                    mutationState.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = "Removed ${item.productName} from Room inventory."
                        )
                    }
                },
                onFailure = { error ->
                    mutationState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to delete feed inventory record."
                        )
                    }
                }
            )
        }
    }

    fun deleteByProductName(productName: String) {
        viewModelScope.launch {
            mutationState.update { it.copy(isSaving = true, errorMessage = null, statusMessage = null) }
            repository.deleteByItemName(productName).fold(
                onSuccess = {
                    mutationState.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = "Removed ${productName.trim()} from Room inventory."
                        )
                    }
                },
                onFailure = { error ->
                    mutationState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to delete feed inventory record."
                        )
                    }
                }
            )
        }
    }

    fun clearAllInventory() {
        viewModelScope.launch {
            mutationState.update { it.copy(isSaving = true, errorMessage = null, statusMessage = null) }
            repository.clearAll().fold(
                onSuccess = {
                    mutationState.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = "Cleared all local Room feed inventory records."
                        )
                    }
                },
                onFailure = { error ->
                    mutationState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to clear Room inventory."
                        )
                    }
                }
            )
        }
    }

    fun consumeStatusMessage() {
        mutationState.update { it.copy(statusMessage = null) }
    }

    fun consumeErrorMessage() {
        mutationState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                FeedInventoryViewModel(
                    repository = FeedInventoryRepository(context.applicationContext)
                )
            }
        }
    }
}
