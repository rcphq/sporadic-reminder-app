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
                // Recreate reminders table to avoid DEFAULT metadata mismatch and drop old notificationText column
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reminders_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        notificationTexts TEXT NOT NULL,
                        cadence TEXT NOT NULL,
                        notificationToneUri TEXT,
                        vibrate INTEGER NOT NULL,
                        priority TEXT NOT NULL,
                        startTime TEXT NOT NULL,
                        endTime TEXT NOT NULL,
                        notificationCount INTEGER NOT NULL,
                        activeDays INTEGER NOT NULL,
                        dndBehavior TEXT NOT NULL,
                        isActive INTEGER NOT NULL,
                        groupId INTEGER,
                        FOREIGN KEY(groupId) REFERENCES reminder_groups(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO reminders_new (id, name, notificationTexts, cadence, notificationToneUri, vibrate, priority, startTime, endTime, notificationCount, activeDays, dndBehavior, isActive, groupId)
                    SELECT id, name, '["' || replace(replace(notificationText, '\', '\\'), '"', '\"') || '"]', 'DAILY', notificationToneUri, vibrate, priority, startTime, endTime, notificationCount, activeDays, dndBehavior, isActive, groupId
                    FROM reminders
                """.trimIndent())
                db.execSQL("DROP TABLE reminders")
                db.execSQL("ALTER TABLE reminders_new RENAME TO reminders")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_groupId ON reminders (groupId)")

                // Add cadence column to reminder_groups (ALTER is fine here since it's nullable with no default)
                db.execSQL("ALTER TABLE reminder_groups ADD COLUMN cadence TEXT DEFAULT NULL")
            }
        }
    }
}
