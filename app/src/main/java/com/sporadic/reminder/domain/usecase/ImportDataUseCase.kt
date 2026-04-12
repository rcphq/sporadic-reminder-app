package com.sporadic.reminder.domain.usecase

import android.content.Context
import android.net.Uri
import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.data.repository.ReminderRepository
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.Priority
import com.sporadic.reminder.export.ExportData
import kotlinx.serialization.json.Json
import java.time.LocalTime
import javax.inject.Inject

class ImportDataUseCase @Inject constructor(
    private val reminderRepo: ReminderRepository,
    private val groupRepo: GroupRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun import(context: Context, uri: Uri, merge: Boolean) {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            ?: throw IllegalStateException("Could not read file")

        val data = json.decodeFromString(ExportData.serializer(), jsonString)
        if (data.schemaVersion > ExportData.CURRENT_SCHEMA_VERSION) {
            throw IllegalStateException("Unsupported schema version: ${data.schemaVersion}")
        }

        val groupIdMap = mutableMapOf<String, Long>()
        for (exportGroup in data.groups) {
            if (merge) {
                val existing = groupRepo.getAllSync().find { it.name == exportGroup.name }
                if (existing != null) { groupIdMap[exportGroup.name] = existing.id; continue }
            }
            val id = groupRepo.insert(ReminderGroupEntity(
                name = exportGroup.name,
                startTime = exportGroup.startTime?.let { LocalTime.parse(it) },
                endTime = exportGroup.endTime?.let { LocalTime.parse(it) },
                notificationCount = exportGroup.notificationCount,
                activeDays = exportGroup.activeDays
            ))
            groupIdMap[exportGroup.name] = id
        }

        for (exportReminder in data.reminders) {
            if (merge) {
                val existing = reminderRepo.getAllSync().find { it.name == exportReminder.name }
                if (existing != null) continue
            }
            reminderRepo.insert(ReminderEntity(
                name = exportReminder.name, notificationText = exportReminder.notificationText,
                notificationToneUri = exportReminder.notificationToneUri, vibrate = exportReminder.vibrate,
                priority = Priority.valueOf(exportReminder.priority),
                startTime = LocalTime.parse(exportReminder.startTime),
                endTime = LocalTime.parse(exportReminder.endTime),
                notificationCount = exportReminder.notificationCount,
                activeDays = exportReminder.activeDays,
                dndBehavior = DndBehavior.valueOf(exportReminder.dndBehavior),
                isActive = exportReminder.isActive,
                groupId = exportReminder.groupName?.let { groupIdMap[it] }
            ))
        }
    }
}
