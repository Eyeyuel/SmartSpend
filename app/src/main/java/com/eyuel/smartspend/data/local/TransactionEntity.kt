package com.eyuel.smartspend.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.eyuel.smartspend.domain.model.ExpenseCategory
import com.eyuel.smartspend.domain.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["referenceId"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["isReviewed"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val referenceId: String?,
    val bankName: String,
    val senderAddress: String,
    val amount: Double,
    val currency: String = "ETB",
    val type: TransactionType,
    val timestamp: Long,
    val balanceAfter: Double? = null,
    val accountNumber: String? = null,
    val rawBody: String,
    val description: String = "",
    val category: String = ExpenseCategory.UNCATEGORIZED,
    val isReviewed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
