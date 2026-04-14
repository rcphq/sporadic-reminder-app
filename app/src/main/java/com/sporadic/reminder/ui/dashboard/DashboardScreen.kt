package com.sporadic.reminder.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sporadic.reminder.data.entity.ScheduledAlarmEntity
import com.sporadic.reminder.domain.model.AlarmStatus
import com.sporadic.reminder.domain.model.NotificationAction
import com.sporadic.reminder.ui.theme.sporadicColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    onNavigateToNewReminder: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.sporadicColors

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNewReminder,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "New reminder")
            }
        }
    ) { scaffoldPadding ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Today",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Pending",
                    count = uiState.pendingCount,
                    icon = Icons.Default.Schedule,
                    containerColor = colors.pendingContainer,
                    contentColor = colors.onPendingContainer,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Done",
                    count = uiState.doneCount,
                    icon = Icons.Default.CheckCircle,
                    containerColor = colors.doneContainer,
                    contentColor = colors.onDoneContainer,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Skipped",
                    count = uiState.skippedCount,
                    icon = Icons.Default.Cancel,
                    containerColor = colors.skippedContainer,
                    contentColor = colors.onSkippedContainer,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Snoozed",
                    count = uiState.snoozedCount,
                    icon = Icons.Default.Snooze,
                    containerColor = colors.snoozedContainer,
                    contentColor = colors.onSnoozedContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = "Activity",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val firedAlarms = uiState.todayAlarms.filter { it.status == AlarmStatus.FIRED }
        if (firedAlarms.isEmpty()) {
            item {
                Text(
                    text = "No fired alarms yet today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(firedAlarms, key = { it.id }) { alarm ->
                val hasAction = uiState.todayLogs.any {
                    it.scheduledTime == alarm.scheduledTime && it.action != null
                }
                FiredAlarmCard(
                    alarm = alarm,
                    hasAction = hasAction,
                    onAction = { action -> viewModel.onAction(alarm.id, action) }
                )
            }
        }
    }
    }
}

@Composable
private fun StatCard(
    label: String,
    count: Int,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

@Composable
private fun FiredAlarmCard(
    alarm: ScheduledAlarmEntity,
    hasAction: Boolean,
    onAction: (NotificationAction) -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val timeStr = alarm.scheduledTime
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)
    val colors = MaterialTheme.sporadicColors

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = 12.dp, top = 2.dp)
                    .size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Fired at $timeStr",
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!hasAction) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onAction(NotificationAction.DONE) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.doneContainer,
                                contentColor = colors.onDoneContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Done")
                        }
                        OutlinedButton(onClick = { onAction(NotificationAction.SKIPPED) }) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = null,
                                tint = colors.skipped,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Skip")
                        }
                        OutlinedButton(onClick = { onAction(NotificationAction.SNOOZED) }) {
                            Icon(
                                Icons.Default.Snooze,
                                contentDescription = null,
                                tint = colors.snoozed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Snooze")
                        }
                    }
                } else {
                    Text(
                        text = "Action recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
