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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sporadic.reminder.data.entity.ScheduledAlarmEntity
import com.sporadic.reminder.domain.model.AlarmStatus
import com.sporadic.reminder.domain.model.NotificationAction
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Today",
                style = MaterialTheme.typography.headlineMedium,
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
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Done",
                    count = uiState.doneCount,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Skipped",
                    count = uiState.skippedCount,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Snoozed",
                    count = uiState.snoozedCount,
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

@Composable
private fun StatCard(label: String, count: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
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

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Fired at $timeStr",
                style = MaterialTheme.typography.bodyLarge
            )
            if (!hasAction) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onAction(NotificationAction.DONE) }) {
                        Text("Done")
                    }
                    OutlinedButton(onClick = { onAction(NotificationAction.SKIPPED) }) {
                        Text("Skip")
                    }
                    OutlinedButton(onClick = { onAction(NotificationAction.SNOOZED) }) {
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
