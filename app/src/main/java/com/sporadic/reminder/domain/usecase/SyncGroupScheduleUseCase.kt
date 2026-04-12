package com.sporadic.reminder.domain.usecase

import com.sporadic.reminder.data.repository.ReminderRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class SyncGroupScheduleUseCase @Inject constructor(
    private val reminderRepo: ReminderRepository,
    private val scheduleUseCase: ScheduleRemindersUseCase
) {
    suspend fun syncGroup(groupId: Long, date: LocalDate, zone: ZoneId) {
        val members = reminderRepo.getByGroupId(groupId)
        members.filter { it.isActive }.forEach { scheduleUseCase.scheduleForReminder(it, date, zone) }
    }
}
