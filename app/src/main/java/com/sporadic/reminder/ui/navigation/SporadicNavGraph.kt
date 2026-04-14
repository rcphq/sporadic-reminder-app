package com.sporadic.reminder.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sporadic.reminder.ui.dashboard.DashboardScreen
import com.sporadic.reminder.ui.groups.GroupEditScreen
import com.sporadic.reminder.ui.reminders.ReminderEditScreen
import com.sporadic.reminder.ui.reminders.ReminderListScreen
import com.sporadic.reminder.ui.settings.SettingsScreen

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun SporadicNavGraph(navController: NavHostController = rememberNavController()) {
    val bottomNavItems = listOf(
        BottomNavItem(
            screen = Screen.Dashboard,
            label = "Dashboard",
            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") }
        ),
        BottomNavItem(
            screen = Screen.ReminderList,
            label = "Reminders",
            icon = { Icon(Icons.Default.Notifications, contentDescription = "Reminders") }
        ),
        BottomNavItem(
            screen = Screen.Settings,
            label = "Settings",
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
        )
    )

    val bottomNavRoutes = setOf(
        Screen.Dashboard.route,
        Screen.ReminderList.route,
        Screen.Settings.route
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToNewReminder = {
                        navController.navigate(Screen.ReminderEdit.createNewRoute())
                    }
                )
            }
            composable(Screen.ReminderList.route) {
                ReminderListScreen(
                    onNavigateToEdit = { reminderId ->
                        navController.navigate(Screen.ReminderEdit.createRoute(reminderId))
                    },
                    onNavigateToNewReminder = {
                        navController.navigate(Screen.ReminderEdit.createNewRoute())
                    },
                    onNavigateToGroupEdit = { groupId ->
                        navController.navigate(Screen.GroupEdit.createRoute(groupId))
                    },
                    onNavigateToNewGroup = {
                        navController.navigate(Screen.GroupEdit.createNewRoute())
                    }
                )
            }
            composable(
                route = Screen.ReminderEdit.route,
                arguments = listOf(navArgument("reminderId") { type = NavType.LongType })
            ) {
                ReminderEditScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.GroupEdit.route,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) {
                GroupEditScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
