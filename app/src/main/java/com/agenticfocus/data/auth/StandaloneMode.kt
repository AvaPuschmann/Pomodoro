package com.agenticfocus.data.auth

import android.content.Context
import com.agenticfocus.data.db.AppDatabase
import com.agenticfocus.data.supabase.SupabaseClientProvider
import io.github.jan.supabase.gotrue.auth

/**
 * Mode standalone (hors ligne) — hotfix 2026-08-06, pendant du mode desktop.
 *
 * Quand Supabase est injoignable (quota egress dépassé, panne, avion), l'auth réseau est
 * impossible et l'app enferme l'utilisateur dehors alors que toutes ses données sont en
 * Room. Ce module permet d'ouvrir l'app avec le VRAI user_id, sans aucun appel réseau.
 *
 * Contrat : tant que [isActive] est vrai, RIEN ne doit partir sur le réseau —
 * ni Realtime, ni pullSync, ni flush. Les mutations continuent de s'empiler dans
 * `sync_queue` et partiront au prochain vrai signIn.
 */
object StandaloneMode {

    private const val PREFS_NAME = "agenticfocus_standalone"
    private const val KEY_USER_ID = "user_id"

    /**
     * applicationContext, posé une fois par AgenticFocusApp.onCreate().
     *
     * Story 31-3 : porté ici plutôt que dans le constructeur d'AuthViewModel. Un ViewModel
     * qui exige un Context n'est pas instanciable depuis un test JUnit pur, et l'ajouter
     * avait cassé la compilation des 8 cas d'AuthViewModelTest. Le tenir dans cet objet
     * garde le ViewModel testable sans introduire Robolectric ni framework de mock.
     *
     * Volontairement tolérant : si init() n'a pas été appelé, les méthodes se comportent
     * comme « pas de mode local » au lieu de lever — un oubli d'initialisation dégrade,
     * il ne crashe pas l'application au démarrage.
     */
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Lu depuis la coroutine de sync (Dispatchers.IO) et écrit depuis le thread UI.
     * Consulté par SyncEngine.flush() et RealtimeSyncManager avant tout appel réseau.
     */
    @Volatile
    var isActive: Boolean = false
        private set

    /** user_id réel utilisé pour estampiller les DTO mis en file pendant le mode local. */
    @Volatile
    var userId: String = ""
        private set

    /** Restaure le mode au démarrage. Retourne le user_id si le mode était actif. */
    fun restore(): String? {
        val stored = prefs()?.getString(KEY_USER_ID, null)
        if (stored.isNullOrBlank()) return null
        isActive = true
        userId = stored
        return stored
    }

    fun enable(resolvedUserId: String) {
        prefs()?.edit()?.putString(KEY_USER_ID, resolvedUserId)?.apply()
        isActive = true
        userId = resolvedUserId
    }

    fun disable() {
        prefs()?.edit()?.remove(KEY_USER_ID)?.apply()
        isActive = false
        userId = ""
    }

    /**
     * Résout le user_id réel SANS appel réseau.
     *
     * L'ordre compte : il faut le VRAI user_id, car il est estampillé dans les DTO au
     * moment de la mise en file (`entity.toDto(currentUserId)`). Un id inventé ferait
     * partir des lignes orphelines au retour en ligne — pire qu'un échec franc.
     * On préfère donc échouer que deviner.
     */
    suspend fun resolveUserId(): String? {
        // 1. Session encore en mémoire/stockage supabase-kt (aucun appel réseau).
        //    Souvent null ici : sur une réponse 4xx du endpoint auth (cas du quota dépassé),
        //    supabase-kt purge la session stockée — d'où les replis suivants.
        SupabaseClientProvider.client.auth.currentUserOrNull()?.id
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        // 2. Mode déjà activé précédemment sur cet appareil.
        prefs()?.getString(KEY_USER_ID, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        // 3. Les données locales elles-mêmes : les entités Room portent une colonne
        //    user_id (même si les DAO ne filtrent pas dessus).
        val ctx = appContext ?: return null
        val db = AppDatabase.getInstance(ctx)
        return try {
            db.dayTaskDao().findAnyUserId()?.takeIf { it.isNotBlank() }
                ?: db.domainDao().findAnyUserId()?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun prefs() =
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
