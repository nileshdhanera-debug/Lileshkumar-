package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "site_measurements")
data class SiteMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String, // Name/Label for the item (e.g. "Hall Cupboard")
    val height: Double,
    val width: Double,
    val isFeet: Boolean, // false = Inches, true = Feet
    val isDouble: Boolean, // true = double the calculated Area (as requested by user)
    val sqFt: Double, // Calculated net square feet
    val timestamp: Long = System.currentTimeMillis()
)
