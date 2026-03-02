package com.agenticfocus.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.agenticfocus.data.dao.DayTaskDao
import com.agenticfocus.data.dao.DomainDao
import com.agenticfocus.data.dao.PomodoroSessionDao
import com.agenticfocus.data.dao.TaskTemplateDao
import com.agenticfocus.data.entity.DayTaskEntity
import com.agenticfocus.data.entity.DomainEntity
import com.agenticfocus.data.entity.PomodoroSessionEntity
import com.agenticfocus.data.entity.TaskTemplateEntity

@Database(
    entities = [
        DayTaskEntity::class,
        PomodoroSessionEntity::class,
        DomainEntity::class,
        TaskTemplateEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dayTaskDao(): DayTaskDao
    abstract fun pomodoroSessionDao(): PomodoroSessionDao
    abstract fun domainDao(): DomainDao
    abstract fun taskTemplateDao(): TaskTemplateDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `domains`
                    (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `color` TEXT NOT NULL,
                     PRIMARY KEY(`id`))"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `task_templates`
                    (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `note` TEXT,
                     `domainId` TEXT NOT NULL, `storyPoints` INTEGER NOT NULL,
                     `defaultPomodoros` INTEGER NOT NULL, PRIMARY KEY(`id`))"""
                )
                db.execSQL("ALTER TABLE `day_tasks` ADD COLUMN `templateId` TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agenticfocus.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
