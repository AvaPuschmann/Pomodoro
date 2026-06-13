package com.agenticfocus.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agenticfocus.data.db.AppDatabase
import com.agenticfocus.data.entity.DailyReflectionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Story 24-1 / Sprint 22 / Epic 24 — DAO test for daily_reflections.
 *
 * AC11 coverage (Party Mode 2026-05-21 décision Q-Murat) :
 * - upsert puis read → entry trouvée
 * - upsert deux fois avec même (user_id, period_key) → update pas duplication
 * - violation index unique (id différents même period_key) → exception attendue
 * - observeByPeriodKey émet sur changement
 *
 * Run via : ./gradlew :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class DailyReflectionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: DailyReflectionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dailyReflectionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun makeEntity(
        id: String = "id-1",
        userId: String = "user-1",
        periodKey: String = "2026-05-21",
        dayFacts: String = "Test day facts",
        learning: String = "Test learning",
        dayFactsWordCount: Int = 3,
        dayFactsCharCount: Int = 14,
        learningWordCount: Int = 2,
        learningCharCount: Int = 13,
        createdAt: Long = 1_000L,
        updatedAt: Long = 1_000L,
    ) = DailyReflectionEntity(
        id = id,
        userId = userId,
        periodKey = periodKey,
        dayFacts = dayFacts,
        learning = learning,
        dayFactsWordCount = dayFactsWordCount,
        dayFactsCharCount = dayFactsCharCount,
        learningWordCount = learningWordCount,
        learningCharCount = learningCharCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    @Test
    fun upsert_then_read_returns_entry() = runBlocking {
        val entity = makeEntity()
        dao.upsert(entity)

        val read = dao.getByPeriodKey(entity.userId, entity.periodKey)

        assertNotNull(read)
        assertEquals(entity, read)
    }

    @Test
    fun upsert_twice_same_id_user_period_updates_not_duplicates() = runBlocking {
        val original = makeEntity(dayFacts = "First", updatedAt = 1_000L)
        dao.upsert(original)

        val updated = original.copy(dayFacts = "Second", updatedAt = 2_000L)
        dao.upsert(updated)

        val read = dao.getByPeriodKey(original.userId, original.periodKey)
        assertNotNull(read)
        assertEquals("Second", read?.dayFacts)
        assertEquals(2_000L, read?.updatedAt)

        // Confirm only 1 row in DB for this period (no duplication)
        val allForUser = dao.observeAllForUser(original.userId).first()
        assertEquals(1, allForUser.size)
    }

    @Test
    fun unique_index_violation_when_different_ids_same_user_period_throws() = runBlocking {
        val first = makeEntity(id = "id-A")
        dao.upsert(first)

        // Same (user_id, period_key), different id → must violate unique index
        val collision = makeEntity(id = "id-B")
        try {
            dao.upsert(collision)
            fail("Expected unique index violation (user_id, period_key) — applyRemoteUpsert must handle this case")
        } catch (e: Exception) {
            assertTrue(
                "Expected SQLiteConstraintException-like, got: ${e::class.java.simpleName}: ${e.message}",
                e.message?.contains("UNIQUE", ignoreCase = true) == true ||
                    e::class.java.simpleName.contains("Constraint")
            )
        }
    }

    @Test
    fun observeByPeriodKey_emits_on_insert_and_update() = runBlocking {
        val entity = makeEntity()

        // No entry yet → first emission should be null
        val initial = dao.observeByPeriodKey(entity.userId, entity.periodKey).first()
        assertNull(initial)

        // After upsert → entry present
        dao.upsert(entity)
        val afterInsert = dao.observeByPeriodKey(entity.userId, entity.periodKey).first()
        assertNotNull(afterInsert)
        assertEquals(entity.dayFacts, afterInsert?.dayFacts)

        // After update → reflects new value
        dao.upsert(entity.copy(dayFacts = "Updated"))
        val afterUpdate = dao.observeByPeriodKey(entity.userId, entity.periodKey).first()
        assertEquals("Updated", afterUpdate?.dayFacts)
    }

    @Test
    fun observeAllForUser_returns_entries_ordered_period_key_desc() = runBlocking {
        dao.upsert(makeEntity(id = "id-1", periodKey = "2026-05-19"))
        dao.upsert(makeEntity(id = "id-2", periodKey = "2026-05-21"))
        dao.upsert(makeEntity(id = "id-3", periodKey = "2026-05-20"))

        val all = dao.observeAllForUser("user-1").first()

        assertEquals(3, all.size)
        assertEquals("2026-05-21", all[0].periodKey)
        assertEquals("2026-05-20", all[1].periodKey)
        assertEquals("2026-05-19", all[2].periodKey)
    }

    @Test
    fun deleteById_removes_entry() = runBlocking {
        val entity = makeEntity()
        dao.upsert(entity)
        assertNotNull(dao.getByPeriodKey(entity.userId, entity.periodKey))

        dao.deleteById(entity.id)

        assertNull(dao.getByPeriodKey(entity.userId, entity.periodKey))
    }
}
