package com.sporadic.reminder.domain.usecase

import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.repository.ReminderRepository
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.Priority
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SyncGroupScheduleUseCaseTest {
    private lateinit var useCase: SyncGroupScheduleUseCase
    private val reminderRepo: ReminderRepository = mockk(relaxed = true)
    private val scheduleUseCase: ScheduleRemindersUseCase = mockk(relaxed = true)

    @Before
    fun setup() { useCase = SyncGroupScheduleUseCase(reminderRepo, scheduleUseCase) }

    private fun createReminder(id: Long, groupId: Long, isActive: Boolean = true) = ReminderEntity(
        id = id, name = "R$id", notificationText = "Text", notificationToneUri = null,
        vibrate = true, priority = Priority.DEFAULT, startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(17, 0), notificationCount = 5, activeDays = 0b1111111,
        dndBehavior = DndBehavior.SKIP, isActive = isActive, groupId = groupId)

    @Test
    fun `reschedules all active group members`() = runTest {
        coEvery { reminderRepo.getByGroupId(5) } returns listOf(createReminder(1, 5), createReminder(2, 5))
        useCase.syncGroup(5, LocalDate.now(), ZoneId.systemDefault())
        coVerify(exactly = 2) { scheduleUseCase.scheduleForReminder(any(), any(), any()) }
    }

    @Test
    fun `skips inactive members`() = runTest {
        coEvery { reminderRepo.getByGroupId(5) } returns listOf(createReminder(1, 5), createReminder(2, 5, isActive = false))
        useCase.syncGroup(5, LocalDate.now(), ZoneId.systemDefault())
        coVerify(exactly = 1) { scheduleUseCase.scheduleForReminder(any(), any(), any()) }
    }
}
