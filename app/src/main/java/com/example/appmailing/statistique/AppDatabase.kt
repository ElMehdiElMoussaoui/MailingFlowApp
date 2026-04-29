package com.example.appmailing.statistique

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.appmailing.Thecampaing.Campaign
import com.example.appmailing.TheContact.Contact
import com.example.appmailing.TheContact.ContactDao
import com.example.appmailing.TheProducts.Product
import com.example.appmailing.TheProducts.ProductDao
import com.example.appmailing.Thecampaing.CampaignDao
import com.example.appmailing.Thecampaing.CampaignContact

@Database(
    entities = [
        Contact::class, 
        Product::class, 
        Campaign::class, 
        SentEmailEntity::class,
        CampaignContact::class
    ],
    version = 10
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sentEmailDao(): SentEmailDao
    abstract fun contactDao(): ContactDao
    abstract fun productDao(): ProductDao
    abstract fun campaignDao(): CampaignDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mailflow_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
