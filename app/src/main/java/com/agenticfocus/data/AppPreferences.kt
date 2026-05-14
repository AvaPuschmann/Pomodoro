package com.agenticfocus.data

import android.content.Context

/**
 * Thin SharedPreferences wrapper for user-configurable app settings.
 * All keys are private constants; callers use typed properties.
 */
class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Whether the timer auto-chains (break → focus) for multi-pomodoro tasks. Default: true. */
    var autoChain: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CHAIN, true)
        set(value) { prefs.edit().putBoolean(KEY_AUTO_CHAIN, value).apply() }

    /** Master toggle — disables ALL timer sounds when false. Default: true. */
    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) { prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply() }

    /** Milestone alert at 10 minutes remaining. Default: true. */
    var sound10min: Boolean
        get() = prefs.getBoolean(KEY_SOUND_10MIN, true)
        set(value) { prefs.edit().putBoolean(KEY_SOUND_10MIN, value).apply() }

    /** Milestone alert at 5 minutes remaining. Default: true. */
    var sound5min: Boolean
        get() = prefs.getBoolean(KEY_SOUND_5MIN, true)
        set(value) { prefs.edit().putBoolean(KEY_SOUND_5MIN, value).apply() }

    /** Milestone alert at 3 minutes remaining. Default: true. */
    var sound3min: Boolean
        get() = prefs.getBoolean(KEY_SOUND_3MIN, true)
        set(value) { prefs.edit().putBoolean(KEY_SOUND_3MIN, value).apply() }

    /** Milestone alert at 1 minute remaining. Default: true. */
    var sound1min: Boolean
        get() = prefs.getBoolean(KEY_SOUND_1MIN, true)
        set(value) { prefs.edit().putBoolean(KEY_SOUND_1MIN, value).apply() }

    /** Feature flag — show/hide the Goals panel in the Day Planner. Default: true. */
    var featureGoals: Boolean
        get() = prefs.getBoolean(KEY_FEATURE_GOALS, true)
        set(value) { prefs.edit().putBoolean(KEY_FEATURE_GOALS, value).apply() }

    /** Feature flag — show/hide Routines module (Library sub-tab + injection). Default: true. */
    var featureRoutines: Boolean
        get() = prefs.getBoolean(KEY_FEATURE_ROUTINES, true)
        set(value) { prefs.edit().putBoolean(KEY_FEATURE_ROUTINES, value).apply() }

    // Mode Projet — Story 17-7 / Sprint 17. Local-only per-device V1 [D25/F4].
    // 4 colonnes Kanban : WIP limit par colonne (0 = illimité, défaut Doing=3 reste WIP convention).
    /** WIP limit pour Backlog. 0 = illimité (par défaut). */
    var wipLimitBacklog: Int
        get() = prefs.getInt(KEY_WIP_LIMIT_BACKLOG, 0)
        set(value) { prefs.edit().putInt(KEY_WIP_LIMIT_BACKLOG, value).apply() }
    /** WIP limit pour Todo. 0 = illimité (par défaut). */
    var wipLimitTodo: Int
        get() = prefs.getInt(KEY_WIP_LIMIT_TODO, 0)
        set(value) { prefs.edit().putInt(KEY_WIP_LIMIT_TODO, value).apply() }
    /** WIP limit pour Doing. Default 3 (warning visuel, pas bloquant). */
    var wipLimitDoing: Int
        get() = prefs.getInt(KEY_WIP_LIMIT_DOING, 3)
        set(value) { prefs.edit().putInt(KEY_WIP_LIMIT_DOING, value).apply() }
    /** WIP limit pour Done. 0 = illimité (par défaut — historique). */
    var wipLimitDone: Int
        get() = prefs.getInt(KEY_WIP_LIMIT_DONE, 0)
        set(value) { prefs.edit().putInt(KEY_WIP_LIMIT_DONE, value).apply() }

    /** Nom personnalisé des colonnes Kanban. Defaults = noms classiques. */
    var labelBacklog: String
        get() = prefs.getString(KEY_LABEL_BACKLOG, "Backlog") ?: "Backlog"
        set(value) { prefs.edit().putString(KEY_LABEL_BACKLOG, value).apply() }
    var labelTodo: String
        get() = prefs.getString(KEY_LABEL_TODO, "Todo") ?: "Todo"
        set(value) { prefs.edit().putString(KEY_LABEL_TODO, value).apply() }
    var labelDoing: String
        get() = prefs.getString(KEY_LABEL_DOING, "Doing") ?: "Doing"
        set(value) { prefs.edit().putString(KEY_LABEL_DOING, value).apply() }
    var labelDone: String
        get() = prefs.getString(KEY_LABEL_DONE, "Done") ?: "Done"
        set(value) { prefs.edit().putString(KEY_LABEL_DONE, value).apply() }

    /**
     * Sort order persisté par projet pour ProjectDetailScreen. Retourne null si jamais
     * set — l'appelant (Story 17-10 ViewModel) applique son défaut via SortOrder enum.
     */
    fun getProjectDetailSortOrder(projectId: String): String? =
        prefs.getString("project_detail_sort_$projectId", null)

    fun setProjectDetailSortOrder(projectId: String, order: String) {
        prefs.edit().putString("project_detail_sort_$projectId", order).apply()
    }

    // Story 22-5 / Sprint 20 — Work Item Age thresholds (en jours).
    // Defaults : 7 / 21 / 45 (cohérent desktop workItemAge.ts DEFAULT_THRESHOLDS).
    var wiaThresholdsGreen: Int
        get() = prefs.getInt(KEY_WIA_THRESH_GREEN, 7)
        set(value) { prefs.edit().putInt(KEY_WIA_THRESH_GREEN, value).apply() }
    var wiaThresholdsYellow: Int
        get() = prefs.getInt(KEY_WIA_THRESH_YELLOW, 21)
        set(value) { prefs.edit().putInt(KEY_WIA_THRESH_YELLOW, value).apply() }
    var wiaThresholdsOrange: Int
        get() = prefs.getInt(KEY_WIA_THRESH_ORANGE, 45)
        set(value) { prefs.edit().putInt(KEY_WIA_THRESH_ORANGE, value).apply() }

    companion object {
        private const val PREFS_NAME          = "app_preferences"
        private const val KEY_AUTO_CHAIN      = "auto_chain"
        private const val KEY_SOUND_ENABLED   = "sound_enabled"
        private const val KEY_SOUND_10MIN     = "sound_10min"
        private const val KEY_SOUND_5MIN      = "sound_5min"
        private const val KEY_SOUND_3MIN      = "sound_3min"
        private const val KEY_SOUND_1MIN      = "sound_1min"
        private const val KEY_FEATURE_GOALS   = "feature_goals"
        private const val KEY_FEATURE_ROUTINES = "feature_routines"
        private const val KEY_WIP_LIMIT_BACKLOG = "wip_limit_backlog"
        private const val KEY_WIP_LIMIT_TODO    = "wip_limit_todo"
        private const val KEY_WIP_LIMIT_DOING   = "wip_limit_doing"
        private const val KEY_WIP_LIMIT_DONE    = "wip_limit_done"
        private const val KEY_LABEL_BACKLOG     = "label_backlog"
        private const val KEY_LABEL_TODO        = "label_todo"
        private const val KEY_LABEL_DOING       = "label_doing"
        private const val KEY_LABEL_DONE        = "label_done"
        private const val KEY_WIA_THRESH_GREEN  = "wia_thresh_green"
        private const val KEY_WIA_THRESH_YELLOW = "wia_thresh_yellow"
        private const val KEY_WIA_THRESH_ORANGE = "wia_thresh_orange"
    }
}
