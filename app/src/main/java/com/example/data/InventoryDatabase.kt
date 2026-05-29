package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        InventoryItem::class,
        FurnitureRecipe::class,
        TransactionLog::class,
        SiteMeasurement::class,
        CustomerKhata::class,
        KhataEntry::class,
        Supplier::class,
        SupplierMaterial::class,
        SupplierReceipt::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class InventoryDatabase : RoomDatabase() {
    abstract val inventoryDao: InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: InventoryDatabase? = null

        fun getDatabase(context: Context): InventoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InventoryDatabase::class.java,
                    "upvc_inventory_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
