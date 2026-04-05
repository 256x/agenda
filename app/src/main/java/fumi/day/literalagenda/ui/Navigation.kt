package fumi.day.literalagenda.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AgendaNavigation() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = hiltViewModel()
    val selectedEvent by viewModel.selectedEvent.collectAsState()
    val events by viewModel.events.collectAsState(initial = emptyList())

    // Keep selectedEvent in sync when the underlying event is updated (e.g. after edit+save)
    LaunchedEffect(events) {
        val current = viewModel.selectedEvent.value ?: return@LaunchedEffect
        val updated = events.find { it.filename == current.filename && it.date == current.date }
        if (updated != null && updated != current) viewModel.selectEvent(updated)
    }

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                onNavigateToEdit = { event ->
                    viewModel.selectEvent(event)
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
            val event = selectedEvent
            if (event == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                EventDetailScreen(
                    event = event,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { navController.navigate("edit") }
                )
            }
        }
        composable("edit") {
            val event = selectedEvent
            EditScreen(
                event = event,
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
