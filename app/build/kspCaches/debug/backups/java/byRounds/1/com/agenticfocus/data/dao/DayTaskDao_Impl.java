package com.agenticfocus.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityUpsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.agenticfocus.data.entity.DayTaskEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DayTaskDao_Impl implements DayTaskDao {
  private final RoomDatabase __db;

  private final EntityDeletionOrUpdateAdapter<DayTaskEntity> __deletionAdapterOfDayTaskEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final EntityUpsertionAdapter<DayTaskEntity> __upsertionAdapterOfDayTaskEntity;

  public DayTaskDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__deletionAdapterOfDayTaskEntity = new EntityDeletionOrUpdateAdapter<DayTaskEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `day_tasks` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DayTaskEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM day_tasks WHERE id = ?";
        return _query;
      }
    };
    this.__upsertionAdapterOfDayTaskEntity = new EntityUpsertionAdapter<DayTaskEntity>(new EntityInsertionAdapter<DayTaskEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `day_tasks` (`id`,`date`,`name`,`plannedPomodoros`,`completedPomodoros`,`position`,`templateId`,`createdAt`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DayTaskEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getDate());
        statement.bindString(3, entity.getName());
        statement.bindLong(4, entity.getPlannedPomodoros());
        statement.bindLong(5, entity.getCompletedPomodoros());
        statement.bindLong(6, entity.getPosition());
        if (entity.getTemplateId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getTemplateId());
        }
        statement.bindLong(8, entity.getCreatedAt());
      }
    }, new EntityDeletionOrUpdateAdapter<DayTaskEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `day_tasks` SET `id` = ?,`date` = ?,`name` = ?,`plannedPomodoros` = ?,`completedPomodoros` = ?,`position` = ?,`templateId` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DayTaskEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getDate());
        statement.bindString(3, entity.getName());
        statement.bindLong(4, entity.getPlannedPomodoros());
        statement.bindLong(5, entity.getCompletedPomodoros());
        statement.bindLong(6, entity.getPosition());
        if (entity.getTemplateId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getTemplateId());
        }
        statement.bindLong(8, entity.getCreatedAt());
        statement.bindString(9, entity.getId());
      }
    });
  }

  @Override
  public Object delete(final DayTaskEntity task, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfDayTaskEntity.handle(task);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<DayTaskEntity> tasks,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfDayTaskEntity.upsert(tasks);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getTasksForDate(final String date,
      final Continuation<? super List<DayTaskEntity>> $completion) {
    final String _sql = "SELECT * FROM day_tasks WHERE date = ? ORDER BY position ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DayTaskEntity>>() {
      @Override
      @NonNull
      public List<DayTaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPlannedPomodoros = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedPomodoros");
          final int _cursorIndexOfCompletedPomodoros = CursorUtil.getColumnIndexOrThrow(_cursor, "completedPomodoros");
          final int _cursorIndexOfPosition = CursorUtil.getColumnIndexOrThrow(_cursor, "position");
          final int _cursorIndexOfTemplateId = CursorUtil.getColumnIndexOrThrow(_cursor, "templateId");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<DayTaskEntity> _result = new ArrayList<DayTaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DayTaskEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpPlannedPomodoros;
            _tmpPlannedPomodoros = _cursor.getInt(_cursorIndexOfPlannedPomodoros);
            final int _tmpCompletedPomodoros;
            _tmpCompletedPomodoros = _cursor.getInt(_cursorIndexOfCompletedPomodoros);
            final int _tmpPosition;
            _tmpPosition = _cursor.getInt(_cursorIndexOfPosition);
            final String _tmpTemplateId;
            if (_cursor.isNull(_cursorIndexOfTemplateId)) {
              _tmpTemplateId = null;
            } else {
              _tmpTemplateId = _cursor.getString(_cursorIndexOfTemplateId);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new DayTaskEntity(_tmpId,_tmpDate,_tmpName,_tmpPlannedPomodoros,_tmpCompletedPomodoros,_tmpPosition,_tmpTemplateId,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
