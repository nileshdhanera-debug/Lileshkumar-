package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BuildResult
import com.example.data.FurnitureRecipe
import com.example.data.InventoryItem
import com.example.data.InventoryRepository
import com.example.data.RecipeIngredient
import com.example.data.TransactionLog
import com.example.data.SiteMeasurement
import com.example.data.CustomerKhata
import com.example.data.KhataEntry
import com.example.data.Supplier
import com.example.data.SupplierMaterial
import com.example.data.SupplierReceipt
import com.example.data.ReceiptItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.seedMockDataIfEmpty()
        }
    }

    val inventoryItems: StateFlow<List<InventoryItem>> = repository.allInventoryItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recipes: StateFlow<List<FurnitureRecipe>> = repository.allRecipes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val logs: StateFlow<List<TransactionLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val siteMeasurements: StateFlow<List<SiteMeasurement>> = repository.allMeasurements
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val customers: StateFlow<List<CustomerKhata>> = repository.allCustomers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val suppliers: StateFlow<List<Supplier>> = repository.allSuppliers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val supplierReceipts: StateFlow<List<SupplierReceipt>> = repository.allSupplierReceipts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI States
    private val _buildResultEvent = MutableSharedFlow<BuildResult>()
    val buildResultEvent: SharedFlow<BuildResult> = _buildResultEvent.asSharedFlow()

    // Temporary building state to customize recipes on the fly
    private val _selectedRecipe = MutableStateFlow<FurnitureRecipe?>(null)
    val selectedRecipe: StateFlow<FurnitureRecipe?> = _selectedRecipe.asStateFlow()

    private val _adjustingIngredients = MutableStateFlow<List<RecipeIngredient>>(emptyList())
    val adjustingIngredients: StateFlow<List<RecipeIngredient>> = _adjustingIngredients.asStateFlow()

    // Add stock item
    fun addNewItem(name: String, type: String, specification: String, quantity: Double, unit: String, minThreshold: Double) {
        viewModelScope.launch {
            val newItem = InventoryItem(
                name = name,
                type = type.uppercase(),
                specification = specification,
                quantity = quantity,
                unit = unit,
                minThreshold = minThreshold
            )
            repository.addInventoryItem(newItem)
        }
    }

    // Add stock to existing item (Restock)
    fun addStockQuantity(itemId: Int, amount: Double) {
        viewModelScope.launch {
            repository.addQuantityToStock(itemId, amount)
        }
    }

    // Direct edit/adjust stock level
    fun updateItemDirectly(item: InventoryItem) {
        viewModelScope.launch {
            repository.updateInventoryItemDirect(item)
        }
    }

    // Delete item
    fun removeItem(itemId: Int) {
        viewModelScope.launch {
            repository.deleteInventoryItem(itemId)
        }
    }

    // Create a whole recipe template
    fun createRecipeTemplate(name: String, description: String, ingredients: List<RecipeIngredient>) {
        viewModelScope.launch {
            repository.addNewRecipe(name, description, ingredients)
        }
    }

    // Delete a recipe template
    fun removeRecipe(recipeId: Int) {
        viewModelScope.launch {
            repository.deleteRecipe(recipeId)
        }
    }

    // Set the active recipe to customize and build
    fun selectRecipeForBuild(recipe: FurnitureRecipe) {
        _selectedRecipe.value = recipe
        val converters = com.example.data.Converters()
        val ingredients = converters.stringToIngredients(recipe.ingredientsJson) ?: emptyList()
        _adjustingIngredients.value = ingredients
    }

    fun clearBuildSelection() {
        _selectedRecipe.value = null
        _adjustingIngredients.value = emptyList()
    }

    // Live update of ingredients count on current recipe configuration screen
    fun updateIngredientQuantity(itemId: Int, newQuantity: Double) {
        val currentList = _adjustingIngredients.value.toMutableList()
        val index = currentList.indexOfFirst { it.itemId == itemId }
        if (index != -1) {
            val updated = currentList[index].copy(neededQty = if (newQuantity < 0.0) 0.0 else newQuantity)
            currentList[index] = updated
            _adjustingIngredients.value = currentList
        }
    }

    // Confirm building and deduct items from database
    fun buildFurnitureAndDeduct(recipeName: String, deductStock: Boolean = true) {
        viewModelScope.launch {
            val ingredientsToUse = _adjustingIngredients.value
            val result = repository.buildFurniture(recipeName, ingredientsToUse, deductStock)
            _buildResultEvent.emit(result)
            if (result is BuildResult.Success) {
                // Clear state on successful build/deduction
                clearBuildSelection()
            }
        }
    }

    // Custom quick/on-the-fly furniture deduction without any predefined recipe
    fun buildCustomFurnitureDirect(title: String, ingredients: List<RecipeIngredient>, deductStock: Boolean = true) {
        viewModelScope.launch {
            val result = repository.buildFurniture(title, ingredients, deductStock)
            _buildResultEvent.emit(result)
        }
    }

    // -------------------------------------------------------------
    // Site Measurements Methods
    // -------------------------------------------------------------
    fun addSiteMeasurement(title: String, height: Double, width: Double, isFeet: Boolean, isDouble: Boolean, sqFt: Double) {
        viewModelScope.launch {
            val m = SiteMeasurement(
                title = title,
                height = height,
                width = width,
                isFeet = isFeet,
                isDouble = isDouble,
                sqFt = sqFt
            )
            repository.addSiteMeasurement(m)
        }
    }

    fun deleteSiteMeasurement(id: Int) {
        viewModelScope.launch {
            repository.deleteSiteMeasurement(id)
        }
    }

    fun clearMeasurements() {
        viewModelScope.launch {
            repository.clearMeasurements()
        }
    }

    // -------------------------------------------------------------
    // Customer Khata Ledger Methods
    // -------------------------------------------------------------
    fun addCustomer(name: String, phone: String) {
        viewModelScope.launch {
            val c = CustomerKhata(name = name, phone = phone)
            repository.addCustomer(c)
        }
    }

    fun deleteCustomer(id: Int) {
        viewModelScope.launch {
            repository.deleteCustomer(id)
        }
    }

    fun getEntriesForCustomer(customerId: Int): Flow<List<KhataEntry>> {
        return repository.getEntriesForCustomer(customerId)
    }

    fun addKhataEntry(customerId: Int, amount: Double, type: String, note: String) {
        viewModelScope.launch {
            val e = KhataEntry(
                customerId = customerId,
                amount = amount,
                type = type,
                note = note
            )
            repository.addKhataEntry(e)
        }
    }

    fun deleteKhataEntry(id: Int) {
        viewModelScope.launch {
            repository.deleteKhataEntry(id)
        }
    }

    fun clearHistoryLogs() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // -------------------------------------------------------------
    // Supplier ViewModel Methods
    // -------------------------------------------------------------
    fun addSupplier(name: String, phone: String, address: String) {
        viewModelScope.launch {
            val s = Supplier(name = name, phone = phone, address = address)
            repository.addSupplier(s)
        }
    }

    fun deleteSupplier(id: Int) {
        viewModelScope.launch {
            repository.deleteSupplier(id)
        }
    }

    fun getMaterialsForSupplier(supplierId: Int): Flow<List<SupplierMaterial>> {
        return repository.getMaterialsForSupplier(supplierId)
    }

    fun addSupplierMaterial(supplierId: Int, name: String, sku: String, unit: String, unitPrice: Double, linkedItemId: Int?) {
        viewModelScope.launch {
            val m = SupplierMaterial(
                supplierId = supplierId,
                name = name,
                sku = sku,
                unit = unit,
                unitPrice = unitPrice,
                linkedInventoryItemId = linkedItemId
            )
            repository.addSupplierMaterial(m)
        }
    }

    fun deleteSupplierMaterial(id: Int) {
        viewModelScope.launch {
            repository.deleteSupplierMaterial(id)
        }
    }

    fun getReceiptsForSupplier(supplierId: Int): Flow<List<SupplierReceipt>> {
        return repository.getReceiptsForSupplier(supplierId)
    }

    fun addSupplierReceipt(supplierId: Int, receiptNumber: String, itemsJson: String, totalAmount: Double, amountPaid: Double, amountDue: Double, notes: String, restockStock: Boolean) {
        viewModelScope.launch {
            val r = SupplierReceipt(
                supplierId = supplierId,
                receiptNumber = receiptNumber,
                itemsJson = itemsJson,
                totalAmount = totalAmount,
                amountPaid = amountPaid,
                amountDue = amountDue,
                notes = notes
            )
            repository.addSupplierReceipt(r, restockStock)
        }
    }
}

class InventoryViewModelFactory(private val repository: InventoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
