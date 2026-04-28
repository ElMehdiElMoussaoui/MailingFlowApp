package com.example.appmailing.Thecampaing

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.appmailing.TheProducts.Product
import java.io.Serializable

@Entity(
    tableName = "templates_table",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["productId"])]
)
data class MailTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val subject: String,
    val body: String,        // HTML or Text body
    val productId: Int,      // Link to products_table
    val imageUri: String = ""
) : Serializable