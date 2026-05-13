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

    // Mode Projet — Story 17-7 / Sprint 17. Local-only per-device V1 [D25/F4].
    /** WIP limit for the Doing column (Kanban warning visuel, pas bloquant). Default: 3. */
    var wipLimitDoing: Int
        get() = prefs.getInt(KEY_WIP_LIMIT_DOING, 3)
        set(value) { prefs.edit().putInt(KEY_WIP_LIMIT_DOING, value).apply() }

    /**
     * Sort order persisté par projet pour ProjectDetailScreen. Retourne null si jamais
     * set — l'appelant (Story 17-10 ViewModel) applique son défaut via SortOrder enum.
     */
    fun getProjectDetailSortOrder(projectId: String): String? =
        prefs.getString("project_detail_sort_$projectId", null)

    fun setProjectDetailSortOrder(projectId: String, order: String) {
        prefs.edit().putString("project_detail_sort_$projectId", order).apply()
    }

    companion object {
        private const val PREFS_NAME          = "app_preferences"
        private const val KEY_AUTO_CHAIN      = "auto_chain"
        private const val KEY_SOUND_ENABLED   = "sound_enabled"
        private const val KEY_SOUND_10MIN     = "sound_10min"
        private const val KEY_SOUND_5MIN      = "sound_5min"
        private const val KEY_SOUND_3MIN      = "sound_3min"
        private const val KEY_SOUND_1MIN      = "sound_1min"
        private const val KEY_FEATURE_GOALS   = "feature_goals"
        private const val KEY_WIP_LIMIT_DOING = "wip_limit_doing"
    }
}
