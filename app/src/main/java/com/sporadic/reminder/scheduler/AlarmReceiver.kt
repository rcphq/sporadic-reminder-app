package com.sporadic.reminder.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.data.repository.ReminderRepository
import com.sporadic.reminder.data.repository.ScheduledAlarmRepository
import com.sporadic.reminder.domain.model.AlarmStatus
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.notification.DndStateChecker
import com.sporadic.reminder.notification.NotificationPublisher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var alarmRepo: ScheduledAlarmRepository
    @Inject lateinit var reminderRepo: ReminderRepository
    @Inject lateinit var groupRepo: GroupRepository
    @Inject lateinit var dndChecker: DndStateChecker
    @Inject lateinit var notificationPublisher: NotificationPublisher
    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        if (alarmId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { handleAlarm(alarmId) }
            finally { pendingResult.finish() }
        }
    }

    private suspend fun handleAlarm(alarmId: Long) {
        val alarm = alarmRepo.getById(alarmId) ?: return
        var reminder = reminderRepo.getById(alarm.reminderId) ?: return

        // Group pooling: if group has shared schedule, randomly select a member
        if (reminder.groupId != null) {
            val group = groupRepo.getById(reminder.groupId)
            if (group?.startTime != null) {
                val members = reminderRepo.getByGroupId(reminder.groupId).filter { it.isActive }
                if (members.isNotEmpty()) reminder = members.random()
            }
        }

        if (dndChecker.isDndActive()) {
            when (reminder.dndBehavior) {
                DndBehavior.SKIP -> alarmRepo.update(alarm.copy(status = AlarmStatus.SKIPPED))
                DndBehavior.SNOOZE -> {
                    val retryTime = Instant.now().plusSeconds(DND_RETRY_SECONDS)
                    val endOfWindow = LocalDate.now().atTime(reminder.endTime)
                        .atZone(ZoneId.systemDefault()).toInstant()
                    if (retryTime.isBefore(endOfWindow)) {
                        alarmScheduler.scheduleExactAlarm(alarmId, retryTime)
                        alarmRepo.update(alarm.copy(status = AlarmStatus.SNOOZED))
                    } else {
                        alarmRepo.update(alarm.copy(status = AlarmStatus.SKIPPED))
                    }
                }
            }
        } else {
            notificationPublisher.publish(reminder, alarmId)
            alarmRepo.update(alarm.copy(status = AlarmStatus.FIRED))
        }
    }

    companion object {
        const val DND_RETRY_SECONDS = 900L // 15 minutes
    }
}
