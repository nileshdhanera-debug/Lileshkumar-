package com.example.data

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    
    private val ingredientListType = Types.newParameterizedType(List::class.java, RecipeIngredient::class.java)
    private val adapter = moshi.adapter<List<RecipeIngredient>>(ingredientListType)

    private val receiptItemListType = Types.newParameterizedType(List::class.java, ReceiptItem::class.java)
    private val receiptAdapter = moshi.adapter<List<ReceiptItem>>(receiptItemListType)

    @TypeConverter
    fun stringToIngredients(value: String?): List<RecipeIngredient>? {
        if (value == null) return emptyList()
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun ingredientsToString(list: List<RecipeIngredient>?): String {
        if (list == null) return "[]"
        return adapter.toJson(list)
    }

    @TypeConverter
    fun stringToReceiptItems(value: String?): List<ReceiptItem>? {
        if (value == null) return emptyList()
        return try {
            receiptAdapter.fromJson(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun receiptItemsToString(list: List<ReceiptItem>?): String {
        if (list == null) return "[]"
        return receiptAdapter.toJson(list)
    }
}
