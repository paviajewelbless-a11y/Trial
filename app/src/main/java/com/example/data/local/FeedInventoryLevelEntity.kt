package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

enum class FeedMeasurementUnit(
    val symbol: String,
    val displayLabel: String
) {
    KILOGRAMS("kg", "Kilograms (kg)"),
    BAGS("bags", "Bags (bags)"),
    METRIC_TONS("tons", "Metric Tons (tons)"),
    POUNDS("lbs", "Pounds (lbs)"),
    SACKS("sacks", "Sacks (sacks)");

    companion object {
        val defaultSymbols: List<String> = entries.map { it.symbol }

        fun normalize(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return KILOGRAMS.symbol
            return entries.firstOrNull {
                it.symbol.equals(trimmed, ignoreCase = true) ||
                    it.name.equals(trimmed, ignoreCase = true)
            }?.symbol ?: trimmed
        }
    }
}

@Entity(
    tableName = "feed_inventory_levels",
    indices = [
        Index(value = ["item_name"], unique = true)
    ]
)
data class FeedInventoryLevelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "item_name")
    val itemName: String,
    @ColumnInfo(name = "stock_quantity")
    val stockQuantity: Double,
    @ColumnInfo(name = "unit")
    val unit: String = FeedMeasurementUnit.KILOGRAMS.symbol,
    @ColumnInfo(name = "category")
    val category: String = "Finished Feed",
    @ColumnInfo(name = "section")
    val section: String = "",
    @ColumnInfo(name = "reorder_threshold")
    val reorderThreshold: Double = 500.0,
    @ColumnInfo(name = "notes")
    val notes: String = "",
    @ColumnInfo(name = "last_updated_timestamp")
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
) {
    @get:Ignore
    val productName: String
        get() = itemName

    @get:Ignore
    val unitOfMeasurement: String
        get() = unit

    @get:Ignore
    val isLowStock: Boolean
        get() = stockQuantity <= reorderThreshold

    companion object {
        fun create(
            productName: String,
            stockQuantity: Double,
            unitOfMeasurement: String = FeedMeasurementUnit.KILOGRAMS.symbol,
            category: String = "Finished Feed",
            section: String = "",
            reorderThreshold: Double = 500.0,
            notes: String = "",
            lastUpdatedTimestamp: Long = System.currentTimeMillis(),
            id: Long = 0L
        ): FeedInventoryLevelEntity = FeedInventoryLevelEntity(
            id = id,
            itemName = productName.trim(),
            stockQuantity = stockQuantity,
            unit = FeedMeasurementUnit.normalize(unitOfMeasurement),
            category = category.trim().ifBlank { "Finished Feed" },
            section = section.trim(),
            reorderThreshold = reorderThreshold.coerceAtLeast(0.0),
            notes = notes.trim(),
            lastUpdatedTimestamp = lastUpdatedTimestamp
        )
    }
}
