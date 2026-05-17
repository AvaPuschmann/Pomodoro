package com.agenticfocus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agenticfocus.data.db.AppDatabase
import com.agenticfocus.data.entity.DomainEntity
import com.agenticfocus.data.entity.TagEntity
import com.agenticfocus.data.entity.TaskTemplateEntity
import com.agenticfocus.data.repository.LibraryRepository
import com.agenticfocus.data.sync.SyncEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryState(
    val domains: List<DomainEntity> = emptyList(),
    val templatesByDomain: Map<String, List<TaskTemplateEntity>> = emptyMap(),
    val tags: List<TagEntity> = emptyList(),
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LibraryRepository(
        domainDao = AppDatabase.getInstance(application).domainDao(),
        templateDao = AppDatabase.getInstance(application).taskTemplateDao(),
        tagDao = AppDatabase.getInstance(application).tagDao(),
    )

    val state: StateFlow<LibraryState> = combine(
        repository.domains,
        repository.templates,
        repository.tags,
    ) { domains, templates, tags ->
        val domainIds = domains.map { it.id }.toSet()
        val validTemplates = templates.filter { it.domainId in domainIds }
        val byDomain = validTemplates.groupBy { it.domainId }
        LibraryState(domains = domains, templatesByDomain = byDomain, tags = tags)
    }.catch { e ->
        // Story 23-9 / Sprint 21 — defense in depth : si combine throw (rare car les
        // sources sont catch déjà côté Repository), on émet LibraryState vide plutôt
        // que freeze le UI. Log pour debug.
        android.util.Log.e("LibraryViewModel", "combine state flow failed", e)
        emit(LibraryState())
    }.stateIn(
        scope = viewModelScope,
        // Eagerly: keep the state populated as long as the VM is alive.
        // WhileSubscribed(5_000) caused the Library tab + EditTaskForm domain dropdown
        // to flash empty when returning from another tab after 5s of inactivity:
        // Flow stopped → re-subscribed → initialValue = LibraryState() (empty) emitted
        // before Room re-collected. Eagerly avoids that window. Cost is negligible.
        started = SharingStarted.Eagerly,
        initialValue = LibraryState()
    )

    init {
        viewModelScope.launch {
            // Insert defaults only for new unauthenticated users.
            // Authenticated users get domains from Supabase via pullSync.
            if (repository.domainCount() == 0 && SyncEngine.currentUserId.isEmpty()) {
                repository.insertDefaultDomains()
            }
        }
    }

    fun addTemplate(
        title: String,
        note: String?,
        domainId: String,
        storyPoints: Int,
        defaultPomodoros: Int,
        impact: String? = null,
        urgency: String? = null,
        dueDate: Long? = null
    ) {
        viewModelScope.launch {
            repository.addTemplate(title, note, domainId, storyPoints, defaultPomodoros, impact, urgency, dueDate)
        }
    }

    fun updateTemplate(
        id: String,
        title: String,
        note: String?,
        domainId: String,
        storyPoints: Int,
        defaultPomodoros: Int,
        impact: String? = null,
        urgency: String? = null,
        dueDate: Long? = null
    ) {
        viewModelScope.launch {
            repository.updateTemplate(
                TaskTemplateEntity(
                    id, title, note?.takeIf { it.isNotBlank() }, domainId, storyPoints, defaultPomodoros,
                    impact = impact, urgency = urgency, dueDate = dueDate
                )
            )
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch { repository.deleteTemplate(id) }
    }

    fun addDomain(name: String, color: String) {
        viewModelScope.launch { repository.addDomain(name, color) }
    }

    fun updateDomain(id: String, name: String, color: String) {
        viewModelScope.launch {
            repository.updateDomain(
                com.agenticfocus.data.entity.DomainEntity(id, name, color)
            )
        }
    }

    fun deleteDomain(id: String) {
        viewModelScope.launch { repository.deleteDomain(id) }
    }

    // ── Tags (Story 22-2 / Sprint 20) ──────────────────────────────────────

    fun addTag(name: String, color: String) {
        viewModelScope.launch { repository.addTag(name, color) }
    }

    fun updateTag(id: String, name: String, color: String) {
        viewModelScope.launch {
            val existing = state.value.tags.find { it.id == id } ?: return@launch
            repository.updateTag(
                existing.copy(name = name.trim(), color = color)
            )
        }
    }

    fun deleteTag(id: String) {
        viewModelScope.launch { repository.deleteTag(id) }
    }
}
