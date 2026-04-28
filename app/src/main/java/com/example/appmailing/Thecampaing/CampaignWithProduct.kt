package com.example.appmailing.Thecampaing

import androidx.room.Embedded
import androidx.room.Relation
import com.example.appmailing.TheProducts.Product

data class CampaignWithProduct(
    @Embedded val campaign: Campaign,
    @Relation(
        parentColumn = "productId",
        entityColumn = "id"
    )
    val product: Product?
)