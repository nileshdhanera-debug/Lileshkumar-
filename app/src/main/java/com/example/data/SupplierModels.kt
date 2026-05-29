package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val address: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "supplier_materials")
data class SupplierMaterial(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val supplierId: Int,
    val name: String,
    val sku: String = "", // e.g., specifications or color
    val unit: String, // e.g., Sheets, Pieces, kgs
    val unitPrice: Double, // supply price
    val linkedInventoryItemId: Int? = null // optional link to stock item
)

@Entity(tableName = "supplier_receipts")
data class SupplierReceipt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val supplierId: Int,
    val receiptNumber: String, // Invoice or Receipt No.
    val itemsJson: String, // serialized List<ReceiptItem>
    val totalAmount: Double,
    val amountPaid: Double,
    val amountDue: Double,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class ReceiptItem(
    val materialId: Int,
    val materialName: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double
)
