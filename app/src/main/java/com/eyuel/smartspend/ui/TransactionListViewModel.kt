package com.eyuel.smartspend.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyuel.smartspend.SmartSpendApp
import com.eyuel.smartspend.data.local.TransactionEntity
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
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalBalance: Double = 0.0,
    val unreviewedCount: Int = 0,
    val activeFilter: TransactionFilter = TransactionFilter.ALL,
    val searchQuery: String = "",
    val editingTransaction: TransactionEntity? = null,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null
)

private data class UiControls(
    val filter: TransactionFilter,
    val query: String,
    val editing: TransactionEntity?,
    val syncing: Boolean,
    val syncMsg: String?
)

class TransactionListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmartSpendApp.instance.repository
    private val syncManager = SmsSyncManager(application, repository)

    private val _activeFilter = MutableStateFlow(TransactionFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _editingTransaction = MutableStateFlow<TransactionEntity?>(null)
    private val _isSyncing = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)

    private val _uiControls = combine(
        _activeFilter,
        _searchQuery,
        _editingTransaction,
        _isSyncing,
        _syncMessage
    ) { filter, query, editing, syncing, syncMsg ->
        UiControls(filter, query, editing, syncing, syncMsg)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAllTransactions(),
        repository.getTotalIncome(),
        repository.getTotalExpenses(),
        repository.getUnreviewedCount(),
        _uiControls
    ) { allTx, income, expense, unreviewedCount, controls ->
        val filtered = allTx.filter { item ->
            val matchesFilter = when (controls.filter) {
                TransactionFilter.ALL -> true
                TransactionFilter.UNREVIEWED -> !item.isReviewed
                TransactionFilter.EXPENSES -> item.type == TransactionType.DEBIT
                TransactionFilter.INCOME -> item.type == TransactionType.CREDIT
            }
            val matchesSearch = controls.query.isBlank() ||
                    item.bankName.contains(controls.query, ignoreCase = true) ||
                    item.description.contains(controls.query, ignoreCase = true) ||
                    item.category.contains(controls.query, ignoreCase = true) ||
                    (item.referenceId?.contains(controls.query, ignoreCase = true) == true) ||
                    item.rawBody.contains(controls.query, ignoreCase = true)

            matchesFilter && matchesSearch
        }

        val inc = income ?: 0.0
        val exp = expense ?: 0.0

        DashboardUiState(
            transactions = filtered,
            totalIncome = inc,
            totalExpense = exp,
            totalBalance = inc - exp,
            unreviewedCount = unreviewedCount,
            activeFilter = controls.filter,
            searchQuery = controls.query,
            editingTransaction = controls.editing,
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

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
