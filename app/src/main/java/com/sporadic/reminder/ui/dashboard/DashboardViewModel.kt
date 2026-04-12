package com.sporadic.reminder.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sporadic.reminder.data.entity.NotificationLogEntity
import com.sporadic.reminder.data.entity.ScheduledAlarmEntity
import com.sporadic.reminder.data.repository.NotificationLogRepository
import com.sporadic.reminder.data.repository.ScheduledAlarmRepository
import com.sporadic.reminder.domain.model.AlarmStatus
import com.sporadic.reminder.domain.model.NotificationAction
import com.sporadic.reminder.domain.usecase.HandleNotificationActionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class DashboardUiState(
    val pendingCount: Int = 0,
    val doneCount: Int = 0,
    val skippedCount: Int = 0,
    val snoozedCount: Int = 0,
    val firedCount: Int = 0,
    val todayLogs: List<NotificationLogEntity> = emptyList(),
    val todayAlarms: List<ScheduledAlarmEntity> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val logRepo: NotificationLogRepository,
    private val alarmRepo: ScheduledAlarmRepository,
    private val handleActionUseCase: HandleNotificationActionUseCase
) : ViewModel() {

    private val today = LocalDate.now()
    private val zone = ZoneId.systemDefault()
    private val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
    private val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    val uiState = combine(
        logRepo.getLogsForDay(startOfDay, endOfDay),
        alarmRepo.getAlarmsForDay(startOfDay, endOfDay)
    ) { logs, alarms ->
        DashboardUiState(
            pendingCount = alarms.count { it.status == AlarmStatus.PENDING },
            doneCount = logs.count { it.action == NotificationAction.DONE },
            skippedCount = logs.count { it.action == NotificationAction.SKIPPED },
            snoozedCount = logs.count { it.action == NotificationAction.SNOOZED },
            firedCount = alarms.count { it.status == AlarmStatus.FIRED },
            todayLogs = logs,
            todayAlarms = alarms
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun onAction(alarmId: Long, action: NotificationAction) {
        viewModelScope.launch {
            handleActionUseCase.handle(alarmId, action)
        }
    }
}
