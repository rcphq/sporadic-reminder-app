package com.sporadic.reminder.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import com.sporadic.reminder.data.entity.ScheduledAlarmEntity
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.data.repository.ReminderRepository
import com.sporadic.reminder.data.repository.ScheduledAlarmRepository
import com.sporadic.reminder.domain.model.AlarmStatus
import com.sporadic.reminder.domain.model.Cadence
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.Priority
import com.sporadic.reminder.domain.scheduling.CadenceDistributor
import com.sporadic.reminder.scheduler.AlarmScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ScheduleRemindersUseCaseTest {
    private lateinit var useCase: ScheduleRemindersUseCase
    private val reminderRepo: ReminderRepository = mockk(relaxed = true)
    private val groupRepo: GroupRepository = mockk(relaxed = true)
    private val alarmRepo: ScheduledAlarmRepository = mockk(relaxed = true)
    private val alarmScheduler: AlarmScheduler = mockk(relaxed = true)
    private val cadenceDistributor: CadenceDistributor = CadenceDistributor()

    @Before
    fun setup() {
        useCase = ScheduleRemindersUseCase(reminderRepo, groupRepo, alarmRepo, alarmScheduler, cadenceDistributor)
    }

    private fun createReminder(
        id: Long = 1, startTime: LocalTime = LocalTime.of(9, 0),
        endTime: LocalTime = LocalTime.of(17, 0), notificationCount: Int = 5,
        activeDays: Int = 0b1111111, groupId: Long? = null,
        cadence: Cadence = Cadence.DAILY
    ) = ReminderEntity(id = id, name = "Test", notificationTexts = listOf("Test"),
        cadence = cadence, notificationToneUri = null, vibrate = true, priority = Priority.DEFAULT,
        startTime = startTime, endTime = endTime, notificationCount = notificationCount,
        activeDays = activeDays, dndBehavior = DndBehavior.SKIP, isActive = true, groupId = groupId)

    @Test
    fun `generates correct number of alarms within time window`() = runTest {
        val reminder = createReminder(notificationCount = 3)
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val capturedAlarms = slot<List<ScheduledAlarmEntity>>()
        coEvery { alarmRepo.insertAll(capture(capturedAlarms)) } returns listOf(1, 2, 3)

        useCase.scheduleForReminder(reminder, today, zone)

        val alarms = capturedAlarms.captured
        assertThat(alarms).hasSize(3)
        val startInstant = today.atTime(LocalTime.of(9, 0)).atZone(zone).toInstant()
        val endInstant = today.atTime(LocalTime.of(17, 0)).atZone(zone).toInstant()
        alarms.forEach { alarm ->
            assertThat(alarm.scheduledTime).isAtLeast(startInstant)
            assertThat(alarm.scheduledTime).isAtMost(endInstant)
            assertThat(alarm.status).isEqualTo(AlarmStatus.PENDING)
            assertThat(alarm.reminderId).isEqualTo(1)
        }
    }

    @Test
    fun `clears existing pending alarms before scheduling`() = runTest {
        val reminder = createReminder()
        coEvery { alarmRepo.insertAll(any()) } returns listOf(1, 2, 3, 4, 5)
        useCase.scheduleForReminder(reminder, LocalDate.now(), ZoneId.systemDefault())
        coVerify { alarmRepo.deletePendingForReminder(reminder.id) }
    }

    @Test
    fun `registers alarms with AlarmScheduler`() = runTest {
        val reminder = createReminder(notificationCount = 2)
        coEvery { alarmRepo.insertAll(any()) } returns listOf(10, 11)
        useCase.scheduleForReminder(reminder, LocalDate.now(), ZoneId.systemDefault())
        coVerify(exactly = 2) { alarmScheduler.scheduleExactAlarm(any(), any()) }
    }

    @Test
    fun `uses group schedule when group has shared schedule`() = runTest {
        val group = ReminderGroupEntity(id = 5, name = "Health", startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(20, 0), notificationCount = 2, activeDays = 0b1111111)
        val reminder = createReminder(groupId = 5)
        coEvery { groupRepo.getById(5) } returns group
        val capturedAlarms = slot<List<ScheduledAlarmEntity>>()
        coEvery { alarmRepo.insertAll(capture(capturedAlarms)) } returns listOf(1, 2)

        useCase.scheduleForReminder(reminder, LocalDate.now(), ZoneId.systemDefault())

        val alarms = capturedAlarms.captured
        assertThat(alarms).hasSize(2) // group's count
        val zone = ZoneId.systemDefault()
        val groupStart = LocalDate.now().atTime(LocalTime.of(8, 0)).atZone(zone).toInstant()
        val groupEnd = LocalDate.now().atTime(LocalTime.of(20, 0)).atZone(zone).toInstant()
        alarms.forEach { assertThat(it.scheduledTime).isAtLeast(groupStart); assertThat(it.scheduledTime).isAtMost(groupEnd) }
    }
}
