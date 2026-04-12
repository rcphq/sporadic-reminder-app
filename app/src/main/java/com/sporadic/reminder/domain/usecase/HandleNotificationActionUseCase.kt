package com.sporadic.reminder.domain.usecase

import com.sporadic.reminder.data.entity.NotificationLogEntity
import com.sporadic.reminder.data.repository.NotificationLogRepository
import com.sporadic.reminder.data.repository.ScheduledAlarmRepository
import com.sporadic.reminder.domain.model.AlarmStatus
import com.sporadic.reminder.domain.model.NotificationAction
import java.time.Instant
import javax.inject.Inject

class HandleNotificationActionUseCase @Inject constructor(
    private val logRepo: NotificationLogRepository,
    private val alarmRepo: ScheduledAlarmRepository
) {
    suspend fun handle(alarmId: Long, action: NotificationAction) {
        val alarm = alarmRepo.getById(alarmId) ?: return
        logRepo.insert(NotificationLogEntity(
            reminderId = alarm.reminderId, scheduledTime = alarm.scheduledTime,
            firedTime = Instant.now(), action = action, actionTimestamp = Instant.now()
        ))
        if (action == NotificationAction.SNOOZED) {
            alarmRepo.update(alarm.copy(status = AlarmStatus.SNOOZED))
        }
    }
}
