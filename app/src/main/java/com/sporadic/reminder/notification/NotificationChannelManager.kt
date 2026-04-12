package com.sporadic.reminder.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import com.sporadic.reminder.domain.model.Priority
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelManager @Inject constructor(
    private val notificationManager: NotificationManager
) {
    fun createChannels() {
        Priority.entries.forEach { priority ->
            val channel = NotificationChannel(
                channelId(priority), channelName(priority), priority.importance
            ).apply { description = "Sporadic reminders with ${priority.name.lowercase()} priority" }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun channelId(priority: Priority): String = "sporadic_${priority.name.lowercase()}"

    private fun channelName(priority: Priority): String = when (priority) {
        Priority.LOW -> "Low Priority Reminders"
        Priority.DEFAULT -> "Default Priority Reminders"
        Priority.HIGH -> "High Priority Reminders"
        Priority.URGENT -> "Urgent Priority Reminders"
    }
}
