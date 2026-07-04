package com.internmail.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.internmail.tracker.data.Event
import com.internmail.tracker.data.EventSource
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    onAddManual: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intern Tracker") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh now")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Log out")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddManual) {
                Icon(Icons.Default.Add, contentDescription = "Add event manually")
            }
        }
    ) { padding ->
        if (events.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding).fillMaxSize())
        } else {
            val (open, closed) = events.partition { !it.opened }
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (open.isNotEmpty()) {
                    item { SectionHeader("Upcoming deadlines") }
                    items(open, key = { it.id }) { event ->
                        EventCard(event, onClick = { onEventClick(event) })
                    }
                }
                if (closed.isNotEmpty()) {
                    item { SectionHeader("Done") }
                    items(closed, key = { it.id }) { event ->
                        EventCard(event, onClick = { onEventClick(event) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Business,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text("No internship deadlines yet", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "New opportunities detected in your inbox will show up here automatically, " +
                "or tap + to add one yourself.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EventCard(event: Event, onClick: () -> Unit) {
    val daysLeft = TimeUnit.MILLISECONDS.toDays(event.deadlineEpochMillis - System.currentTimeMillis())
    val urgencyColor = when {
        event.opened -> MaterialTheme.colorScheme.surfaceVariant
        daysLeft < 0 -> Color(0xFFBDBDBD)
        daysLeft == 0L -> Color(0xFFD32F2F)
        daysLeft <= 2 -> Color(0xFFFFA000)
        else -> MaterialTheme.colorScheme.primary
    }

    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(urgencyColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.companyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    formatDeadline(event.deadlineEpochMillis, daysLeft),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (event.source == EventSource.EMAIL_AUTO && !event.emailSubject.isNullOrBlank()) {
                    Text(
                        event.emailSubject,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun formatDeadline(epoch: Long, daysLeft: Long): String {
    val fmt = SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH)
    val dateStr = fmt.format(Date(epoch))
    return when {
        daysLeft < 0 -> "Deadline passed — $dateStr"
        daysLeft == 0L -> "Due TODAY — $dateStr"
        daysLeft == 1L -> "Due tomorrow — $dateStr"
        else -> "Due in $daysLeft days — $dateStr"
    }
}
