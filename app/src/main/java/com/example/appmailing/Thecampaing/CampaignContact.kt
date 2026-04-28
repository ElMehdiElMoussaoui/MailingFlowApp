package com.example.appmailing.Thecampaing

import androidx.room.Entity
import androidx.room.ForeignKey
import com.example.appmailing.TheContact.Contact

@Entity(
    tableName = "campaign_contacts_table",
    primaryKeys = ["campaignId", "contactId"],
    foreignKeys = [
        ForeignKey(
            entity = Campaign::class,
            parentColumns = ["id"],
            childColumns = ["campaignId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CampaignContact(
    val campaignId: Int,
    val contactId: Int
)