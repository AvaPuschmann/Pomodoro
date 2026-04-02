package com.agenticfocus.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.agenticfocus.data.entity.TaskTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskTemplateDao {

    @Query("SELECT * FROM task_templates ORDER BY title ASC")
    fun observeAll(): Flow<List<TaskTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: TaskTemplateEntity)

    @Update
    suspend fun update(template: TaskTemplateEntity)

    @Query("SELECT updated_at FROM task_templates WHERE id = :id")
    suspend fun getUpdatedAtById(id: String): Long?

    @Query("DELETE FROM task_templates WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM task_templates WHERE domain_id = :domainId")
    suspend fun deleteByDomainId(domainId: String)

    @Query("DELETE FROM task_templates")
    suspend fun deleteAll()
}
