package com.sporadic.reminder.data.repository

import com.sporadic.reminder.data.dao.ReminderDao
import com.sporadic.reminder.data.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(private val dao: ReminderDao) {
    fun getAll(): Flow<List<ReminderEntity>> = dao.getAll()
    suspend fun getById(id: Long): ReminderEntity? = dao.getById(id)
    suspend fun getActiveForDay(dayBit: Int): List<ReminderEntity> = dao.getActiveForDay(dayBit)
    suspend fun getByGroupId(groupId: Long): List<ReminderEntity> = dao.getByGroupId(groupId)
    suspend fun insert(reminder: ReminderEntity): Long = dao.insert(reminder)
    suspend fun update(reminder: ReminderEntity) = dao.update(reminder)
    suspend fun delete(reminder: ReminderEntity) = dao.delete(reminder)
    suspend fun getAllSync(): List<ReminderEntity> = dao.getAllSync()
}
