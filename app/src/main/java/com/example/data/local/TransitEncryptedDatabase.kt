package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        PoiEntity::class,
        StreetEntity::class,
        StreetJunctionEntity::class,
        CachedVipProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(TransitConverters::class)
abstract class TransitEncryptedDatabase : RoomDatabase() {

    abstract fun poiDao(): PoiDao
    abstract fun streetDao(): StreetDao
    abstract fun streetJunctionDao(): StreetJunctionDao
    abstract fun vipCacheDao(): VipCacheDao

    companion object {
        @Volatile
        private var INSTANCE: TransitEncryptedDatabase? = null

        // Secure passphrase for SQLCipher AES-256 local database encryption
        private const val DB_PASSPHRASE = "TransitApp_AES256_Encrypted_Key_2026!#"
        private const val DB_NAME = "transit_encrypted.db"

        fun getDatabase(context: Context): TransitEncryptedDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportOpenHelperFactory(DB_PASSPHRASE.toByteArray(Charsets.UTF_8))

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TransitEncryptedDatabase::class.java,
                    DB_NAME
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
