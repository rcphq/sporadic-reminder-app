package com.sporadic.reminder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sporadic.reminder.data.entity.NotificationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs WHERE firedTime >= :startOfDay AND firedTime < :endOfDay ORDER BY firedTime DESC")
    fun getLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<NotificationLogEntity>>

    @Query("SELECT * FROM notification_logs WHERE reminderId = :reminderId ORDER BY firedTime DESC LIMIT :limit")
    suspend fun getRecentForReminder(reminderId: Long, limit: Int = 50): List<NotificationLogEntity>

    @Insert
    suspend fun insert(log: NotificationLogEntity): Long

    @Update
    suspend fun update(log: NotificationLogEntity)

    @Query("SELECT * FROM notification_logs WHERE id = :id")
    suspend fun getById(id: Long): NotificationLogEntity?

    @Query("SELECT * FROM notification_logs")
    suspend fun getAllSync(): List<NotificationLogEntity>
}
