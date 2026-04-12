package com.sporadic.reminder.di

import android.content.Context
import androidx.room.Room
import com.sporadic.reminder.data.dao.NotificationLogDao
import com.sporadic.reminder.data.dao.ReminderDao
import com.sporadic.reminder.data.dao.ReminderGroupDao
import com.sporadic.reminder.data.dao.ScheduledAlarmDao
import com.sporadic.reminder.data.db.SporadicDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SporadicDatabase {
        return Room.databaseBuilder(context, SporadicDatabase::class.java, "sporadic_reminder.db").build()
    }

    @Provides fun provideReminderDao(db: SporadicDatabase): ReminderDao = db.reminderDao()
    @Provides fun provideReminderGroupDao(db: SporadicDatabase): ReminderGroupDao = db.reminderGroupDao()
    @Provides fun provideNotificationLogDao(db: SporadicDatabase): NotificationLogDao = db.notificationLogDao()
    @Provides fun provideScheduledAlarmDao(db: SporadicDatabase): ScheduledAlarmDao = db.scheduledAlarmDao()
}
