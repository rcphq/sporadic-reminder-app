package com.sporadic.reminder.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sporadic.reminder.domain.model.NotificationAction
import com.sporadic.reminder.domain.usecase.HandleNotificationActionUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    @Inject lateinit var handleAction: HandleNotificationActionUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(NotificationPublisher.EXTRA_ALARM_ID, -1)
        val actionName = intent.getStringExtra(NotificationPublisher.EXTRA_ACTION) ?: return
        val action = NotificationAction.valueOf(actionName)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(alarmId.toInt())

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { handleAction.handle(alarmId, action) }
            finally { pendingResult.finish() }
        }
    }
}
