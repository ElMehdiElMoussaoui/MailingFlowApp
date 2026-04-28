package com.example.appmailing.statistique

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.lifecycle.LiveData

@Dao
interface SentEmailDao {
    @Insert
    suspend fun insertSentEmail(email: SentEmailEntity)

    @Query("SELECT COUNT(*) FROM sent_emails_table WHERE status = 'SUCCESS'")
    fun getTotalSentCount(): LiveData<Int>

    @Query("SELECT * FROM sent_emails_table ORDER BY timestamp DESC")
    fun getAllSentEmails(): LiveData<List<SentEmailEntity>>

    @Query("SELECT recipientEmail FROM sent_emails_table WHERE campaignId = :campaignId AND status = 'SUCCESS'")
    suspend fun getEmailsSentForCampaign(campaignId: Int): List<String>
}