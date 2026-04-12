package com.sporadic.reminder.ui.reminders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.data.repository.ReminderRepository
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class ReminderEditUiState(
    val name: String = "",
    val notificationText: String = "",
    val notificationToneUri: String? = null,
    val vibrate: Boolean = true,
    val priority: Priority = Priority.DEFAULT,
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(18, 0),
    val notificationCount: Int = 3,
    val activeDays: Int = 0b1111111, // all 7 days
    val dndBehavior: DndBehavior = DndBehavior.SKIP,
    val groupId: Long? = null,
    val availableGroups: List<ReminderGroupEntity> = emptyList(),
    val isSaved: Boolean = false,
    val isNew: Boolean = true
)

@HiltViewModel
class ReminderEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reminderRepo: ReminderRepository,
    private val groupRepo: GroupRepository
) : ViewModel() {

    private val reminderId: Long = savedStateHandle["reminderId"] ?: -1L

    private val _uiState = MutableStateFlow(ReminderEditUiState())
    val uiState: StateFlow<ReminderEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val groups = groupRepo.getAllSync()
            if (reminderId != -1L) {
                val reminder = reminderRepo.getById(reminderId)
                if (reminder != null) {
                    _uiState.update {
                        ReminderEditUiState(
                            name = reminder.name,
                            notificationText = reminder.notificationText,
                            notificationToneUri = reminder.notificationToneUri,
                            vibrate = reminder.vibrate,
                            priority = reminder.priority,
                            startTime = reminder.startTime,
                            endTime = reminder.endTime,
                            notificationCount = reminder.notificationCount,
                            activeDays = reminder.activeDays,
                            dndBehavior = reminder.dndBehavior,
                            groupId = reminder.groupId,
                            availableGroups = groups,
                            isNew = false
                        )
                    }
                    return@launch
                }
            }
            _uiState.update { it.copy(availableGroups = groups) }
        }
    }

    fun updateName(name: String) = _uiState.update { it.copy(name = name) }
    fun updateNotificationText(text: String) = _uiState.update { it.copy(notificationText = text) }
    fun updateToneUri(uri: String?) = _uiState.update { it.copy(notificationToneUri = uri) }
    fun updateVibrate(vibrate: Boolean) = _uiState.update { it.copy(vibrate = vibrate) }
    fun updatePriority(priority: Priority) = _uiState.update { it.copy(priority = priority) }
    fun updateStartTime(time: LocalTime) = _uiState.update { it.copy(startTime = time) }
    fun updateEndTime(time: LocalTime) = _uiState.update { it.copy(endTime = time) }
    fun updateNotificationCount(count: Int) = _uiState.update { it.copy(notificationCount = count) }
    fun updateDndBehavior(behavior: DndBehavior) = _uiState.update { it.copy(dndBehavior = behavior) }
    fun updateGroupId(groupId: Long?) = _uiState.update { it.copy(groupId = groupId) }

    fun toggleDay(dayBit: Int) {
        _uiState.update { it.copy(activeDays = it.activeDays xor dayBit) }
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) return
        viewModelScope.launch {
            val entity = ReminderEntity(
                id = if (state.isNew) 0L else reminderId,
                name = state.name,
                notificationText = state.notificationText,
                notificationToneUri = state.notificationToneUri,
                vibrate = state.vibrate,
                priority = state.priority,
                startTime = state.startTime,
                endTime = state.endTime,
                notificationCount = state.notificationCount,
                activeDays = state.activeDays,
                dndBehavior = state.dndBehavior,
                isActive = true,
                groupId = state.groupId
            )
            if (state.isNew) {
                reminderRepo.insert(entity)
            } else {
                reminderRepo.update(entity)
            }
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
