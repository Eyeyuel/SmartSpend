package com.eyuel.smartspend.data.local

import androidx.room.TypeConverter
import com.eyuel.smartspend.domain.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String {
        return type.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return try {
            TransactionType.valueOf(value)
        } catch (e: Exception) {
            TransactionType.DEBIT
        }
    }
}
