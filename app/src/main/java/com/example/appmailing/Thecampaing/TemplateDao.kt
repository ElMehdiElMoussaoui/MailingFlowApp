package com.example.appmailing.Thecampaing

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: MailTemplate)

    @Update
    suspend fun update(template: MailTemplate)

    @Delete
    suspend fun delete(template: MailTemplate)

    @Query("SELECT * FROM templates_table ORDER BY name ASC")
    fun getAllTemplates(): LiveData<List<MailTemplate>>

    @Query("SELECT * FROM templates_table ORDER BY name ASC")
    suspend fun getAllTemplatesList(): List<MailTemplate>

    @Query("SELECT * FROM templates_table WHERE productId = :productId")
    fun getTemplatesByProductId(productId: Int): LiveData<List<MailTemplate>>

    @Query("SELECT * FROM templates_table WHERE productId = :productId")
    suspend fun getTemplatesByProductIdSync(productId: Int): List<MailTemplate>

    @Query("SELECT * FROM templates_table WHERE id = :id")
    suspend fun getTemplateById(id: Int): MailTemplate?
}