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
import com.timereci.focus.ui.nextup.BreakScreen
import com.timereci.focus.ui.nextup.NextUpScreen
import com.timereci.focus.ui.publish.PublishScreen
import com.timereci.focus.ui.settings.SettingsScreen
import com.timereci.focus.ui.timer.TimerScreen
import com.timereci.focus.ui.todo.TodoScreen

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
                onOpenTodo = { navController.navigate(Routes.TODO) },
            )
        }

        composable(Routes.TODO) {
            TodoScreen(
                onBack = { navController.popBackStack() },
                onStartPlanned = { label, minutes ->
                    navController.navigate(Routes.timer(label, minutes))
                },
            )
        }

        composable(Routes.PUBLISH) {
            PublishScreen(
                onFinished = { next ->
                    if (next != null) {
                        // Queue has more — offer to continue straight into it.
                        val nextMinutes = (next.plannedMs / 60_000L).toInt().coerceAtLeast(1)
                        navController.navigate(Routes.nextUp(next.id, next.label, nextMinutes)) {
                            popUpTo(Routes.TIMER) { inclusive = false }
                        }
                    } else {
                        navController.navigate(Routes.FEED) {
                            popUpTo(Routes.TIMER) { inclusive = false }
                        }
                    }
                },
            )
        }

        composable(
            route = Routes.NEXT_UP,
            arguments = listOf(
                navArgument(Routes.ARG_PLANNED_ID) { type = NavType.LongType },
                navArgument(Routes.ARG_TASK) { type = NavType.StringType; defaultValue = "" },
                navArgument(Routes.ARG_MINUTES) { type = NavType.IntType; defaultValue = 25 },
            ),
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            val plannedId = args?.getLong(Routes.ARG_PLANNED_ID) ?: 0L
            val task = args?.getString(Routes.ARG_TASK).orEmpty()
            val minutes = args?.getInt(Routes.ARG_MINUTES) ?: 25
            NextUpScreen(
                plannedId = plannedId,
                task = task,
                minutes = minutes,
                onContinueNow = {
                    navController.navigate(Routes.timer(task, minutes)) {
                        popUpTo(Routes.TIMER) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBreak = { breakMinutes ->
                    navController.navigate(Routes.breakScreen(breakMinutes, task, minutes))
                },
                onSkip = {
                    navController.navigate(Routes.FEED) {
                        popUpTo(Routes.TIMER) { inclusive = false }
                    }
                },
            )
        }

        composable(
            route = Routes.BREAK,
            arguments = listOf(
                navArgument(Routes.ARG_BREAK_MINUTES) { type = NavType.IntType },
                navArgument(Routes.ARG_TASK) { type = NavType.StringType; defaultValue = "" },
                navArgument(Routes.ARG_MINUTES) { type = NavType.IntType; defaultValue = 25 },
            ),
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            val breakMinutes = args?.getInt(Routes.ARG_BREAK_MINUTES) ?: 5
            val task = args?.getString(Routes.ARG_TASK).orEmpty()
            val minutes = args?.getInt(Routes.ARG_MINUTES) ?: 25
            BreakScreen(
                minutes = breakMinutes,
                onDone = {
                    navController.navigate(Routes.timer(task, minutes)) {
                        popUpTo(Routes.TIMER) { inclusive = true }
                        launchSingleTop = true
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
