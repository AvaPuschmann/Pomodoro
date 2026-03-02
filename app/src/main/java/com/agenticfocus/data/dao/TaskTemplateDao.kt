package com.agenticfocus.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agenticfocus.data.entity.TaskTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskTemplateDao {

    @Query("SELECT * FROM task_templates ORDER BY title ASC")
    fun observeAll(): Flow<List<TaskTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: TaskTemplateEntity)

    @Query("DELETE FROM task_templates WHERE id = :id")
    suspend fun deleteById(id: String)
}
