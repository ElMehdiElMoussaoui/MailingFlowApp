package com.example.appmailing.TheProducts

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "products_table")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val price: Double,
    val currency: String = "MAD",
    val description: String,
    val imageUri: String? = null
) : Serializable

object ProductCategory {
    const val ALL         = "All"
    const val ELECTRONICS = "Electronics"
    const val WEARABLES   = "Wearables"
    const val FITNESS     = "Fitness"
    const val AUDIO       = "Audio"
    const val OFFICE      = "Office"

    val all = listOf(ALL, ELECTRONICS, WEARABLES, FITNESS, AUDIO, OFFICE)
}

object CurrencyType {
    val all = listOf("MAD", "USD", "EUR", "GBP")
}