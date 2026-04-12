package com.sporadic.reminder.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportData(context, it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importData(context, it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Text("Theme", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = uiState.themeMode == mode,
                    onClick = { viewModel.updateThemeMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ThemeMode.entries.size
                    )
                ) {
                    Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Data", style = MaterialTheme.typography.titleMedium)

        Button(
            onClick = { exportLauncher.launch("sporadic_backup.json") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isExporting
        ) {
            Text(if (uiState.isExporting) "Exporting…" else "Export data")
        }

        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/json")) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isImporting
        ) {
            Text(if (uiState.isImporting) "Importing…" else "Import data")
        }

        uiState.errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
