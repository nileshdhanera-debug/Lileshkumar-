package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "SHEET" or "HARDWARE"
    val specification: String, // e.g. "12mm White", "3-inch SS", "1.5-inch Self-Tapping"
    val quantity: Double,
    val unit: String, // e.g. "Sheets", "Pieces", "Boxes", "Packets"
    val minThreshold: Double = 5.0
)
