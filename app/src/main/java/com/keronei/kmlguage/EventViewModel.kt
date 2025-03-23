package com.keronei.kmlguage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class EventViewModel(application: Application) : AndroidViewModel(application) {
    private val _eventFlow = MutableSharedFlow<String>(replay = 0)
    val eventFlow = _eventFlow.asSharedFlow()

    suspend fun sendEvent(event: String) {
        _eventFlow.emit(event) // Notify observers
    }
}
