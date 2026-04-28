package com.example.appmailing.statistique

import androidx.room.TypeConverter
import com.example.appmailing.TheContact.EmailStatus
import com.example.appmailing.TheContact.SendingStatus

class Converters {
    @TypeConverter
    fun fromEmailStatus(value: EmailStatus): String = value.name

    @TypeConverter
    fun toEmailStatus(value: String): EmailStatus = EmailStatus.valueOf(value)

    @TypeConverter
    fun fromSendingStatus(value: SendingStatus): String = value.name

    @TypeConverter
    fun toSendingStatus(value: String): SendingStatus {
        return try {
            SendingStatus.valueOf(value)
        } catch (e: Exception) {
            SendingStatus.PENDING
        }
    }
}
