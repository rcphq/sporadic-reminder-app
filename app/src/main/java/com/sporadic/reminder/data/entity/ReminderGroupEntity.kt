package com.sporadic.reminder.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sporadic.reminder.domain.model.Cadence
import java.time.LocalTime

@Entity(tableName = "reminder_groups")
data class ReminderGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val notificationCount: Int?,
    val activeDays: Int?,
    val cadence: Cadence? = null
)
