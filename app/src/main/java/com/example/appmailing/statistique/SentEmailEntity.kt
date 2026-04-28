package com.example.appmailing.statistique

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sent_emails_table")
data class SentEmailEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val campaignId: Int,
    val recipientEmail: String,
    val subject: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "SUCCESS" or "FAILED"
    val opened: Boolean = false,
    val clicked: Boolean = false
)