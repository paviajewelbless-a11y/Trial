package com.example

import com.example.data.local.FeedInventoryLevelEntity
import com.example.data.local.FeedInventoryRepository
import com.example.data.local.FeedItemDao
import com.example.data.local.FeedMeasurementUnit
import com.example.viewmodel.FeedInventorySortOption
import com.example.viewmodel.FeedInventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeFeedItemDao : FeedItemDao {
    private var nextId = 1L
    private val rows = MutableStateFlow<List<FeedInventoryLevelEntity>>(emptyList())

    override fun observeAllItems(): Flow<List<FeedInventoryLevelEntity>> =
        rows.map { list ->
            list.sortedWith(
                compareByDescending<FeedInventoryLevelEntity> { it.lastUpdatedTimestamp }
                    .thenBy { it.itemName }
            )
        }

    override fun observeItemsByCategory(category: String): Flow<List<FeedInventoryLevelEntity>> =
        rows.map { list -> list.filter { it.category == category }.sortedBy { it.itemName } }

    override fun observeItemsByUnit(unit: String): Flow<List<FeedInventoryLevelEntity>> =
        rows.map { list -> list.filter { it.unit.equals(unit, ignoreCase = true) }.sortedBy { it.itemName } }

    override fun searchItemsByName(query: String): Flow<List<FeedInventoryLevelEntity>> =
        rows.map { list -> list.filter { it.itemName.contains(query, ignoreCase = true) } }

    override fun observeLowStockItems(threshold: Double): Flow<List<FeedInventoryLevelEntity>> =
        rows.map { list -> list.filter { it.stockQuantity <= threshold } }

    override fun observeItemsBelowReorderThreshold(): Flow<List<FeedInventoryLevelEntity>> =
        rows.map { list -> list.filter { it.stockQuantity <= it.reorderThreshold } }

    override fun observeDistinctUnits(): Flow<List<String>> =
        rows.map { list -> list.map { it.unit }.distinct().sorted() }

    override suspend fun getItemById(id: Long): FeedInventoryLevelEntity? =
        rows.value.firstOrNull { it.id == id }

    override suspend fun getItemByName(itemName: String): FeedInventoryLevelEntity? =
        rows.value.firstOrNull { it.itemName == itemName }

    override suspend fun getItemCount(): Int = rows.value.size

    override suspend fun insertItem(item: FeedInventoryLevelEntity): Long {
        val list = rows.value.toMutableList()
        val idx = list.indexOfFirst {
            (item.id != 0L && it.id == item.id) || it.itemName == item.itemName
        }
        val id = if (idx >= 0) list[idx].id else if (item.id != 0L) item.id else nextId++
        val entity = item.copy(id = id)
        if (idx >= 0) list[idx] = entity else list.add(entity)
        rows.value = list
        return id
    }

    override suspend fun insertItems(items: List<FeedInventoryLevelEntity>): List<Long> =
        items.map { insertItem(it) }

    override suspend fun updateItem(item: FeedInventoryLevelEntity): Int {
        val list = rows.value.toMutableList()
        val idx = list.indexOfFirst { it.id == item.id }
        if (idx < 0) return 0
        list[idx] = item
        rows.value = list
        return 1
    }

    override suspend fun updateStockByItemName(
        itemName: String,
        stockQuantity: Double,
        lastUpdatedTimestamp: Long
    ): Int = 0

    override suspend fun updateStockAndUnitByProductName(
        productName: String,
        stockQuantity: Double,
        unit: String,
        lastUpdatedTimestamp: Long
    ): Int = 0

    override suspend fun updateUnitByProductName(
        productName: String,
        unit: String,
        lastUpdatedTimestamp: Long
    ): Int = 0

    override suspend fun deleteItem(item: FeedInventoryLevelEntity): Int {
        val before = rows.value.size
        rows.value = rows.value.filterNot { it.id == item.id }
        return before - rows.value.size
    }

    override suspend fun deleteItemById(id: Long): Int {
        val before = rows.value.size
        rows.value = rows.value.filterNot { it.id == id }
        return before - rows.value.size
    }

    override suspend fun deleteItemByName(itemName: String): Int {
        val before = rows.value.size
        rows.value = rows.value.filterNot { it.itemName == itemName }
        return before - rows.value.size
    }

    override suspend fun deleteAllItems() {
        rows.value = emptyList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FeedInventoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FeedInventoryRepository
    private lateinit var viewModel: FeedInventoryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FeedInventoryRepository(FakeFeedItemDao())
        viewModel = FeedInventoryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveFeedInventoryItem tracks productName stockQuantity and normalized unitOfMeasurement`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect() }

        viewModel.saveFeedInventoryItem(
            productName = "MB BROODSOW MASH",
            stockQuantity = 240.0,
            unitOfMeasurement = "BAGS",
            category = "Finished Feed",
            section = "SAN ROQUE",
            reorderThreshold = 100.0,
            notes = "Bay A2"
        )
        viewModel.saveFeedInventoryItem(
            productName = "Yellow Corn",
            stockQuantity = 350.0,
            unitOfMeasurement = "kg",
            category = "Grains & Energy",
            reorderThreshold = 500.0
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.totalTrackedProducts)
        assertEquals(1, state.lowStockCount)

        val broodsow = state.items.firstOrNull { it.productName == "MB BROODSOW MASH" }
        assertNotNull(broodsow)
        assertEquals(240.0, broodsow!!.stockQuantity, 0.001)
        assertEquals(FeedMeasurementUnit.BAGS.symbol, broodsow.unitOfMeasurement)
        assertFalse(broodsow.isLowStock)

        val corn = state.items.firstOrNull { it.productName == "Yellow Corn" }
        assertNotNull(corn)
        assertTrue(corn!!.isLowStock)

        val unitMap = state.unitTotals.associateBy { it.unitOfMeasurement }
        assertEquals(240.0, unitMap["bags"]?.totalQuantity ?: 0.0, 0.001)
        assertEquals(350.0, unitMap["kg"]?.totalQuantity ?: 0.0, 0.001)

        collectJob.cancel()
    }

    @Test
    fun `filters by unitOfMeasurement search query lowStockOnly and adjusts stock or unit`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect() }

        viewModel.saveFeedInventoryItem(
            productName = "CHOOKMAX 1000",
            stockQuantity = 150.0,
            unitOfMeasurement = "bags",
            reorderThreshold = 200.0
        )
        viewModel.saveFeedInventoryItem(
            productName = "BARAKO DUCK PELLET",
            stockQuantity = 4500.0,
            unitOfMeasurement = "kg",
            reorderThreshold = 500.0
        )
        advanceUntilIdle()

        viewModel.setUnitFilter("bags")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.filteredItems.size)
        assertEquals("CHOOKMAX 1000", viewModel.uiState.value.filteredItems.single().productName)

        viewModel.setUnitFilter("ALL")
        viewModel.setLowStockOnly(true)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.filteredItems.size)

        viewModel.adjustStock("CHOOKMAX 1000", 75.0)
        viewModel.changeUnitOfMeasurement("BARAKO DUCK PELLET", "tons")
        advanceUntilIdle()

        viewModel.setLowStockOnly(false)
        viewModel.setSortOption(FeedInventorySortOption.NAME_ASC)
        advanceUntilIdle()

        val updated = viewModel.uiState.value.filteredItems
        assertEquals("BARAKO DUCK PELLET", updated[0].productName)
        assertEquals("tons", updated[0].unitOfMeasurement)
        assertEquals(225.0, updated[1].stockQuantity, 0.001)
        assertEquals(0, viewModel.uiState.value.lowStockCount)

        collectJob.cancel()
    }

    @Test
    fun `invalid inputs expose validation errorMessage without corrupting inventory`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect() }

        viewModel.saveFeedInventoryItem(
            productName = "   ",
            stockQuantity = 100.0,
            unitOfMeasurement = "kg"
        )
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(0, viewModel.uiState.value.totalTrackedProducts)

        viewModel.consumeErrorMessage()
        viewModel.saveFeedInventoryItem(
            productName = "Starter Mash",
            stockQuantity = -25.0,
            unitOfMeasurement = "kg"
        )
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(0, viewModel.uiState.value.totalTrackedProducts)

        collectJob.cancel()
    }
}
