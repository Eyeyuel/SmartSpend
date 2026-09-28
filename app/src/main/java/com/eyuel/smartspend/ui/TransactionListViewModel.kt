package com.eyuel.smartspend.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyuel.smartspend.SmartSpendApp
import com.eyuel.smartspend.data.local.TransactionEntity
import com.eyuel.smartspend.data.repository.CsvExportHelper
import com.eyuel.smartspend.data.repository.SmsSyncManager
import com.eyuel.smartspend.domain.model.TransactionType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TransactionFilter {
    ALL,
    UNREVIEWED, // "Needs Note"
    EXPENSES,   // Debits
    INCOME      // Credits
}

data class DashboardUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val rawAllTransactions: List<TransactionEntity> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalBalance: Double = 0.0,
    val unreviewedCount: Int = 0,
    val activeFilter: TransactionFilter = TransactionFilter.ALL,
    val searchQuery: String = "",
    val editingTransaction: TransactionEntity? = null,
    val showAnalytics: Boolean = false,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null
)

private data class UiControls(
    val filter: TransactionFilter,
    val query: String,
    val editing: TransactionEntity?,
    val showAnalytics: Boolean,
    val syncing: Boolean,
    val syncMsg: String?
)

class TransactionListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmartSpendApp.instance.repository
    private val syncManager = SmsSyncManager(application, repository)

    private val _activeFilter = MutableStateFlow(TransactionFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _editingTransaction = MutableStateFlow<TransactionEntity?>(null)
    private val _showAnalytics = MutableStateFlow(false)
    private val _isSyncing = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)

    private val _uiControls = combine(
        combine(_activeFilter, _searchQuery, _editingTransaction) { f, q, e -> Triple(f, q, e) },
        _showAnalytics,
        _isSyncing,
        _syncMessage
    ) { (filter, query, editing), showAnalytics, syncing, syncMsg ->
        UiControls(filter, query, editing, showAnalytics, syncing, syncMsg)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAllTransactions(),
        _uiControls
    ) { allTx, controls ->
        var totalIncome = 0.0
        var totalExpense = 0.0
        var unreviewedCount = 0

        val filtered = ArrayList<TransactionEntity>(allTx.size)
        val query = controls.query.trim()
        val hasQuery = query.isNotEmpty()

        for (item in allTx) {
            if (item.type == TransactionType.CREDIT) {
                totalIncome += item.amount
            } else {
                totalExpense += item.amount
            }
            if (!item.isReviewed) {
                unreviewedCount++
            }

            val matchesFilter = when (controls.filter) {
                TransactionFilter.ALL -> true
                TransactionFilter.UNREVIEWED -> !item.isReviewed
                TransactionFilter.EXPENSES -> item.type == TransactionType.DEBIT
                TransactionFilter.INCOME -> item.type == TransactionType.CREDIT
            }

            val matchesSearch = !hasQuery ||
                    item.bankName.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true) ||
                    (item.referenceId?.contains(query, ignoreCase = true) == true)

            if (matchesFilter && matchesSearch) {
                filtered.add(item)
            }
        }

        DashboardUiState(
            transactions = filtered,
            rawAllTransactions = allTx,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalBalance = totalIncome - totalExpense,
            unreviewedCount = unreviewedCount,
            activeFilter = controls.filter,
            searchQuery = controls.query,
            editingTransaction = controls.editing,
            showAnalytics = controls.showAnalytics,
            isSyncing = controls.syncing,
            syncMessage = controls.syncMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun setFilter(filter: TransactionFilter) {
        _activeFilter.value = filter
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

    fun setAnalyticsVisible(visible: Boolean) {
        _showAnalytics.value = visible
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
