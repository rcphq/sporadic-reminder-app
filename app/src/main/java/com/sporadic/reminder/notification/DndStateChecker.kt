package com.sporadic.reminder.notification

import android.app.NotificationManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DndStateChecker @Inject constructor(
    private val notificationManager: NotificationManager
) {
    fun isDndActive(): Boolean {
        return notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }
}
