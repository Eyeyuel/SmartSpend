package com.eyuel.smartspend.data.repository

import com.eyuel.smartspend.data.local.CategorySpending
import com.eyuel.smartspend.data.local.TransactionDao
import com.eyuel.smartspend.data.local.TransactionEntity
import com.eyuel.smartspend.domain.model.ParsedTransaction
import com.eyuel.smartspend.domain.model.TransactionType
import com.eyuel.smartspend.domain.parser.ParserUtils
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getUnreviewedTransactions(): Flow<List<TransactionEntity>>
    fun getUnreviewedCount(): Flow<Int>
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>
    fun getTransactionById(id: Long): Flow<TransactionEntity?>
    suspend fun saveParsedTransaction(parsed: ParsedTransaction): Boolean
    suspend fun saveParsedTransactions(parsedList: List<ParsedTransaction>): Int
    suspend fun updateNoteAndCategory(id: Long, description: String, category: String)
    suspend fun deleteTransaction(id: Long)
    suspend fun clearAll()
    fun getTotalIncome(): Flow<Double?>
    fun getTotalExpenses(): Flow<Double?>
    fun getIncomeInRange(startMillis: Long, endMillis: Long): Flow<Double?>
    fun getExpensesInRange(startMillis: Long, endMillis: Long): Flow<Double?>
    fun getCategorySpending(startMillis: Long, endMillis: Long): Flow<List<CategorySpending>>
}

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions()

    override fun getUnreviewedTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getUnreviewedTransactions()

    override fun getUnreviewedCount(): Flow<Int> =
        transactionDao.getUnreviewedCount()

    override fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByType(type)

    override fun getTransactionById(id: Long): Flow<TransactionEntity?> =
        transactionDao.getTransactionById(id)

    override suspend fun saveParsedTransaction(parsed: ParsedTransaction): Boolean {
        val ref = parsed.referenceId ?: ParserUtils.generateDeterministicReference(
            parsed.senderAddress,
            parsed.timestamp,
            parsed.amount,
            parsed.rawBody
        )

        // Deduplication check: check referenceId or matching sender/amount/timestamp
        if (transactionDao.hasTransactionWithReference(ref)) {
            return false // Already recorded
        }
        if (transactionDao.hasMatchingTransaction(parsed.senderAddress, parsed.amount, parsed.timestamp)) {
            return false
        }

        val entity = TransactionEntity(
            referenceId = ref,
            bankName = parsed.bankName,
            senderAddress = parsed.senderAddress,
            amount = parsed.amount,
            currency = parsed.currency,
            type = parsed.type,
            timestamp = parsed.timestamp,
            balanceAfter = parsed.balanceAfter,
            accountNumber = parsed.accountNumber,
            rawBody = parsed.rawBody,
            description = parsed.counterparty ?: "",
            category = parsed.suggestedCategory ?: "Uncategorized",
            isReviewed = false
        )
        val rowId = transactionDao.insertOrIgnore(entity)
        return rowId > 0
    }

    override suspend fun saveParsedTransactions(parsedList: List<ParsedTransaction>): Int {
        if (parsedList.isEmpty()) return 0

        // 1. Fetch existing reference IDs in a single query
        val existingRefs = transactionDao.getAllReferenceIds().toHashSet()

        // 2. Filter duplicates in-memory
        val toInsert = mutableListOf<TransactionEntity>()
        val seenRefsInBatch = mutableSetOf<String>()

        for (parsed in parsedList) {
            val ref = parsed.referenceId ?: ParserUtils.generateDeterministicReference(
                parsed.senderAddress,
                parsed.timestamp,
                parsed.amount,
                parsed.rawBody
            )

            if (existingRefs.contains(ref) || seenRefsInBatch.contains(ref)) {
                continue // Already recorded
            }
            seenRefsInBatch.add(ref)

            toInsert.add(
                TransactionEntity(
                    referenceId = ref,
                    bankName = parsed.bankName,
                    senderAddress = parsed.senderAddress,
                    amount = parsed.amount,
                    currency = parsed.currency,
                    type = parsed.type,
                    timestamp = parsed.timestamp,
                    balanceAfter = parsed.balanceAfter,
                    accountNumber = parsed.accountNumber,
                    rawBody = parsed.rawBody,
                    description = parsed.counterparty ?: "",
                    category = parsed.suggestedCategory ?: "Uncategorized",
                    isReviewed = false
                )
            )
        }

        if (toInsert.isEmpty()) return 0

        // 3. Single atomic batch write to SQLite
        val rowIds = transactionDao.insertAllOrIgnore(toInsert)
        return rowIds.count { it > 0 }
    }

    override suspend fun updateNoteAndCategory(id: Long, description: String, category: String) {
        transactionDao.updateNoteAndCategory(
            id = id,
            description = description.trim(),
            category = category,
            isReviewed = true
        )
    }

    override suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    override suspend fun clearAll() {
        transactionDao.deleteAll()
    }

    override fun getTotalIncome(): Flow<Double?> = transactionDao.getTotalIncome()

    override fun getTotalExpenses(): Flow<Double?> = transactionDao.getTotalExpenses()

    override fun getIncomeInRange(startMillis: Long, endMillis: Long): Flow<Double?> =
        transactionDao.getIncomeInRange(startMillis, endMillis)

    override fun getExpensesInRange(startMillis: Long, endMillis: Long): Flow<Double?> =
        transactionDao.getExpensesInRange(startMillis, endMillis)

    override fun getCategorySpending(startMillis: Long, endMillis: Long): Flow<List<CategorySpending>> =
        transactionDao.getCategorySpending(startMillis, endMillis)
}
