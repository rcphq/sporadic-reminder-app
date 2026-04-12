package com.sporadic.reminder.data.repository

import com.sporadic.reminder.data.dao.ReminderGroupDao
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(private val dao: ReminderGroupDao) {
    fun getAll(): Flow<List<ReminderGroupEntity>> = dao.getAll()
    suspend fun getById(id: Long): ReminderGroupEntity? = dao.getById(id)
    suspend fun insert(group: ReminderGroupEntity): Long = dao.insert(group)
    suspend fun update(group: ReminderGroupEntity) = dao.update(group)
    suspend fun delete(group: ReminderGroupEntity) = dao.delete(group)
    suspend fun getAllSync(): List<ReminderGroupEntity> = dao.getAllSync()
}
