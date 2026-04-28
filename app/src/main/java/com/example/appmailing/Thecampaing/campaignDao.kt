package com.example.appmailing.Thecampaing

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CampaignDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: Campaign)

    @Query("SELECT * FROM campaigns_table ORDER BY id DESC")
    fun getAllCampaigns(): LiveData<List<Campaign>>

    @Transaction
    @Query("SELECT * FROM campaigns_table ORDER BY id DESC")
    fun getAllCampaignsWithProduct(): LiveData<List<CampaignWithProduct>>

    @Transaction
    @Query("SELECT * FROM campaigns_table ORDER BY id DESC")
    fun getAllCampaignsFullDetails(): LiveData<List<CampaignFullDetails>>

    @Query("SELECT * FROM campaigns_table WHERE id = :id")
    fun getCampaignById(id: Int): LiveData<Campaign>

    @Transaction
    @Query("SELECT * FROM campaigns_table WHERE id = :id")
    suspend fun getCampaignFullDetailsById(id: Int): CampaignFullDetails?

    @Query("UPDATE campaigns_table SET status = :newStatus WHERE id = :id")
    suspend fun updateStatus(id: Int, newStatus: String)

    @Query("DELETE FROM campaigns_table WHERE id = :id")
    suspend fun deleteById(id: Int)
}