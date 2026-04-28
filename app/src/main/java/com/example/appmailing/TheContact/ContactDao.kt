package com.example.appmailing.TheContact

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContact(contact: Contact): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContacts(contacts: List<Contact>)

    @Update
    suspend fun updateContact(contact: Contact)

    @Query("SELECT * FROM contacts_table ORDER BY fullName ASC")
    fun getAllContacts(): LiveData<List<Contact>>

    @Query("SELECT * FROM contacts_table ORDER BY fullName ASC")
    suspend fun getAllContactsList(): List<Contact>

    @Query("SELECT * FROM contacts_table WHERE id = :id")
    fun getContactById(id: Int): LiveData<Contact>

    @Query("SELECT * FROM contacts_table WHERE email = :email LIMIT 1")
    suspend fun getContactByEmail(email: String): Contact?

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("DELETE FROM contacts_table")
    suspend fun deleteAllContacts()

    @Query("SELECT * FROM contacts_table WHERE fullName LIKE :searchQuery OR email LIKE :searchQuery")
    fun searchContacts(searchQuery: String): LiveData<List<Contact>>

    @Query("SELECT * FROM contacts_table WHERE sendingStatus = 'PENDING'")
    suspend fun getPendingContacts(): List<Contact>

    @Query("UPDATE contacts_table SET sendingStatus = :newStatus WHERE id = :contactId")
    suspend fun updateStatus(contactId: Int, newStatus: String)

    @Query("SELECT COUNT(*) FROM contacts_table")
    suspend fun getContactsCount(): Int

    @Query("UPDATE contacts_table SET sendingStatus = 'PENDING'")
    suspend fun resetAllSendingStatus()
}
