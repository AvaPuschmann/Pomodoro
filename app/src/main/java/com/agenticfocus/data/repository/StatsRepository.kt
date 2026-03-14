package com.agenticfocus.data.repository

import com.agenticfocus.data.dao.StatsDao
import com.agenticfocus.viewmodel.DailyStatsPoint
import com.agenticfocus.viewmodel.StatsPeriod
import java.time.DayOfWeek
import java.time.LocalDate

class StatsRepository(private val dao: StatsDao) {

    suspend fun getStats(period: StatsPeriod): List<DailyStatsPoint> {
        val today = LocalDate.now()
        val startDate = today.minusDays((period.days - 1).toLong())
        val fromDate = startDate.toString()

        val rawStats = dao.getDailyStats(fromDate).associateBy { it.date }

        return (0 until period.days).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            val stat = rawStats[date.toString()]

            DailyStatsPoint(
                date = date.toString(),
                shortLabel = date.toShortLabel(period),
                pomodoros = stat?.totalPomodoros ?: 0,
                points = stat?.totalPoints ?: 0
            )
        }
    }
}

private fun LocalDate.toShortLabel(period: StatsPeriod): String = when (period) {
    StatsPeriod.WEEK -> dayOfWeek.frenchAbbrev()
    StatsPeriod.MONTH -> if (dayOfMonth == 1 || dayOfMonth % 7 == 1) dayOfMonth.toString() else ""
}

private fun DayOfWeek.frenchAbbrev(): String = when (this) {
    DayOfWeek.MONDAY    -> "L"
    DayOfWeek.TUESDAY   -> "M"
    DayOfWeek.WEDNESDAY -> "M"
    DayOfWeek.THURSDAY  -> "J"
    DayOfWeek.FRIDAY    -> "V"
    DayOfWeek.SATURDAY  -> "S"
    DayOfWeek.SUNDAY    -> "D"
}
