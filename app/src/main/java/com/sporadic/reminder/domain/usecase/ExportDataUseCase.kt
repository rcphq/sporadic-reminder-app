package com.sporadic.reminder.domain.usecase

import android.content.Context
import android.net.Uri
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.data.repository.NotificationLogRepository
import com.sporadic.reminder.data.repository.ReminderRepository
import com.sporadic.reminder.export.ExportData
import com.sporadic.reminder.export.ExportGroup
import com.sporadic.reminder.export.ExportLog
import com.sporadic.reminder.export.ExportReminder
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ExportDataUseCase @Inject constructor(
    private val reminderRepo: ReminderRepository,
    private val groupRepo: GroupRepository,
    private val logRepo: NotificationLogRepository
) {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    suspend fun buildExportData(): ExportData {
        val reminders = reminderRepo.getAllSync()
        val groups = groupRepo.getAllSync()
        val logs = logRepo.getAllSync()
        val groupNameMap = groups.associate { it.id to it.name }

        return ExportData(
            reminders = reminders.map { r ->
                ExportReminder(
                    name = r.name, notificationText = r.notificationText,
                    notificationToneUri = r.notificationToneUri, vibrate = r.vibrate,
                    priority = r.priority.name, startTime = r.startTime.toString(),
                    endTime = r.endTime.toString(), notificationCount = r.notificationCount,
                    activeDays = r.activeDays, dndBehavior = r.dndBehavior.name,
                    isActive = r.isActive, groupName = r.groupId?.let { groupNameMap[it] }
                )
            },
            groups = groups.map { g ->
                ExportGroup(name = g.name, startTime = g.startTime?.toString(),
                    endTime = g.endTime?.toString(), notificationCount = g.notificationCount,
                    activeDays = g.activeDays)
            },
            logs = logs.map { l ->
                ExportLog(
                    reminderName = reminders.find { it.id == l.reminderId }?.name ?: "Unknown",
                    scheduledTime = l.scheduledTime.toEpochMilli(), firedTime = l.firedTime.toEpochMilli(),
                    action = l.action?.name, actionTimestamp = l.actionTimestamp?.toEpochMilli()
                )
            }
        )
    }

    suspend fun export(context: Context, uri: Uri) {
        val data = buildExportData()
        val jsonString = json.encodeToString(ExportData.serializer(), data)
        context.contentResolver.openOutputStream(uri)?.use { it.write(jsonString.toByteArray()) }
    }
}
