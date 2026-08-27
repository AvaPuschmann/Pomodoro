package com.agenticfocus.data.auth

import com.agenticfocus.data.dao.SyncQueueDao
import com.agenticfocus.data.entity.SyncQueueEntity
import com.agenticfocus.data.sync.SyncEngine
import com.agenticfocus.data.sync.SyncStatusManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Story 31-3 — invariants du mode local (hors ligne).
 *
 * Ces tests couvrent l'invariant qui coûte des DONNÉES si on le casse : `flush()` ne doit
 * jamais s'exécuter en mode local. Sur mobile c'est plus grave que sur desktop — `flush()`
 * PURGE les UPSERT arrivés à MAX_RETRIES (`dao.deleteById`), là où le desktop les conserve.
 * Laisser tourner le flush pendant une coupure supprimerait donc les modifications faites
 * hors ligne, au lieu de simplement bloquer la file.
 *
 * `StandaloneMode.init()` n'est volontairement PAS appelé ici : sans Context, les accès
 * SharedPreferences retournent null et le mode se comporte comme inactif — ce qui permet
 * de piloter `isActive` via enable/disable sans dépendre d'Android.
 */
class StandaloneModeTest {

    private lateinit var fakeDao: RecordingSyncQueueDao

    @Before
    fun setUp() {
        fakeDao = RecordingSyncQueueDao()
        SyncEngine.currentUserId = "user-123"
        SyncEngine.initialize(
            context = null,
            syncQueueDao = fakeDao,
            connectivityProvider = { true },
        )
        StandaloneMode.disable()
    }

    @After
    fun tearDown() {
        // État global : ne pas laisser fuir le mode local vers les autres tests.
        StandaloneMode.disable()
    }

    @Test
    fun `enable active le mode et memorise le user_id`() {
        StandaloneMode.enable("d9dd9a28-real-user")

        assertTrue(StandaloneMode.isActive)
        assertEquals("d9dd9a28-real-user", StandaloneMode.userId)
    }

    @Test
    fun `disable desactive le mode et vide le user_id`() {
        StandaloneMode.enable("d9dd9a28-real-user")
        StandaloneMode.disable()

        assertFalse(StandaloneMode.isActive)
        assertEquals("", StandaloneMode.userId)
    }

    @Test
    fun `flush ne touche PAS la file en mode local`() = runBlocking {
        fakeDao.insert(
            SyncQueueEntity(
                id = "q-1",
                entityType = "day_tasks",
                entityId = "task-x",
                operation = "UPSERT",
                payload = "{}",
                retryCount = 0,
            )
        )
        StandaloneMode.enable("user-123")

        SyncEngine.flush()

        assertEquals(1, fakeDao.getAll().size)
        assertEquals(0, fakeDao.deleteCallCount)
        // Assertion décisive : sans la garde, flush tenterait de pousser, échouerait et
        // incrémenterait retryCount. Un compteur resté à 0 prouve qu'aucune tentative n'a
        // eu lieu. Les deux assertions précédentes passaient MÊME SANS la garde (le mutation
        // test l'a montré) : elles ne prouvaient rien à elles seules.
        assertEquals(0, fakeDao.getAll().first().retryCount)
    }

    @Test
    fun `flush ne purge PAS un UPSERT bloque en mode local`() = runBlocking {
        // Hors mode local, un UPSERT à MAX_RETRIES est purgé (cf. SyncEngineTest).
        // En mode local il doit survivre : c'est une modification hors ligne de l'utilisateur.
        fakeDao.insert(
            SyncQueueEntity(
                id = "q-stuck",
                entityType = "day_tasks",
                entityId = "task-y",
                operation = "UPSERT",
                payload = "{}",
                retryCount = SyncEngine.MAX_RETRIES,
            )
        )
        StandaloneMode.enable("user-123")

        SyncEngine.flush()

        assertEquals(1, fakeDao.getAll().size)
        assertEquals(0, fakeDao.deleteCallCount)
    }

    @Test
    fun `flush repositionne le statut sur OFFLINE en mode local`() = runBlocking {
        // AgenticFocusApp.onAvailable appelle setSyncing() AVANT flush(). Un simple `return`
        // laisserait l'indicateur coincé sur SYNCING et il tournerait indéfiniment.
        StandaloneMode.enable("user-123")
        SyncStatusManager.setSyncing()

        SyncEngine.flush()

        assertEquals(SyncStatusManager.SyncStatus.OFFLINE, SyncStatusManager.status.value)
    }
}

/** Fake DAO comptant les suppressions, pour distinguer « rien à faire » de « purgé ». */
class RecordingSyncQueueDao : SyncQueueDao {
    private val store = mutableListOf<SyncQueueEntity>()
    var deleteCallCount = 0
        private set

    override suspend fun insert(entry: SyncQueueEntity) {
        store.removeIf { it.id == entry.id }
        store.add(entry)
    }

    override suspend fun getAll(): List<SyncQueueEntity> = store.sortedBy { it.createdAt }

    override suspend fun deleteById(id: String) {
        deleteCallCount++
        store.removeIf { it.id == id }
    }

    override suspend fun deletePendingUpserts(entityId: String, entityType: String) {
        deleteCallCount++
        store.removeIf {
            it.entityId == entityId && it.entityType == entityType && it.operation == "UPSERT"
        }
    }

    override suspend fun incrementRetry(id: String) {
        val i = store.indexOfFirst { it.id == id }
        if (i >= 0) store[i] = store[i].copy(retryCount = store[i].retryCount + 1)
    }

    override suspend fun deleteByEntityType(entityType: String) {
        store.removeIf { it.entityType == entityType }
    }

    override suspend fun deleteWhereRetryExceeds(maxRetry: Int) {
        deleteCallCount++
        store.removeIf { it.retryCount >= maxRetry }
    }
}
