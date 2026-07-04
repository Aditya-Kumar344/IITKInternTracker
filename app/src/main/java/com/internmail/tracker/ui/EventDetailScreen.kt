package com.internmail.tracker.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.internmail.tracker.data.Event
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: Long,
    viewModel: EventsViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    var event by remember { mutableStateOf<Event?>(null) }

    LaunchedEffect(eventId) {
        event = viewModel.getById(eventId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(event?.companyName ?: "Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = {
                        event?.let { viewModel.deleteEvent(it) }
                        onBack()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        val e = event
        if (e == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp)) {
            Text(e.companyName, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))

            val fmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH)
            Text(
                "Deadline: ${fmt.format(Date(e.deadlineEpochMillis))}",
                style = MaterialTheme.typography.titleMedium
            )

            if (!e.emailSubject.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("From email:", style = MaterialTheme.typography.labelLarge)
                Text(e.emailSubject, style = MaterialTheme.typography.bodyLarge)
            }

            if (!e.emailSnippet.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(e.emailSnippet, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))

            if (e.opened) {
                AssistChip(onClick = {}, label = { Text("Opened") })
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.markOpened(e)
                    e.link?.let {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                    }
                    event = e.copy(opened = true)
                },
                enabled = !e.link.isNullOrBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (e.link.isNullOrBlank()) "No link available" else "Open application link")
            }

            if (!e.opened) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { viewModel.markOpened(e); event = e.copy(opened = true) }) {
                    Text("Mark as done without opening a link")
                }
            }
        }
    }
}
