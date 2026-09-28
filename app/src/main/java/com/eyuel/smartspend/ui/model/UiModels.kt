package com.eyuel.smartspend.ui.model

import com.eyuel.smartspend.data.local.TransactionEntity

data class DateGroupedTransactions(
    val dateLabel: String,
    val dayEpoch: Long,
    val dailySpent: Double,
    val dailyIncome: Double,
    val transactions: List<TransactionEntity>
)

data class CategorySpendingItem(
    val category: String,
    val totalAmount: Double,
    val percentage: Float, // 0.0 to 1.0
    val transactionCount: Int
)

data class DailySpendingBar(
    val dayLabel: String, // e.g. "Mon", "Tue"
    val dateNumber: String, // e.g. "24"
    val amount: Double,
    val isToday: Boolean
)

data class SpendingInsights(
    val dailyAverage: Double,
    val largestExpense: TransactionEntity?,
    val topBankSource: String?,
    val topCategory: String?,
    val transactionCount: Int
)
