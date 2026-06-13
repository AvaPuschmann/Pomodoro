package com.agenticfocus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agenticfocus.data.db.AppDatabase
import com.agenticfocus.data.repository.DailyReflectionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel for the daily reflection screen (Story 24-2 Sprint 22 Epic 24).
 *
 * State :
 * - [periodKey] — YYYY-MM-DD identifying the entry (defaults to today)
 * - [dayFacts], [learning] — narrative texts
 * - [isEditable] — toujours true : tous les jours éditables (journaling rétroactif, 2026-06-07)
 * - [lastSavedAt] — last successful save timestamp, drives "✓ Enregistré" feedback
 *
 * Behavior :
 * - [setUserId] + [loadForPeriod] must be called by the screen on first composition
 * - Local state updates via [onDayFactsChange] / [onLearningChange] trigger auto-save
 *   with a 500ms debounce (Party Mode Sally UX decision)
 * - Auto-save calls [DailyReflectionRepository.saveReflection] which computes word/char counts
 *   and pushes to SyncEngine (best-effort, sync queue retries)
 * - [flush] forces immediate save (call from screen onDispose or LifecycleObserver ON_PAUSE)
 *
 * @see com.agenticfocus.data.repository.DailyReflectionRepository
 */
class DailyReflectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DailyReflectionRepository(
        AppDatabase.getInstance(application).dailyReflectionDao()
    )

    data class State(
        val periodKey: String = LocalDate.now().toString(),
        val dayFacts: String = "",
        val learning: String = "",
        val isEditable: Boolean = true,
        val lastSavedAt: Long? = null,
        val isLoading: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var userId: String = ""
    private var saveJob: Job? = null

    fun setUserId(id: String) {
        userId = id
    }

    /**
     * Load the entry for [periodKey] (defaults to today) from local DB.
     * Tous les jours sont directement éditables, y compris le passé, pour permettre
     * le journaling rétroactif sans friction — décision Philippe 2026-06-07.
     */
    fun loadForPeriod(periodKey: String = LocalDate.now().toString()) {
        _state.update { it.copy(periodKey = periodKey, isEditable = true, isLoading = true) }
        viewModelScope.launch {
            val existing = repository.getReflection(userId, periodKey)
            _state.update {
                it.copy(
                    dayFacts = existing?.dayFacts ?: "",
                    learning = existing?.learning ?: "",
                    isLoading = false,
                )
            }
        }
    }

    fun onDayFactsChange(text: String) {
        _state.update { it.copy(dayFacts = text) }
        scheduleSave()
    }

    fun onLearningChange(text: String) {
        _state.update { it.copy(learning = text) }
        scheduleSave()
    }

    /**
     * Schedule a save after 500ms debounce.
     * Cancels any pending save job — only the last keystroke triggers a real save.
     */
    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            performSave()
        }
    }

    /**
     * Immediate save (no debounce). Call from screen onDispose / ON_PAUSE / "Save now" button.
     */
    fun flush() {
        saveJob?.cancel()
        viewModelScope.launch {
            performSave()
        }
    }

    private suspend fun performSave() {
        val current = _state.value
        if (userId.isBlank()) return
        repository.saveReflection(
            userId = userId,
            periodKey = current.periodKey,
            dayFacts = current.dayFacts,
            learning = current.learning,
        )
        _state.update { it.copy(lastSavedAt = System.currentTimeMillis()) }
    }

    override fun onCleared() {
        super.onCleared()
        // Best-effort flush on dispose. viewModelScope is cancelled after onCleared returns,
        // so this only catches saves with debounce still pending in-memory state.
        saveJob?.cancel()
    }
}
