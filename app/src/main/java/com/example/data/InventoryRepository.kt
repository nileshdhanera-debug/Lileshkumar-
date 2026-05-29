package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Locale

sealed interface BuildResult {
    object Success : BuildResult
    data class Error(val message: String, val deficientItems: List<String>) : BuildResult
}

class InventoryRepository(private val inventoryDao: InventoryDao) {

    val allInventoryItems: Flow<List<InventoryItem>> = inventoryDao.getAllInventoryItemsFlow()
    val allRecipes: Flow<List<FurnitureRecipe>> = inventoryDao.getAllRecipesFlow()
    val allLogs: Flow<List<TransactionLog>> = inventoryDao.getAllLogsFlow()

    suspend fun addInventoryItem(item: InventoryItem): Long {
        val result = inventoryDao.insertInventoryItem(item)
        val description = "Added ${item.quantity} ${item.unit} of ${item.name} (${item.specification})"
        inventoryDao.insertLog(
            TransactionLog(
                type = "ADD_STOCK",
                title = "New Item Added",
                description = description
            )
        )
        return result
    }

    suspend fun updateInventoryItemDirect(item: InventoryItem) {
        inventoryDao.updateInventoryItem(item)
    }

    suspend fun addQuantityToStock(itemId: Int, amount: Double) {
        val item = inventoryDao.getInventoryItemById(itemId)
        if (item != null) {
            val updated = item.copy(quantity = item.quantity + amount)
            inventoryDao.updateInventoryItem(updated)
            inventoryDao.insertLog(
                TransactionLog(
                    type = "ADD_STOCK",
                    title = "Stock Restocked",
                    description = "Added $amount ${item.unit} to ${item.name}. Total: ${updated.quantity}"
                )
            )
        }
    }

    suspend fun deleteInventoryItem(itemId: Int) {
        val item = inventoryDao.getInventoryItemById(itemId)
        if (item != null) {
            inventoryDao.deleteInventoryItemById(itemId)
            inventoryDao.insertLog(
                TransactionLog(
                    type = "DELETE_STOCK",
                    title = "Item Removed",
                    description = "Deleted ${item.name} (${item.specification}) from system"
                )
            )
        }
    }

    suspend fun addNewRecipe(name: String, description: String, ingredients: List<RecipeIngredient>) {
        val converters = Converters()
        val json = converters.ingredientsToString(ingredients)
        val recipe = FurnitureRecipe(
            name = name,
            description = description,
            ingredientsJson = json
        )
        inventoryDao.insertRecipe(recipe)
        inventoryDao.insertLog(
            TransactionLog(
                type = "EDIT_STOCK",
                title = "Created Recipe",
                description = "Added template for '$name' requiring ${ingredients.size} parts"
            )
        )
    }

    suspend fun deleteRecipe(recipeId: Int) {
        inventoryDao.deleteRecipeById(recipeId)
    }

