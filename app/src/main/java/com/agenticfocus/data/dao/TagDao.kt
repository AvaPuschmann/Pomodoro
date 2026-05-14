package com.agenticfocus.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.agenticfocus.data.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * Sprint 20 / Story 22-2.
 */
@Dao
interface TagDao {

    @Upsert
    suspend fun upsert(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tags")
    suspend fun deleteAll()

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: String): TagEntity?

    @Query("SELECT * FROM tags WHERE user_id = :userId ORDER BY position ASC, name ASC")
    fun observeAll(userId: String): Flow<List<TagEntity>>

    @Query("SELECT MAX(position) FROM tags WHERE user_id = :userId")
    suspend fun getMaxPosition(userId: String): Int?
}
