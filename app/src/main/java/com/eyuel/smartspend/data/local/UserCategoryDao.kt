package com.eyuel.smartspend.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserCategoryDao {

    @Query("SELECT * FROM user_categories ORDER BY isDefault DESC, createdAt ASC")
    fun getAllCategories(): Flow<List<UserCategoryEntity>>

    @Query("SELECT * FROM user_categories ORDER BY isDefault DESC, createdAt ASC")
    suspend fun getAllCategoriesSync(): List<UserCategoryEntity>

    @Query("SELECT COUNT(*) FROM user_categories")
    suspend fun getCategoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: UserCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<UserCategoryEntity>)

    @Query("DELETE FROM user_categories WHERE name = :name AND isDefault = 0")
    suspend fun deleteCategory(name: String): Int

    @Query("SELECT * FROM user_categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): UserCategoryEntity?
}
