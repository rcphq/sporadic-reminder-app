package com.sporadic.reminder.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.data.repository.NotificationLogRepository
import com.sporadic.reminder.data.repository.ReminderRepository
import com.sporadic.reminder.domain.model.Cadence
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.Priority
import com.sporadic.reminder.export.ExportData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

class ExportDataUseCaseTest {
    private lateinit var useCase: ExportDataUseCase
    private val reminderRepo: ReminderRepository = mockk()
    private val groupRepo: GroupRepository = mockk()
    private val logRepo: NotificationLogRepository = mockk()

    @Before
    fun setup() { useCase = ExportDataUseCase(reminderRepo, groupRepo, logRepo) }

    @Test
    fun `buildExportData includes schema version and all data`() = runTest {
        coEvery { reminderRepo.getAllSync() } returns listOf(
            ReminderEntity(id = 1, name = "Water", notificationTexts = listOf("Drink water"),
                cadence = Cadence.DAILY, notificationToneUri = null, vibrate = true,
                priority = Priority.DEFAULT, startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(17, 0), notificationCount = 5, activeDays = 0b1111111,
                dndBehavior = DndBehavior.SKIP, isActive = true, groupId = null)
        )
        coEvery { groupRepo.getAllSync() } returns emptyList()
        coEvery { logRepo.getAllSync() } returns emptyList()

        val exportData = useCase.buildExportData()
        assertThat(exportData.schemaVersion).isEqualTo(ExportData.CURRENT_SCHEMA_VERSION)
        assertThat(exportData.reminders).hasSize(1)
        assertThat(exportData.reminders[0].name).isEqualTo("Water")

        val json = Json { encodeDefaults = true }.encodeToString(ExportData.serializer(), exportData)
        assertThat(json).contains("\"schemaVersion\":2")
    }
}
