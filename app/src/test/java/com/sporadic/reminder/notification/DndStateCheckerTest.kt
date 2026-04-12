package com.sporadic.reminder.notification

import android.app.NotificationManager
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class DndStateCheckerTest {
    private val notificationManager: NotificationManager = mockk()
    private val checker = DndStateChecker(notificationManager)

    @Test
    fun `returns true when DND is active (PRIORITY filter)`() {
        every { notificationManager.currentInterruptionFilter } returns NotificationManager.INTERRUPTION_FILTER_PRIORITY
        assertThat(checker.isDndActive()).isTrue()
    }

    @Test
    fun `returns false when interruption filter is ALL`() {
        every { notificationManager.currentInterruptionFilter } returns NotificationManager.INTERRUPTION_FILTER_ALL
        assertThat(checker.isDndActive()).isFalse()
    }

    @Test
    fun `returns true when interruption filter is NONE`() {
        every { notificationManager.currentInterruptionFilter } returns NotificationManager.INTERRUPTION_FILTER_NONE
        assertThat(checker.isDndActive()).isTrue()
    }
}