    suspend fun buildFurniture(
        recipeName: String,
        adjustedIngredients: List<RecipeIngredient>,
        deductStock: Boolean = true
    ): BuildResult {
        if (!deductStock) {
            // User chose NOT to deduct stock. Just log in the diary history.
            val buildDetails = StringBuilder("બિલ્ડ કરેલ સાધનો (Items used in build):\n")
            for (ing in adjustedIngredients) {
                buildDetails.append("- ${ing.neededQty} of ${ing.name}\n")
            }
            inventoryDao.insertLog(
                TransactionLog(
                    type = "BUILD_FURNITURE",
                    title = "Banayi di: $recipeName (સ્ટોક કાપ્યા વગર / No Stock Deduct)",
                    description = "માત્ર ડાયરીમાં રેકોર્ડ સેવ કર્યો છે. સ્ટોક ઓછો કરવામાં આવ્યો નથી.\n$buildDetails"
                )
            )
            return BuildResult.Success
        }

        // Double check stock sufficiency
        val deficientItems = mutableListOf<String>()
        val itemsToDeduct = mutableListOf<Pair<InventoryItem, Double>>()

        for (ingredient in adjustedIngredients) {
            val item = inventoryDao.getInventoryItemById(ingredient.itemId)
            if (item == null) {
                deficientItems.add("${ingredient.name} (Not found in system)")
                continue
            }
            if (item.quantity < ingredient.neededQty) {
                deficientItems.add("${item.name} (${ingredient.neededQty} required, but only ${item.quantity} in stock)")
            } else {
                itemsToDeduct.add(Pair(item, ingredient.neededQty))
            }
        }

        if (deficientItems.isNotEmpty()) {
            return BuildResult.Error("Aamachi stock kam che!", deficientItems)
        }

        // Subtract quantity
        val loggedDetails = StringBuilder()
        for ((item, neededQty) in itemsToDeduct) {
            val updated = item.copy(quantity = item.quantity - neededQty)
            inventoryDao.updateInventoryItem(updated)
            loggedDetails.append("- $neededQty ${item.unit} of ${item.name}\n")
        }

        // Add history log
        inventoryDao.insertLog(
            TransactionLog(
                type = "BUILD_FURNITURE",
                title = "Banayi di: $recipeName",
                description = "Stock auto deleted (deducted):\n$loggedDetails"
            )
        )

        return BuildResult.Success
    }

    // -------------------------------------------------------------
    // Site Measurements Methods
    // -------------------------------------------------------------
    val allMeasurements: Flow<List<SiteMeasurement>> = inventoryDao.getAllMeasurementsFlow()

    suspend fun addSiteMeasurement(measurement: SiteMeasurement): Long {
        return inventoryDao.insertMeasurement(measurement)
    }

    suspend fun deleteSiteMeasurement(id: Int) {
        inventoryDao.deleteMeasurementById(id)
    }

    suspend fun clearMeasurements() {
        inventoryDao.clearAllMeasurements()
    }

    // -------------------------------------------------------------
    // Customer Khata Ledger Methods
    // -------------------------------------------------------------
    val allCustomers: Flow<List<CustomerKhata>> = inventoryDao.getAllCustomersFlow()

    suspend fun addCustomer(customer: CustomerKhata): Long {
        return inventoryDao.insertCustomer(customer)
    }

    suspend fun deleteCustomer(id: Int) {
        inventoryDao.deleteCustomerById(id)
        inventoryDao.deleteEntriesForCustomer(id)
    }

    fun getEntriesForCustomer(customerId: Int): Flow<List<KhataEntry>> {
        return inventoryDao.getEntriesForCustomerFlow(customerId)
    }

    suspend fun addKhataEntry(entry: KhataEntry): Long {
        return inventoryDao.insertKhataEntry(entry)
    }

    suspend fun deleteKhataEntry(id: Int) {
        inventoryDao.deleteKhataEntryById(id)
    }

    suspend fun clearHistory() {
        inventoryDao.clearAllLogs()
    }

