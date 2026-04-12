package com.sporadic.reminder.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sporadic.reminder.domain.model.NotificationAction
import java.time.Instant

@Entity(
    tableName = "notification_logs",
    foreignKeys = [
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("reminderId")]
)
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    val scheduledTime: Instant,
    val firedTime: Instant,
    val action: NotificationAction?,
    val actionTimestamp: Instant?
)
