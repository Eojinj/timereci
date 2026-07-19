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
                navController.navigate(Routes.TIMER) { launchSingleTop = true }
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
        startDestination = Routes.FEED,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) },
    ) {
        composable(Routes.FEED) {
            FeedScreen(
                onStartFocus = { navController.navigate(Routes.TIMER) },
                onOpenDay = { epochDay -> navController.navigate(Routes.day(epochDay)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.TIMER) {
            TimerScreen(
                onCompleted = {
                    navController.navigate(Routes.PUBLISH) {
                        popUpTo(Routes.FEED)
                        launchSingleTop = true
                    }
                },
                onAbandon = { navController.popBackStack(Routes.FEED, inclusive = false) },
            )
        }

        composable(Routes.PUBLISH) {
            PublishScreen(
                onStored = {
                    navController.navigate(Routes.FEED) {
                        popUpTo(Routes.FEED) { inclusive = true }
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
