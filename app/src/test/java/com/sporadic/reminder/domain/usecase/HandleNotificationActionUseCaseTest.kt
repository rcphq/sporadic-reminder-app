package com.sporadic.reminder.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.sporadic.reminder.data.entity.NotificationLogEntity
import com.sporadic.reminder.data.entity.ScheduledAlarmEntity
import com.sporadic.reminder.data.repository.NotificationLogRepository
import com.sporadic.reminder.data.repository.ScheduledAlarmRepository
import com.sporadic.reminder.domain.model.AlarmStatus
import com.sporadic.reminder.domain.model.NotificationAction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class HandleNotificationActionUseCaseTest {
    private lateinit var useCase: HandleNotificationActionUseCase
    private val logRepo: NotificationLogRepository = mockk(relaxed = true)
    private val alarmRepo: ScheduledAlarmRepository = mockk(relaxed = true)

    @Before
    fun setup() { useCase = HandleNotificationActionUseCase(logRepo, alarmRepo) }

    private fun createAlarm(id: Long = 1, reminderId: Long = 10) = ScheduledAlarmEntity(
        id = id, reminderId = reminderId, scheduledTime = Instant.now().minusSeconds(60), status = AlarmStatus.FIRED)

    @Test
    fun `handleDone logs DONE action`() = runTest {
        coEvery { alarmRepo.getById(1) } returns createAlarm()
        val logSlot = slot<NotificationLogEntity>()
        coEvery { logRepo.insert(capture(logSlot)) } returns 1
        useCase.handle(alarmId = 1, action = NotificationAction.DONE)
        assertThat(logSlot.captured.action).isEqualTo(NotificationAction.DONE)
        assertThat(logSlot.captured.reminderId).isEqualTo(10)
    }

    @Test
    fun `handleSkipped logs SKIPPED action`() = runTest {
        coEvery { alarmRepo.getById(1) } returns createAlarm()
        val logSlot = slot<NotificationLogEntity>()
        coEvery { logRepo.insert(capture(logSlot)) } returns 1
        useCase.handle(alarmId = 1, action = NotificationAction.SKIPPED)
        assertThat(logSlot.captured.action).isEqualTo(NotificationAction.SKIPPED)
    }

    @Test
    fun `handleSnoozed logs SNOOZED and updates alarm status`() = runTest {
        val alarm = createAlarm()
        coEvery { alarmRepo.getById(1) } returns alarm
        coEvery { logRepo.insert(any()) } returns 1
        useCase.handle(alarmId = 1, action = NotificationAction.SNOOZED)
        coVerify { alarmRepo.update(alarm.copy(status = AlarmStatus.SNOOZED)) }
    }
}
