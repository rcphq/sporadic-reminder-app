package com.sporadic.reminder.ui.groups

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: GroupEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNew) "New Group" else "Edit Group") },
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
                label = { Text("Group name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Use shared schedule", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = uiState.useSharedSchedule,
                    onCheckedChange = viewModel::updateUseSharedSchedule
                )
            }

            if (uiState.useSharedSchedule) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start time", style = MaterialTheme.typography.titleSmall)
                        Text(uiState.startTime.format(timeFormatter), style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End time", style = MaterialTheme.typography.titleSmall)
                        Text(uiState.endTime.format(timeFormatter), style = MaterialTheme.typography.bodyLarge)
                    }
                }

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

                Text("Active days", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dayLabels.forEachIndexed { index, label ->
                        val bit = 1 shl index
                        FilterChip(
                            selected = (uiState.activeDays and bit) != 0,
                            onClick = { viewModel.toggleDay(bit) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.name.isNotBlank()
            ) {
                Text("Save")
            }
        }
    }
}
