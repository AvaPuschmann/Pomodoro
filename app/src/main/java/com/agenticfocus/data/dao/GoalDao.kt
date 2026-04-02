package com.agenticfocus.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.agenticfocus.data.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Upsert
    suspend fun upsert(goal: GoalEntity)

    @Upsert
    suspend fun upsertAll(goals: List<GoalEntity>)

    @Query("SELECT * FROM goals ORDER BY type ASC, period_key DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE type = :type AND period_key = :periodKey LIMIT 1")
    suspend fun getByTypePeriod(type: String, periodKey: String): GoalEntity?

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: String)
}
