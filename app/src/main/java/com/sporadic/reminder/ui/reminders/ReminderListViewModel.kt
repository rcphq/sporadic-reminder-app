package com.sporadic.reminder.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderListUiState(
    val reminders: List<ReminderEntity> = emptyList(),
    val groups: List<ReminderGroupEntity> = emptyList()
)

@HiltViewModel
class ReminderListViewModel @Inject constructor(
    private val reminderRepo: ReminderRepository,
    private val groupRepo: GroupRepository
) : ViewModel() {

    val uiState = combine(
        reminderRepo.getAll(),
        groupRepo.getAll()
    ) { reminders, groups ->
        ReminderListUiState(reminders = reminders, groups = groups)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReminderListUiState()
    )

    fun toggleActive(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderRepo.update(reminder.copy(isActive = !reminder.isActive))
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderRepo.delete(reminder)
        }
    }
}
