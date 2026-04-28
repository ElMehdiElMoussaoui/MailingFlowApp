package com.example.appmailing.TheContact

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "contacts_table",
    indices = [Index(value = ["email"], unique = true)]
)
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val email: String,
    val role: String = "",
    val phone: String = "",
    val emailStatus: EmailStatus = EmailStatus.CONFORME,
    val sendingStatus: SendingStatus = SendingStatus.PENDING
) : Serializable

enum class EmailStatus : Serializable { CONFORME, NON_CONFORME }
enum class SendingStatus : Serializable { SENT, FAILED, PENDING }
