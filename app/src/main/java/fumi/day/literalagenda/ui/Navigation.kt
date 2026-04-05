package fumi.day.literalagenda.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fumi.day.literalagenda.data.Event

@Composable
fun AgendaNavigation() {
    val navController = rememberNavController()
    var selectedEvent by remember { mutableStateOf<Event?>(null) }

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                onNavigateToEdit = { event ->
                    selectedEvent = event
                    if (event != null) {
                        navController.navigate("detail")
                    } else {
                        navController.navigate("edit")
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }
        composable("detail") {
            selectedEvent?.let { event ->
                EventDetailScreen(
                    event = event,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { navController.navigate("edit") }
                )
            }
        }
        composable("edit") {
            EditScreen(
                event = selectedEvent,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
