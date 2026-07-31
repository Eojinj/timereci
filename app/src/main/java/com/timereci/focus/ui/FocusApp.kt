package com.timereci.focus.ui

import android.content.pm.ActivityInfo
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.timereci.focus.ui.detail.DetailScreen
import com.timereci.focus.ui.feed.FeedScreen
import com.timereci.focus.ui.nextup.BreakScreen
import com.timereci.focus.ui.nextup.NextUpScreen
import com.timereci.focus.ui.publish.PublishScreen
import com.timereci.focus.ui.quickstart.QuickStartScreen
import com.timereci.focus.ui.settings.SettingsScreen
import com.timereci.focus.ui.stats.StatsScreen
import com.timereci.focus.ui.stats.TaskStatsScreen
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.timer.TimerScreen
import com.timereci.focus.ui.todo.TodoScreen
import com.timereci.focus.ui.util.findActivity

/** The three tabs — everything else (quick start, timer, session complete, …) is a drill-down. */
private val TAB_ROUTES = setOf(Routes.TODAY, Routes.HISTORY, Routes.SETTINGS)

@Composable
fun FocusApp(root: RootViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val resume by root.resume.collectAsStateWithLifecycle()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Only the timer screen ever unlocks rotation. Enforcing that here — not just inside the
    // timer screen itself — guarantees every other screen is portrait the moment it becomes
    // current, regardless of whatever orientation state the timer screen leaves behind.
    val context = LocalContext.current
    LaunchedEffect(currentRoute) {
        if (currentRoute != null && currentRoute != Routes.TIMER) {
            context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    LaunchedEffect(resume) {
        when (resume) {
            ResumeTarget.TIMER -> {
                navController.navigate(Routes.TIMER) { launchSingleTop = true }
                root.consumeResume()
            }
            ResumeTarget.PUBLISH -> {
                navController.navigate(Routes.SESSION_COMPLETE) { launchSingleTop = true }
                root.consumeResume()
            }
            null -> Unit
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(180)) },
        ) {
            composable(Routes.TODAY) {
                TodoScreen(
                    onOpenQuickStart = { navController.navigate(Routes.QUICK_START) },
                    onStartedSession = {
                        navController.navigate(Routes.TIMER) { launchSingleTop = true }
                    },
                )
            }

            composable(Routes.QUICK_START) {
                QuickStartScreen(
                    onCancel = { navController.popBackStack() },
                    onStarted = {
                        navController.navigate(Routes.TIMER) {
                            popUpTo(Routes.TODAY) { inclusive = false }
                        }
                    },
                )
            }

            composable(Routes.TIMER) {
                TimerScreen(
                    onCompleted = {
                        navController.navigate(Routes.SESSION_COMPLETE) { launchSingleTop = true }
                    },
                    // Stopping never navigates — it just resets in place, back to Today.
                    onAbandon = {
                        navController.navigate(Routes.TODAY) {
                            popUpTo(Routes.TODAY) { inclusive = false }
                        }
                    },
                )
            }

            composable(Routes.SESSION_COMPLETE) {
                PublishScreen(
                    onFinished = { next ->
                        if (next != null) {
                            // Queue has more — offer to continue straight into it.
                            navController.navigate(Routes.UP_NEXT) {
                                popUpTo(Routes.TODAY) { inclusive = false }
                            }
                        } else {
                            navController.navigate(Routes.HISTORY) {
                                popUpTo(Routes.TODAY) { inclusive = false }
                            }
                        }
                    },
                )
            }

            composable(Routes.UP_NEXT) {
                NextUpScreen(
                    onContinueNow = {
                        navController.navigate(Routes.TIMER) {
                            popUpTo(Routes.TODAY) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onBreak = { breakMinutes, task, minutes ->
                        navController.navigate(Routes.breakScreen(breakMinutes, task, minutes))
                    },
                    onSkip = {
                        navController.navigate(Routes.HISTORY) {
                            popUpTo(Routes.TODAY) { inclusive = false }
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
            ) {
                BreakScreen(
                    onDone = {
                        navController.navigate(Routes.TIMER) {
                            popUpTo(Routes.TODAY) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.HISTORY) {
                FeedScreen(
                    onOpenReceipt = { id -> navController.navigate(Routes.sessionDetail(id)) },
                    onOpenStats = { navController.navigate(Routes.STATS) },
                )
            }

            composable(Routes.STATS) {
                StatsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTask = { key -> navController.navigate(Routes.taskStats(key)) },
                )
            }

            composable(
                route = Routes.TASK_STATS,
                arguments = listOf(
                    navArgument(Routes.ARG_TASK_KEY) { type = NavType.StringType; defaultValue = "" },
                ),
            ) {
                TaskStatsScreen(
                    onBack = { navController.popBackStack() },
                    onStarted = {
                        navController.navigate(Routes.TIMER) {
                            popUpTo(Routes.TODAY) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(
                route = Routes.SESSION_DETAIL,
                arguments = listOf(navArgument(Routes.ARG_RECEIPT_ID) { type = NavType.LongType }),
            ) {
                DetailScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
        }

        if (currentRoute in TAB_ROUTES) {
            BottomTabBar(
                currentRoute = currentRoute,
                onNavigate = { route -> navController.navigateToTab(route) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(bottom = 14.dp),
            )
        }
    }
}

/**
 * Combining `popUpTo(startDestination){saveState=true}` with `restoreState=true` while
 * navigating to that *same* start destination (Today) is a Navigation-Compose edge case that
 * can silently no-op — the requested back-stack shape already "matches" — which is why Today
 * could stop responding from other tabs. Today instead gets a plain, unambiguous "clear the
 * stack and go"; there's nothing on its own back-stack entry worth restoring anyway.
 */
private fun NavHostController.navigateToTab(route: String) {
    val isStart = route == Routes.TODAY
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = isStart
            saveState = !isStart
        }
        launchSingleTop = true
        restoreState = !isStart
    }
}

private data class TabSpec(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    TabSpec(Routes.TODAY, "Today", Icons.Outlined.Checklist),
    TabSpec(Routes.HISTORY, "History", Icons.Outlined.PhotoLibrary),
    TabSpec(Routes.SETTINGS, "Settings", Icons.Outlined.Settings),
)

@Composable
private fun BottomTabBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(FocusColors.Paper)
            .border(1.dp, FocusColors.LineSoft, RoundedCornerShape(28.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        TABS.forEach { tab ->
            val selected = currentRoute == tab.route
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigate(tab.route) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    tab.icon,
                    contentDescription = tab.label,
                    tint = if (selected) FocusColors.AccentBlue else FocusColors.Muted2,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    tab.label,
                    color = if (selected) FocusColors.AccentBlue else FocusColors.Muted2,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
