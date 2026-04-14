package com.sporadic.reminder.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.domain.model.Cadence
import com.sporadic.reminder.domain.usecase.SyncGroupScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class GroupEditUiState(
    val name: String = "",
    val useSharedSchedule: Boolean = false,
    val cadence: Cadence = Cadence.DAILY,
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(18, 0),
    val notificationCount: Int = 3,
    val activeDays: Int = 0b1111111,
    val isSaved: Boolean = false,
    val isNew: Boolean = true
)

@HiltViewModel
class GroupEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepo: GroupRepository,
    private val syncGroupScheduleUseCase: SyncGroupScheduleUseCase
) : ViewModel() {

    private val groupId: Long = savedStateHandle["groupId"] ?: -1L

    private val _uiState = MutableStateFlow(GroupEditUiState())
    val uiState: StateFlow<GroupEditUiState> = _uiState.asStateFlow()

    init {
        if (groupId != -1L) {
            viewModelScope.launch {
                val group = groupRepo.getById(groupId)
                if (group != null) {
                    _uiState.update {
                        GroupEditUiState(
                            name = group.name,
                            useSharedSchedule = group.startTime != null,
                            cadence = group.cadence ?: Cadence.DAILY,
                            startTime = group.startTime ?: LocalTime.of(9, 0),
                            endTime = group.endTime ?: LocalTime.of(18, 0),
                            notificationCount = group.notificationCount ?: 3,
                            activeDays = group.activeDays ?: 0b1111111,
                            isNew = false
                        )
                    }
                }
            }
        }
    }

    fun updateName(name: String) = _uiState.update { it.copy(name = name) }
    fun updateUseSharedSchedule(use: Boolean) = _uiState.update { it.copy(useSharedSchedule = use) }
    fun updateStartTime(time: LocalTime) = _uiState.update { it.copy(startTime = time) }
    fun updateEndTime(time: LocalTime) = _uiState.update { it.copy(endTime = time) }
    fun updateNotificationCount(count: Int) = _uiState.update { it.copy(notificationCount = count) }

    fun updateCadence(cadence: Cadence) {
        val maxCount = when (cadence) {
            Cadence.DAILY -> 20
            Cadence.WEEKLY -> 50
            Cadence.MONTHLY -> 100
        }
        _uiState.update { it.copy(cadence = cadence, notificationCount = it.notificationCount.coerceAtMost(maxCount)) }
    }

    fun toggleDay(dayBit: Int) {
        _uiState.update { it.copy(activeDays = it.activeDays xor dayBit) }
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) return
        viewModelScope.launch {
            val entity = ReminderGroupEntity(
                id = if (state.isNew) 0L else groupId,
                name = state.name,
                startTime = if (state.useSharedSchedule) state.startTime else null,
                endTime = if (state.useSharedSchedule) state.endTime else null,
                notificationCount = if (state.useSharedSchedule) state.notificationCount else null,
                activeDays = if (state.useSharedSchedule) state.activeDays else null,
                cadence = if (state.useSharedSchedule) state.cadence else null
            )
            val savedId: Long
            if (state.isNew) {
                savedId = groupRepo.insert(entity)
            } else {
                groupRepo.update(entity)
                savedId = groupId
                if (state.useSharedSchedule) {
                    syncGroupScheduleUseCase.syncGroup(
                        groupId = savedId,
                        date = LocalDate.now(),
                        zone = ZoneId.systemDefault()
                    )
                }
            }
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
