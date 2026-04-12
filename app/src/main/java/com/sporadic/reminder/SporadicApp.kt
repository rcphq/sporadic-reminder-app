package com.sporadic.reminder

import android.app.Application
import com.sporadic.reminder.notification.NotificationChannelManager
import com.sporadic.reminder.scheduler.DailySchedulerReceiver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SporadicApp : Application() {
    @Inject lateinit var channelManager: NotificationChannelManager

    override fun onCreate() {
        super.onCreate()
        channelManager.createChannels()
        DailySchedulerReceiver.scheduleDailyTrigger(this)
    }
}
