package com.eyuel.smartspend.ui

import com.eyuel.smartspend.data.local.TransactionEntity
import com.eyuel.smartspend.domain.model.ExpenseCategory
import com.eyuel.smartspend.domain.model.TransactionType
import com.eyuel.smartspend.ui.model.CashflowBucket
import com.eyuel.smartspend.ui.model.CashflowCategoryItem
import com.eyuel.smartspend.ui.model.CashflowGranularity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CashflowEngine {

    fun buildBuckets(
        allTx: List<TransactionEntity>,
        granularity: CashflowGranularity,
        now: Long = System.currentTimeMillis()
    ): List<CashflowBucket> {
        val eligibleTx = allTx.filter { it.category != ExpenseCategory.INTERNAL_TRANSFER }
        val earliestTxTime = eligibleTx.minOfOrNull { it.timestamp }

        return when (granularity) {
            CashflowGranularity.DAILY -> buildDailyBuckets(eligibleTx, now, earliestTxTime)
            CashflowGranularity.WEEKLY -> buildWeeklyBuckets(eligibleTx, now, earliestTxTime)
            CashflowGranularity.MONTHLY -> buildMonthlyBuckets(eligibleTx, now, earliestTxTime)
            CashflowGranularity.YEARLY -> buildYearlyBuckets(eligibleTx, now, earliestTxTime)
        }
    }

    private fun buildDailyBuckets(
        eligibleTx: List<TransactionEntity>,
        now: Long,
        earliestTxTime: Long?
    ): List<CashflowBucket> {
        val todayStart = DateGroupingUtils.getDayStartMillis(now)
        val defaultEarliest = todayStart - (29L * 24 * 60 * 60 * 1000L) // Default 30 days
        val actualEarliest = if (earliestTxTime != null) {
            val txDayStart = DateGroupingUtils.getDayStartMillis(earliestTxTime)
            minOf(txDayStart, defaultEarliest)
        } else {
            defaultEarliest
        }

        // Group transactions by day
        val txByDay = eligibleTx.groupBy { DateGroupingUtils.getDayStartMillis(it.timestamp) }

        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayOfMonthFormat = SimpleDateFormat("d", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())

        val buckets = mutableListOf<CashflowBucket>()
        val cal = Calendar.getInstance().apply { timeInMillis = actualEarliest }

        while (cal.timeInMillis <= todayStart) {
            val dayStart = cal.timeInMillis
            val dayEnd = dayStart + (24L * 60 * 60 * 1000L) - 1L
            val date = Date(dayStart)
            val isToday = dayStart == todayStart
            val txList = txByDay[dayStart] ?: emptyList()

            var income = 0.0
            var expense = 0.0
            val catMap = mutableMapOf<String, Double>()
            val countMap = mutableMapOf<String, Int>()

            for (tx in txList) {
                if (tx.type == TransactionType.CREDIT) {
                    income += tx.amount
                } else {
                    expense += tx.amount
                    catMap[tx.category] = (catMap[tx.category] ?: 0.0) + tx.amount
                    countMap[tx.category] = (countMap[tx.category] ?: 0) + 1
                }
            }

            val categoryBreakdown = catMap.map { (cat, amount) ->
                CashflowCategoryItem(
                    category = cat,
                    amount = amount,
                    percentage = if (expense > 0) (amount / expense).toFloat() else 0f,
                    count = countMap[cat] ?: 0
                )
            }.sortedByDescending { it.amount }

            val net = income - expense
            val savingsRate = if (income > 0) ((income - expense).coerceAtLeast(0.0) / income * 100).toFloat() else 0f

            val dayLabel = if (isToday) "Today" else dayOfWeekFormat.format(date)

            buckets.add(
                CashflowBucket(
                    id = "daily_$dayStart",
                    primaryLabel = dayLabel,
                    secondaryLabel = dayOfMonthFormat.format(date),
                    fullPeriodTitle = if (isToday) "Today • ${fullDateFormat.format(date)}" else fullDateFormat.format(date),
                    income = income,
                    expense = expense,
                    net = net,
                    savingsRate = savingsRate,
                    timestampStart = dayStart,
                    timestampEnd = dayEnd,
                    isCurrentPeriod = isToday,
                    transactions = txList.sortedByDescending { it.timestamp },
                    categoryBreakdown = categoryBreakdown
                )
            )

            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return buckets
    }

    private fun buildWeeklyBuckets(
        eligibleTx: List<TransactionEntity>,
        now: Long,
        earliestTxTime: Long?
    ): List<CashflowBucket> {
        val calNow = Calendar.getInstance().apply {
            timeInMillis = now
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentWeekMonday = calNow.timeInMillis

        val defaultEarliestWeek = currentWeekMonday - (11L * 7 * 24 * 60 * 60 * 1000L) // Default 12 weeks
        val actualEarliestWeek = if (earliestTxTime != null) {
            val calEarly = Calendar.getInstance().apply {
                timeInMillis = earliestTxTime
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            minOf(calEarly.timeInMillis, defaultEarliestWeek)
        } else {
            defaultEarliestWeek
        }

        val rangeFormat = SimpleDateFormat("d MMM", Locale.getDefault())
        val monthYearFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        val buckets = mutableListOf<CashflowBucket>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = actualEarliestWeek
            firstDayOfWeek = Calendar.MONDAY
        }

        while (cal.timeInMillis <= currentWeekMonday) {
            val mondayMillis = cal.timeInMillis
            val weekEndMillis = mondayMillis + (7L * 24 * 60 * 60 * 1000L) - 1L
            val isCurrentWeek = mondayMillis == currentWeekMonday

            val weekNumber = cal.get(Calendar.WEEK_OF_YEAR)
            val mondayDate = Date(mondayMillis)
            val sundayDate = Date(weekEndMillis)

            val txList = eligibleTx.filter { it.timestamp in mondayMillis..weekEndMillis }

            var income = 0.0
            var expense = 0.0
            val catMap = mutableMapOf<String, Double>()
            val countMap = mutableMapOf<String, Int>()

            for (tx in txList) {
                if (tx.type == TransactionType.CREDIT) {
                    income += tx.amount
                } else {
                    expense += tx.amount
                    catMap[tx.category] = (catMap[tx.category] ?: 0.0) + tx.amount
                    countMap[tx.category] = (countMap[tx.category] ?: 0) + 1
                }
            }

            val categoryBreakdown = catMap.map { (cat, amount) ->
                CashflowCategoryItem(
                    category = cat,
                    amount = amount,
                    percentage = if (expense > 0) (amount / expense).toFloat() else 0f,
                    count = countMap[cat] ?: 0
                )
            }.sortedByDescending { it.amount }

            val net = income - expense
            val savingsRate = if (income > 0) ((income - expense).coerceAtLeast(0.0) / income * 100).toFloat() else 0f

            val primary = if (isCurrentWeek) "This Wk" else "W$weekNumber"
            val secondary = "${rangeFormat.format(mondayDate)} - ${rangeFormat.format(sundayDate)}"
            val fullTitle = "Week $weekNumber (${rangeFormat.format(mondayDate)} - ${rangeFormat.format(sundayDate)} ${cal.get(Calendar.YEAR)})"

            buckets.add(
                CashflowBucket(
                    id = "week_$mondayMillis",
                    primaryLabel = primary,
                    secondaryLabel = rangeFormat.format(mondayDate),
                    fullPeriodTitle = fullTitle,
                    income = income,
                    expense = expense,
                    net = net,
                    savingsRate = savingsRate,
                    timestampStart = mondayMillis,
                    timestampEnd = weekEndMillis,
                    isCurrentPeriod = isCurrentWeek,
                    transactions = txList.sortedByDescending { it.timestamp },
                    categoryBreakdown = categoryBreakdown
                )
            )

            cal.add(Calendar.WEEK_OF_YEAR, 1)
        }

        return buckets
    }

    private fun buildMonthlyBuckets(
        eligibleTx: List<TransactionEntity>,
        now: Long,
        earliestTxTime: Long?
    ): List<CashflowBucket> {
        val calNow = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentMonthStart = calNow.timeInMillis

        val defaultEarliestMonth = Calendar.getInstance().apply {
            timeInMillis = currentMonthStart
            add(Calendar.MONTH, -11) // Default 12 months
        }.timeInMillis

        val actualEarliestMonth = if (earliestTxTime != null) {
            val calEarly = Calendar.getInstance().apply {
                timeInMillis = earliestTxTime
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            minOf(calEarly.timeInMillis, defaultEarliestMonth)
        } else {
            defaultEarliestMonth
        }

        val monthShortFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val yearShortFormat = SimpleDateFormat("yy", Locale.getDefault())

        val buckets = mutableListOf<CashflowBucket>()
        val cal = Calendar.getInstance().apply { timeInMillis = actualEarliestMonth }

        while (cal.timeInMillis <= currentMonthStart) {
            val monthStart = cal.timeInMillis
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val calEnd = (cal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, maxDay)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val monthEnd = calEnd.timeInMillis
            val isCurrentMonth = monthStart == currentMonthStart

            val txList = eligibleTx.filter { it.timestamp in monthStart..monthEnd }

            var income = 0.0
            var expense = 0.0
            val catMap = mutableMapOf<String, Double>()
            val countMap = mutableMapOf<String, Int>()

            for (tx in txList) {
                if (tx.type == TransactionType.CREDIT) {
                    income += tx.amount
                } else {
                    expense += tx.amount
                    catMap[tx.category] = (catMap[tx.category] ?: 0.0) + tx.amount
                    countMap[tx.category] = (countMap[tx.category] ?: 0) + 1
                }
            }

            val categoryBreakdown = catMap.map { (cat, amount) ->
                CashflowCategoryItem(
                    category = cat,
                    amount = amount,
                    percentage = if (expense > 0) (amount / expense).toFloat() else 0f,
                    count = countMap[cat] ?: 0
                )
            }.sortedByDescending { it.amount }

            val net = income - expense
            val savingsRate = if (income > 0) ((income - expense).coerceAtLeast(0.0) / income * 100).toFloat() else 0f

            val date = Date(monthStart)
            buckets.add(
                CashflowBucket(
                    id = "month_$monthStart",
                    primaryLabel = monthShortFormat.format(date),
                    secondaryLabel = "'${yearShortFormat.format(date)}",
                    fullPeriodTitle = monthYearFormat.format(date),
                    income = income,
                    expense = expense,
                    net = net,
                    savingsRate = savingsRate,
                    timestampStart = monthStart,
                    timestampEnd = monthEnd,
                    isCurrentPeriod = isCurrentMonth,
                    transactions = txList.sortedByDescending { it.timestamp },
                    categoryBreakdown = categoryBreakdown
                )
            )

            cal.add(Calendar.MONTH, 1)
        }

        return buckets
    }

    private fun buildYearlyBuckets(
        eligibleTx: List<TransactionEntity>,
        now: Long,
        earliestTxTime: Long?
    ): List<CashflowBucket> {
        val calNow = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentYearStart = calNow.timeInMillis

        val defaultEarliestYear = Calendar.getInstance().apply {
            timeInMillis = currentYearStart
            add(Calendar.YEAR, -3) // Default 4 years
        }.timeInMillis

        val actualEarliestYear = if (earliestTxTime != null) {
            val calEarly = Calendar.getInstance().apply {
                timeInMillis = earliestTxTime
                set(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            minOf(calEarly.timeInMillis, defaultEarliestYear)
        } else {
            defaultEarliestYear
        }

        val buckets = mutableListOf<CashflowBucket>()
        val cal = Calendar.getInstance().apply { timeInMillis = actualEarliestYear }

        while (cal.timeInMillis <= currentYearStart) {
            val yearStart = cal.timeInMillis
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
            val calEnd = (cal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_YEAR, maxDay)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val yearEnd = calEnd.timeInMillis
            val isCurrentYear = yearStart == currentYearStart

            val yearInt = cal.get(Calendar.YEAR)
            val txList = eligibleTx.filter { it.timestamp in yearStart..yearEnd }

            var income = 0.0
            var expense = 0.0
            val catMap = mutableMapOf<String, Double>()
            val countMap = mutableMapOf<String, Int>()

            for (tx in txList) {
                if (tx.type == TransactionType.CREDIT) {
                    income += tx.amount
                } else {
                    expense += tx.amount
                    catMap[tx.category] = (catMap[tx.category] ?: 0.0) + tx.amount
                    countMap[tx.category] = (countMap[tx.category] ?: 0) + 1
                }
            }

            val categoryBreakdown = catMap.map { (cat, amount) ->
                CashflowCategoryItem(
                    category = cat,
                    amount = amount,
                    percentage = if (expense > 0) (amount / expense).toFloat() else 0f,
                    count = countMap[cat] ?: 0
                )
            }.sortedByDescending { it.amount }

            val net = income - expense
            val savingsRate = if (income > 0) ((income - expense).coerceAtLeast(0.0) / income * 100).toFloat() else 0f

            buckets.add(
                CashflowBucket(
                    id = "year_$yearStart",
                    primaryLabel = "$yearInt",
                    secondaryLabel = "",
                    fullPeriodTitle = "Year $yearInt",
                    income = income,
                    expense = expense,
                    net = net,
                    savingsRate = savingsRate,
                    timestampStart = yearStart,
                    timestampEnd = yearEnd,
                    isCurrentPeriod = isCurrentYear,
                    transactions = txList.sortedByDescending { it.timestamp },
                    categoryBreakdown = categoryBreakdown
                )
            )

            cal.add(Calendar.YEAR, 1)
        }

        return buckets
    }
}
