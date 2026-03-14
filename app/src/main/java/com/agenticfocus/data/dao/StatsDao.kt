package com.agenticfocus.data.dao

import androidx.room.Dao
import androidx.room.Query

data class DailyStats(
    val date: String,
    val totalPomodoros: Int,
    val totalPoints: Int
)

@Dao
interface StatsDao {

    @Query("""
        SELECT dt.date as date,
               SUM(dt.completedPomodoros) as totalPomodoros,
               SUM(CASE WHEN dt.completedPomodoros > 0
                        THEN COALESCE(tt.storyPoints, 0)
                        ELSE 0 END) as totalPoints
        FROM day_tasks dt
        LEFT JOIN task_templates tt ON dt.templateId = tt.id
        WHERE dt.date >= :fromDate
        GROUP BY dt.date
        ORDER BY dt.date ASC
    """)
    suspend fun getDailyStats(fromDate: String): List<DailyStats>
}
