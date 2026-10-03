package com.eyuel.smartspend.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SmartSpendDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: SmartSpendDatabase? = null

        fun getDatabase(context: Context): SmartSpendDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartSpendDatabase::class.java,
                    "smartspend_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // 1. Purge duplicate rows created by repeated scanning
                            db.execSQL("""
                                DELETE FROM transactions WHERE id NOT IN (
                                    SELECT MIN(id) FROM transactions 
                                    GROUP BY bankName, senderAddress, amount, type, timestamp, rawBody
                                )
                            """.trimIndent())

                            // 2. Ensure every legacy transaction has a non-null referenceId
                            db.execSQL("""
                                UPDATE transactions 
                                SET referenceId = 'legacy_' || id || '_' || timestamp 
                                WHERE referenceId IS NULL
                            """.trimIndent())
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
