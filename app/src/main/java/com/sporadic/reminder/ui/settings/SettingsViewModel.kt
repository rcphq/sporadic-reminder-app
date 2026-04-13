package com.sporadic.reminder.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sporadic.reminder.data.repository.ThemeRepository
import com.sporadic.reminder.domain.usecase.ExportDataUseCase
import com.sporadic.reminder.domain.usecase.ImportDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val themeRepo: ThemeRepository
) : ViewModel() {

    private val _localState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(
        _localState,
        themeRepo.themeMode
    ) { local, theme ->
        local.copy(themeMode = theme)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun updateThemeMode(mode: ThemeMode) = themeRepo.setThemeMode(mode)

    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch {
            _localState.update { it.copy(isExporting = true, errorMessage = null) }
            try {
                exportDataUseCase.export(context, uri)
            } catch (e: NotImplementedError) {
                _localState.update { it.copy(errorMessage = "Export not yet implemented") }
            } catch (e: Exception) {
                _localState.update { it.copy(errorMessage = "Export failed: ${e.message}") }
            } finally {
                _localState.update { it.copy(isExporting = false) }
            }
        }
    }

    fun importData(context: Context, uri: Uri, merge: Boolean = false) {
        viewModelScope.launch {
            _localState.update { it.copy(isImporting = true, errorMessage = null) }
            try {
                importDataUseCase.import(context, uri, merge)
            } catch (e: NotImplementedError) {
                _localState.update { it.copy(errorMessage = "Import not yet implemented") }
            } catch (e: Exception) {
                _localState.update { it.copy(errorMessage = "Import failed: ${e.message}") }
            } finally {
                _localState.update { it.copy(isImporting = false) }
            }
        }
    }

    fun clearError() = _localState.update { it.copy(errorMessage = null) }
}
