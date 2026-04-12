package com.sporadic.reminder.data.repository

import com.sporadic.reminder.data.dao.ScheduledAlarmDao
import com.sporadic.reminder.data.entity.ScheduledAlarmEntity
import com.sporadic.reminder.domain.model.AlarmStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduledAlarmRepository @Inject constructor(private val dao: ScheduledAlarmDao) {
    suspend fun getByStatus(status: AlarmStatus): List<ScheduledAlarmEntity> = dao.getByStatus(status)
    fun getAlarmsForDay(startOfDay: Long, endOfDay: Long): Flow<List<ScheduledAlarmEntity>> = dao.getAlarmsForDay(startOfDay, endOfDay)
    suspend fun getById(id: Long): ScheduledAlarmEntity? = dao.getById(id)
    suspend fun getPendingFutureAlarms(now: Long): List<ScheduledAlarmEntity> = dao.getPendingFutureAlarms(now)
    suspend fun insert(alarm: ScheduledAlarmEntity): Long = dao.insert(alarm)
    suspend fun insertAll(alarms: List<ScheduledAlarmEntity>): List<Long> = dao.insertAll(alarms)
    suspend fun update(alarm: ScheduledAlarmEntity) = dao.update(alarm)
    suspend fun deleteAlarmsForDay(startOfDay: Long, endOfDay: Long) = dao.deleteAlarmsForDay(startOfDay, endOfDay)
    suspend fun deletePendingForReminder(reminderId: Long) = dao.deletePendingForReminder(reminderId)
}
