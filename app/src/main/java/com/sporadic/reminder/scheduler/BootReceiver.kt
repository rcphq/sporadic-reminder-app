package com.sporadic.reminder.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sporadic.reminder.data.repository.ScheduledAlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var alarmRepo: ScheduledAlarmRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pendingAlarms = alarmRepo.getPendingFutureAlarms(Instant.now().toEpochMilli())
                pendingAlarms.forEach { alarmScheduler.scheduleExactAlarm(it.id, it.scheduledTime) }
            } finally { pendingResult.finish() }
        }
    }
}
