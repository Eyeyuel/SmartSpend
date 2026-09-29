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
    suspend fun rescanAndHealTransactions(): Int
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

    override suspend fun rescanAndHealTransactions(): Int {
        val all = transactionDao.getAllTransactionsList()
        var updatedCount = 0
        for (tx in all) {
            if (tx.isReviewed) continue
            val parsed = com.eyuel.smartspend.domain.parser.BankParserRegistry.defaultInstance.parse(tx.senderAddress, tx.rawBody, tx.timestamp) ?: continue
            val newCategory = parsed.suggestedCategory ?: tx.category
            val newDesc = parsed.counterparty ?: tx.description
            if (Math.abs(tx.amount - parsed.amount) > 0.001 || tx.category != newCategory || tx.description != newDesc) {
                val updated = tx.copy(
                    amount = parsed.amount,
                    category = newCategory,
                    description = newDesc,
                    type = parsed.type,
                    balanceAfter = parsed.balanceAfter ?: tx.balanceAfter
                )
                transactionDao.updateTransaction(updated)
                updatedCount++
            }
        }
        return updatedCount
    }

    override suspend fun saveParsedTransactions(parsedList: List<ParsedTransaction>): Int {
        if (parsedList.isEmpty()) return 0

        // 1. Fetch existing transactions to handle deduplication & auto-healing
        val existingList = transactionDao.getAllTransactionsList()
        val existingMap = existingList.associateBy { it.referenceId }
        val seenRefsInBatch = mutableSetOf<String>()
        val toInsert = mutableListOf<TransactionEntity>()
        var healedCount = 0

        for (parsed in parsedList) {
            val ref = parsed.referenceId ?: ParserUtils.generateDeterministicReference(
                parsed.senderAddress,
                parsed.timestamp,
                parsed.amount,
                parsed.rawBody
            )

            val existing = existingMap[ref]
            if (existing != null) {
                // Auto-heal if unreviewed and newly improved parser extracted more accurate amount or category
                if (!existing.isReviewed) {
                    val targetCategory = parsed.suggestedCategory ?: existing.category
                    val targetDesc = parsed.counterparty ?: existing.description
                    if (Math.abs(existing.amount - parsed.amount) > 0.001 || existing.category != targetCategory || existing.description != targetDesc) {
                        transactionDao.updateTransaction(
                            existing.copy(
                                amount = parsed.amount,
                                category = targetCategory,
                                description = targetDesc,
                                type = parsed.type,
                                balanceAfter = parsed.balanceAfter ?: existing.balanceAfter
                            )
                        )
                        healedCount++
                    }
                }
                continue
            }

            if (seenRefsInBatch.contains(ref)) {
                continue
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

        val insertedCount = if (toInsert.isNotEmpty()) {
            val rowIds = transactionDao.insertAllOrIgnore(toInsert)
            rowIds.count { it > 0 }
        } else 0

        return insertedCount + healedCount
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
