package com.internmail.tracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.internmail.tracker.data.Event
import com.internmail.tracker.data.EventRepository
import com.internmail.tracker.mail.MailSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EventsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = EventRepository(application)

    val events: StateFlow<List<Event>> = repo.observeEvents()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoggedIn: Boolean get() = repo.prefs.isLoggedIn

    fun login(email: String, password: String, imapHost: String, imapPort: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.login(email, password, imapHost, imapPort)
            onDone()
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.logout()
            onDone()
        }
    }

    fun refreshNow() {
        MailSyncWorker.syncNow(getApplication())
    }

    fun saveManualEvent(companyName: String, deadlineEpochMillis: Long, link: String?, existing: Event?) {
        viewModelScope.launch {
            val event = existing?.copy(
                companyName = companyName,
                deadlineEpochMillis = deadlineEpochMillis,
                link = link
            ) ?: Event(
                companyName = companyName,
                deadlineEpochMillis = deadlineEpochMillis,
                link = link,
                source = com.internmail.tracker.data.EventSource.MANUAL
            )
            repo.saveEvent(event)
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch { repo.deleteEvent(event) }
    }

    fun markOpened(event: Event) {
        viewModelScope.launch { repo.markOpened(event) }
    }

    suspend fun getById(id: Long): Event? = repo.getById(id)
}
