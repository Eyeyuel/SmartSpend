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

enum class ChartGranularity(val displayName: String, val avgLabel: String, val avgUnit: String) {
    DAILY_7("7 Days", "Daily Avg", "/day"),
    WEEKLY("Weekly", "Weekly Avg", "/wk"),
    MONTHLY("Monthly", "Monthly Avg", "/mo"),
    YEARLY("Yearly", "Yearly Avg", "/yr")
}

data class SpendingChartBar(
    val id: String,
    val primaryLabel: String,
    val secondaryLabel: String,
    val fullTitle: String,
    val amount: Double,
    val isCurrent: Boolean,
    val timestamp: Long = 0L
)

data class SpendingChartData(
    val granularity: ChartGranularity = ChartGranularity.DAILY_7,
    val bars: List<SpendingChartBar> = emptyList(),
    val totalAmount: Double = 0.0,
    val averageAmount: Double = 0.0,
    val maxAmount: Double = 1.0,
    val pages: Int = 1
)
