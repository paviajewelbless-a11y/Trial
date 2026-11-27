package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FeedInventoryLevelEntity::class],
    version = 2,
    exportSchema = false
)
abstract class FeedInventoryDatabase : RoomDatabase() {

    abstract fun feedItemDao(): FeedItemDao

    companion object {
        private const val DATABASE_NAME = "feed_inventory_levels.db"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE feed_inventory_levels ADD COLUMN reorder_threshold REAL NOT NULL DEFAULT 500.0"
                )
                db.execSQL(
                    "ALTER TABLE feed_inventory_levels ADD COLUMN notes TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        @Volatile
        private var instance: FeedInventoryDatabase? = null

        fun getInstance(context: Context): FeedInventoryDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FeedInventoryDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }

        fun createInMemory(
            context: Context,
            allowMainThreadQueries: Boolean = false
        ): FeedInventoryDatabase {
            val builder = Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                FeedInventoryDatabase::class.java
            )
            if (allowMainThreadQueries) {
                builder.allowMainThreadQueries()
            }
            return builder.build()
        }
    }
}
