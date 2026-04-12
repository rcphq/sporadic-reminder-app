package com.sporadic.reminder.export

import kotlinx.serialization.Serializable

@Serializable
data class ExportData(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val reminders: List<ExportReminder>,
    val groups: List<ExportGroup>,
    val logs: List<ExportLog>
) {
    companion object { const val CURRENT_SCHEMA_VERSION = 1 }
}

@Serializable
data class ExportReminder(
    val name: String, val notificationText: String, val notificationToneUri: String?,
    val vibrate: Boolean, val priority: String, val startTime: String, val endTime: String,
    val notificationCount: Int, val activeDays: Int, val dndBehavior: String,
    val isActive: Boolean, val groupName: String?
)

@Serializable
data class ExportGroup(
    val name: String, val startTime: String?, val endTime: String?,
    val notificationCount: Int?, val activeDays: Int?
)

@Serializable
data class ExportLog(
    val reminderName: String, val scheduledTime: Long, val firedTime: Long,
    val action: String?, val actionTimestamp: Long?
)
