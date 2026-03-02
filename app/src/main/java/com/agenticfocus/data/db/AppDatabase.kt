package com.agenticfocus.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.agenticfocus.data.dao.DayTaskDao
import com.agenticfocus.data.dao.PomodoroSessionDao
import com.agenticfocus.data.entity.DayTaskEntity
import com.agenticfocus.data.entity.PomodoroSessionEntity

@Database(
    entities = [DayTaskEntity::class, PomodoroSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dayTaskDao(): DayTaskDao
    abstract fun pomodoroSessionDao(): PomodoroSessionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agenticfocus.db"
                ).build().also { INSTANCE = it }
            }
    }
}
