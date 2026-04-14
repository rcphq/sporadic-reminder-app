package com.sporadic.reminder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class SporadicColors(
    // Status colors
    val pending: Color,
    val pendingContainer: Color,
    val onPendingContainer: Color,
    val done: Color,
    val doneContainer: Color,
    val onDoneContainer: Color,
    val skipped: Color,
    val skippedContainer: Color,
    val onSkippedContainer: Color,
    val snoozed: Color,
    val snoozedContainer: Color,
    val onSnoozedContainer: Color,
    // Priority colors
    val priorityLow: Color,
    val priorityLowContainer: Color,
    val priorityDefault: Color,
    val priorityDefaultContainer: Color,
    val priorityHigh: Color,
    val priorityHighContainer: Color,
    val priorityUrgent: Color,
    val priorityUrgentContainer: Color,
)

val LightSporadicColors = SporadicColors(
    pending = Color(0xFFDF8E1D),
    pendingContainer = Color(0xFFFFF3D6),
    onPendingContainer = Color(0xFF4A3000),
    done = Color(0xFF40A02B),
    doneContainer = Color(0xFFD8F5D3),
    onDoneContainer = Color(0xFF0A3200),
    skipped = Color(0xFFD20F39),
    skippedContainer = Color(0xFFFFDAD6),
    onSkippedContainer = Color(0xFF410002),
    snoozed = Color(0xFF209FB5),
    snoozedContainer = Color(0xFFD0EFFA),
    onSnoozedContainer = Color(0xFF003544),
    priorityLow = Color(0xFF40A02B),
    priorityLowContainer = Color(0xFFD8F5D3),
    priorityDefault = Color(0xFF1E66F5),
    priorityDefaultContainer = Color(0xFFDCE4FF),
    priorityHigh = Color(0xFFFE640B),
    priorityHighContainer = Color(0xFFFFE0CC),
    priorityUrgent = Color(0xFFD20F39),
    priorityUrgentContainer = Color(0xFFFFDAD6),
)

val DarkSporadicColors = SporadicColors(
    pending = Color(0xFFF9E2AF),
    pendingContainer = Color(0xFF4A3800),
    onPendingContainer = Color(0xFFF9E2AF),
    done = Color(0xFFA6E3A1),
    doneContainer = Color(0xFF1A4A18),
    onDoneContainer = Color(0xFFA6E3A1),
    skipped = Color(0xFFF38BA8),
    skippedContainer = Color(0xFF5E1028),
    onSkippedContainer = Color(0xFFF38BA8),
    snoozed = Color(0xFF74C7EC),
    snoozedContainer = Color(0xFF0D3B5E),
    onSnoozedContainer = Color(0xFF74C7EC),
    priorityLow = Color(0xFFA6E3A1),
    priorityLowContainer = Color(0xFF1A4A18),
    priorityDefault = Color(0xFF89B4FA),
    priorityDefaultContainer = Color(0xFF1A3468),
    priorityHigh = Color(0xFFFAB387),
    priorityHighContainer = Color(0xFF4A2800),
    priorityUrgent = Color(0xFFF38BA8),
    priorityUrgentContainer = Color(0xFF5E1028),
)

val LocalSporadicColors = staticCompositionLocalOf { LightSporadicColors }

val MaterialTheme.sporadicColors: SporadicColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSporadicColors.current
