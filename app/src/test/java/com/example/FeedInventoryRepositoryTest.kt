package com.example

import com.example.data.local.FeedInventoryLevelEntity
import com.example.data.local.FeedInventoryRepository
import com.example.data.local.FeedItemDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class InMemoryFeedItemDao : FeedItemDao {
    private var nextId = 1L
    private val items = MutableStateFlow<List<FeedInventoryLevelEntity>>(emptyList())

    override fun observeAllItems(): Flow<List<FeedInventoryLevelEntity>> =
        items.map { list ->
            list.sortedWith(
                compareByDescending<FeedInventoryLevelEntity> { it.lastUpdatedTimestamp }
                    .thenBy { it.itemName }
            )
        }

    override fun observeItemsByCategory(category: String): Flow<List<FeedInventoryLevelEntity>> =
        items.map { list ->
            list.filter { it.category == category }.sortedBy { it.itemName }
        }

    override fun observeItemsByUnit(unit: String): Flow<List<FeedInventoryLevelEntity>> =
        items.map { list ->
            list.filter { it.unit.equals(unit, ignoreCase = true) }.sortedBy { it.itemName }
        }

    override fun searchItemsByName(query: String): Flow<List<FeedInventoryLevelEntity>> =
        items.map { list ->
            list.filter { it.itemName.contains(query, ignoreCase = true) }
                .sortedBy { it.itemName }
        }

    override fun observeLowStockItems(threshold: Double): Flow<List<FeedInventoryLevelEntity>> =
        items.map { list ->
            list.filter { it.stockQuantity <= threshold }
                .sortedWith(
                    compareBy<FeedInventoryLevelEntity> { it.stockQuantity }
                        .thenBy { it.itemName }
                )
        }

    override fun observeItemsBelowReorderThreshold(): Flow<List<FeedInventoryLevelEntity>> =
        items.map { list ->
            list.filter { it.stockQuantity <= it.reorderThreshold }
                .sortedWith(
                    compareBy<FeedInventoryLevelEntity> { it.stockQuantity }
                        .thenBy { it.itemName }
                )
        }

    override fun observeDistinctUnits(): Flow<List<String>> =
        items.map { list ->
            list.map { it.unit }.distinct().sorted()
        }

    override suspend fun getItemById(id: Long): FeedInventoryLevelEntity? =
        items.value.firstOrNull { it.id == id }

    override suspend fun getItemByName(itemName: String): FeedInventoryLevelEntity? =
        items.value.firstOrNull { it.itemName == itemName }

    override suspend fun getItemCount(): Int = items.value.size

    override suspend fun insertItem(item: FeedInventoryLevelEntity): Long {
        val current = items.value.toMutableList()
        val existingIdx = current.indexOfFirst {
            (item.id != 0L && it.id == item.id) || it.itemName == item.itemName
        }
        val assignedId = if (existingIdx >= 0) {
            current[existingIdx].id
        } else if (item.id != 0L) {
            item.id
        } else {
            nextId++
        }
        val saved = item.copy(id = assignedId)
        if (existingIdx >= 0) {
            current[existingIdx] = saved
        } else {
            current.add(saved)
        }
        items.value = current
        return assignedId
    }

    override suspend fun insertItems(items: List<FeedInventoryLevelEntity>): List<Long> =
        items.map { insertItem(it) }

    override suspend fun updateItem(item: FeedInventoryLevelEntity): Int {
        val current = items.value.toMutableList()
        val idx = current.indexOfFirst { it.id == item.id }
        if (idx < 0) return 0
        current[idx] = item
        items.value = current
        return 1
    }

    override suspend fun updateStockByItemName(
        itemName: String,
        stockQuantity: Double,
        lastUpdatedTimestamp: Long
    ): Int {
        val current = items.value.toMutableList()
        val idx = current.indexOfFirst { it.itemName == itemName }
        if (idx < 0) return 0
        current[idx] = current[idx].copy(
            stockQuantity = stockQuantity,
            lastUpdatedTimestamp = lastUpdatedTimestamp
        )
        items.value = current
        return 1
    }

    override suspend fun updateStockAndUnitByProductName(
        productName: String,
        stockQuantity: Double,
        unit: String,
        lastUpdatedTimestamp: Long
    ): Int {
        val current = items.value.toMutableList()
        val idx = current.indexOfFirst { it.itemName == productName }
        if (idx < 0) return 0
        current[idx] = current[idx].copy(
            stockQuantity = stockQuantity,
            unit = unit,
            lastUpdatedTimestamp = lastUpdatedTimestamp
        )
        items.value = current
        return 1
    }

    override suspend fun updateUnitByProductName(
        productName: String,
        unit: String,
        lastUpdatedTimestamp: Long
    ): Int {
        val current = items.value.toMutableList()
        val idx = current.indexOfFirst { it.itemName == productName }
        if (idx < 0) return 0
        current[idx] = current[idx].copy(
            unit = unit,
            lastUpdatedTimestamp = lastUpdatedTimestamp
        )
        items.value = current
        return 1
    }

    override suspend fun deleteItem(item: FeedInventoryLevelEntity): Int {
        val before = items.value.size
        items.value = items.value.filterNot { it.id == item.id }
        return before - items.value.size
    }

    override suspend fun deleteItemById(id: Long): Int {
        val before = items.value.size
        items.value = items.value.filterNot { it.id == id }
        return before - items.value.size
    }

    override suspend fun deleteItemByName(itemName: String): Int {
        val before = items.value.size
        items.value = items.value.filterNot { it.itemName == itemName }
        return before - items.value.size
    }

    override suspend fun deleteAllItems() {
        items.value = emptyList()
    }
}

class FeedInventoryRepositoryTest {

    private lateinit var repository: FeedInventoryRepository

