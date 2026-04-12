package com.sporadic.reminder.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import java.time.format.DateTimeFormatter

@Composable
fun ReminderListScreen(
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToNewReminder: () -> Unit,
    onNavigateToGroupEdit: (Long) -> Unit,
    onNavigateToNewGroup: () -> Unit,
    viewModel: ReminderListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToNewReminder) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.groups.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Groups",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = onNavigateToNewGroup) {
                            Text("+ New Group")
                        }
                    }
                }
                items(uiState.groups, key = { "group-${it.id}" }) { group ->
                    GroupCard(
                        group = group,
                        onClick = { onNavigateToGroupEdit(group.id) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reminders",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (uiState.reminders.isEmpty()) {
                item {
                    Text(
                        text = "No reminders yet. Tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.reminders, key = { "reminder-${it.id}" }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onClick = { onNavigateToEdit(reminder.id) },
                        onToggleActive = { viewModel.toggleActive(reminder) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCard(group: ReminderGroupEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = if (group.startTime != null) "Shared schedule" else "No shared schedule",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderEntity,
    onClick: () -> Unit,
    onToggleActive: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                androidx.compose.foundation.layout.Column {
                    Text(
                        text = reminder.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${reminder.startTime.format(timeFormatter)} – ${reminder.endTime.format(timeFormatter)}  •  ${reminder.notificationCount}x",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = reminder.isActive,
                onCheckedChange = { onToggleActive() }
            )
        }
    }
}
