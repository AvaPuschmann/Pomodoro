package com.agenticfocus.data.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object SyncStatusManager {

    enum class SyncStatus { SYNCED, SYNCING, OFFLINE, ERROR }

    private val _status = MutableStateFlow(SyncStatus.SYNCED)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val _lastSyncAt = MutableStateFlow<Long?>(null)
    val lastSyncAt: StateFlow<Long?> = _lastSyncAt.asStateFlow()

    private val _conflictEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val conflictEvents: SharedFlow<String> = _conflictEvents.asSharedFlow()

    fun setSyncing() { _status.value = SyncStatus.SYNCING }

    fun setSynced() {
        _lastSyncAt.value = System.currentTimeMillis()
        _status.value = SyncStatus.SYNCED
    }

    fun setOffline() { _status.value = SyncStatus.OFFLINE }

    fun setError() { _status.value = SyncStatus.ERROR }

    fun emitConflict(message: String) { _conflictEvents.tryEmit(message) }
}
