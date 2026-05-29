package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_logs")
data class TransactionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "ADD_STOCK", "REMOVE_STOCK", "BUILD_FURNITURE", "EDIT_STOCK"
    val title: String, 
    val description: String 
)
