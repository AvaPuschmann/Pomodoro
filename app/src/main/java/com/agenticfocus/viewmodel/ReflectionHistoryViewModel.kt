package com.agenticfocus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agenticfocus.data.db.AppDatabase
import com.agenticfocus.data.entity.DailyReflectionEntity
import com.agenticfocus.data.repository.DailyReflectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * ViewModel for the reflection history screen (Story 24-4 Sprint 22 Epic 24).
 *
 * Observes all daily reflections for the current user, filtered to non-empty entries
 * (UX décision Sally 2026-05-18 : afficher uniquement les bilans avec contenu).
 *
 * Ordering : `period_key DESC` (chronological reverse — most recent first, déjà appliqué côté DAO).
 *
 * Defensive Flow per feedback_continuous_git_during_sprint pattern (Story 23-9) :
 * - `.onStart(emptyList())` — émet une liste vide initiale même si DAO tarde
 * - `.catch { emit(emptyList()) }` — si throw (ex: DB pas initialisée), affiche écran vide gracieux
 */
class ReflectionHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DailyReflectionRepository(
        AppDatabase.getInstance(application).dailyReflectionDao()
    )

    private val _state = MutableStateFlow<List<DailyReflectionEntity>>(emptyList())
    val state: StateFlow<List<DailyReflectionEntity>> = _state.asStateFlow()

    fun setUserId(userId: String) {
        viewModelScope.launch {
            repository.observeAllForUser(userId)
                .map { list ->
                    // Filtre Sally : afficher uniquement les bilans non-vides
                    list.filter { it.dayFacts.isNotBlank() || it.learning.isNotBlank() }
                }
                .onStart { emit(emptyList()) }
                .catch { emit(emptyList()) }
                .collect { _state.value = it }
        }
    }
}
