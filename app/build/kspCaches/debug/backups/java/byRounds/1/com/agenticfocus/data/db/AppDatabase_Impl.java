package com.agenticfocus.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.agenticfocus.data.dao.DayTaskDao;
import com.agenticfocus.data.dao.DayTaskDao_Impl;
import com.agenticfocus.data.dao.PomodoroSessionDao;
import com.agenticfocus.data.dao.PomodoroSessionDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile DayTaskDao _dayTaskDao;

  private volatile PomodoroSessionDao _pomodoroSessionDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `day_tasks` (`id` TEXT NOT NULL, `date` TEXT NOT NULL, `name` TEXT NOT NULL, `plannedPomodoros` INTEGER NOT NULL, `completedPomodoros` INTEGER NOT NULL, `position` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `pomodoro_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dayTaskId` TEXT NOT NULL, `date` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '67a6f2825fdb5febca72f70a0a1887d1')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `day_tasks`");
        db.execSQL("DROP TABLE IF EXISTS `pomodoro_sessions`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsDayTasks = new HashMap<String, TableInfo.Column>(7);
        _columnsDayTasks.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDayTasks.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDayTasks.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDayTasks.put("plannedPomodoros", new TableInfo.Column("plannedPomodoros", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDayTasks.put("completedPomodoros", new TableInfo.Column("completedPomodoros", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDayTasks.put("position", new TableInfo.Column("position", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDayTasks.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDayTasks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDayTasks = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDayTasks = new TableInfo("day_tasks", _columnsDayTasks, _foreignKeysDayTasks, _indicesDayTasks);
        final TableInfo _existingDayTasks = TableInfo.read(db, "day_tasks");
        if (!_infoDayTasks.equals(_existingDayTasks)) {
          return new RoomOpenHelper.ValidationResult(false, "day_tasks(com.agenticfocus.data.entity.DayTaskEntity).\n"
                  + " Expected:\n" + _infoDayTasks + "\n"
                  + " Found:\n" + _existingDayTasks);
        }
        final HashMap<String, TableInfo.Column> _columnsPomodoroSessions = new HashMap<String, TableInfo.Column>(6);
        _columnsPomodoroSessions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPomodoroSessions.put("dayTaskId", new TableInfo.Column("dayTaskId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPomodoroSessions.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPomodoroSessions.put("startTime", new TableInfo.Column("startTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPomodoroSessions.put("endTime", new TableInfo.Column("endTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPomodoroSessions.put("durationMinutes", new TableInfo.Column("durationMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPomodoroSessions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPomodoroSessions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPomodoroSessions = new TableInfo("pomodoro_sessions", _columnsPomodoroSessions, _foreignKeysPomodoroSessions, _indicesPomodoroSessions);
        final TableInfo _existingPomodoroSessions = TableInfo.read(db, "pomodoro_sessions");
        if (!_infoPomodoroSessions.equals(_existingPomodoroSessions)) {
          return new RoomOpenHelper.ValidationResult(false, "pomodoro_sessions(com.agenticfocus.data.entity.PomodoroSessionEntity).\n"
                  + " Expected:\n" + _infoPomodoroSessions + "\n"
                  + " Found:\n" + _existingPomodoroSessions);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "67a6f2825fdb5febca72f70a0a1887d1", "cfd61a8cd59033e35d22adf054217914");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "day_tasks","pomodoro_sessions");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `day_tasks`");
      _db.execSQL("DELETE FROM `pomodoro_sessions`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(DayTaskDao.class, DayTaskDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PomodoroSessionDao.class, PomodoroSessionDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public DayTaskDao dayTaskDao() {
    if (_dayTaskDao != null) {
      return _dayTaskDao;
    } else {
      synchronized(this) {
        if(_dayTaskDao == null) {
          _dayTaskDao = new DayTaskDao_Impl(this);
        }
        return _dayTaskDao;
      }
    }
  }

  @Override
  public PomodoroSessionDao pomodoroSessionDao() {
    if (_pomodoroSessionDao != null) {
      return _pomodoroSessionDao;
    } else {
      synchronized(this) {
        if(_pomodoroSessionDao == null) {
          _pomodoroSessionDao = new PomodoroSessionDao_Impl(this);
        }
        return _pomodoroSessionDao;
      }
    }
  }
}
