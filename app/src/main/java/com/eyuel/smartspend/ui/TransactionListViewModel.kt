package com.eyuel.smartspend.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyuel.smartspend.SmartSpendApp
import com.eyuel.smartspend.data.local.TransactionEntity
import com.eyuel.smartspend.data.repository.CsvExportHelper
import com.eyuel.smartspend.data.repository.SmsSyncManager
import com.eyuel.smartspend.domain.model.ExpenseCategory
import com.eyuel.smartspend.domain.model.TimeframePeriod
import com.eyuel.smartspend.domain.model.TransactionType
import com.eyuel.smartspend.ui.model.CategorySpendingItem
import com.eyuel.smartspend.ui.model.ChartGranularity
import com.eyuel.smartspend.ui.model.DailySpendingBar
import com.eyuel.smartspend.ui.model.DateGroupedTransactions
import com.eyuel.smartspend.ui.model.SpendingChartBar
import com.eyuel.smartspend.ui.model.SpendingChartData
import com.eyuel.smartspend.ui.model.SpendingInsights
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class TransactionFilter {
    ALL,
    UNREVIEWED, // "Needs Note"
    EXPENSES,   // Debits
    INCOME      // Credits
}

data class DashboardUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val rawAllTransactions: List<TransactionEntity> = emptyList(),
    val groupedTransactions: List<DateGroupedTransactions> = emptyList(),
    val analyticsGroupedTransactions: List<DateGroupedTransactions> = emptyList(),
    val categoryBreakdown: List<CategorySpendingItem> = emptyList(),
    val weeklyChartBars: List<DailySpendingBar> = emptyList(),
    val chartData: SpendingChartData = SpendingChartData(),
    val insights: SpendingInsights? = null,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalBalance: Double = 0.0,
    val ledgerTotalBalance: Double = 0.0,
    val unreviewedCount: Int = 0,
    val activeTimeframe: TimeframePeriod = TimeframePeriod.TODAY,
    val customDateRange: Pair<Long, Long>? = null,
    val activeFilter: TransactionFilter = TransactionFilter.ALL,
    val selectedCategoryFilter: String? = null,
    val searchQuery: String = "",
    val editingTransaction: TransactionEntity? = null,
    val selectedTab: Int = 0, // 0 = Ledger, 1 = Analytics
    val isSyncing: Boolean = false,
    val syncMessage: String? = null
)

private data class UiControls(
    val timeframe: TimeframePeriod,
    val customRange: Pair<Long, Long>?,
    val filter: TransactionFilter,
    val categoryFilter: String?,
    val query: String,
    val editing: TransactionEntity?,
    val tab: Int,
    val syncing: Boolean,
    val syncMsg: String?,
    val chartGranularity: ChartGranularity,
    val chartPages: Int
)

class TransactionListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmartSpendApp.instance.repository
    private val syncManager = SmsSyncManager(application, repository)

    private val _activeTimeframe = MutableStateFlow(TimeframePeriod.ALL_TIME)
    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    private val _activeFilter = MutableStateFlow(TransactionFilter.ALL)
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _editingTransaction = MutableStateFlow<TransactionEntity?>(null)
    private val _selectedTab = MutableStateFlow(0)
    private val _isSyncing = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)
    private val _chartGranularity = MutableStateFlow(ChartGranularity.DAILY_7)
    private val _chartPages = MutableStateFlow(1)

    private val _isSplashDismissed = MutableStateFlow(false)
    val isSplashDismissed: StateFlow<Boolean> = _isSplashDismissed.asStateFlow()

    fun dismissSplash() {
        _isSplashDismissed.value = true
    }

    private val _uiControls = combine(
        combine(_activeTimeframe, _customDateRange, _activeFilter, _selectedCategoryFilter) { t, r, f, c ->
            data class TimeAndFilter(val t: TimeframePeriod, val r: Pair<Long, Long>?, val f: TransactionFilter, val c: String?)
            TimeAndFilter(t, r, f, c)
        },
        combine(_searchQuery, _editingTransaction, _selectedTab) { q, e, tab -> Triple(q, e, tab) },
        _isSyncing,
        _syncMessage,
        combine(_chartGranularity, _chartPages) { g, p -> Pair(g, p) }
    ) { tf, (query, editing, tab), syncing, syncMsg, (granularity, pages) ->
        UiControls(tf.t, tf.r, tf.f, tf.c, query, editing, tab, syncing, syncMsg, granularity, pages)
    }

    init {
        viewModelScope.launch {
            repository.rescanAndHealTransactions()
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAllTransactions(),
        _uiControls
    ) { allTx, controls ->
        val now = System.currentTimeMillis()
        val (timeframeStart, timeframeEnd) = if (controls.timeframe == TimeframePeriod.CUSTOM && controls.customRange != null) {
            val start = DateGroupingUtils.getDayStartMillis(controls.customRange.first)
            val end = DateGroupingUtils.getDayStartMillis(controls.customRange.second) + (24 * 60 * 60 * 1000L) - 1L
            start to end
        } else {
            controls.timeframe.getStartMillis(now) to Long.MAX_VALUE
        }
        val query = controls.query.trim()
        val hasQuery = query.isNotEmpty()

        var totalIncome = 0.0
        var totalExpense = 0.0
        var ledgerTotalIncome = 0.0
        var ledgerTotalExpense = 0.0
        var unreviewedCount = 0

        val categorySpendingMap = mutableMapOf<String, Double>()
        val categoryCountMap = mutableMapOf<String, Int>()
        val bankCountMap = mutableMapOf<String, Int>()
        var largestDebit: TransactionEntity? = null

        // 7-day spending bucket initialization
        val sevenDaysAgoStart = DateGroupingUtils.getDayStartMillis(now - (6 * 24 * 60 * 60 * 1000L))
        val dailySpendingBuckets = DoubleArray(7) // 7 days: day 0 is 6 days ago, day 6 is today
        val dayStartMillisArray = LongArray(7) { idx ->
            sevenDaysAgoStart + (idx * 24 * 60 * 60 * 1000L)
        }

        val filteredTransactions = ArrayList<TransactionEntity>(allTx.size)
        val dateGroupMap = linkedMapOf<Long, MutableList<TransactionEntity>>()
        val analyticsDateGroupMap = linkedMapOf<Long, MutableList<TransactionEntity>>()
        var analyticsTransactionsCount = 0

        for (item in allTx) {
            // Count unreviewed across all time
            if (!item.isReviewed) {
                unreviewedCount++
            }

            // 7-day spending aggregation (exclude internal self-transfers)
            val isInternalTransfer = item.category == ExpenseCategory.INTERNAL_TRANSFER
            if (item.type == TransactionType.DEBIT && !isInternalTransfer && item.timestamp >= sevenDaysAgoStart) {
                val dayStart = DateGroupingUtils.getDayStartMillis(item.timestamp)
                val dayIndex = ((dayStart - sevenDaysAgoStart) / (24 * 60 * 60 * 1000L)).toInt()
                if (dayIndex in 0..6) {
                    dailySpendingBuckets[dayIndex] += item.amount
                }
            }

            // --- ALL TIME LEDGER CALCULATIONS ---
            if (!isInternalTransfer) {
                if (item.type == TransactionType.CREDIT) ledgerTotalIncome += item.amount
                else ledgerTotalExpense += item.amount
            }

            // Check Tab filter (All / Needs Note / Expense / Income)
            val matchesFilter = when (controls.filter) {
                TransactionFilter.ALL -> true
                TransactionFilter.UNREVIEWED -> !item.isReviewed
                TransactionFilter.EXPENSES -> item.type == TransactionType.DEBIT && !isInternalTransfer
                TransactionFilter.INCOME -> item.type == TransactionType.CREDIT && !isInternalTransfer
            }

            // Check Search Query
            val matchesSearch = !hasQuery ||
                    item.bankName.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true) ||
                    (item.referenceId?.contains(query, ignoreCase = true) == true)

            // Ledger Transaction Stream ONLY cares about filter and search (NOT timeframe, NOT category filter)
            if (matchesFilter && matchesSearch) {
                filteredTransactions.add(item)
                val dayStart = DateGroupingUtils.getDayStartMillis(item.timestamp)
                val listForDay = dateGroupMap.getOrPut(dayStart) { mutableListOf() }
                listForDay.add(item)
            }

            // --- INSIGHTS TIMEFRAME CALCULATIONS ---
            val isInTimeframe = item.timestamp in timeframeStart..timeframeEnd
            if (!isInTimeframe) continue

            if (!isInternalTransfer) {
                if (item.type == TransactionType.CREDIT) {
                    totalIncome += item.amount
                } else {
                    totalExpense += item.amount
                    // Category breakdown (Insights only)
                    categorySpendingMap[item.category] = (categorySpendingMap[item.category] ?: 0.0) + item.amount
                    categoryCountMap[item.category] = (categoryCountMap[item.category] ?: 0) + 1

                    if (largestDebit == null || item.amount > largestDebit.amount) {
                        largestDebit = item
                    }
                }
            }
            // Bank breakdown (Insights only)
            bankCountMap[item.bankName] = (bankCountMap[item.bankName] ?: 0) + 1
            
            // Analytics transaction stream
            val dayStartAnalytics = DateGroupingUtils.getDayStartMillis(item.timestamp)
            val listForDayAnalytics = analyticsDateGroupMap.getOrPut(dayStartAnalytics) { mutableListOf() }
            listForDayAnalytics.add(item)
            analyticsTransactionsCount++
        }

        // Build Date-Grouped stream
        val groupedList = dateGroupMap.map { (dayStart, dayItems) ->
            var daySpent = 0.0
            var dayIncome = 0.0
            for (t in dayItems) {
                if (t.category == ExpenseCategory.INTERNAL_TRANSFER) continue
                if (t.type == TransactionType.DEBIT) daySpent += t.amount
                else dayIncome += t.amount
            }
            DateGroupedTransactions(
                dateLabel = DateGroupingUtils.getDateLabel(dayStart, now),
                dayEpoch = dayStart,
                dailySpent = daySpent,
                dailyIncome = dayIncome,
                transactions = dayItems
            )
        }

        // Build Analytics Date-Grouped stream
        val analyticsGroupedList = analyticsDateGroupMap.map { (dayStart, dayItems) ->
            var daySpent = 0.0
            var dayIncome = 0.0
            for (t in dayItems) {
                if (t.category == ExpenseCategory.INTERNAL_TRANSFER) continue
                if (t.type == TransactionType.DEBIT) daySpent += t.amount
                else dayIncome += t.amount
            }
            DateGroupedTransactions(
                dateLabel = DateGroupingUtils.getDateLabel(dayStart, now),
                dayEpoch = dayStart,
                dailySpent = daySpent,
                dailyIncome = dayIncome,
                transactions = dayItems
            )
        }

        // Build Category items with percentages
        val categoryItems = categorySpendingMap.map { (cat, amount) ->
            CategorySpendingItem(
                category = cat,
                totalAmount = amount,
                percentage = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f,
                transactionCount = categoryCountMap[cat] ?: 0
            )
        }.sortedByDescending { it.totalAmount }

        // Build 7-day chart bars
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayOfMonthFormat = SimpleDateFormat("d", Locale.getDefault())
        val todayStart = DateGroupingUtils.getDayStartMillis(now)

        val chartBars = dayStartMillisArray.mapIndexed { idx, dayEpoch ->
            val dateObj = Date(dayEpoch)
            DailySpendingBar(
                dayLabel = dayOfWeekFormat.format(dateObj),
                dateNumber = dayOfMonthFormat.format(dateObj),
                amount = dailySpendingBuckets[idx],
                isToday = dayEpoch == todayStart
            )
        }

        // Build Smart Insights
        val topBank = bankCountMap.maxByOrNull { it.value }?.key
        val topCategory = categorySpendingMap.maxByOrNull { it.value }?.key
        val daysInPeriod = when (controls.timeframe) {
            TimeframePeriod.TODAY -> 1
            TimeframePeriod.THIS_WEEK -> 7
            TimeframePeriod.THIS_MONTH -> Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
            TimeframePeriod.ALL_TIME -> 30
            TimeframePeriod.CUSTOM -> {
                if (controls.customRange != null) {
                    val diffMs = maxOf(0L, timeframeEnd - timeframeStart + 1000L)
                    (diffMs / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(1)
                } else 1
            }
        }
        val dailyAverage = totalExpense / daysInPeriod

        val insights = SpendingInsights(
            dailyAverage = dailyAverage,
            largestExpense = largestDebit,
            topBankSource = topBank,
            topCategory = topCategory,
            transactionCount = analyticsTransactionsCount
        )

        val chartData = buildSpendingChartData(allTx, controls.chartGranularity, now, controls.chartPages)

        DashboardUiState(
            transactions = filteredTransactions,
            rawAllTransactions = allTx,
            groupedTransactions = groupedList,
            analyticsGroupedTransactions = analyticsGroupedList,
            categoryBreakdown = categoryItems,
            weeklyChartBars = chartBars,
            chartData = chartData,
            insights = insights,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalBalance = totalIncome - totalExpense,
            ledgerTotalBalance = ledgerTotalIncome - ledgerTotalExpense,
            unreviewedCount = unreviewedCount,
            activeTimeframe = controls.timeframe,
            customDateRange = controls.customRange,
            activeFilter = controls.filter,
            selectedCategoryFilter = controls.categoryFilter,
            searchQuery = controls.query,
            editingTransaction = controls.editing,
            selectedTab = controls.tab,
            isSyncing = controls.syncing,
            syncMessage = controls.syncMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun setChartGranularity(granularity: ChartGranularity) {
        _chartGranularity.value = granularity
        _chartPages.value = 1
    }

    fun loadMoreChartHistory() {
        if (_chartPages.value < 25) {
            _chartPages.value += 1
        }
    }

    fun setTimeframe(timeframe: TimeframePeriod) {
        if (timeframe != TimeframePeriod.CUSTOM) {
            _customDateRange.value = null
        }
        _activeTimeframe.value = timeframe
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        val start = minOf(startMillis, endMillis)
        val end = maxOf(startMillis, endMillis)
        _customDateRange.value = start to end
        _activeTimeframe.value = TimeframePeriod.CUSTOM
    }

    fun clearCustomDateRange() {
        _customDateRange.value = null
        _activeTimeframe.value = TimeframePeriod.ALL_TIME
    }

    fun setFilter(filter: TransactionFilter) {
        _activeFilter.value = filter
    }

    fun toggleCategoryFilter(category: String) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
    }

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openEdit(transaction: TransactionEntity) {
        _editingTransaction.value = transaction
    }

    fun closeEdit() {
        _editingTransaction.value = null
    }

    fun saveDescriptionAndCategory(id: Long, description: String, category: String) {
        viewModelScope.launch {
            repository.updateNoteAndCategory(id, description, category)
            _editingTransaction.value = null
        }
    }

    fun updateManualTransaction(
        id: Long,
        amount: Double,
        type: TransactionType,
        category: String,
        description: String,
        timestamp: Long
    ) {
        viewModelScope.launch {
            repository.updateManualTransaction(
                id = id,
                amount = amount,
                type = type,
                category = category,
                description = description,
                timestamp = timestamp
            )
            _editingTransaction.value = null
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            if (_editingTransaction.value?.id == id) {
                _editingTransaction.value = null
            }
        }
    }

    fun addCashTransaction(
        amount: Double,
        type: TransactionType,
        category: String,
        description: String,
        timestamp: Long
    ) {
        viewModelScope.launch {
            repository.addManualTransaction(
                amount = amount,
                type = type,
                category = category,
                description = description,
                timestamp = timestamp,
                bankName = "Cash"
            )
        }
    }

    fun syncHistoricalSms() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Scanning inbox for banking SMS..."
            val result = syncManager.syncInbox(maxDaysBack = 180)
            _isSyncing.value = false
            _syncMessage.value = if (result.importedCount > 0) {
                "Imported ${result.importedCount} new transactions from ${result.scannedCount} SMS!"
            } else if (result.scannedCount > 0) {
                "All ${result.scannedCount} scanned messages are already up to date."
            } else {
                "No banking SMS found or permission needed."
            }
        }
    }

    fun exportTransactionsCsv(context: Context) {
        val list = uiState.value.rawAllTransactions
        if (list.isEmpty()) {
            _syncMessage.value = "No transactions to export yet."
            return
        }
        val success = CsvExportHelper.exportAndShareTransactions(context, list)
        if (!success) {
            _syncMessage.value = "Failed to export CSV report."
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    private fun buildSpendingChartData(
        allTx: List<TransactionEntity>,
        granularity: ChartGranularity,
        now: Long,
        pages: Int = 1
    ): SpendingChartData {
        val debits = allTx.filter { it.type == TransactionType.DEBIT && it.category != ExpenseCategory.INTERNAL_TRANSFER }
        val earliestTxTimestamp = debits.minOfOrNull { it.timestamp }

        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayOfMonthFormat = SimpleDateFormat("d", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
        val weekRangeFormat = SimpleDateFormat("d MMM", Locale.getDefault())
        val monthShortFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        val bars: List<SpendingChartBar>
        val recentCount: Int

        when (granularity) {
            ChartGranularity.DAILY_7 -> {
                recentCount = 7
                val debitsByDay = mutableMapOf<Long, Double>()
                for (tx in debits) {
                    val dayStart = DateGroupingUtils.getDayStartMillis(tx.timestamp)
                    debitsByDay[dayStart] = (debitsByDay[dayStart] ?: 0.0) + tx.amount
                }

                val todayStart = DateGroupingUtils.getDayStartMillis(now)
                // Initial chunk: 14 days, prepends +14 days per page on scroll
                val daysToLoad = 14 * pages
                val targetEarliestDay = todayStart - ((daysToLoad - 1) * 24 * 60 * 60 * 1000L)
                val actualEarliest = if (earliestTxTimestamp != null) {
                    maxOf(DateGroupingUtils.getDayStartMillis(earliestTxTimestamp), targetEarliestDay)
                } else {
                    targetEarliestDay
                }

                val resultList = mutableListOf<SpendingChartBar>()
                val cal = Calendar.getInstance().apply { timeInMillis = actualEarliest }
                while (cal.timeInMillis <= todayStart) {
                    val dayStart = cal.timeInMillis
                    val amount = debitsByDay[dayStart] ?: 0.0
                    val isToday = dayStart == todayStart
                    val date = Date(dayStart)

                    resultList.add(
                        SpendingChartBar(
                            id = "day_$dayStart",
                            primaryLabel = dayOfWeekFormat.format(date),
                            secondaryLabel = dayOfMonthFormat.format(date),
                            fullTitle = fullDateFormat.format(date),
                            amount = amount,
                            isCurrent = isToday,
                            timestamp = dayStart
                        )
                    )
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                bars = resultList
            }

            ChartGranularity.WEEKLY -> {
                recentCount = 6
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

                // Initial chunk: 8 weeks, prepends +8 weeks per page on scroll
                val weeksToLoad = 8 * pages
                val targetEarliestWeek = currentWeekMonday - ((weeksToLoad - 1) * 7 * 24 * 60 * 60 * 1000L)
                val actualEarliestWeek = if (earliestTxTimestamp != null) {
                    val calEarly = Calendar.getInstance().apply {
                        timeInMillis = earliestTxTimestamp
                        firstDayOfWeek = Calendar.MONDAY
                        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    maxOf(calEarly.timeInMillis, targetEarliestWeek)
                } else {
                    targetEarliestWeek
                }

                val resultList = mutableListOf<SpendingChartBar>()
                val cal = Calendar.getInstance().apply {
                    timeInMillis = actualEarliestWeek
                    firstDayOfWeek = Calendar.MONDAY
                }
                while (cal.timeInMillis <= currentWeekMonday) {
                    val weekStart = cal.timeInMillis
                    val calEnd = Calendar.getInstance().apply {
                        timeInMillis = weekStart
                        add(Calendar.DAY_OF_YEAR, 7)
                    }
                    val weekEnd = calEnd.timeInMillis
                    val amount = debits.filter { it.timestamp in weekStart until weekEnd }.sumOf { it.amount }
                    val isCurrent = weekStart == currentWeekMonday

                    val startDate = Date(weekStart)
                    val endDate = Date(weekEnd - 1000L)
                    val primary = if (isCurrent) "This Wk" else weekRangeFormat.format(startDate)
                    val secondary = if (isCurrent) weekRangeFormat.format(startDate) else ""
                    val fullTitle = "Week: ${weekRangeFormat.format(startDate)} – ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(endDate)}"

                    resultList.add(
                        SpendingChartBar(
                            id = "week_$weekStart",
                            primaryLabel = primary,
                            secondaryLabel = secondary,
                            fullTitle = fullTitle,
                            amount = amount,
                            isCurrent = isCurrent,
                            timestamp = weekStart
                        )
                    )
                    cal.add(Calendar.DAY_OF_YEAR, 7)
                }
                bars = resultList
            }

            ChartGranularity.MONTHLY -> {
                recentCount = 6
                val calNow = Calendar.getInstance().apply {
                    timeInMillis = now
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val currentMonthStart = calNow.timeInMillis

                // Initial chunk: 6 months, prepends +6 months per page on scroll
                val monthsToLoad = 6 * pages
                val calTargetEarly = Calendar.getInstance().apply {
                    timeInMillis = currentMonthStart
                    add(Calendar.MONTH, -(monthsToLoad - 1))
                }
                val actualEarliestMonth = if (earliestTxTimestamp != null) {
                    val calEarly = Calendar.getInstance().apply {
                        timeInMillis = earliestTxTimestamp
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    maxOf(calEarly.timeInMillis, calTargetEarly.timeInMillis)
                } else {
                    calTargetEarly.timeInMillis
                }

                val resultList = mutableListOf<SpendingChartBar>()
                val cal = Calendar.getInstance().apply { timeInMillis = actualEarliestMonth }
                while (cal.timeInMillis <= currentMonthStart) {
                    val monthStart = cal.timeInMillis
                    val calEnd = Calendar.getInstance().apply {
                        timeInMillis = monthStart
                        add(Calendar.MONTH, 1)
                    }
                    val monthEnd = calEnd.timeInMillis
                    val amount = debits.filter { it.timestamp in monthStart until monthEnd }.sumOf { it.amount }
                    val isCurrent = monthStart == currentMonthStart

                    val date = Date(monthStart)
                    val monthShort = monthShortFormat.format(date)
                    val yearShort = SimpleDateFormat("yy", Locale.getDefault()).format(date)
                    val fullTitle = monthYearFormat.format(date)

                    resultList.add(
                        SpendingChartBar(
                            id = "month_$monthStart",
                            primaryLabel = monthShort,
                            secondaryLabel = "'$yearShort",
                            fullTitle = fullTitle,
                            amount = amount,
                            isCurrent = isCurrent,
                            timestamp = monthStart
                        )
                    )
                    cal.add(Calendar.MONTH, 1)
                }
                bars = resultList
            }

            ChartGranularity.YEARLY -> {
                recentCount = 4
                val calNow = Calendar.getInstance().apply { timeInMillis = now }
                val currentYear = calNow.get(Calendar.YEAR)

                // Initial chunk: 4 years, prepends +4 years per page on scroll
                val yearsToLoad = 4 * pages
                val targetEarlyYear = currentYear - (yearsToLoad - 1)
                val earliestYear = if (earliestTxTimestamp != null) {
                    val calEarly = Calendar.getInstance().apply { timeInMillis = earliestTxTimestamp }
                    maxOf(calEarly.get(Calendar.YEAR), targetEarlyYear)
                } else {
                    targetEarlyYear
                }

                val resultList = mutableListOf<SpendingChartBar>()
                for (year in earliestYear..currentYear) {
                    val calStart = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val calEnd = Calendar.getInstance().apply {
                        timeInMillis = calStart.timeInMillis
                        add(Calendar.YEAR, 1)
                    }
                    val yearStart = calStart.timeInMillis
                    val yearEnd = calEnd.timeInMillis
                    val amount = debits.filter { it.timestamp in yearStart until yearEnd }.sumOf { it.amount }
                    val isCurrent = year == currentYear

                    resultList.add(
                        SpendingChartBar(
                            id = "year_$year",
                            primaryLabel = year.toString(),
                            secondaryLabel = "",
                            fullTitle = "Year $year",
                            amount = amount,
                            isCurrent = isCurrent,
                            timestamp = yearStart
                        )
                    )
                }
                bars = resultList
            }
        }

        // Summary metrics computed on recent window to match visible cards exactly
        val recentBars = bars.takeLast(recentCount)
        val total = recentBars.sumOf { it.amount }
        val average = if (recentBars.isNotEmpty()) total / recentBars.size else 0.0
        // Use recent window max so bars are prominently visible and not flattened by distant historical outliers
        val recentMax = (recentBars.maxOfOrNull { it.amount } ?: 1.0).let { if (it <= 0.0) 1.0 else it }

        return SpendingChartData(
            granularity = granularity,
            bars = bars,
            totalAmount = total,
            averageAmount = average,
            maxAmount = recentMax,
            pages = pages
        )
    }
}
