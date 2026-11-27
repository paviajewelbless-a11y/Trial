package com.example.data.local

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

class FeedInventoryRepository(
    private val feedItemDao: FeedItemDao
) {
    constructor(context: Context) : this(
        FeedInventoryDatabase.getInstance(context).feedItemDao()
    )

    val inventoryLevels: Flow<List<FeedInventoryLevelEntity>> = feedItemDao.observeAllItems()

    fun observeByCategory(category: String): Flow<List<FeedInventoryLevelEntity>> =
        feedItemDao.observeItemsByCategory(category.trim())

    fun observeByUnit(unitOfMeasurement: String): Flow<List<FeedInventoryLevelEntity>> =
        feedItemDao.observeItemsByUnit(FeedMeasurementUnit.normalize(unitOfMeasurement))

    fun searchInventoryLevels(query: String): Flow<List<FeedInventoryLevelEntity>> =
        feedItemDao.searchItemsByName(query.trim())

    fun observeLowStockLevels(threshold: Double): Flow<List<FeedInventoryLevelEntity>> =
        feedItemDao.observeLowStockItems(threshold)

    fun observeItemsBelowReorderThreshold(): Flow<List<FeedInventoryLevelEntity>> =
        feedItemDao.observeItemsBelowReorderThreshold()

    fun observeDistinctUnits(): Flow<List<String>> = feedItemDao.observeDistinctUnits()

    suspend fun getById(id: Long): FeedInventoryLevelEntity? = feedItemDao.getItemById(id)

    suspend fun getByItemName(itemName: String): FeedInventoryLevelEntity? =
        feedItemDao.getItemByName(itemName.trim())

    suspend fun getItemCount(): Int = feedItemDao.getItemCount()

    suspend fun upsertProductStock(
        productName: String,
        stockQuantity: Double,
        unitOfMeasurement: String = FeedMeasurementUnit.KILOGRAMS.symbol,
        category: String = "Finished Feed",
        section: String = "",
        reorderThreshold: Double? = null,
        notes: String? = null,
        lastUpdatedTimestamp: Long = System.currentTimeMillis()
    ): Result<Long> = upsertInventoryLevel(
        itemName = productName,
        stockQuantity = stockQuantity,
        unit = unitOfMeasurement,
        category = category,
        section = section,
        reorderThreshold = reorderThreshold,
        notes = notes,
        lastUpdatedTimestamp = lastUpdatedTimestamp
    )

    suspend fun upsertInventoryLevel(
        itemName: String,
        stockQuantity: Double,
        unit: String = FeedMeasurementUnit.KILOGRAMS.symbol,
        category: String = "Finished Feed",
        section: String = "",
        reorderThreshold: Double? = null,
        notes: String? = null,
        lastUpdatedTimestamp: Long = System.currentTimeMillis()
    ): Result<Long> {
        val normalizedName = itemName.trim()
        if (normalizedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Product name must not be blank."))
        }
        if (!stockQuantity.isFinite() || stockQuantity < 0.0) {
            return Result.failure(IllegalArgumentException("Stock quantity must be a non-negative finite number."))
        }
        val normalizedUnit = unit.trim()
        if (normalizedUnit.isBlank()) {
            return Result.failure(IllegalArgumentException("Unit of measurement must not be blank."))
        }
        if (reorderThreshold != null && (!reorderThreshold.isFinite() || reorderThreshold < 0.0)) {
            return Result.failure(IllegalArgumentException("Reorder threshold must be non-negative."))
        }
        return try {
            val existing = feedItemDao.getItemByName(normalizedName)
            val entity = FeedInventoryLevelEntity(
                id = existing?.id ?: 0L,
                itemName = normalizedName,
                stockQuantity = stockQuantity,
                unit = FeedMeasurementUnit.normalize(normalizedUnit),
                category = category.trim().ifBlank { existing?.category ?: "Finished Feed" },
                section = section.trim().ifBlank { existing?.section.orEmpty() },
                reorderThreshold = reorderThreshold ?: existing?.reorderThreshold ?: 500.0,
                notes = notes?.trim() ?: existing?.notes.orEmpty(),
                lastUpdatedTimestamp = lastUpdatedTimestamp
            )
            val rowId = feedItemDao.insertItem(entity)
            Result.success(if (existing != null) existing.id else rowId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateInventoryRecord(
        id: Long,
        productName: String,
        stockQuantity: Double,
        unitOfMeasurement: String,
        category: String = "Finished Feed",
        section: String = "",
        reorderThreshold: Double = 500.0,
        notes: String = "",
        lastUpdatedTimestamp: Long = System.currentTimeMillis()
    ): Result<FeedInventoryLevelEntity> {
        val normalizedName = productName.trim()
        if (normalizedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Product name must not be blank."))
        }
        if (!stockQuantity.isFinite() || stockQuantity < 0.0) {
            return Result.failure(IllegalArgumentException("Stock quantity must be a non-negative finite number."))
        }
        val rawUnit = unitOfMeasurement.trim()
        if (rawUnit.isBlank()) {
            return Result.failure(IllegalArgumentException("Unit of measurement must not be blank."))
        }
        if (!reorderThreshold.isFinite() || reorderThreshold < 0.0) {
            return Result.failure(IllegalArgumentException("Reorder threshold must be non-negative."))
        }
        return try {
            val existingById = if (id > 0L) feedItemDao.getItemById(id) else null
            val existingByName = feedItemDao.getItemByName(normalizedName)
            if (existingById != null && existingByName != null && existingByName.id != existingById.id) {
                return Result.failure(
                    IllegalStateException("Another inventory record already uses product name '$normalizedName'.")
                )
            }
            val normalizedUnit = FeedMeasurementUnit.normalize(rawUnit)
            val resolvedId = existingById?.id ?: existingByName?.id ?: 0L
            val entity = FeedInventoryLevelEntity(
                id = resolvedId,
                itemName = normalizedName,
                stockQuantity = stockQuantity,
                unit = normalizedUnit,
                category = category.trim().ifBlank { "Finished Feed" },
                section = section.trim(),
                reorderThreshold = reorderThreshold,
                notes = notes.trim(),
                lastUpdatedTimestamp = lastUpdatedTimestamp
            )
            if (resolvedId > 0L && existingById != null) {
                feedItemDao.updateItem(entity)
                Result.success(entity)
            } else {
                val insertedId = feedItemDao.insertItem(entity)
                Result.success(entity.copy(id = if (resolvedId > 0L) resolvedId else insertedId))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUnitOfMeasurement(
        productName: String,
        unitOfMeasurement: String,
        lastUpdatedTimestamp: Long = System.currentTimeMillis()
    ): Result<FeedInventoryLevelEntity> {
        val normalizedName = productName.trim()
        val rawUnit = unitOfMeasurement.trim()
        if (normalizedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Product name must not be blank."))
        }
        if (rawUnit.isBlank()) {
            return Result.failure(IllegalArgumentException("Unit of measurement must not be blank."))
        }
        return try {
            val existing = feedItemDao.getItemByName(normalizedName)
                ?: return Result.failure(NoSuchElementException("Inventory item '$normalizedName' not found."))
            val normalizedUnit = FeedMeasurementUnit.normalize(rawUnit)
            val updated = existing.copy(
                unit = normalizedUnit,
                lastUpdatedTimestamp = lastUpdatedTimestamp
            )
            feedItemDao.updateItem(updated)
            Result.success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adjustStockQuantity(
        itemName: String,
        deltaQuantity: Double,
        lastUpdatedTimestamp: Long = System.currentTimeMillis()
    ): Result<FeedInventoryLevelEntity> {
        val normalizedName = itemName.trim()
        if (normalizedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Product name must not be blank."))
        }
        if (!deltaQuantity.isFinite()) {
            return Result.failure(IllegalArgumentException("Stock delta must be finite."))
        }
        return try {
            val existing = feedItemDao.getItemByName(normalizedName)
                ?: return Result.failure(NoSuchElementException("Inventory item '$normalizedName' not found."))
            val updatedQty = max(0.0, existing.stockQuantity + deltaQuantity)
            val updated = existing.copy(
                stockQuantity = updatedQty,
                lastUpdatedTimestamp = lastUpdatedTimestamp
            )
            feedItemDao.updateItem(updated)
            Result.success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncInventorySnapshot(items: List<FeedInventoryLevelEntity>): Result<Unit> {
        return try {
            val sanitized = items.mapNotNull { item ->
                val name = item.itemName.trim()
                if (name.isBlank() || !item.stockQuantity.isFinite() || item.stockQuantity < 0.0) {
                    null
                } else {
                    val existing = feedItemDao.getItemByName(name)
                    val existingNote = existing?.notes?.trim().orEmpty()
                    val incomingNote = item.notes.trim()
                    val isAutoReprocessNote = existingNote.startsWith("REPROCESSED ") ||
                        existingNote.startsWith("IN REPROCESS ") ||
                        existingNote.startsWith("DAMAGED -")
                    val resolvedNotes = when {
                        incomingNote.isNotBlank() && (existingNote.isBlank() || isAutoReprocessNote) -> incomingNote
                        isAutoReprocessNote -> ""
                        else -> existingNote.ifBlank { incomingNote }
                    }
                    item.copy(
                        id = existing?.id ?: 0L,
                        itemName = name,
                        unit = FeedMeasurementUnit.normalize(existing?.unit ?: item.unit),
                        category = item.category.trim().ifBlank { existing?.category ?: "Finished Feed" },
                        section = item.section.trim().ifBlank { existing?.section.orEmpty() },
                        reorderThreshold = existing?.reorderThreshold ?: item.reorderThreshold.coerceAtLeast(0.0),
                        notes = resolvedNotes
                    )
                }
            }
            if (sanitized.isNotEmpty()) {
                feedItemDao.insertItems(sanitized)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun replaceSnapshot(items: List<FeedInventoryLevelEntity>): Result<Unit> {
        return try {
            val sanitized = items.mapNotNull { item ->
                val name = item.itemName.trim()
                if (name.isBlank() || !item.stockQuantity.isFinite() || item.stockQuantity < 0.0) {
                    null
                } else {
                    item.copy(
                        id = 0L,
                        itemName = name,
                        unit = FeedMeasurementUnit.normalize(item.unit),
                        category = item.category.trim().ifBlank { "Finished Feed" },
                        section = item.section.trim(),
                        reorderThreshold = item.reorderThreshold.coerceAtLeast(0.0),
                        notes = item.notes.trim()
                    )
                }
            }
            feedItemDao.replaceAllItems(sanitized)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteItem(item: FeedInventoryLevelEntity): Result<Unit> {
        return try {
            feedItemDao.deleteItem(item)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteById(id: Long): Result<Unit> {
        return try {
            feedItemDao.deleteItemById(id)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteByItemName(itemName: String): Result<Unit> {
        val normalizedName = itemName.trim()
        if (normalizedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Product name must not be blank."))
        }
        return try {
            feedItemDao.deleteItemByName(normalizedName)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAll(): Result<Unit> {
        return try {
            feedItemDao.deleteAllItems()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
