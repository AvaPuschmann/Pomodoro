package com.agenticfocus.viewmodel

import java.util.UUID

data class DayTask(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val plannedPomodoros: Int = 1,
    val completedPomodoros: Int = 0,
    val templateId: String? = null
)

data class DayPlannerState(
    val tasks: List<DayTask> = emptyList(),
    val activeTaskId: String? = null
)
