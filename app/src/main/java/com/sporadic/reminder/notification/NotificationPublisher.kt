package com.sporadic.reminder.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.sporadic.reminder.MainActivity
import com.sporadic.reminder.R
import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.domain.model.NotificationAction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val channelManager: NotificationChannelManager
) {
    fun publish(reminder: ReminderEntity, alarmId: Long) {
        val contentIntent = PendingIntent.getActivity(
            context, alarmId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_REMINDER_ID, reminder.id)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelManager.channelId(reminder.priority))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.name)
            .setContentText(reminder.notificationTexts.random())
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setVibrate(if (reminder.vibrate) longArrayOf(0, 250, 250, 250) else null)
            .apply { reminder.notificationToneUri?.let { setSound(Uri.parse(it)) } }
            .addAction(buildActionButton(alarmId, NotificationAction.DONE, "Done"))
            .addAction(buildActionButton(alarmId, NotificationAction.SKIPPED, "Skip"))
            .addAction(buildActionButton(alarmId, NotificationAction.SNOOZED, "Snooze"))
            .build()

        val nm = context.getSystemService(android.app.NotificationManager::class.java)
        nm.notify(alarmId.toInt(), notification)
    }

    private fun buildActionButton(alarmId: Long, action: NotificationAction, label: String): NotificationCompat.Action {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ACTION, action.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, "${alarmId}_${action.ordinal}".hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(0, label, pendingIntent).build()
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_ACTION = "extra_action"
    }
}
