package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "furniture_recipes")
data class FurnitureRecipe(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    // JSON representation of required ingredients
    val ingredientsJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class RecipeIngredient(
    val itemId: Int,          // ID of the target InventoryItem
    val name: String,          // Cached item name
    val specification: String, // Cached item spec
    val neededQty: Double
)
