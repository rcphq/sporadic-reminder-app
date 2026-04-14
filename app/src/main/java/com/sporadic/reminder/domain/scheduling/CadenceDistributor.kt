package com.sporadic.reminder.domain.scheduling

import com.sporadic.reminder.domain.model.Cadence
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

class CadenceDistributor @Inject constructor() {

    fun countForDate(
        cadence: Cadence,
        totalCount: Int,
        activeDays: Int,
        date: LocalDate,
        reminderId: Long
    ): Int {
        val dayBit = 1 shl (date.dayOfWeek.value - 1)
        if (activeDays and dayBit == 0) return 0

        return when (cadence) {
            Cadence.DAILY -> totalCount
            Cadence.WEEKLY -> distributeAcrossDays(
                totalCount = totalCount,
                activeDays = activeDays,
                eligibleDatesInPeriod = eligibleDaysInWeek(date, activeDays),
                dateIndexInPeriod = indexInWeek(date, activeDays),
                seed = reminderId * 31 + isoWeekNumber(date)
            )
            Cadence.MONTHLY -> distributeAcrossDays(
                totalCount = totalCount,
                activeDays = activeDays,
                eligibleDatesInPeriod = eligibleDaysInMonth(date, activeDays),
                dateIndexInPeriod = indexInMonth(date, activeDays),
                seed = reminderId * 31 + date.year.toLong() * 13 + date.monthValue
            )
        }
    }

    private fun distributeAcrossDays(
        totalCount: Int,
        activeDays: Int,
        eligibleDatesInPeriod: Int,
        dateIndexInPeriod: Int,
        seed: Long
    ): Int {
        if (eligibleDatesInPeriod == 0) return 0
        val base = totalCount / eligibleDatesInPeriod
        val remainder = totalCount % eligibleDatesInPeriod

        val rng = Random(seed)
        val bonusDays = (0 until eligibleDatesInPeriod).shuffled(rng).take(remainder).toSet()
        return base + if (dateIndexInPeriod in bonusDays) 1 else 0
    }

    private fun eligibleDaysInWeek(date: LocalDate, activeDays: Int): Int {
        val weekStart = date.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
        return (0L until 7).count { offset ->
            val d = weekStart.plusDays(offset)
            val bit = 1 shl (d.dayOfWeek.value - 1)
            activeDays and bit != 0
        }
    }

    private fun indexInWeek(date: LocalDate, activeDays: Int): Int {
        val weekStart = date.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
        var index = 0
        for (offset in 0L until 7) {
            val d = weekStart.plusDays(offset)
            val bit = 1 shl (d.dayOfWeek.value - 1)
            if (activeDays and bit != 0) {
                if (d == date) return index
                index++
            }
        }
        return index
    }

    private fun eligibleDaysInMonth(date: LocalDate, activeDays: Int): Int {
        val monthStart = date.withDayOfMonth(1)
        val daysInMonth = date.lengthOfMonth()
        return (0 until daysInMonth).count { offset ->
            val d = monthStart.plusDays(offset.toLong())
            val bit = 1 shl (d.dayOfWeek.value - 1)
            activeDays and bit != 0
        }
    }

    private fun indexInMonth(date: LocalDate, activeDays: Int): Int {
        val monthStart = date.withDayOfMonth(1)
        var index = 0
        for (day in 1..date.dayOfMonth) {
            val d = monthStart.plusDays((day - 1).toLong())
            val bit = 1 shl (d.dayOfWeek.value - 1)
            if (activeDays and bit != 0) {
                if (d == date) return index
                index++
            }
        }
        return index
    }

    private fun isoWeekNumber(date: LocalDate): Long =
        date.get(WeekFields.ISO.weekOfWeekBasedYear()).toLong()
}
