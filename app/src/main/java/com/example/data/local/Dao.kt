package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChandelierDao {
    @Query("SELECT * FROM chandeliers ORDER BY timestamp DESC")
    fun getAllCached(): Flow<List<ChandelierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chandelier: ChandelierEntity)

    @Query("DELETE FROM chandeliers")
    suspend fun clearAll()
}

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY timestamp DESC")
    fun getAllCached(): Flow<List<ArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: ArticleEntity)

    @Query("DELETE FROM articles")
    suspend fun clearAll()
}

@Dao
interface CustomDesignRequestDao {
    @Query("SELECT * FROM custom_design_requests ORDER BY timestamp DESC")
    fun getAllRequests(): Flow<List<CustomDesignRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: CustomDesignRequestEntity)

    @Query("DELETE FROM custom_design_requests WHERE id = :id")
    suspend fun deleteRequestById(id: Int)
}
