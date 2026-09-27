package com.eyuel.smartspend.data.local

import androidx.room.*
import com.eyuel.smartspend.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

data class CategorySpending(
    val category: String,
    val totalAmount: Double
)

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isReviewed = 0 ORDER BY timestamp DESC")
    fun getUnreviewedTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE isReviewed = 0")
    fun getUnreviewedCount(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Long): Flow<TransactionEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllOrIgnore(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET description = :description, category = :category, isReviewed = :isReviewed WHERE id = :id")
    suspend fun updateNoteAndCategory(id: Long, description: String, category: String, isReviewed: Boolean = true)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE referenceId = :referenceId LIMIT 1)")
    suspend fun hasTransactionWithReference(referenceId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE senderAddress = :sender AND ABS(amount - :amount) < 0.001 AND timestamp = :timestamp LIMIT 1)")
    suspend fun hasMatchingTransaction(sender: String, amount: Double, timestamp: Long): Boolean

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT'")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT'")
    fun getTotalExpenses(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT' AND timestamp BETWEEN :startMillis AND :endMillis")
    fun getIncomeInRange(startMillis: Long, endMillis: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT' AND timestamp BETWEEN :startMillis AND :endMillis")
    fun getExpensesInRange(startMillis: Long, endMillis: Long): Flow<Double?>

    @Query("SELECT category, SUM(amount) AS totalAmount FROM transactions WHERE type = 'DEBIT' AND timestamp BETWEEN :startMillis AND :endMillis GROUP BY category ORDER BY totalAmount DESC")
    fun getCategorySpending(startMillis: Long, endMillis: Long): Flow<List<CategorySpending>>
}
