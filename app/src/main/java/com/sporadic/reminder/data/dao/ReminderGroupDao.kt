package com.sporadic.reminder.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderGroupDao {
    @Query("SELECT * FROM reminder_groups ORDER BY name ASC")
    fun getAll(): Flow<List<ReminderGroupEntity>>

    @Query("SELECT * FROM reminder_groups WHERE id = :id")
    suspend fun getById(id: Long): ReminderGroupEntity?

    @Insert
    suspend fun insert(group: ReminderGroupEntity): Long

    @Update
    suspend fun update(group: ReminderGroupEntity)

    @Delete
    suspend fun delete(group: ReminderGroupEntity)

    @Query("SELECT * FROM reminder_groups")
    suspend fun getAllSync(): List<ReminderGroupEntity>
}
