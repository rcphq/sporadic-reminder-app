package com.sporadic.reminder.domain.usecase

import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.entity.ScheduledAlarmEntity
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.data.repository.ReminderRepository
import com.sporadic.reminder.data.repository.ScheduledAlarmRepository
import com.sporadic.reminder.domain.model.AlarmStatus
import com.sporadic.reminder.domain.model.Cadence
import com.sporadic.reminder.domain.scheduling.CadenceDistributor
import com.sporadic.reminder.scheduler.AlarmScheduler
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.random.Random

class ScheduleRemindersUseCase @Inject constructor(
    private val reminderRepo: ReminderRepository,
    private val groupRepo: GroupRepository,
    private val alarmRepo: ScheduledAlarmRepository,
    private val alarmScheduler: AlarmScheduler,
    private val cadenceDistributor: CadenceDistributor
) {
    suspend fun scheduleForReminder(reminder: ReminderEntity, date: LocalDate, zone: ZoneId) {
        val effectiveStartTime: LocalTime
        val effectiveEndTime: LocalTime
        val effectiveCount: Int
        val effectiveCadence: Cadence
        val effectiveActiveDays: Int

        if (reminder.groupId != null) {
            val group = groupRepo.getById(reminder.groupId)
            effectiveStartTime = group?.startTime ?: reminder.startTime
            effectiveEndTime = group?.endTime ?: reminder.endTime
            effectiveCount = group?.notificationCount ?: reminder.notificationCount
            effectiveCadence = group?.cadence ?: reminder.cadence
            effectiveActiveDays = group?.activeDays ?: reminder.activeDays
        } else {
            effectiveStartTime = reminder.startTime
            effectiveEndTime = reminder.endTime
            effectiveCount = reminder.notificationCount
            effectiveCadence = reminder.cadence
            effectiveActiveDays = reminder.activeDays
        }

        val dailyCount = cadenceDistributor.countForDate(
            cadence = effectiveCadence,
            totalCount = effectiveCount,
            activeDays = effectiveActiveDays,
            date = date,
            reminderId = reminder.id
        )

        alarmRepo.deletePendingForReminder(reminder.id)

        if (dailyCount == 0) return

        val startInstant = date.atTime(effectiveStartTime).atZone(zone).toInstant()
        val endInstant = date.atTime(effectiveEndTime).atZone(zone).toInstant()
        val windowMillis = endInstant.toEpochMilli() - startInstant.toEpochMilli()

        val alarms = (1..dailyCount).map {
            val offsetMillis = Random.nextLong(windowMillis + 1)
            ScheduledAlarmEntity(
                reminderId = reminder.id,
                scheduledTime = Instant.ofEpochMilli(startInstant.toEpochMilli() + offsetMillis),
                status = AlarmStatus.PENDING
            )
        }

        val ids = alarmRepo.insertAll(alarms)
        ids.zip(alarms).forEach { (id, alarm) ->
            alarmScheduler.scheduleExactAlarm(id, alarm.scheduledTime)
        }
    }

    suspend fun scheduleAllForDay(date: LocalDate, zone: ZoneId) {
        val reminders = reminderRepo.getActiveReminders()
        reminders.forEach { scheduleForReminder(it, date, zone) }
    }
}
