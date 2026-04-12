package com.sporadic.reminder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sporadic.reminder.data.entity.ScheduledAlarmEntity
import com.sporadic.reminder.domain.model.AlarmStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledAlarmDao {
    @Query("SELECT * FROM scheduled_alarms WHERE status = :status ORDER BY scheduledTime ASC")
    suspend fun getByStatus(status: AlarmStatus): List<ScheduledAlarmEntity>

    @Query("SELECT * FROM scheduled_alarms WHERE scheduledTime >= :startOfDay AND scheduledTime < :endOfDay ORDER BY scheduledTime ASC")
    fun getAlarmsForDay(startOfDay: Long, endOfDay: Long): Flow<List<ScheduledAlarmEntity>>

    @Query("SELECT * FROM scheduled_alarms WHERE id = :id")
    suspend fun getById(id: Long): ScheduledAlarmEntity?

    @Query("SELECT * FROM scheduled_alarms WHERE status = 'PENDING' AND scheduledTime > :now")
    suspend fun getPendingFutureAlarms(now: Long): List<ScheduledAlarmEntity>

    @Insert
    suspend fun insert(alarm: ScheduledAlarmEntity): Long

    @Insert
    suspend fun insertAll(alarms: List<ScheduledAlarmEntity>): List<Long>

    @Update
    suspend fun update(alarm: ScheduledAlarmEntity)

    @Query("DELETE FROM scheduled_alarms WHERE scheduledTime >= :startOfDay AND scheduledTime < :endOfDay")
    suspend fun deleteAlarmsForDay(startOfDay: Long, endOfDay: Long)

    @Query("DELETE FROM scheduled_alarms WHERE reminderId = :reminderId AND status = 'PENDING'")
    suspend fun deletePendingForReminder(reminderId: Long)
}
