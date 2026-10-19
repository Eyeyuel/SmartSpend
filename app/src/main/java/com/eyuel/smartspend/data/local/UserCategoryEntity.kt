package com.eyuel.smartspend.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eyuel.smartspend.domain.model.TransactionType

@Entity(tableName = "user_categories")
data class UserCategoryEntity(
    @PrimaryKey
    val name: String,
    val type: TransactionType, // DEBIT (Expense) or CREDIT (Income)
    val iconKey: String = "Category",
    val colorHex: Long = 0xFF38BDF8,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
