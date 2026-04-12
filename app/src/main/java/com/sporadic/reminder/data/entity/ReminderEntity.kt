package com.sporadic.reminder.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.Priority
import java.time.LocalTime

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = ReminderGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("groupId")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notificationText: String,
    val notificationToneUri: String?,
    val vibrate: Boolean,
    val priority: Priority,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val notificationCount: Int,
    val activeDays: Int,
    val dndBehavior: DndBehavior,
    val isActive: Boolean,
    val groupId: Long?
)
