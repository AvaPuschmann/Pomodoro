package com.agenticfocus.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.agenticfocus.data.dao.DayTaskDao
import com.agenticfocus.data.dao.DomainDao
import com.agenticfocus.data.dao.GoalDao
import com.agenticfocus.data.dao.PomodoroSessionDao
import com.agenticfocus.data.dao.RoutineDao
import com.agenticfocus.data.dao.StatsDao
import com.agenticfocus.data.dao.SubtaskDao
import com.agenticfocus.data.dao.SyncQueueDao
import com.agenticfocus.data.dao.TaskTemplateDao
import com.agenticfocus.data.entity.DayTaskEntity
import com.agenticfocus.data.entity.DomainEntity
import com.agenticfocus.data.entity.GoalEntity
import com.agenticfocus.data.entity.PomodoroSessionEntity
import com.agenticfocus.data.entity.ProjectEntity
import com.agenticfocus.data.entity.RoutineEntity
import com.agenticfocus.data.entity.RoutineItemEntity
import com.agenticfocus.data.entity.SubtaskEntity
import com.agenticfocus.data.entity.SyncQueueEntity
import com.agenticfocus.data.entity.TaskTemplateEntity

@Database(
    entities = [
        DayTaskEntity::class,
        PomodoroSessionEntity::class,
        DomainEntity::class,
        TaskTemplateEntity::class,
        SyncQueueEntity::class,
        SubtaskEntity::class,
        GoalEntity::class,
        RoutineEntity::class,
        RoutineItemEntity::class,
        ProjectEntity::class
    ],
    version = 16,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dayTaskDao(): DayTaskDao
    abstract fun pomodoroSessionDao(): PomodoroSessionDao
    abstract fun domainDao(): DomainDao
    abstract fun taskTemplateDao(): TaskTemplateDao
    abstract fun statsDao(): StatsDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun goalDao(): GoalDao
    abstract fun routineDao(): RoutineDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // ── day_tasks ─────────────────────────────────────────────
                db.execSQL("""
                    CREATE TABLE day_tasks_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        date TEXT NOT NULL,
                        name TEXT NOT NULL,
                        planned_pomodoros INTEGER NOT NULL,
                        completed_pomodoros INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        template_id TEXT,
                        created_at INTEGER NOT NULL DEFAULT 0,
                        user_id TEXT NOT NULL DEFAULT '',
                        updated_at INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("""
                    INSERT INTO day_tasks_new
                        (id, date, name, planned_pomodoros, completed_pomodoros, position,
                         template_id, created_at, user_id, updated_at)
                    SELECT id, date, name, plannedPomodoros, completedPomodoros, position,
                           templateId, createdAt, '', 0
                    FROM day_tasks
                """)
                db.execSQL("DROP TABLE day_tasks")
                db.execSQL("ALTER TABLE day_tasks_new RENAME TO day_tasks")

                // ── pomodoro_sessions ──────────────────────────────────────
                // id changes from INTEGER AUTOINCREMENT to TEXT UUID
                db.execSQL("""
                    CREATE TABLE pomodoro_sessions_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        day_task_id TEXT NOT NULL,
                        date TEXT NOT NULL,
                        start_time INTEGER NOT NULL,
                        end_time INTEGER NOT NULL,
                        duration_minutes INTEGER NOT NULL DEFAULT 25,
                        user_id TEXT NOT NULL DEFAULT '',
                        updated_at INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("""
                    INSERT INTO pomodoro_sessions_new
                        (id, day_task_id, date, start_time, end_time, duration_minutes,
                         user_id, updated_at)
                    SELECT CAST(id AS TEXT), dayTaskId, date, startTime, endTime, durationMinutes,
                           '', 0
                    FROM pomodoro_sessions
                """)
                db.execSQL("DROP TABLE pomodoro_sessions")
                db.execSQL("ALTER TABLE pomodoro_sessions_new RENAME TO pomodoro_sessions")

                // ── domains ────────────────────────────────────────────────
                // Only ADD COLUMN — no column renames needed for domains
                db.execSQL("ALTER TABLE domains ADD COLUMN user_id TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE domains ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")

                // ── task_templates ─────────────────────────────────────────
                db.execSQL("""
                    CREATE TABLE task_templates_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        note TEXT,
                        domain_id TEXT NOT NULL,
                        story_points INTEGER NOT NULL,
                        default_pomodoros INTEGER NOT NULL,
                        user_id TEXT NOT NULL DEFAULT '',
                        updated_at INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("""
                    INSERT INTO task_templates_new
                        (id, title, note, domain_id, story_points, default_pomodoros,
                         user_id, updated_at)
                    SELECT id, title, note, domainId, storyPoints, defaultPomodoros,
                           '', 0
                    FROM task_templates
                """)
                db.execSQL("DROP TABLE task_templates")
                db.execSQL("ALTER TABLE task_templates_new RENAME TO task_templates")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_queue (
                        id TEXT NOT NULL PRIMARY KEY,
                        entity_type TEXT NOT NULL,
                        entity_id TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        created_at INTEGER NOT NULL DEFAULT 0,
                        retry_count INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_tasks ADD COLUMN impact TEXT")
                db.execSQL("ALTER TABLE day_tasks ADD COLUMN urgency TEXT")
                db.execSQL("ALTER TABLE day_tasks ADD COLUMN due_date INTEGER")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE task_templates ADD COLUMN impact TEXT")
                db.execSQL("ALTER TABLE task_templates ADD COLUMN urgency TEXT")
                db.execSQL("ALTER TABLE task_templates ADD COLUMN due_date INTEGER")
                db.execSQL("ALTER TABLE day_tasks ADD COLUMN note TEXT")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_tasks ADD COLUMN is_completed INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS subtasks (
                        id TEXT NOT NULL PRIMARY KEY,
                        user_id TEXT NOT NULL DEFAULT '',
                        task_id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        is_completed INTEGER NOT NULL DEFAULT 0,
                        position INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL DEFAULT 0,
                        updated_at INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_subtasks_task_id ON subtasks(task_id)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_tasks ADD COLUMN domain_id TEXT")
                db.execSQL("ALTER TABLE day_tasks ADD COLUMN story_points INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_tasks ADD COLUMN source TEXT DEFAULT NULL")
            }
        }

        // No-op migration: schema SQL unchanged from v13. The version bump exists
        // only to refresh Room's identity hash after we added @ColumnInfo(defaultValue)
        // and @Entity(indices=…) on DayTaskEntity to mirror what previous migrations
        // had already created in SQL. Without the bump, Room sees the old hash in
        // room_master_table vs the new entity-derived hash → IllegalStateException
        // "Room cannot verify the data integrity" on devices with the v13 DB.
        // Fresh installs are unaffected (DB is created from current entity).
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Intentionally empty.
            }
        }

        // Recreate idx_day_tasks_routine_item_date WITHOUT the partial WHERE clause.
        // MIGRATION_12_13 created it as a partial index:
        //   CREATE UNIQUE INDEX … WHERE routine_item_id IS NOT NULL
        // Room.TableInfo.read() inconsistently reads partial indices across devices —
        // on some it sees the index, on others it returns indices=[]. The result is
        // either Expected=[] vs Found=[index] OR Expected=[index] vs Found=[] →
        // "Migration didn't properly handle: day_tasks" crash on one or the other.
        // Functional behavior is unchanged: SQLite UNIQUE indexes already treat NULL
        // values as distinct, so the WHERE clause was effectively redundant.
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS idx_day_tasks_routine_item_date")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS idx_day_tasks_routine_item_date " +
                        "ON day_tasks(routine_item_id, date)"
                )
            }
        }

        // Mode Projet — Story 16-4 / Sprint 16.
        // Creates projects table (mirror Supabase + SQLite desktop V13) and adds
        // day_tasks.project_id with matching Index [D26/F5].
        // CHECK constraint on kanban_status is NOT declared here (Room can't validate
        // CHECK constraints in @Entity) — runtime validation done in KanbanStatus
        // sealed object (Story 17-1).
        // No FK locally (cohérent autres tables ; cascade SET NULL = server-side
        // via Postgres FK ON DELETE SET NULL — Story 16-2 / D46).
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS projects (
                        id TEXT NOT NULL PRIMARY KEY,
                        user_id TEXT NOT NULL DEFAULT '',
                        name TEXT NOT NULL DEFAULT '',
                        description TEXT,
                        kanban_status TEXT NOT NULL DEFAULT 'backlog',
                        kanban_position REAL NOT NULL DEFAULT 0,
                        target_date INTEGER,
                        domain_id TEXT,
                        is_archived INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL DEFAULT 0,
                        updated_at INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_projects_user_id ON projects(user_id)")
                db.execSQL("ALTER TABLE day_tasks ADD COLUMN project_id TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_day_tasks_project_id ON day_tasks(project_id)")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS routines (
                        id TEXT NOT NULL PRIMARY KEY,
                        user_id TEXT NOT NULL DEFAULT '',
                        type TEXT NOT NULL DEFAULT '',
                        name TEXT NOT NULL DEFAULT '',
                        trigger_time TEXT NOT NULL DEFAULT '06:00',
                        is_active INTEGER NOT NULL DEFAULT 1,
                        streak_count INTEGER NOT NULL DEFAULT 0,
                        streak_last_completed_date TEXT,
                        last_injected_date TEXT,
                        created_at INTEGER NOT NULL DEFAULT 0,
                        updated_at INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_routines_user_id_type ON routines(user_id, type)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS routine_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        routine_id TEXT NOT NULL,
                        template_id TEXT,
                        snapshot_title TEXT NOT NULL DEFAULT '',
                        snapshot_domain_id TEXT,
                        snapshot_story_points INTEGER NOT NULL DEFAULT 0,
                        override_pomodoros INTEGER,
                        is_check_only INTEGER NOT NULL DEFAULT 0,
                        position INTEGER NOT NULL DEFAULT 0,
                        user_id TEXT NOT NULL DEFAULT '',
                        created_at INTEGER NOT NULL DEFAULT 0,
                        updated_at INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (routine_id) REFERENCES routines(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_items_routine_id_position ON routine_items(routine_id, position)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_items_template_id ON routine_items(template_id)")
                db.execSQL("ALTER TABLE day_tasks ADD COLUMN routine_item_id TEXT DEFAULT NULL")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_day_tasks_routine_item_date ON day_tasks(routine_item_id, date) WHERE routine_item_id IS NOT NULL")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS goals (
                        id TEXT NOT NULL PRIMARY KEY,
                        user_id TEXT NOT NULL DEFAULT '',
                        type TEXT NOT NULL DEFAULT '',
                        period_key TEXT NOT NULL DEFAULT '',
                        text TEXT NOT NULL DEFAULT '',
                        is_completed INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL DEFAULT 0,
                        updated_at INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_goals_user_type_period ON goals(user_id, type, period_key)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Make date nullable to support backlog tasks (date = NULL means no scheduled day)
                db.execSQL("""
                    CREATE TABLE day_tasks_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        date TEXT,
                        name TEXT NOT NULL,
                        planned_pomodoros INTEGER NOT NULL,
                        completed_pomodoros INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        template_id TEXT,
                        created_at INTEGER NOT NULL DEFAULT 0,
                        user_id TEXT NOT NULL DEFAULT '',
                        updated_at INTEGER NOT NULL DEFAULT 0,
                        impact TEXT,
                        urgency TEXT,
                        due_date INTEGER,
                        note TEXT,
                        is_completed INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("""
                    INSERT INTO day_tasks_new
                        (id, date, name, planned_pomodoros, completed_pomodoros, position,
                         template_id, created_at, user_id, updated_at, impact, urgency,
                         due_date, note, is_completed)
                    SELECT id, date, name, planned_pomodoros, completed_pomodoros, position,
                           template_id, created_at, user_id, updated_at, impact, urgency,
                           due_date, note, is_completed
                    FROM day_tasks
                """)
                db.execSQL("DROP TABLE day_tasks")
                db.execSQL("ALTER TABLE day_tasks_new RENAME TO day_tasks")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agenticfocus.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
