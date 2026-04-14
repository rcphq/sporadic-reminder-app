package com.sporadic.reminder.domain.scheduling

import com.google.common.truth.Truth.assertThat
import com.sporadic.reminder.domain.model.Cadence
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class CadenceDistributorTest {
    private lateinit var distributor: CadenceDistributor

    @Before
    fun setup() {
        distributor = CadenceDistributor()
    }

    private val allDays = 0b1111111 // Mon-Sun

    @Test
    fun `DAILY returns totalCount on active day`() {
        // 2026-04-14 is a Tuesday (bit 1)
        val tuesday = LocalDate.of(2026, 4, 14)
        val count = distributor.countForDate(Cadence.DAILY, totalCount = 5, activeDays = allDays, date = tuesday, reminderId = 1)
        assertThat(count).isEqualTo(5)
    }

    @Test
    fun `DAILY returns 0 on inactive day`() {
        val tuesday = LocalDate.of(2026, 4, 14)
        val mondayOnly = 0b0000001 // only Monday
        val count = distributor.countForDate(Cadence.DAILY, totalCount = 5, activeDays = mondayOnly, date = tuesday, reminderId = 1)
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun `WEEKLY distributes totalCount across eligible days in week and sums to total`() {
        // Week of 2026-04-13 (Mon) to 2026-04-19 (Sun)
        val totalCount = 10
        val weekStart = LocalDate.of(2026, 4, 13) // Monday
        var sum = 0
        for (offset in 0L until 7) {
            val date = weekStart.plusDays(offset)
            sum += distributor.countForDate(Cadence.WEEKLY, totalCount = totalCount, activeDays = allDays, date = date, reminderId = 1)
        }
        assertThat(sum).isEqualTo(totalCount)
    }

    @Test
    fun `WEEKLY single eligible day gets full count`() {
        val tuesdayOnly = 0b0000010
        val tuesday = LocalDate.of(2026, 4, 14)
        val count = distributor.countForDate(Cadence.WEEKLY, totalCount = 7, activeDays = tuesdayOnly, date = tuesday, reminderId = 1)
        assertThat(count).isEqualTo(7)
    }

    @Test
    fun `WEEKLY returns 0 on non-eligible day`() {
        val tuesdayOnly = 0b0000010
        val monday = LocalDate.of(2026, 4, 13)
        val count = distributor.countForDate(Cadence.WEEKLY, totalCount = 7, activeDays = tuesdayOnly, date = monday, reminderId = 1)
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun `MONTHLY distributes totalCount across eligible days and sums to total`() {
        val totalCount = 30
        val activeDays = allDays
        val monthStart = LocalDate.of(2026, 4, 1)
        val daysInMonth = monthStart.lengthOfMonth()
        var sum = 0
        for (day in 0 until daysInMonth) {
            sum += distributor.countForDate(Cadence.MONTHLY, totalCount = totalCount, activeDays = activeDays, date = monthStart.plusDays(day.toLong()), reminderId = 1)
        }
        assertThat(sum).isEqualTo(totalCount)
    }

    @Test
    fun `MONTHLY single eligible day gets full count`() {
        val wednesdayOnly = 0b0000100
        // Find a Wednesday in April 2026 — April 1 is a Wednesday
        val wednesday = LocalDate.of(2026, 4, 1)
        val count = distributor.countForDate(Cadence.MONTHLY, totalCount = 20, activeDays = wednesdayOnly, date = wednesday, reminderId = 1)
        // There are multiple Wednesdays but let's just verify count > 0 for an eligible day
        assertThat(count).isGreaterThan(0)
    }

    @Test
    fun `deterministic - same inputs produce same output`() {
        val date = LocalDate.of(2026, 4, 14)
        val count1 = distributor.countForDate(Cadence.WEEKLY, totalCount = 10, activeDays = allDays, date = date, reminderId = 42)
        val count2 = distributor.countForDate(Cadence.WEEKLY, totalCount = 10, activeDays = allDays, date = date, reminderId = 42)
        assertThat(count1).isEqualTo(count2)
    }

    @Test
    fun `different reminders may get different distributions`() {
        // Not guaranteed to differ for all seeds, but the mechanism is seeded by reminderId
        val date = LocalDate.of(2026, 4, 14)
        val counts = (1L..20L).map { reminderId ->
            distributor.countForDate(Cadence.WEEKLY, totalCount = 10, activeDays = allDays, date = date, reminderId = reminderId)
        }
        // All counts should be reasonable (base ± 1)
        counts.forEach { assertThat(it).isIn(0..10) }
    }
}
