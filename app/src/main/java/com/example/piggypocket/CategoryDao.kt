package com.example.piggypocket

import androidx.room.*

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId")
    suspend fun getAllCategories(userId: Int): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): Category?

    @Query("SELECT * FROM categories WHERE name = :name")
    suspend fun getCategoryByName(name: String): Category?

    @Insert
    suspend fun insert(category: Category)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)
}