package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerKhata(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "khata_entries")
data class KhataEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val amount: Double,
    val type: String, // "CREDIT" (received money / "જમા") or "DEBIT" (due / work done / "ઉધાર")
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)
