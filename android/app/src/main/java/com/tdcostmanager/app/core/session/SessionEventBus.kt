package com.tdcostmanager.app.core.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SessionEventBus {
    private val _events = MutableSharedFlow<SessionEvent>()
    val events = _events.asSharedFlow()

    suspend fun emit(event: SessionEvent) {
        _events.emit(event)
    }
}

sealed interface SessionEvent {
    data object Logout : SessionEvent
}
