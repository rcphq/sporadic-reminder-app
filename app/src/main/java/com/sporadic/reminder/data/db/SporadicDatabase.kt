package com.sporadic.reminder.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sporadic.reminder.data.dao.NotificationLogDao
import com.sporadic.reminder.data.dao.ReminderDao
import com.sporadic.reminder.data.dao.ReminderGroupDao
import com.sporadic.reminder.data.dao.ScheduledAlarmDao
import com.sporadic.reminder.data.entity.NotificationLogEntity
import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import com.sporadic.reminder.data.entity.ScheduledAlarmEntity

@Database(
    entities = [
        ReminderEntity::class,
        ReminderGroupEntity::class,
        NotificationLogEntity::class,
        ScheduledAlarmEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SporadicDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun reminderGroupDao(): ReminderGroupDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun scheduledAlarmDao(): ScheduledAlarmDao
}
