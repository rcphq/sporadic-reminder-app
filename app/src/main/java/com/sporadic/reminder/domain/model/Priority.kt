package com.sporadic.reminder.domain.model

import android.app.NotificationManager

enum class Priority(val importance: Int) {
    LOW(NotificationManager.IMPORTANCE_LOW),
    DEFAULT(NotificationManager.IMPORTANCE_DEFAULT),
    HIGH(NotificationManager.IMPORTANCE_HIGH),
    URGENT(NotificationManager.IMPORTANCE_MAX);
}
