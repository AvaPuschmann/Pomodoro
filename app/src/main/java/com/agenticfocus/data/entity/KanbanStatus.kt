package com.agenticfocus.data.entity

// Validation Kotlin des statuts Kanban en remplacement du CHECK SQL
// (Room ne supporte pas les CHECK constraints déclaratives). Toute mutation
// de ProjectEntity.kanbanStatus doit passer par isValid() côté Repository
// AVANT d'appeler dao.upsert(...) — sinon la row sera rejetée par Postgres
// au prochain sync (CHECK constraint Supabase), et bloquée dans sync_queue.
object KanbanStatus {
    const val BACKLOG = "backlog"
    const val TODO = "todo"
    const val DOING = "doing"
    const val DONE = "done"

    val ALL: List<String> = listOf(BACKLOG, TODO, DOING, DONE)

    fun isValid(s: String): Boolean = s in ALL
}
