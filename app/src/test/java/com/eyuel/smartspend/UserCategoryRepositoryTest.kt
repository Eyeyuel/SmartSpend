package com.eyuel.smartspend

import com.eyuel.smartspend.data.local.*
import com.eyuel.smartspend.data.repository.TransactionRepositoryImpl
import com.eyuel.smartspend.domain.model.ExpenseCategory
import com.eyuel.smartspend.domain.model.IncomeCategory
import com.eyuel.smartspend.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeUserCategoryDao : UserCategoryDao {
    private val categories = mutableMapOf<String, UserCategoryEntity>()
    private val categoriesFlow = MutableStateFlow<List<UserCategoryEntity>>(emptyList())

    private fun notifyFlow() {
        categoriesFlow.value = categories.values.sortedWith(
            compareByDescending<UserCategoryEntity> { it.isDefault }
                .thenBy { it.createdAt }
        )
    }

    override fun getAllCategories(): Flow<List<UserCategoryEntity>> = categoriesFlow

    override suspend fun getAllCategoriesSync(): List<UserCategoryEntity> = categories.values.toList()

    override suspend fun getCategoryCount(): Int = categories.size

    override suspend fun insertCategory(category: UserCategoryEntity) {
        if (!categories.containsKey(category.name)) {
            categories[category.name] = category
            notifyFlow()
        }
    }

    override suspend fun insertCategories(categoriesList: List<UserCategoryEntity>) {
        categoriesList.forEach {
            if (!categories.containsKey(it.name)) {
                categories[it.name] = it
            }
        }
        notifyFlow()
    }

    override suspend fun deleteCategory(name: String): Int {
        val existing = categories[name]
        return if (existing != null && !existing.isDefault) {
            categories.remove(name)
            notifyFlow()
            1
        } else {
            0
        }
    }

    override suspend fun getCategoryByName(name: String): UserCategoryEntity? = categories[name]
}

class FakeTransactionDao : TransactionDao {
    val transactions = mutableListOf<TransactionEntity>()

    override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf(transactions)
    override fun getUnreviewedTransactions(): Flow<List<TransactionEntity>> = flowOf(transactions.filter { !it.isReviewed })
    override fun getUnreviewedCount(): Flow<Int> = flowOf(transactions.count { !it.isReviewed })
    override fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>> = flowOf(transactions.filter { it.type == type })
    override fun getTransactionById(id: Long): Flow<TransactionEntity?> = flowOf(transactions.firstOrNull { it.id == id })
    override suspend fun insertOrIgnore(transaction: TransactionEntity): Long {
        transactions.add(transaction)
        return transactions.size.toLong()
    }
    override suspend fun insertAllOrIgnore(transactionsList: List<TransactionEntity>): List<Long> {
        transactions.addAll(transactionsList)
        return transactionsList.map { 1L }
    }
    override suspend fun updateTransaction(transaction: TransactionEntity) {}
    override suspend fun updateNoteAndCategory(id: Long, description: String, category: String, isReviewed: Boolean) {}
    override suspend fun updateManualTransactionDetails(id: Long, amount: Double, type: TransactionType, category: String, description: String, timestamp: Long) {}
    override suspend fun deleteTransaction(transaction: TransactionEntity) { transactions.remove(transaction) }
    override suspend fun deleteTransactionById(id: Long) { transactions.removeIf { it.id == id } }
    override suspend fun deleteAll() { transactions.clear() }
    override suspend fun getAllTransactionsList(): List<TransactionEntity> = transactions
    override suspend fun getTransactionByReference(referenceId: String): TransactionEntity? = transactions.firstOrNull { it.referenceId == referenceId }
    override suspend fun getAllReferenceIds(): List<String> = transactions.mapNotNull { it.referenceId }
    override suspend fun hasTransactionWithReference(referenceId: String): Boolean = transactions.any { it.referenceId == referenceId }
    override suspend fun hasMatchingTransaction(sender: String, amount: Double, timestamp: Long): Boolean = false
    override fun getTotalIncome(): Flow<Double?> = flowOf(transactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount })
    override fun getTotalExpenses(): Flow<Double?> = flowOf(transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount })
    override fun getIncomeInRange(startMillis: Long, endMillis: Long): Flow<Double?> = flowOf(0.0)
    override fun getExpensesInRange(startMillis: Long, endMillis: Long): Flow<Double?> = flowOf(0.0)
    override fun getCategorySpending(startMillis: Long, endMillis: Long): Flow<List<CategorySpending>> = flowOf(emptyList())

    override suspend fun countTransactionsByCategory(category: String): Int {
        return transactions.count { it.category == category }
    }

    override suspend fun reassignCategory(oldCategory: String, newCategory: String) {
        val updated = transactions.map {
            if (it.category == oldCategory) {
                it.copy(category = newCategory)
            } else {
                it
            }
        }
        transactions.clear()
        transactions.addAll(updated)
    }
}

class UserCategoryRepositoryTest {

