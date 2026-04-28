package com.example.appmailing.Thecampaing

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.appmailing.TheProducts.Product
import java.io.Serializable

@Entity(
    tableName = "campaigns_table",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"])
    ]
)
data class Campaign(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val status: String,          // "SENT" | "PENDING" | "FAILED" | "SCHEDULED"
    val emailSubject: String,
    val timestamp: String,       // millis as string
    val imageUri: String = "",
    val recipientCount: Int = 0,
    val productId: Int = 0       // Foreign key to products_table
) : Serializable