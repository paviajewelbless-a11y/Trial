package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedItemDao {

    // Create / Upsert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: FeedInventoryLevelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<FeedInventoryLevelEntity>): List<Long>

    // Read (Reactive Flows & Point Lookups)
    @Query("SELECT * FROM feed_inventory_levels ORDER BY last_updated_timestamp DESC, item_name ASC")
    fun observeAllItems(): Flow<List<FeedInventoryLevelEntity>>

    @Query("SELECT * FROM feed_inventory_levels WHERE category = :category ORDER BY item_name ASC")
    fun observeItemsByCategory(category: String): Flow<List<FeedInventoryLevelEntity>>

    @Query("SELECT * FROM feed_inventory_levels WHERE LOWER(unit) = LOWER(:unit) ORDER BY item_name ASC")
    fun observeItemsByUnit(unit: String): Flow<List<FeedInventoryLevelEntity>>

    @Query("SELECT * FROM feed_inventory_levels WHERE item_name LIKE '%' || :query || '%' ORDER BY item_name ASC")
    fun searchItemsByName(query: String): Flow<List<FeedInventoryLevelEntity>>

    @Query("SELECT * FROM feed_inventory_levels WHERE stock_quantity <= :threshold ORDER BY stock_quantity ASC, item_name ASC")
    fun observeLowStockItems(threshold: Double): Flow<List<FeedInventoryLevelEntity>>

    @Query("SELECT * FROM feed_inventory_levels WHERE stock_quantity <= reorder_threshold ORDER BY stock_quantity ASC, item_name ASC")
    fun observeItemsBelowReorderThreshold(): Flow<List<FeedInventoryLevelEntity>>

    @Query("SELECT DISTINCT unit FROM feed_inventory_levels ORDER BY unit ASC")
    fun observeDistinctUnits(): Flow<List<String>>

    @Query("SELECT * FROM feed_inventory_levels WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): FeedInventoryLevelEntity?

    @Query("SELECT * FROM feed_inventory_levels WHERE item_name = :itemName LIMIT 1")
    suspend fun getItemByName(itemName: String): FeedInventoryLevelEntity?

    @Query("SELECT COUNT(*) FROM feed_inventory_levels")
    suspend fun getItemCount(): Int

    // Update
    @Update
    suspend fun updateItem(item: FeedInventoryLevelEntity): Int

    @Query(
        "UPDATE feed_inventory_levels " +
            "SET stock_quantity = :stockQuantity, last_updated_timestamp = :lastUpdatedTimestamp " +
            "WHERE item_name = :itemName"
    )
    suspend fun updateStockByItemName(
        itemName: String,
        stockQuantity: Double,
        lastUpdatedTimestamp: Long
    ): Int

    @Query(
        "UPDATE feed_inventory_levels " +
            "SET stock_quantity = :stockQuantity, unit = :unit, last_updated_timestamp = :lastUpdatedTimestamp " +
            "WHERE item_name = :productName"
    )
    suspend fun updateStockAndUnitByProductName(
        productName: String,
        stockQuantity: Double,
        unit: String,
        lastUpdatedTimestamp: Long
    ): Int

    @Query(
        "UPDATE feed_inventory_levels " +
            "SET unit = :unit, last_updated_timestamp = :lastUpdatedTimestamp " +
            "WHERE item_name = :productName"
    )
    suspend fun updateUnitByProductName(
        productName: String,
        unit: String,
        lastUpdatedTimestamp: Long
    ): Int

    // Delete
    @Delete
    suspend fun deleteItem(item: FeedInventoryLevelEntity): Int

    @Query("DELETE FROM feed_inventory_levels WHERE id = :id")
    suspend fun deleteItemById(id: Long): Int

    @Query("DELETE FROM feed_inventory_levels WHERE item_name = :itemName")
    suspend fun deleteItemByName(itemName: String): Int

    @Query("DELETE FROM feed_inventory_levels")
    suspend fun deleteAllItems()

    // Atomic Snapshot Replacement
    @Transaction
    suspend fun replaceAllItems(items: List<FeedInventoryLevelEntity>) {
        deleteAllItems()
        if (items.isNotEmpty()) {
            insertItems(items)
        }
    }
}
