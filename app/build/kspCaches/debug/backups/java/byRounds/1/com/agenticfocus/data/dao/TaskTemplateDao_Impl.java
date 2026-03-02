package com.agenticfocus.data.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.agenticfocus.data.entity.TaskTemplateEntity;
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
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TaskTemplateDao_Impl implements TaskTemplateDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TaskTemplateEntity> __insertionAdapterOfTaskTemplateEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public TaskTemplateDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTaskTemplateEntity = new EntityInsertionAdapter<TaskTemplateEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `task_templates` (`id`,`title`,`note`,`domainId`,`storyPoints`,`defaultPomodoros`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TaskTemplateEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        if (entity.getNote() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getNote());
        }
        statement.bindString(4, entity.getDomainId());
        statement.bindLong(5, entity.getStoryPoints());
        statement.bindLong(6, entity.getDefaultPomodoros());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM task_templates WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final TaskTemplateEntity template,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTaskTemplateEntity.insert(template);
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
  public Flow<List<TaskTemplateEntity>> observeAll() {
    final String _sql = "SELECT * FROM task_templates ORDER BY title ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"task_templates"}, new Callable<List<TaskTemplateEntity>>() {
      @Override
      @NonNull
      public List<TaskTemplateEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfDomainId = CursorUtil.getColumnIndexOrThrow(_cursor, "domainId");
          final int _cursorIndexOfStoryPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "storyPoints");
          final int _cursorIndexOfDefaultPomodoros = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultPomodoros");
          final List<TaskTemplateEntity> _result = new ArrayList<TaskTemplateEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskTemplateEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final String _tmpDomainId;
            _tmpDomainId = _cursor.getString(_cursorIndexOfDomainId);
            final int _tmpStoryPoints;
            _tmpStoryPoints = _cursor.getInt(_cursorIndexOfStoryPoints);
            final int _tmpDefaultPomodoros;
            _tmpDefaultPomodoros = _cursor.getInt(_cursorIndexOfDefaultPomodoros);
            _item = new TaskTemplateEntity(_tmpId,_tmpTitle,_tmpNote,_tmpDomainId,_tmpStoryPoints,_tmpDefaultPomodoros);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