    private lateinit var fakeUserCategoryDao: FakeUserCategoryDao
    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var repository: TransactionRepositoryImpl

    @Before
    fun setUp() {
        fakeUserCategoryDao = FakeUserCategoryDao()
        fakeTransactionDao = FakeTransactionDao()
        repository = TransactionRepositoryImpl(fakeTransactionDao, fakeUserCategoryDao)
    }

    @Test
    fun testEnsureDefaultCategoriesSeeded() = runBlocking {
        assertEquals(0, fakeUserCategoryDao.getCategoryCount())
        repository.ensureDefaultCategoriesSeeded()

        val count = fakeUserCategoryDao.getCategoryCount()
        assertTrue("Default categories should be seeded", count > 10)

        val food = fakeUserCategoryDao.getCategoryByName(ExpenseCategory.FOOD_DINING)
        assertNotNull(food)
        assertTrue(food!!.isDefault)
        assertEquals(TransactionType.DEBIT, food.type)

        val salary = fakeUserCategoryDao.getCategoryByName(IncomeCategory.SALARY)
        assertNotNull(salary)
        assertTrue(salary!!.isDefault)
        assertEquals(TransactionType.CREDIT, salary.type)
    }

    @Test
    fun testCreateCustomCategory() = runBlocking {
        repository.ensureDefaultCategoriesSeeded()
        val success = repository.createCategory(
            name = "Gym Membership",
            type = TransactionType.DEBIT,
            iconKey = "FitnessCenter",
            colorHex = 0xFF38BDF8
        )
        assertTrue(success)

        val entity = fakeUserCategoryDao.getCategoryByName("Gym Membership")
        assertNotNull(entity)
        assertFalse(entity!!.isDefault)
        assertEquals("FitnessCenter", entity.iconKey)
        assertEquals(0xFF38BDF8, entity.colorHex)
    }

    @Test
    fun testPreventDuplicateCategoryCreation() = runBlocking {
        repository.ensureDefaultCategoriesSeeded()

        // Attempt to create duplicate of default category
        val failDefault = repository.createCategory(
            name = ExpenseCategory.FOOD_DINING,
            type = TransactionType.DEBIT,
            iconKey = "Restaurant",
            colorHex = 0xFFFB923C
        )
        assertFalse("Should not create duplicate of existing category", failDefault)

        // Attempt to create duplicate of custom category
        repository.createCategory("Coffee Shops", TransactionType.DEBIT, "LocalCafe", 0xFFFB923C)
        val failCustom = repository.createCategory("Coffee Shops", TransactionType.DEBIT, "LocalCafe", 0xFFFB923C)
        assertFalse("Should not create duplicate of custom category", failCustom)
    }

    @Test
    fun testDeleteCategoryWithTransactionReassignment() = runBlocking {
        repository.ensureDefaultCategoriesSeeded()
        repository.createCategory("Pet Grooming", TransactionType.DEBIT, "Pets", 0xFFF472B6)

        // Add 2 transactions with this category
        fakeTransactionDao.transactions.add(
            TransactionEntity(
                id = 1,
                referenceId = "REF_1",
                bankName = "Cash",
                senderAddress = "MANUAL",
                amount = 250.0,
                currency = "ETB",
                type = TransactionType.DEBIT,
                timestamp = System.currentTimeMillis(),
                category = "Pet Grooming",
                rawBody = "",
                isReviewed = true
            )
        )
        fakeTransactionDao.transactions.add(
            TransactionEntity(
                id = 2,
                referenceId = "REF_2",
                bankName = "Cash",
                senderAddress = "MANUAL",
                amount = 150.0,
                currency = "ETB",
                type = TransactionType.DEBIT,
                timestamp = System.currentTimeMillis(),
                category = "Pet Grooming",
                rawBody = "",
                isReviewed = true
            )
        )

        assertEquals(2, repository.getTransactionCountForCategory("Pet Grooming"))

        // Delete Pet Grooming with reassignment to "Other"
        val deleteSuccess = repository.deleteCategory("Pet Grooming", reassignTo = ExpenseCategory.OTHER)
        assertTrue(deleteSuccess)

        // Category should be deleted from DB
        assertNull(fakeUserCategoryDao.getCategoryByName("Pet Grooming"))

        // Transactions should now be reassigned to "Other"
        assertEquals(0, repository.getTransactionCountForCategory("Pet Grooming"))
        assertEquals(2, repository.getTransactionCountForCategory(ExpenseCategory.OTHER))
    }

    @Test
    fun testCannotDeleteDefaultCategory() = runBlocking {
        repository.ensureDefaultCategoriesSeeded()
        val deleteSuccess = repository.deleteCategory(ExpenseCategory.FOOD_DINING, ExpenseCategory.OTHER)
        assertFalse("Default system categories cannot be deleted", deleteSuccess)
        assertNotNull(fakeUserCategoryDao.getCategoryByName(ExpenseCategory.FOOD_DINING))
    }
}
