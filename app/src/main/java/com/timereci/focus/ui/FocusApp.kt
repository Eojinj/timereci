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
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
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
import com.timereci.focus.ui.day.DayScreen
import com.timereci.focus.ui.detail.DetailScreen
import com.timereci.focus.ui.feed.FeedScreen
import com.timereci.focus.ui.nextup.BreakScreen
import com.timereci.focus.ui.nextup.NextUpScreen
import com.timereci.focus.ui.publish.PublishScreen
import com.timereci.focus.ui.settings.SettingsScreen
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.MonoFamily
import com.timereci.focus.ui.timer.TimerScreen
import com.timereci.focus.ui.todo.TodoScreen
import com.timereci.focus.ui.util.findActivity

/** The four top-level areas — everything else (publish, day, detail, …) is a drill-down. */
private val TAB_ROUTES = setOf(Routes.TIMER, Routes.FEED, Routes.TODO, Routes.SETTINGS)

@Composable
fun FocusApp(root: RootViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val resume by root.resume.collectAsStateWithLifecycle()
    val timerActive by root.timerActive.collectAsStateWithLifecycle()
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

    Box(Modifier.fillMaxSize()) {
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
                    // Stopping never navigates — it just resets in place, back to the star,
                    // regardless of whether this session was ad-hoc or pushed from the queue.
                    onAbandon = {},
                )
            }

            composable(Routes.FEED) {
                FeedScreen(
                    onOpenDay = { epochDay -> navController.navigate(Routes.day(epochDay)) },
                    onOpenReceipt = { id -> navController.navigate(Routes.detail(id)) },
                )
            }

            composable(Routes.TODO) {
                TodoScreen(
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
                SettingsScreen()
            }
        }

        // Hide the tab bar only on the timer screen while a session runs (so its stop/complete
        // controls own the bottom). On the other tabs it always shows — a session can keep
        // running in the background and you must still be able to navigate.
        val hideForRunningTimer = currentRoute == Routes.TIMER && timerActive
        if (currentRoute in TAB_ROUTES && !hideForRunningTimer) {
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
 * Standard bottom-nav pattern: keeps each tab's own back stack and scroll state — except for
 * Timer, which *is* the start destination. Combining `popUpTo(startDestination){saveState=true}`
 * with `restoreState=true` while navigating to that same start destination is an edge case
 * that can silently no-op (the requested back-stack shape already "matches"), which is why the
 * timer tab could stop responding: tapping it did nothing instead of returning to the timer.
 * Timer instead gets a plain, unambiguous "clear the stack and go" — nothing worth restoring
 * lives on its own back-stack entry anyway; the real session state is the singleton
 * FocusTimerController, not per-entry state.
 */
private fun NavHostController.navigateToTab(route: String) {
    val isTimer = route == Routes.timer()
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = isTimer
            saveState = !isTimer
        }
        launchSingleTop = true
        restoreState = !isTimer
    }
}

/**
 * [matchRoute] is the registered route *pattern* (what [NavDestination.route] reports, used to
 * tell which tab is selected); [target] is the concrete route actually passed to `navigate()`.
 * They differ only for Timer, whose pattern carries unfilled `{task}`/`{minutes}` placeholders.
 */
private data class TabSpec(val matchRoute: String, val target: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    TabSpec(Routes.TIMER, Routes.timer(), "타이머", Icons.Outlined.Timer),
    TabSpec(Routes.FEED, Routes.FEED, "피드", Icons.Outlined.BarChart),
    TabSpec(Routes.TODO, Routes.TODO, "할 일", Icons.Outlined.Checklist),
    TabSpec(Routes.SETTINGS, Routes.SETTINGS, "설정", Icons.Outlined.Settings),
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
            val selected = currentRoute == tab.matchRoute
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigate(tab.target) }
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
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