    suspend fun seedMockDataIfEmpty() {
        val items = inventoryDao.getAllInventoryItems()
        if (items.isEmpty()) {
            // Seed Stock Items
            val item1Id = inventoryDao.insertInventoryItem(
                InventoryItem(
                    name = "uPVC Sheet White (18mm)",
                    type = "SHEET",
                    specification = "8ft x 4ft Super Gloss",
                    quantity = 25.0,
                    unit = "Sheets",
                    minThreshold = 5.0
                )
            ).toInt()

            val item2Id = inventoryDao.insertInventoryItem(
                InventoryItem(
                    name = "uPVC Sheet Grey (12mm)",
                    type = "SHEET",
                    specification = "8ft x 4ft Matte Smooth",
                    quantity = 20.0,
                    unit = "Sheets",
                    minThreshold = 4.0
                )
            ).toInt()

            val item3Id = inventoryDao.insertInventoryItem(
                InventoryItem(
                    name = "uPVC Wood Grain Sheet (15mm)",
                    type = "SHEET",
                    specification = "8ft x 4ft Textured Brown",
                    quantity = 15.0,
                    unit = "Sheets",
                    minThreshold = 3.0
                )
            ).toInt()

            val item4Id = inventoryDao.insertInventoryItem(
                InventoryItem(
                    name = "Premium Stainless Steel Hinge",
                    type = "HARDWARE",
                    specification = "3-inch Heavy Duty",
                    quantity = 120.0,
                    unit = "Pieces",
                    minThreshold = 20.0
                )
            ).toInt()

            val item5Id = inventoryDao.insertInventoryItem(
                InventoryItem(
                    name = "Self-Tapping Wood Screws",
                    type = "HARDWARE",
                    specification = "1.5-inch Star-Head",
                    quantity = 600.0,
                    unit = "Pieces",
                    minThreshold = 100.0
                )
            ).toInt()

            val item6Id = inventoryDao.insertInventoryItem(
                InventoryItem(
                    name = "Magnetic Door Catcher",
                    type = "HARDWARE",
                    specification = "Double Magnet Nylon White",
                    quantity = 40.0,
                    unit = "Pieces",
                    minThreshold = 8.0
                )
            ).toInt()

            val item7Id = inventoryDao.insertInventoryItem(
                InventoryItem(
                    name = "Aluminium Designer Handle",
                    type = "HARDWARE",
                    specification = "6-inch Brushed Silver",
                    quantity = 50.0,
                    unit = "Pieces",
                    minThreshold = 10.0
                )
            ).toInt()

            // Seed Some Recipes
            val converters = Converters()

            // 1. Double Door Wardrobe
            val recipe1Ings = listOf(
                RecipeIngredient(item2Id, "uPVC Sheet Grey (12mm)", "Matte Smooth", 2.5),
                RecipeIngredient(item4Id, "Premium Stainless Steel Hinge", "3-inch Heavy Duty", 4.0),
                RecipeIngredient(item5Id, "Self-Tapping Wood Screws", "1.5-inch Star-Head", 32.0),
                RecipeIngredient(item7Id, "Aluminium Designer Handle", "6-inch Brushed Silver", 2.0),
                RecipeIngredient(item6Id, "Magnetic Door Catcher", "Double Magnet Nylon White", 2.0)
            )
            inventoryDao.insertRecipe(
                FurnitureRecipe(
                    name = "Double Door Wardrobe (Almari)",
                    description = "Standard 2-door wardrobe back & panels",
                    ingredientsJson = converters.ingredientsToString(recipe1Ings)
                )
            )

            // 2. Heavy Premium Bedroom Door
            val recipe2Ings = listOf(
                RecipeIngredient(item1Id, "uPVC Sheet White (18mm)", "Super Gloss", 1.5),
                RecipeIngredient(item4Id, "Premium Stainless Steel Hinge", "3-inch Heavy Duty", 3.0),
                RecipeIngredient(item5Id, "Self-Tapping Wood Screws", "1.5-inch Star-Head", 18.0),
                RecipeIngredient(item7Id, "Aluminium Designer Handle", "6-inch Brushed Silver", 1.0)
            )
            inventoryDao.insertRecipe(
                FurnitureRecipe(
                    name = "Bedroom Door (Standard)",
                    description = "Solid white single panel entrance flush door",
                    ingredientsJson = converters.ingredientsToString(recipe2Ings)
                )
            )

            // 3. Bathroom Wood-Grain Premium Door
            val recipe3Ings = listOf(
                RecipeIngredient(item3Id, "uPVC Wood Grain Sheet (15mm)", "Textured Brown", 1.2),
                RecipeIngredient(item4Id, "Premium Stainless Steel Hinge", "3-inch Heavy Duty", 3.0),
                RecipeIngredient(item5Id, "Self-Tapping Wood Screws", "1.5-inch Star-Head", 16.0),
                RecipeIngredient(item7Id, "Aluminium Designer Handle", "6-inch Brushed Silver", 1.0),
                RecipeIngredient(item6Id, "Magnetic Door Catcher", "Double Magnet Nylon White", 1.0)
            )
            inventoryDao.insertRecipe(
                FurnitureRecipe(
                    name = "Wood-Grain Bathroom Door",
                    description = "Waterproof aesthetic brown door for baths",
                    ingredientsJson = converters.ingredientsToString(recipe3Ings)
                )
            )

            // Log initialization
            inventoryDao.insertLog(
                TransactionLog(
                    type = "EDIT_STOCK",
                    title = "Database Seeded",
                    description = "Preloaded default uPVC sheet materials, premium hardware, and 3 furniture templates!"
                )
            )
        }
    }

