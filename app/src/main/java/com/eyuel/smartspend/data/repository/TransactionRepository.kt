package com.eyuel.smartspend.data.repository

import com.eyuel.smartspend.data.local.CategorySpending
import com.eyuel.smartspend.data.local.TransactionDao
import com.eyuel.smartspend.data.local.TransactionEntity
import com.eyuel.smartspend.domain.model.ParsedTransaction
import com.eyuel.smartspend.domain.model.TransactionType
import com.eyuel.smartspend.domain.parser.ParserUtils
import kotlinx.coroutines.flow.Flow
import java.util.UUID

import com.eyuel.smartspend.data.local.UserCategoryDao
import com.eyuel.smartspend.data.local.UserCategoryEntity
import com.eyuel.smartspend.domain.model.ExpenseCategory
import com.eyuel.smartspend.domain.model.IncomeCategory
import com.eyuel.smartspend.ui.CategoryVisuals
import kotlinx.coroutines.flow.onEach

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getUnreviewedTransactions(): Flow<List<TransactionEntity>>
    fun getUnreviewedCount(): Flow<Int>
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>
    fun getTransactionById(id: Long): Flow<TransactionEntity?>
    suspend fun rescanAndHealTransactions(): Int
    suspend fun saveParsedTransaction(parsed: ParsedTransaction): Boolean
    suspend fun saveParsedTransactions(parsedList: List<ParsedTransaction>): Int
    suspend fun addManualTransaction(
        amount: Double,
        type: TransactionType,
        category: String,
        description: String,
        timestamp: Long,
        bankName: String = "Cash"
    ): Long
    suspend fun updateNoteAndCategory(id: Long, description: String, category: String)
    suspend fun updateManualTransaction(
        id: Long,
        amount: Double,
        type: TransactionType,
        category: String,
        description: String,
        timestamp: Long
    )
    suspend fun deleteTransaction(id: Long)
    suspend fun clearAll()
    fun getTotalIncome(): Flow<Double?>
    fun getTotalExpenses(): Flow<Double?>
    fun getIncomeInRange(startMillis: Long, endMillis: Long): Flow<Double?>
    fun getExpensesInRange(startMillis: Long, endMillis: Long): Flow<Double?>
    fun getCategorySpending(startMillis: Long, endMillis: Long): Flow<List<CategorySpending>>

    // Category Management
    fun getAllCategories(): Flow<List<UserCategoryEntity>>
    suspend fun ensureDefaultCategoriesSeeded()
    suspend fun createCategory(name: String, type: TransactionType, iconKey: String, colorHex: Long): Boolean
    suspend fun deleteCategory(name: String, reassignTo: String = ExpenseCategory.OTHER): Boolean
    suspend fun getTransactionCountForCategory(category: String): Int
}

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val userCategoryDao: UserCategoryDao
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

    override suspend fun addManualTransaction(
        amount: Double,
        type: TransactionType,
        category: String,
        description: String,
        timestamp: Long,
        bankName: String
    ): Long {
        val ref = "CASH_${timestamp}_${UUID.randomUUID().toString().take(6)}"
        val entity = TransactionEntity(
            referenceId = ref,
            bankName = bankName,
            senderAddress = "MANUAL_ENTRY",
            amount = amount,
            currency = "ETB",
            type = type,
            timestamp = timestamp,
            balanceAfter = null,
            accountNumber = null,
            rawBody = "Manual Cash Entry",
            description = description.trim(),
            category = category,
            isReviewed = true
        )
        return transactionDao.insertOrIgnore(entity)
    }

    override suspend fun updateNoteAndCategory(id: Long, description: String, category: String) {
        transactionDao.updateNoteAndCategory(
            id = id,
            description = description.trim(),
            category = category,
            isReviewed = true
        )
    }

    override suspend fun updateManualTransaction(
        id: Long,
        amount: Double,
        type: TransactionType,
        category: String,
        description: String,
        timestamp: Long
    ) {
        transactionDao.updateManualTransactionDetails(
            id = id,
            amount = amount,
            type = type,
            category = category,
            description = description.trim(),
            timestamp = timestamp
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

    override fun getAllCategories(): Flow<List<UserCategoryEntity>> =
        userCategoryDao.getAllCategories().onEach { list ->
            list.forEach { cat ->
                CategoryVisuals.registerCustomCategory(cat.name, cat.iconKey, cat.colorHex)
            }
        }

    override suspend fun ensureDefaultCategoriesSeeded() {
        val count = userCategoryDao.getCategoryCount()
        if (count == 0) {
            val now = System.currentTimeMillis()
            val defaultList = listOf(
                // Expense defaults
                UserCategoryEntity(ExpenseCategory.FOOD_DINING, TransactionType.DEBIT, "Restaurant", 0xFFFB923C, isDefault = true, createdAt = now),
                UserCategoryEntity(ExpenseCategory.GROCERIES, TransactionType.DEBIT, "LocalGroceryStore", 0xFF4ADE80, isDefault = true, createdAt = now),
                UserCategoryEntity(ExpenseCategory.TRANSPORTATION, TransactionType.DEBIT, "DirectionsCar", 0xFF38BDF8, isDefault = true, createdAt = now),
                UserCategoryEntity(ExpenseCategory.BILLS_UTILITIES, TransactionType.DEBIT, "Bolt", 0xFFFACC15, isDefault = true, createdAt = now),
                UserCategoryEntity(ExpenseCategory.SHOPPING, TransactionType.DEBIT, "ShoppingBag", 0xFFC084FC, isDefault = true, createdAt = now),
                UserCategoryEntity(ExpenseCategory.ENTERTAINMENT, TransactionType.DEBIT, "Theaters", 0xFFF472B6, isDefault = true, createdAt = now),
                UserCategoryEntity(ExpenseCategory.HEALTHCARE, TransactionType.DEBIT, "LocalHospital", 0xFFF43F5E, isDefault = true, createdAt = now),
                UserCategoryEntity(ExpenseCategory.TRANSFER, TransactionType.DEBIT, "SwapHoriz", 0xFF818CF8, isDefault = true, createdAt = now),
                UserCategoryEntity(ExpenseCategory.OTHER, TransactionType.DEBIT, "Category", 0xFF94A3B8, isDefault = true, createdAt = now),

                // Income defaults
                UserCategoryEntity(IncomeCategory.SALARY, TransactionType.CREDIT, "Payments", 0xFF34D399, isDefault = true, createdAt = now),
                UserCategoryEntity(IncomeCategory.BUSINESS, TransactionType.CREDIT, "Storefront", 0xFF2DD4BF, isDefault = true, createdAt = now),
                UserCategoryEntity(IncomeCategory.FREELANCE, TransactionType.CREDIT, "Work", 0xFF38BDF8, isDefault = true, createdAt = now),
                UserCategoryEntity(IncomeCategory.TRANSFER, TransactionType.CREDIT, "SwapHoriz", 0xFF818CF8, isDefault = true, createdAt = now),
                UserCategoryEntity(IncomeCategory.GIFT, TransactionType.CREDIT, "CardGiftcard", 0xFFF472B6, isDefault = true, createdAt = now),
                UserCategoryEntity(IncomeCategory.INVESTMENT, TransactionType.CREDIT, "TrendingUp", 0xFFA78BFA, isDefault = true, createdAt = now),
                UserCategoryEntity(IncomeCategory.RENTAL, TransactionType.CREDIT, "Home", 0xFF60A5FA, isDefault = true, createdAt = now),
                UserCategoryEntity(IncomeCategory.REFUND, TransactionType.CREDIT, "Savings", 0xFF2DD4BF, isDefault = true, createdAt = now),
                UserCategoryEntity(IncomeCategory.OTHER_INCOME, TransactionType.CREDIT, "Category", 0xFF94A3B8, isDefault = true, createdAt = now)
            )
            userCategoryDao.insertCategories(defaultList)
        }
    }

    override suspend fun createCategory(
        name: String,
        type: TransactionType,
        iconKey: String,
        colorHex: Long
    ): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val existing = userCategoryDao.getCategoryByName(trimmed)
        if (existing != null) return false
        val entity = UserCategoryEntity(
            name = trimmed,
            type = type,
            iconKey = iconKey,
            colorHex = colorHex,
            isDefault = false,
            createdAt = System.currentTimeMillis()
        )
        userCategoryDao.insertCategory(entity)
        CategoryVisuals.registerCustomCategory(trimmed, iconKey, colorHex)
        return true
    }

    override suspend fun deleteCategory(name: String, reassignTo: String): Boolean {
        transactionDao.reassignCategory(name, reassignTo)
        CategoryVisuals.unregisterCustomCategory(name)
        return userCategoryDao.deleteCategory(name) > 0
    }

    override suspend fun getTransactionCountForCategory(category: String): Int {
        return transactionDao.countTransactionsByCategory(category)
    }
}
