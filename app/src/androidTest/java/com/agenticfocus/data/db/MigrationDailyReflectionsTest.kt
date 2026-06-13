package com.agenticfocus.data.db

import android.content.ContentValues
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Story 24-1 / Sprint 22 / Epic 24 — Migration v18→v19 test.
 *
 * AC12 coverage (Party Mode 2026-05-21 décision Q-Murat) :
 * - Crée DB v18 avec données représentatives (au moins 1 day_task, 1 project, 1 tag)
 * - Lance MIGRATION_18_19
 * - Vérifie : table daily_reflections créée + schema correct + index unique présent + données préexistantes préservées
 *
 * **Empêche perte de data utilisateur réelle si migration foire** (risque haut identifié Murat).
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * PRÉ-REQUIS BUILD (à ajouter pour exécuter ce test) :
 *
 * 1. `gradle/libs.versions.toml` — ajouter :
 *    [versions]
 *    roomTesting = "2.6.1"
 *    testExtJunit = "1.1.5"
 *    testRunner = "1.5.2"
 *
 *    [libraries]
 *    androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "roomTesting" }
 *    androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "testExtJunit" }
 *    androidx-test-runner = { group = "androidx.test", name = "runner", version.ref = "testRunner" }
 *
 * 2. `app/build.gradle.kts` — ajouter :
 *    androidTestImplementation(libs.androidx.room.testing)
 *    androidTestImplementation(libs.androidx.test.ext.junit)
 *    androidTestImplementation(libs.androidx.test.runner)
 *
 *    // Plus, dans `android.defaultConfig` ou bloc kapt/ksp arguments :
 *    ksp { arg("room.schemaLocation", "$projectDir/schemas") }
 *
 * 3. `data/db/AppDatabase.kt` — passer `exportSchema = true` au lieu de `false`.
 *    (Note : ceci génère des fichiers JSON dans `app/schemas/` à committer).
 *
 * ⚠ Si ces 3 pré-requis ne sont pas en place, ce test ne compilera/exécutera pas.
 * À ajouter avant Sprint 22 closure ou marker @Ignore avec issue de suivi.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Run via : ./gradlew :app:connectedDebugAndroidTest --tests com.agenticfocus.data.db.MigrationDailyReflectionsTest
 */
@RunWith(AndroidJUnit4::class)
class MigrationDailyReflectionsTest {

    companion object {
        const val TEST_DB = "migration-test-daily-reflections"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrate18To19_creates_daily_reflections_table_preserves_existing_data() {
        // ─── 1. Create DB v18 with sample existing data ──────────────────────
        helper.createDatabase(TEST_DB, 18).apply {
            // Insert 1 day_task (representative existing data)
            execSQL(
                """
                INSERT INTO day_tasks (id, user_id, name, date, planned_pomodoros, completed_pomodoros, position, is_completed, created_at, updated_at)
                VALUES ('task-1', 'user-1', 'Pre-existing task', '2026-05-21', 1, 0, 0, 0, 1000, 1000)
                """.trimIndent()
            )

            // Insert 1 tag (table created in MIGRATION_17_18)
            execSQL(
                """
                INSERT INTO tags (id, user_id, name, color, position, created_at, updated_at)
                VALUES ('tag-1', 'user-1', 'Sport', '#4CAF50', 0, 1000, 1000)
                """.trimIndent()
            )

            close()
        }

        // ─── 2. Run MIGRATION_18_19 ───────────────────────────────────────────
        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB,
            19,
            /* validateDroppedTables= */ true,
            // Note: MIGRATION_18_19 is private in AppDatabase — for this test we
            // need it accessible. Either make it internal/public, or use
            // AppDatabase.getInstance(context).openHelper.writableDatabase to access.
            // Below assumes internal/public access is granted.
            // AppDatabase.MIGRATION_18_19 // expose if needed
        )

        // ─── 3. Verify daily_reflections table created with correct schema ───
        migratedDb.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='daily_reflections'"
        ).use { cursor ->
            assertTrue("daily_reflections table must exist post-migration", cursor.moveToFirst())
            assertEquals("daily_reflections", cursor.getString(0))
        }

        // ─── 4. Verify unique index on (user_id, period_key) ─────────────────
        migratedDb.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='index_daily_reflections_user_id_period_key'"
        ).use { cursor ->
            assertTrue("unique index must exist", cursor.moveToFirst())
        }

        // ─── 5. Verify 11 columns present (id + user_id + period_key + 2 narrative + 4 counts + 2 timestamps) ─
        migratedDb.query("PRAGMA table_info(daily_reflections)").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
            assertEquals(
                "Expected 11 columns",
                listOf(
                    "id", "user_id", "period_key",
                    "day_facts", "learning",
                    "day_facts_word_count", "day_facts_char_count",
                    "learning_word_count", "learning_char_count",
                    "created_at", "updated_at"
                ).sorted(),
                columns.sorted()
            )
        }

        // ─── 6. Verify pre-existing data preserved ───────────────────────────
        migratedDb.query("SELECT name FROM day_tasks WHERE id='task-1'").use { cursor ->
            assertTrue("Pre-existing day_task must survive migration", cursor.moveToFirst())
            assertEquals("Pre-existing task", cursor.getString(0))
        }
        migratedDb.query("SELECT name FROM tags WHERE id='tag-1'").use { cursor ->
            assertTrue("Pre-existing tag must survive migration", cursor.moveToFirst())
            assertEquals("Sport", cursor.getString(0))
        }

        // ─── 7. Verify daily_reflections INSERT works post-migration ─────────
        val cv = ContentValues().apply {
            put("id", "refl-1")
            put("user_id", "user-1")
            put("period_key", "2026-05-21")
            put("day_facts", "Migration test entry")
            put("learning", "Migrations don't have to be scary")
            put("day_facts_word_count", 3)
            put("day_facts_char_count", 20)
            put("learning_word_count", 6)
            put("learning_char_count", 32)
            put("created_at", 2000L)
            put("updated_at", 2000L)
        }
        val rowId = migratedDb.insert("daily_reflections", android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL, cv)
        assertTrue("Insert into migrated table must succeed", rowId > 0)
    }
}
