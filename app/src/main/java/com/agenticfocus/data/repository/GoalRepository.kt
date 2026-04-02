package com.agenticfocus.data.repository

import com.agenticfocus.data.dao.GoalDao
import com.agenticfocus.data.entity.GoalEntity
import com.agenticfocus.data.sync.SyncEngine
import kotlinx.coroutines.flow.Flow

class GoalRepository(private val goalDao: GoalDao) {

    fun observeAll(): Flow<List<GoalEntity>> = goalDao.observeAll()

    suspend fun save(goal: GoalEntity) {
        goalDao.upsert(goal)
        try { SyncEngine.upsertGoal(goal) } catch (_: Exception) {}
    }

    suspend fun saveAll(goals: List<GoalEntity>) {
        goalDao.upsertAll(goals)
    }

    suspend fun delete(id: String) {
        goalDao.deleteById(id)
        try { SyncEngine.deleteGoal(id) } catch (_: Exception) {}
    }
}
