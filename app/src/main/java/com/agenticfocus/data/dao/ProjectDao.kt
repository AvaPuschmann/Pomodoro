package com.agenticfocus.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.agenticfocus.data.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Upsert
    suspend fun upsert(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM projects")
    suspend fun deleteAll()

    @Query("""
        SELECT * FROM projects
        WHERE user_id = :userId AND is_archived = 0
        ORDER BY kanban_status, kanban_position
    """)
    fun observeActive(userId: String): Flow<List<ProjectEntity>>

    @Query("""
        SELECT * FROM projects
        WHERE user_id = :userId
        ORDER BY kanban_status, kanban_position
    """)
    fun observeAll(userId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): ProjectEntity?

    @Query("""
        SELECT MAX(kanban_position) FROM projects
        WHERE user_id = :userId AND kanban_status = :status
    """)
    suspend fun getMaxPositionInColumn(userId: String, status: String): Double?

    // Tiebreaker stable [D51/F2] : kanban_position ASC, puis updated_at DESC
    // (récent en haut sur égalité), puis id ASC (déterministe absolu sur cas
    // extrême — toutes positions et timestamps identiques).
    @Query("""
        SELECT * FROM projects
        WHERE user_id = :userId AND kanban_status = :status AND is_archived = 0
        ORDER BY kanban_position ASC, updated_at DESC, id ASC
    """)
    suspend fun getByStatusOrdered(userId: String, status: String): List<ProjectEntity>
}
