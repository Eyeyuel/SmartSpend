package com.eyuel.smartspend.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyuel.smartspend.SmartSpendApp
import com.eyuel.smartspend.data.local.TransactionEntity
import com.eyuel.smartspend.data.repository.CsvExportHelper
import com.eyuel.smartspend.data.repository.SmsSyncManager
import com.eyuel.smartspend.domain.model.TimeframePeriod
import com.eyuel.smartspend.domain.model.TransactionType
import com.eyuel.smartspend.ui.model.CategorySpendingItem
import com.eyuel.smartspend.ui.model.DailySpendingBar
import com.eyuel.smartspend.ui.model.DateGroupedTransactions
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
    val categoryBreakdown: List<CategorySpendingItem> = emptyList(),
    val weeklyChartBars: List<DailySpendingBar> = emptyList(),
    val insights: SpendingInsights? = null,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalBalance: Double = 0.0,
    val unreviewedCount: Int = 0,
    val activeTimeframe: TimeframePeriod = TimeframePeriod.TODAY,
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
    val filter: TransactionFilter,
    val categoryFilter: String?,
    val query: String,
    val editing: TransactionEntity?,
    val tab: Int,
    val syncing: Boolean,
    val syncMsg: String?
)

class TransactionListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmartSpendApp.instance.repository
    private val syncManager = SmsSyncManager(application, repository)

    private val _activeTimeframe = MutableStateFlow(TimeframePeriod.TODAY)
    private val _activeFilter = MutableStateFlow(TransactionFilter.ALL)
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _editingTransaction = MutableStateFlow<TransactionEntity?>(null)
    private val _selectedTab = MutableStateFlow(0)
    private val _isSyncing = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)

    private val _uiControls = combine(
        combine(_activeTimeframe, _activeFilter, _selectedCategoryFilter) { t, f, c -> Triple(t, f, c) },
        combine(_searchQuery, _editingTransaction, _selectedTab) { q, e, tab -> Triple(q, e, tab) },
        _isSyncing,
        _syncMessage
    ) { (timeframe, filter, categoryFilter), (query, editing, tab), syncing, syncMsg ->
        UiControls(timeframe, filter, categoryFilter, query, editing, tab, syncing, syncMsg)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAllTransactions(),
        _uiControls
    ) { allTx, controls ->
        val now = System.currentTimeMillis()
        val timeframeStart = controls.timeframe.getStartMillis(now)
        val query = controls.query.trim()
        val hasQuery = query.isNotEmpty()

        var totalIncome = 0.0
        var totalExpense = 0.0
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

        for (item in allTx) {
            // Count unreviewed across all time
            if (!item.isReviewed) {
                unreviewedCount++
            }

            // 7-day spending aggregation
            if (item.type == TransactionType.DEBIT && item.timestamp >= sevenDaysAgoStart) {
                val dayStart = DateGroupingUtils.getDayStartMillis(item.timestamp)
                val dayIndex = ((dayStart - sevenDaysAgoStart) / (24 * 60 * 60 * 1000L)).toInt()
                if (dayIndex in 0..6) {
                    dailySpendingBuckets[dayIndex] += item.amount
                }
            }

            // Check Timeframe filter
            val isInTimeframe = item.timestamp >= timeframeStart
            if (!isInTimeframe) continue

            // Accumulate metrics for active timeframe
            if (item.type == TransactionType.CREDIT) {
                totalIncome += item.amount
            } else {
                totalExpense += item.amount
                // Category breakdown
                categorySpendingMap[item.category] = (categorySpendingMap[item.category] ?: 0.0) + item.amount
                categoryCountMap[item.category] = (categoryCountMap[item.category] ?: 0) + 1

                if (largestDebit == null || item.amount > largestDebit.amount) {
                    largestDebit = item
                }
            }
            bankCountMap[item.bankName] = (bankCountMap[item.bankName] ?: 0) + 1

            // Check Category filter (tap-to-filter)
            val matchesCategory = controls.categoryFilter == null || item.category == controls.categoryFilter

            // Check Tab filter (All / Needs Note / Expense / Income)
            val matchesFilter = when (controls.filter) {
                TransactionFilter.ALL -> true
                TransactionFilter.UNREVIEWED -> !item.isReviewed
                TransactionFilter.EXPENSES -> item.type == TransactionType.DEBIT
                TransactionFilter.INCOME -> item.type == TransactionType.CREDIT
            }

            // Check Search Query
            val matchesSearch = !hasQuery ||
                    item.bankName.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true) ||
                    (item.referenceId?.contains(query, ignoreCase = true) == true)

            if (matchesCategory && matchesFilter && matchesSearch) {
                filteredTransactions.add(item)

                val dayStart = DateGroupingUtils.getDayStartMillis(item.timestamp)
                val listForDay = dateGroupMap.getOrPut(dayStart) { mutableListOf() }
                listForDay.add(item)
            }
        }

        // Build Date-Grouped stream
        val groupedList = dateGroupMap.map { (dayStart, dayItems) ->
            var daySpent = 0.0
            var dayIncome = 0.0
            for (t in dayItems) {
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
        }
        val dailyAverage = totalExpense / daysInPeriod

        val insights = SpendingInsights(
            dailyAverage = dailyAverage,
            largestExpense = largestDebit,
            topBankSource = topBank,
            topCategory = topCategory,
            transactionCount = filteredTransactions.size
        )

        DashboardUiState(
            transactions = filteredTransactions,
            rawAllTransactions = allTx,
            groupedTransactions = groupedList,
            categoryBreakdown = categoryItems,
            weeklyChartBars = chartBars,
            insights = insights,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalBalance = totalIncome - totalExpense,
            unreviewedCount = unreviewedCount,
            activeTimeframe = controls.timeframe,
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

    fun setTimeframe(timeframe: TimeframePeriod) {
        _activeTimeframe.value = timeframe
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

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            if (_editingTransaction.value?.id == id) {
                _editingTransaction.value = null
            }
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
}
