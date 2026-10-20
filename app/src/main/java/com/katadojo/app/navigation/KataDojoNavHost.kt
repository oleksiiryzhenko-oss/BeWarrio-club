package com.katadojo.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.katadojo.app.R
import com.katadojo.app.data.AppContainer
import com.katadojo.app.ui.components.DojoWordmark
import com.katadojo.app.ui.dojo.DojoScreen
import com.katadojo.app.ui.dojo.RegistrationConfirmedScreen
import com.katadojo.app.ui.dojo.RegistrationScreen
import com.katadojo.app.ui.dojo.ScheduleScreen
import com.katadojo.app.ui.dojo.TrainingDetailScreen
import com.katadojo.app.ui.home.HomeScreen
import com.katadojo.app.ui.kata.KataCoachScreen
import com.katadojo.app.ui.kata.KataDetailScreen
import com.katadojo.app.ui.kata.KataLibraryScreen
import com.katadojo.app.ui.kata.KataSessionDetailScreen
import com.katadojo.app.ui.profile.PrivacyPolicyScreen
import com.katadojo.app.ui.profile.ProfileScreen
import com.katadojo.app.ui.profile.SettingsScreen
import com.katadojo.app.ui.profile.TermsScreen
import com.katadojo.app.ui.progress.GoalsScreen
import com.katadojo.app.ui.progress.HistoryScreen
import com.katadojo.app.ui.progress.ProgressScreen
import com.katadojo.app.ui.theme.*
import com.katadojo.app.ui.workout.WorkoutDetailScreen
import com.katadojo.app.ui.workout.WorkoutScreen
import com.katadojo.app.ui.workout.WorkoutSetupScreen

private data class TabItem(val route: String, val label: String, val icon: @Composable () -> Painter)

private val tabs = listOf(
    TabItem(Routes.HOME, "Home") { rememberVectorPainter(Icons.Filled.Home) },
    TabItem(Routes.DOJO, "Dojo") { painterResource(R.drawable.ic_dojo) },
    TabItem(Routes.KATA, "Kata") { painterResource(R.drawable.ic_enso) },
    TabItem(Routes.PROGRESS, "Progress") { painterResource(R.drawable.ic_chart) }
)

fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun KataDojoRoot(container: AppContainer) {
    val ready by container.ready.collectAsStateWithLifecycle()
    if (!ready) {
        Box(Modifier.fillMaxSize().background(Ink), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DojoWordmark()
                Spacer(Modifier.height(20.dp))
                CircularProgressIndicator(color = DojoRed)
            }
        }
    } else {
        KataDojoShell()
    }
}

@Composable
private fun KataDojoShell() {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route

    Scaffold(
        containerColor = Ink,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute in Routes.topLevel) {
                NavigationBar(containerColor = Charcoal, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon(), contentDescription = null, modifier = Modifier.size(24.dp)) },
                            label = { Text(tab.label.uppercase(), style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Rice,
                                selectedTextColor = Rice,
                                indicatorColor = DojoRed,
                                unselectedIconColor = Stone,
                                unselectedTextColor = Stone
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
        ) {
            val navigate: (String) -> Unit = { navController.navigate(it) }
            val back: () -> Unit = { if (navController.previousBackStackEntry != null) navController.popBackStack() }
            val tab: (String) -> Unit = { navController.navigateToTab(it) }

            composable(Routes.HOME) { HomeScreen(onNavigate = navigate, onTab = tab) }
            composable(Routes.DOJO) { DojoScreen(onNavigate = navigate) }
            composable(Routes.KATA) { KataLibraryScreen(onNavigate = navigate) }
            composable(Routes.PROGRESS) { ProgressScreen(onNavigate = navigate, onTab = tab) }

            composable(Routes.TRAINING_DETAIL, listOf(navArgument("trainingId") { type = NavType.LongType })) {
                TrainingDetailScreen(onNavigate = navigate, onBack = back)
            }
            composable(Routes.REGISTER, listOf(navArgument("trainingId") { type = NavType.LongType })) {
                RegistrationScreen(
                    onBack = back,
                    onConfirmed = { id ->
                        navController.navigate(Routes.registrationConfirmed(id)) { popUpTo(Routes.REGISTER) { inclusive = true } }
                    }
                )
            }
            composable(Routes.REGISTRATION_CONFIRMED, listOf(navArgument("registrationId") { type = NavType.LongType })) {
                RegistrationConfirmedScreen(onNavigate = navigate, onBack = back, onTab = tab)
            }
            composable(Routes.SCHEDULE) { ScheduleScreen(onNavigate = navigate, onBack = back) }

            composable(Routes.KATA_DETAIL, listOf(navArgument("kataId") { type = NavType.StringType })) {
                KataDetailScreen(onNavigate = navigate, onBack = back)
            }
            composable(
                Routes.KATA_COACH,
                listOf(navArgument("kataId") { type = NavType.StringType }, navArgument("focus") { type = NavType.IntType })
            ) {
                KataCoachScreen(
                    onExit = back,
                    onDone = { kataId ->
                        navController.popBackStack(Routes.KATA_COACH, inclusive = true)
                        if (navController.currentBackStackEntry?.destination?.route != Routes.KATA_DETAIL) {
                            navController.navigate(Routes.kataDetail(kataId))
                        }
                    }
                )
            }
            composable(Routes.KATA_SESSION, listOf(navArgument("sessionId") { type = NavType.LongType })) {
                KataSessionDetailScreen(onNavigate = navigate, onBack = back)
            }

            composable(Routes.WORKOUT_SETUP, listOf(navArgument("type") { type = NavType.StringType })) {
                WorkoutSetupScreen(
                    onBack = back,
                    onStart = { type, minutes ->
                        navController.navigate(Routes.workout(type, minutes)) { popUpTo(Routes.WORKOUT_SETUP) { inclusive = true } }
                    }
                )
            }
            composable(
                Routes.WORKOUT,
                listOf(navArgument("type") { type = NavType.StringType }, navArgument("minutes") { type = NavType.IntType })
            ) {
                WorkoutScreen(
                    onExit = back,
                    onDone = {
                        navController.popBackStack(Routes.WORKOUT, inclusive = true)
                        tab(Routes.PROGRESS)
                    }
                )
            }
            composable(Routes.WORKOUT_DETAIL, listOf(navArgument("workoutId") { type = NavType.LongType })) {
                WorkoutDetailScreen(onBack = back)
            }

            composable(Routes.HISTORY) { HistoryScreen(onNavigate = navigate, onBack = back, onTab = tab) }
            composable(Routes.GOALS) { GoalsScreen(onNavigate = navigate, onBack = back) }
            composable(Routes.PROFILE) { ProfileScreen(onNavigate = navigate, onBack = back) }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = back) }
            composable(Routes.PRIVACY) { PrivacyPolicyScreen(onBack = back) }
            composable(Routes.TERMS) { TermsScreen(onBack = back) }
        }
    }
}
