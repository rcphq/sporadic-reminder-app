package com.sporadic.reminder.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sporadic.reminder.domain.usecase.ScheduleRemindersUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class DailySchedulerReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduleUseCase: ScheduleRemindersUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { scheduleUseCase.scheduleAllForDay(LocalDate.now(), ZoneId.systemDefault()) }
            finally { pendingResult.finish() }
        }
    }

    companion object {
        private const val DAILY_REQUEST_CODE = 999_999

        fun scheduleDailyTrigger(context: Context) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val intent = Intent(context, DailySchedulerReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, DAILY_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerTime = LocalDate.now().plusDays(1).atTime(LocalTime.of(4, 0))
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }
}
