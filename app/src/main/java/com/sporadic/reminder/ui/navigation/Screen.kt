package com.sporadic.reminder.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object ReminderList : Screen("reminders")
    data object ReminderEdit : Screen("reminders/{reminderId}") {
        fun createRoute(reminderId: Long) = "reminders/$reminderId"
        fun createNewRoute() = "reminders/-1"
    }
    data object GroupEdit : Screen("groups/{groupId}") {
        fun createRoute(groupId: Long) = "groups/$groupId"
        fun createNewRoute() = "groups/-1"
    }
    data object Settings : Screen("settings")
}
