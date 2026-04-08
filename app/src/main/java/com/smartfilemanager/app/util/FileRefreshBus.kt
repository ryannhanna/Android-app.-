package com.smartfilemanager.app.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide event bus. ScanViewModel emits here after a successful rule-based
 * deletion so FileBrowserViewModel can reload without any direct coupling.
 */
object FileRefreshBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun notifyFilesChanged() {
        _events.tryEmit(Unit)
    }
}
