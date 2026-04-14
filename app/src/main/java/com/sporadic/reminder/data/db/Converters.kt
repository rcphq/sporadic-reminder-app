package com.sporadic.reminder.data.db

import androidx.room.TypeConverter
import com.sporadic.reminder.domain.model.AlarmStatus
import com.sporadic.reminder.domain.model.Cadence
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.NotificationAction
import com.sporadic.reminder.domain.model.Priority
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalTime

class Converters {
    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromPriority(value: Priority): String = value.name

    @TypeConverter
    fun toPriority(value: String): Priority = Priority.valueOf(value)

    @TypeConverter
    fun fromDndBehavior(value: DndBehavior): String = value.name

    @TypeConverter
    fun toDndBehavior(value: String): DndBehavior = DndBehavior.valueOf(value)

    @TypeConverter
    fun fromNotificationAction(value: NotificationAction): String = value.name

    @TypeConverter
    fun toNotificationAction(value: String): NotificationAction = NotificationAction.valueOf(value)

    @TypeConverter
    fun fromAlarmStatus(value: AlarmStatus): String = value.name

    @TypeConverter
    fun toAlarmStatus(value: String): AlarmStatus = AlarmStatus.valueOf(value)

    @TypeConverter
    fun fromCadence(value: Cadence): String = value.name

    @TypeConverter
    fun toCadence(value: String): Cadence = Cadence.valueOf(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = Json.decodeFromString(value)
}
