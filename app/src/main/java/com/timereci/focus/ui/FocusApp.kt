package com.timereci.focus.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.timereci.focus.ui.day.DayScreen
import com.timereci.focus.ui.detail.DetailScreen
import com.timereci.focus.ui.feed.FeedScreen
import com.timereci.focus.ui.publish.PublishScreen
import com.timereci.focus.ui.settings.SettingsScreen
import com.timereci.focus.ui.timer.TimerScreen

@Composable
fun FocusApp(root: RootViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val resume by root.resume.collectAsStateWithLifecycle()

    LaunchedEffect(resume) {
        when (resume) {
            ResumeTarget.TIMER -> {
                navController.navigate(Routes.timer()) { launchSingleTop = true }
                root.consumeResume()
            }
            ResumeTarget.PUBLISH -> {
                navController.navigate(Routes.PUBLISH) { launchSingleTop = true }
                root.consumeResume()
            }
            null -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.TIMER,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) },
    ) {
        composable(
            route = Routes.TIMER,
            arguments = listOf(
                navArgument(Routes.ARG_TASK) { type = NavType.StringType; defaultValue = "" },
                navArgument(Routes.ARG_MINUTES) { type = NavType.IntType; defaultValue = 0 },
            ),
        ) {
            TimerScreen(
                onCompleted = {
                    navController.navigate(Routes.PUBLISH) { launchSingleTop = true }
                },
                onAbandon = {
                    // Pushed (planned) timer returns to the feed; the home timer just stays idle.
                    navController.popBackStack(Routes.FEED, inclusive = false)
                },
                onOpenFeed = { navController.navigate(Routes.FEED) },
            )
        }

        composable(Routes.FEED) {
            FeedScreen(
                onStartFocus = {
                    // Home timer is the root — return to it rather than stacking another.
                    if (!navController.popBackStack(Routes.TIMER, inclusive = false)) {
                        navController.navigate(Routes.timer())
                    }
                },
                onStartPlanned = { label, minutes ->
                    navController.navigate(Routes.timer(label, minutes))
                },
                onOpenDay = { epochDay -> navController.navigate(Routes.day(epochDay)) },
                onOpenReceipt = { id -> navController.navigate(Routes.detail(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.PUBLISH) {
            PublishScreen(
                onStored = {
                    // Land on the feed to see the new record; drop timer+publish from the stack.
                    navController.navigate(Routes.FEED) {
                        popUpTo(Routes.TIMER) { inclusive = false }
                    }
                },
            )
        }

        composable(
            route = Routes.DAY,
            arguments = listOf(navArgument(Routes.ARG_EPOCH_DAY) { type = NavType.LongType }),
        ) {
            DayScreen(
                onBack = { navController.popBackStack() },
                onOpenReceipt = { id -> navController.navigate(Routes.detail(id)) },
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument(Routes.ARG_RECEIPT_ID) { type = NavType.LongType }),
        ) {
            DetailScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