    // -------------------------------------------------------------
    // Supplier Management Methods
    // -------------------------------------------------------------
    val allSuppliers: Flow<List<Supplier>> = inventoryDao.getAllSuppliersFlow()
    val allSupplierReceipts: Flow<List<SupplierReceipt>> = inventoryDao.getAllSupplierReceiptsFlow()

    suspend fun addSupplier(supplier: Supplier): Long {
        val result = inventoryDao.insertSupplier(supplier)
        inventoryDao.insertLog(
            TransactionLog(
                type = "ADD_STOCK",
                title = "New Supplier Added",
                description = "Onboarded supplier '${supplier.name}' to the system."
            )
        )
        return result
    }

    suspend fun deleteSupplier(supplierId: Int) {
        inventoryDao.deleteSupplierById(supplierId)
        inventoryDao.deleteMaterialsForSupplier(supplierId)
        inventoryDao.deleteReceiptsForSupplier(supplierId)
    }

    fun getMaterialsForSupplier(supplierId: Int): Flow<List<SupplierMaterial>> {
        return inventoryDao.getMaterialsForSupplierFlow(supplierId)
    }

    suspend fun addSupplierMaterial(material: SupplierMaterial): Long {
        return inventoryDao.insertSupplierMaterial(material)
    }

    suspend fun deleteSupplierMaterial(id: Int) {
        inventoryDao.deleteSupplierMaterialById(id)
    }

    fun getReceiptsForSupplier(supplierId: Int): Flow<List<SupplierReceipt>> {
        return inventoryDao.getReceiptsForSupplierFlow(supplierId)
    }

    suspend fun addSupplierReceipt(receipt: SupplierReceipt, restockLinkedItems: Boolean): Long {
        val result = inventoryDao.insertSupplierReceipt(receipt)
        
        inventoryDao.insertLog(
            TransactionLog(
                type = "ADD_STOCK",
                title = "Supplier Invoice created",
                description = "Created receipt #${receipt.receiptNumber} with total ₹${receipt.totalAmount}."
            )
        )

        if (restockLinkedItems) {
            val converters = Converters()
            val items = converters.stringToReceiptItems(receipt.itemsJson) ?: emptyList()
            for (item in items) {
                val mat = inventoryDao.getSupplierMaterialById(item.materialId)
                if (mat != null && mat.linkedInventoryItemId != null) {
                    val stockItem = inventoryDao.getInventoryItemById(mat.linkedInventoryItemId)
                    if (stockItem != null) {
                        val updated = stockItem.copy(quantity = stockItem.quantity + item.quantity)
                        inventoryDao.updateInventoryItem(updated)
                        inventoryDao.insertLog(
                            TransactionLog(
                                type = "ADD_STOCK",
                                title = "Restocked from Receipt",
                                description = "Receipt auto-credit: Added ${item.quantity} ${stockItem.unit} to ${stockItem.name}"
                            )
                        )
                    }
                }
            }
        }
        return result
    }
}
