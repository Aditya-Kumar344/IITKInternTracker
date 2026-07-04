package com.internmail.tracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private const val ROUTE_LOGIN = "login"
private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_DETAIL = "detail/{eventId}"
private const val ROUTE_ADD_EDIT = "addEdit?eventId={eventId}"

@Composable
fun InternTrackerApp(isLoggedIn: Boolean, initialEventId: Long?) {
    val navController = rememberNavController()
    val viewModel: EventsViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) ROUTE_DASHBOARD else ROUTE_LOGIN
    ) {
        composable(ROUTE_LOGIN) {
            LoginScreen(
                viewModel = viewModel,
                onLoggedIn = {
                    navController.navigate(ROUTE_DASHBOARD) {
                        popUpTo(ROUTE_LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(ROUTE_DASHBOARD) {
            val events by viewModel.events.collectAsState()
            DashboardScreen(
                events = events,
                onEventClick = { navController.navigate("detail/${it.id}") },
                onAddManual = { navController.navigate("addEdit?eventId=-1") },
                onRefresh = { viewModel.refreshNow() },
                onLogout = {
                    viewModel.logout {
                        navController.navigate(ROUTE_LOGIN) {
                            popUpTo(ROUTE_DASHBOARD) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(ROUTE_DETAIL) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")?.toLongOrNull() ?: -1L
            EventDetailScreen(
                eventId = eventId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("addEdit?eventId=$eventId") }
            )
        }

        composable(ROUTE_ADD_EDIT) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")?.toLongOrNull() ?: -1L
            AddEditEventScreen(
                eventId = if (eventId == -1L) null else eventId,
                viewModel = viewModel,
                onDone = { navController.popBackStack() }
            )
        }
    }

    // If launched from a notification tap, jump straight to that event's detail.
    androidx.compose.runtime.LaunchedEffect(initialEventId) {
        if (initialEventId != null && initialEventId != -1L) {
            navController.navigate("detail/$initialEventId")
        }
    }
}
