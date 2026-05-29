package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    // Inventory Items
    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    fun getAllInventoryItemsFlow(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    suspend fun getAllInventoryItems(): List<InventoryItem>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getInventoryItemById(id: Int): InventoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem): Long

    @Update
    suspend fun updateInventoryItem(item: InventoryItem)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteInventoryItemById(id: Int)

    // Furniture Recipes
    @Query("SELECT * FROM furniture_recipes ORDER BY timestamp DESC")
    fun getAllRecipesFlow(): Flow<List<FurnitureRecipe>>

    @Query("SELECT * FROM furniture_recipes ORDER BY timestamp DESC")
    suspend fun getAllRecipes(): List<FurnitureRecipe>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: FurnitureRecipe): Long

    @Query("DELETE FROM furniture_recipes WHERE id = :id")
    suspend fun deleteRecipeById(id: Int)

    // Transaction Logs
    @Query("SELECT * FROM transaction_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<TransactionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TransactionLog)

    @Query("DELETE FROM transaction_logs")
    suspend fun clearAllLogs()

    // -------------------------------------------------------------
    // Site Measurements Queries
    // -------------------------------------------------------------
    @Query("SELECT * FROM site_measurements ORDER BY timestamp DESC")
    fun getAllMeasurementsFlow(): Flow<List<SiteMeasurement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: SiteMeasurement): Long

    @Query("DELETE FROM site_measurements WHERE id = :id")
    suspend fun deleteMeasurementById(id: Int)

    @Query("DELETE FROM site_measurements")
    suspend fun clearAllMeasurements()

    // -------------------------------------------------------------
    // Customer Ledger Queries (Khata)
    // -------------------------------------------------------------
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomersFlow(): Flow<List<CustomerKhata>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerKhata): Long

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: Int)

    @Query("SELECT * FROM khata_entries WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getEntriesForCustomerFlow(customerId: Int): Flow<List<KhataEntry>>

    @Query("SELECT * FROM khata_entries ORDER BY timestamp DESC")
    fun getAllKhataEntriesFlow(): Flow<List<KhataEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhataEntry(entry: KhataEntry): Long

    @Query("DELETE FROM khata_entries WHERE id = :id")
    suspend fun deleteKhataEntryById(id: Int)

    @Query("DELETE FROM khata_entries WHERE customerId = :customerId")
    suspend fun deleteEntriesForCustomer(customerId: Int)

    // -------------------------------------------------------------
    // Supplier Queries
    // -------------------------------------------------------------
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliersFlow(): Flow<List<Supplier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Query("DELETE FROM suppliers WHERE id = :id")
    suspend fun deleteSupplierById(id: Int)

    @Query("SELECT * FROM supplier_materials WHERE supplierId = :supplierId ORDER BY name ASC")
    fun getMaterialsForSupplierFlow(supplierId: Int): Flow<List<SupplierMaterial>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplierMaterial(material: SupplierMaterial): Long

    @Query("SELECT * FROM supplier_materials WHERE id = :id")
    suspend fun getSupplierMaterialById(id: Int): SupplierMaterial?

    @Query("DELETE FROM supplier_materials WHERE id = :id")
    suspend fun deleteSupplierMaterialById(id: Int)

    @Query("DELETE FROM supplier_materials WHERE supplierId = :supplierId")
    suspend fun deleteMaterialsForSupplier(supplierId: Int)

    @Query("SELECT * FROM supplier_receipts ORDER BY timestamp DESC")
    fun getAllSupplierReceiptsFlow(): Flow<List<SupplierReceipt>>

    @Query("SELECT * FROM supplier_receipts WHERE supplierId = :supplierId ORDER BY timestamp DESC")
    fun getReceiptsForSupplierFlow(supplierId: Int): Flow<List<SupplierReceipt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplierReceipt(receipt: SupplierReceipt): Long

    @Query("DELETE FROM supplier_receipts WHERE id = :id")
    suspend fun deleteSupplierReceiptById(id: Int)

    @Query("DELETE FROM supplier_receipts WHERE supplierId = :supplierId")
    suspend fun deleteReceiptsForSupplier(supplierId: Int)
}
