package com.eyuel.smartspend.ui.model

import com.eyuel.smartspend.data.local.TransactionEntity

enum class CashflowGranularity(val title: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

data class CashflowCategoryItem(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val count: Int
)

data class CashflowBucket(
    val id: String,
    val primaryLabel: String,
    val secondaryLabel: String,
    val fullPeriodTitle: String,
    val income: Double,
    val expense: Double,
    val net: Double,
    val savingsRate: Float,
    val timestampStart: Long,
    val timestampEnd: Long,
    val isCurrentPeriod: Boolean,
    val transactions: List<TransactionEntity>,
    val categoryBreakdown: List<CashflowCategoryItem>
)
