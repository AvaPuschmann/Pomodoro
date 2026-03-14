package com.agenticfocus.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.DBUtil;
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
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class StatsDao_Impl implements StatsDao {
  private final RoomDatabase __db;

  public StatsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
  }

  @Override
  public Object getDailyStats(final String fromDate,
      final Continuation<? super List<DailyStats>> $completion) {
    final String _sql = "\n"
            + "        SELECT dt.date as date,\n"
            + "               SUM(dt.completedPomodoros) as totalPomodoros,\n"
            + "               SUM(CASE WHEN dt.completedPomodoros > 0\n"
            + "                        THEN COALESCE(tt.storyPoints, 0)\n"
            + "                        ELSE 0 END) as totalPoints\n"
            + "        FROM day_tasks dt\n"
            + "        LEFT JOIN task_templates tt ON dt.templateId = tt.id\n"
            + "        WHERE dt.date >= ?\n"
            + "        GROUP BY dt.date\n"
            + "        ORDER BY dt.date ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, fromDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DailyStats>>() {
      @Override
      @NonNull
      public List<DailyStats> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = 0;
          final int _cursorIndexOfTotalPomodoros = 1;
          final int _cursorIndexOfTotalPoints = 2;
          final List<DailyStats> _result = new ArrayList<DailyStats>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyStats _item;
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpTotalPomodoros;
            _tmpTotalPomodoros = _cursor.getInt(_cursorIndexOfTotalPomodoros);
            final int _tmpTotalPoints;
            _tmpTotalPoints = _cursor.getInt(_cursorIndexOfTotalPoints);
            _item = new DailyStats(_tmpDate,_tmpTotalPomodoros,_tmpTotalPoints);
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
