package com.sporadic.reminder.data.repository

import com.sporadic.reminder.data.dao.NotificationLogDao
import com.sporadic.reminder.data.entity.NotificationLogEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationLogRepository @Inject constructor(private val dao: NotificationLogDao) {
    fun getLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<NotificationLogEntity>> = dao.getLogsForDay(startOfDay, endOfDay)
    suspend fun getRecentForReminder(reminderId: Long, limit: Int = 50): List<NotificationLogEntity> = dao.getRecentForReminder(reminderId, limit)
    suspend fun insert(log: NotificationLogEntity): Long = dao.insert(log)
    suspend fun update(log: NotificationLogEntity) = dao.update(log)
    suspend fun getById(id: Long): NotificationLogEntity? = dao.getById(id)
    suspend fun getAllSync(): List<NotificationLogEntity> = dao.getAllSync()
}
