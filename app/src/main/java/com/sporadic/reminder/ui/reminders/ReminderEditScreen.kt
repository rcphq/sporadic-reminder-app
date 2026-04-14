package com.sporadic.reminder.ui.reminders

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.Priority
import com.sporadic.reminder.ui.theme.sporadicColors
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReminderEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.sporadicColors

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            viewModel.updateToneUri(uri?.toString())
        }
    }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    val priorityIcons = mapOf(
        Priority.LOW to Icons.Default.ArrowDownward,
        Priority.DEFAULT to Icons.Default.Remove,
        Priority.HIGH to Icons.Default.ArrowUpward,
        Priority.URGENT to Icons.Default.Warning,
    )
    val priorityColors = mapOf(
        Priority.LOW to colors.priorityLow,
        Priority.DEFAULT to colors.priorityDefault,
        Priority.HIGH to colors.priorityHigh,
        Priority.URGENT to colors.priorityUrgent,
    )
    val priorityContainerColors = mapOf(
        Priority.LOW to colors.priorityLowContainer,
        Priority.DEFAULT to colors.priorityDefaultContainer,
        Priority.HIGH to colors.priorityHighContainer,
        Priority.URGENT to colors.priorityUrgentContainer,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNew) "New Reminder" else "Edit Reminder") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.notificationText,
                onValueChange = viewModel::updateNotificationText,
                label = { Text("Notification text") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Tone picker
            FormRow(icon = Icons.Default.MusicNote) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notification tone", style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                            uiState.notificationToneUri?.let {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                            }
                        }
                        ringtoneLauncher.launch(intent)
                    }) {
                        Text(if (uiState.notificationToneUri != null) "Change" else "Select")
                    }
                }
            }

            // Vibrate
            FormRow(icon = Icons.Default.Vibration) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibrate", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = uiState.vibrate, onCheckedChange = viewModel::updateVibrate)
                }
            }

            // Priority
            Text("Priority", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Priority.entries.forEachIndexed { index, priority ->
                    SegmentedButton(
                        selected = uiState.priority == priority,
                        onClick = { viewModel.updatePriority(priority) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = Priority.entries.size
                        ),
                        colors = if (uiState.priority == priority) {
                            SegmentedButtonDefaults.colors(
                                activeContainerColor = priorityContainerColors[priority]!!,
                                activeContentColor = priorityColors[priority]!!,
                            )
                        } else {
                            SegmentedButtonDefaults.colors()
                        },
                        icon = {
                            Icon(
                                priorityIcons[priority]!!,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text(priority.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            // Time range
            var showStartTimePicker by remember { mutableStateOf(false) }
            var showEndTimePicker by remember { mutableStateOf(false) }

            FormRow(icon = Icons.Default.AccessTime) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start time", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { showStartTimePicker = true }) {
                            Text(uiState.startTime.format(timeFormatter), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End time", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { showEndTimePicker = true }) {
                            Text(uiState.endTime.format(timeFormatter), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            if (showStartTimePicker) {
                TimePickerDialog(
                    initialHour = uiState.startTime.hour,
                    initialMinute = uiState.startTime.minute,
                    onConfirm = { hour, minute ->
                        viewModel.updateStartTime(LocalTime.of(hour, minute))
                        showStartTimePicker = false
                    },
                    onDismiss = { showStartTimePicker = false }
                )
            }

            if (showEndTimePicker) {
                TimePickerDialog(
                    initialHour = uiState.endTime.hour,
                    initialMinute = uiState.endTime.minute,
                    onConfirm = { hour, minute ->
                        viewModel.updateEndTime(LocalTime.of(hour, minute))
                        showEndTimePicker = false
                    },
                    onDismiss = { showEndTimePicker = false }
                )
            }

            // Notification count slider
            Text(
                "Notification count: ${uiState.notificationCount}",
                style = MaterialTheme.typography.titleSmall
            )
            Slider(
                value = uiState.notificationCount.toFloat(),
                onValueChange = { viewModel.updateNotificationCount(it.roundToInt()) },
                valueRange = 1f..20f,
                steps = 18,
                modifier = Modifier.fillMaxWidth()
            )

            // Days of week
            Text("Active days", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                dayLabels.forEachIndexed { index, label ->
                    val bit = 1 shl index
                    val selected = (uiState.activeDays and bit) != 0
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.toggleDay(bit) },
                        label = { Text(label) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    )
                }
            }

            // DND behavior
            Text("DND behavior", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DndBehavior.entries.forEachIndexed { index, behavior ->
                    val isSkip = behavior == DndBehavior.SKIP
                    SegmentedButton(
                        selected = uiState.dndBehavior == behavior,
                        onClick = { viewModel.updateDndBehavior(behavior) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = DndBehavior.entries.size
                        ),
                        colors = if (uiState.dndBehavior == behavior) {
                            SegmentedButtonDefaults.colors(
                                activeContainerColor = if (isSkip) colors.skippedContainer else colors.snoozedContainer,
                                activeContentColor = if (isSkip) colors.onSkippedContainer else colors.onSnoozedContainer,
                            )
                        } else {
                            SegmentedButtonDefaults.colors()
                        },
                        icon = {
                            Icon(
                                if (isSkip) Icons.Default.Cancel else Icons.Default.Snooze,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text(behavior.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            // Group dropdown
            if (uiState.availableGroups.isNotEmpty()) {
                FormRow(icon = Icons.Default.Folder) {
                    Column {
                        Text("Group", style = MaterialTheme.typography.titleSmall)
                        var expanded by remember { mutableStateOf(false) }
                        val selectedGroup = uiState.availableGroups.find { it.id == uiState.groupId }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedGroup?.name ?: "None",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Group") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None") },
                                    onClick = {
                                        viewModel.updateGroupId(null)
                                        expanded = false
                                    }
                                )
                                uiState.availableGroups.forEach { group ->
                                    DropdownMenuItem(
                                        text = { Text(group.name) },
                                        onClick = {
                                            viewModel.updateGroupId(group.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save")
            }
        }
    }
}

@Composable
private fun FormRow(
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = { TimePicker(state = state) }
    )
}
