package com.agenticfocus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agenticfocus.data.db.AppDatabase
import com.agenticfocus.data.entity.DomainEntity
import com.agenticfocus.data.entity.TaskTemplateEntity
import com.agenticfocus.data.repository.LibraryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryState(
    val domains: List<DomainEntity> = emptyList(),
    val templatesByDomain: Map<String, List<TaskTemplateEntity>> = emptyMap()
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LibraryRepository(
        domainDao = AppDatabase.getInstance(application).domainDao(),
        templateDao = AppDatabase.getInstance(application).taskTemplateDao()
    )

    val state: StateFlow<LibraryState> = combine(
        repository.domains,
        repository.templates
    ) { domains, templates ->
        val byDomain = templates.groupBy { it.domainId }
        LibraryState(domains = domains, templatesByDomain = byDomain)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryState()
    )

    init {
        viewModelScope.launch {
            if (repository.domainCount() == 0) {
                repository.insertDefaultDomains()
            }
        }
    }

    fun addTemplate(
        title: String,
        note: String?,
        domainId: String,
        storyPoints: Int,
        defaultPomodoros: Int
    ) {
        viewModelScope.launch {
            repository.addTemplate(title, note, domainId, storyPoints, defaultPomodoros)
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch { repository.deleteTemplate(id) }
    }
}
