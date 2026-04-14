package com.sporadic.reminder.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SporadicDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun reminderGroupDao(): ReminderGroupDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun scheduledAlarmDao(): ScheduledAlarmDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN cadence TEXT NOT NULL DEFAULT 'DAILY'")
                db.execSQL("ALTER TABLE reminders ADD COLUMN notificationTexts TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("UPDATE reminders SET notificationTexts = '[\"' || replace(replace(notificationText, '\\', '\\\\'), '\"', '\\\"') || '\"]'")
                db.execSQL("ALTER TABLE reminder_groups ADD COLUMN cadence TEXT DEFAULT NULL")
            }
        }
    }
}
