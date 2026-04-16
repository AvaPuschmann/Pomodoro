package com.agenticfocus.viewmodel

import java.util.UUID

data class DayTask(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val plannedPomodoros: Int = 0,
    val completedPomodoros: Int = 0,
    val templateId: String? = null,
    val domainId: String? = null,
    val storyPoints: Int = 0,
    val impact: String? = null,       // "high" | "low" | null
    val urgency: String? = null,      // "urgent" | "not_urgent" | null
    val dueDate: Long? = null,        // Unix ms, null = no deadline
    val note: String? = null,
    val isCompleted: Boolean = false,
    val routineItemId: String? = null
)

data class DayPlannerState(
    val tasks: List<DayTask> = emptyList(),
    val activeTaskId: String? = null
)
