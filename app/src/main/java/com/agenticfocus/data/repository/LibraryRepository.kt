package com.agenticfocus.data.repository

import com.agenticfocus.data.dao.DomainDao
import com.agenticfocus.data.dao.TaskTemplateDao
import com.agenticfocus.data.entity.DomainEntity
import com.agenticfocus.data.entity.TaskTemplateEntity
import com.agenticfocus.data.sync.SyncEngine
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class LibraryRepository(
    private val domainDao: DomainDao,
    private val templateDao: TaskTemplateDao
) {

    val domains: Flow<List<DomainEntity>> = domainDao.observeAll()
    val templates: Flow<List<TaskTemplateEntity>> = templateDao.observeAll()

    suspend fun domainCount(): Int = domainDao.count()

    suspend fun insertDefaultDomains() {
        val defaults = listOf(
            DomainEntity(UUID.randomUUID().toString(), "Sport",              "#4CAF50"),
            DomainEntity(UUID.randomUUID().toString(), "Bien-être",          "#8BC34A"),
            DomainEntity(UUID.randomUUID().toString(), "Intellectuel",       "#2196F3"),
            DomainEntity(UUID.randomUUID().toString(), "Créatif",            "#FFC107"),
            DomainEntity(UUID.randomUUID().toString(), "Professionnel",      "#FF9800"),
            DomainEntity(UUID.randomUUID().toString(), "Famille / Maison",   "#009688"),
            DomainEntity(UUID.randomUUID().toString(), "Jardin / Propriété", "#66BB6A"),
            DomainEntity(UUID.randomUUID().toString(), "Planning / Gestion", "#3F51B5")
        )
        domainDao.insertAll(defaults)
        defaults.forEach { entity ->
            try { SyncEngine.upsertDomain(entity) } catch (_: Exception) {}
        }
    }

    suspend fun addTemplate(
        title: String,
        note: String?,
        domainId: String,
        storyPoints: Int,
        defaultPomodoros: Int,
        impact: String? = null,
        urgency: String? = null,
        dueDate: Long? = null
    ) {
        val entity = TaskTemplateEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            note = note?.takeIf { it.isNotBlank() },
            domainId = domainId,
            storyPoints = storyPoints,
            defaultPomodoros = defaultPomodoros,
            impact = impact,
            urgency = urgency,
            dueDate = dueDate
        )
        templateDao.insert(entity)
        try { SyncEngine.upsertTemplate(entity) } catch (_: Exception) {}
    }

    suspend fun updateTemplate(template: TaskTemplateEntity) {
        templateDao.update(template)
        try { SyncEngine.upsertTemplate(template) } catch (_: Exception) {}
    }

    suspend fun deleteTemplate(id: String) {
        templateDao.deleteById(id)
        try { SyncEngine.deleteTemplate(id) } catch (_: Exception) {}
    }

    suspend fun addDomain(name: String, color: String) {
        val entity = DomainEntity(UUID.randomUUID().toString(), name, color)
        domainDao.insert(entity)
        try { SyncEngine.upsertDomain(entity) } catch (_: Exception) {}
    }

    suspend fun updateDomain(domain: DomainEntity) {
        domainDao.update(domain)
        try { SyncEngine.upsertDomain(domain) } catch (_: Exception) {}
    }

    suspend fun deleteDomain(id: String) {
        templateDao.deleteByDomainId(id)
        domainDao.deleteById(id)
        try { SyncEngine.deleteDomain(id) } catch (_: Exception) {}
    }
}
