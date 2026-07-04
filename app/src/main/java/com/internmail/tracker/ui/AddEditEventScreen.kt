package com.internmail.tracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.internmail.tracker.data.Event
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEventScreen(
    eventId: Long?,
    viewModel: EventsViewModel,
    onDone: () -> Unit
) {
    var existing by remember { mutableStateOf<Event?>(null) }
    var companyName by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var deadlineMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        if (eventId != null) {
            val e = viewModel.getById(eventId)
            existing = e
            e?.let {
                companyName = it.companyName
                link = it.link ?: ""
                deadlineMillis = it.deadlineEpochMillis
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = deadlineMillis ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadlineMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (eventId == null) "Add deadline" else "Edit deadline") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp)) {
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    deadlineMillis?.let { SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH).format(Date(it)) }
                        ?: "Pick deadline date"
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("Application link (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val deadline = deadlineMillis ?: return@Button
                    viewModel.saveManualEvent(
                        companyName = companyName.trim(),
                        deadlineEpochMillis = endOfDay(deadline),
                        link = link.trim().ifBlank { null },
                        existing = existing
                    )
                    onDone()
                },
                enabled = companyName.isNotBlank() && deadlineMillis != null,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Save")
            }
        }
    }
}

private fun endOfDay(epoch: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = epoch
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    return cal.timeInMillis
}