    @Before
    fun setup() {
        repository = FeedInventoryRepository(InMemoryFeedItemDao())
    }

    @Test
    fun upsertAndRetrieveFeedInventoryLevelWithTimestamp() = runBlocking {
        val timestamp = 1795400000000L
        val insertResult = repository.upsertInventoryLevel(
            itemName = "MB BROODSOW MASH",
            stockQuantity = 19150.0,
            unit = "kg",
            category = "Finished Feed",
            section = "SAN ROQUE",
            lastUpdatedTimestamp = timestamp
        )
        assertTrue(insertResult.isSuccess)

        val stored = repository.getByItemName("MB BROODSOW MASH")
        assertNotNull(stored)
        assertEquals("MB BROODSOW MASH", stored?.itemName)
        assertEquals(19150.0, stored?.stockQuantity ?: 0.0, 0.001)
        assertEquals(timestamp, stored?.lastUpdatedTimestamp)

        val updatedTimestamp = timestamp + 60_000L
        val updateResult = repository.upsertInventoryLevel(
            itemName = "MB BROODSOW MASH",
            stockQuantity = 18500.0,
            lastUpdatedTimestamp = updatedTimestamp
        )
        assertTrue(updateResult.isSuccess)
        assertEquals(1, repository.getItemCount())

        val allItems = repository.inventoryLevels.first()
        assertEquals(1, allItems.size)
        assertEquals(18500.0, allItems.first().stockQuantity, 0.001)
        assertEquals(updatedTimestamp, allItems.first().lastUpdatedTimestamp)
    }

    @Test
    fun adjustStockQuantityUpdatesStockAndClampsAtZero() = runBlocking {
        repository.upsertInventoryLevel(
            itemName = "Yellow Corn (Grains)",
            stockQuantity = 4200.0,
            category = "Grains & Energy",
            lastUpdatedTimestamp = 1000L
        )

        val added = repository.adjustStockQuantity(
            itemName = "Yellow Corn (Grains)",
            deltaQuantity = 800.0,
            lastUpdatedTimestamp = 2000L
        )
        assertTrue(added.isSuccess)
        assertEquals(5000.0, added.getOrNull()?.stockQuantity ?: 0.0, 0.001)
        assertEquals(2000L, added.getOrNull()?.lastUpdatedTimestamp)

        val deducted = repository.adjustStockQuantity(
            itemName = "Yellow Corn (Grains)",
            deltaQuantity = -6000.0,
            lastUpdatedTimestamp = 3000L
        )
        assertTrue(deducted.isSuccess)
        assertEquals(0.0, deducted.getOrNull()?.stockQuantity ?: -1.0, 0.001)
        assertEquals(3000L, deducted.getOrNull()?.lastUpdatedTimestamp)
    }

    @Test
    fun rejectsBlankNameOrNegativeStockAndSupportsSearchLowStockDelete() = runBlocking {
        assertTrue(repository.upsertInventoryLevel("   ", 100.0).isFailure)
        assertTrue(repository.upsertInventoryLevel("Test Feed", -5.0).isFailure)

        repository.syncInventorySnapshot(
            listOf(
                FeedInventoryLevelEntity(
                    itemName = "CHOOKMAX 1000",
                    stockQuantity = 250.0,
                    lastUpdatedTimestamp = 1000L
                ),
                FeedInventoryLevelEntity(
                    itemName = "BARAKO DUCK PELLET",
                    stockQuantity = 3500.0,
                    lastUpdatedTimestamp = 2000L
                )
            )
        )

        val searchResults = repository.searchInventoryLevels("CHOOK").first()
        assertEquals(1, searchResults.size)
        assertEquals("CHOOKMAX 1000", searchResults.first().itemName)

        val lowStock = repository.observeLowStockLevels(500.0).first()
        assertEquals(1, lowStock.size)
        assertEquals("CHOOKMAX 1000", lowStock.first().itemName)

        val toDelete = repository.getByItemName("CHOOKMAX 1000")!!
        assertTrue(repository.deleteItem(toDelete).isSuccess)
        assertNull(repository.getByItemName("CHOOKMAX 1000"))
        assertEquals(1, repository.getItemCount())
    }

    @Test
    fun tracksProductNameUnitOfMeasurementAndReorderThreshold() = runBlocking {
        val saveResult = repository.upsertProductStock(
            productName = "HOG GROWER PELLET",
            stockQuantity = 120.0,
            unitOfMeasurement = "BAGS",
            reorderThreshold = 150.0,
            notes = "Warehouse B"
        )
        assertTrue(saveResult.isSuccess)
        val stored = repository.getByItemName("HOG GROWER PELLET")
        assertNotNull(stored)
        assertEquals("HOG GROWER PELLET", stored?.productName)
        assertEquals("bags", stored?.unitOfMeasurement)
        assertTrue(stored?.isLowStock == true)

        val belowReorder = repository.observeItemsBelowReorderThreshold().first()
        assertEquals(1, belowReorder.size)

        val unitUpdated = repository.updateUnitOfMeasurement("HOG GROWER PELLET", "tons")
        assertTrue(unitUpdated.isSuccess)
        assertEquals("tons", unitUpdated.getOrNull()?.unitOfMeasurement)

        val recordUpdated = repository.updateInventoryRecord(
            id = stored!!.id,
            productName = "HOG GROWER PELLET",
            stockQuantity = 12.5,
            unitOfMeasurement = "tons",
            reorderThreshold = 5.0,
            notes = "Pallet 4"
        )
        assertTrue(recordUpdated.isSuccess)
        assertEquals(12.5, recordUpdated.getOrNull()?.stockQuantity ?: 0.0, 0.001)
        assertTrue(repository.observeItemsBelowReorderThreshold().first().isEmpty())
    }
}
