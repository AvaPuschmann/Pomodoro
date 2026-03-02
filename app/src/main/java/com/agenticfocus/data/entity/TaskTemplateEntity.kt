package com.agenticfocus.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_templates")
data class TaskTemplateEntity(
    @PrimaryKey val id: String,
    val title: String,
    val note: String? = null,
    val domainId: String,
    val storyPoints: Int,
    val defaultPomodoros: Int
)
