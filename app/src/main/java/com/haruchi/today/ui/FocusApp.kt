package com.haruchi.today.ui

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
import androidx.compose.runtime.CompositionLocalProvider
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
import com.haruchi.today.ui.detail.DetailScreen
import com.haruchi.today.ui.history.HistoryScreen
import com.haruchi.today.ui.i18n.LocalStrings
import com.haruchi.today.ui.i18n.Strings
import com.haruchi.today.ui.completion.CompletionSheet
import com.haruchi.today.ui.nextup.BreakScreen
import com.haruchi.today.ui.quickstart.QuickStartScreen
import com.haruchi.today.ui.settings.SettingsScreen
import com.haruchi.today.ui.stats.StatsScreen
import com.haruchi.today.ui.stats.TaskStatsScreen
import com.haruchi.today.ui.theme.FocusColors
import com.haruchi.today.ui.timer.TimerScreen
import com.haruchi.today.ui.today.TodayScreen
import com.haruchi.today.ui.util.findActivity

/** The three tabs — everything else (quick start, timer, session complete, …) is a drill-down. */
private val TAB_ROUTES = setOf(Routes.TODAY, Routes.HISTORY, Routes.SETTINGS)

@Composable
fun FocusApp(root: RootViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val resume by root.resume.collectAsStateWithLifecycle()
    val strings by root.strings.collectAsStateWithLifecycle()
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
            null -> Unit
        }
    }

    CompositionLocalProvider(LocalStrings provides strings) {
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
                    TodayScreen(
                        onOpenQuickStart = { navController.navigate(Routes.QUICK_START) },
                        onStartedSession = {
                            navController.navigate(Routes.TIMER) { launchSingleTop = true }
                        },
                        onOpenTaskStats = { key -> navController.navigate(Routes.taskStats(key)) },
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
                        // The session is saved the moment it completes and the sheet rides
                        // on top of Today, so finishing just walks back out of the timer.
                        onCompleted = {
                            navController.navigate(Routes.TODAY) {
                                popUpTo(Routes.TODAY) { inclusive = false }
                            }
                        },
                        // Stopping never navigates — it just resets in place, back to Today.
                        onAbandon = {
                            navController.navigate(Routes.TODAY) {
                                popUpTo(Routes.TODAY) { inclusive = false }
                            }
                        },
                    )
                }

                composable(
                    route = Routes.BREAK,
                    arguments = listOf(
                        navArgument(Routes.ARG_BREAK_MINUTES) { type = NavType.IntType },
                    ),
                ) {
                    BreakScreen(
                        onDone = {
                            navController.navigate(Routes.TODAY) {
                                popUpTo(Routes.TODAY) { inclusive = false }
                            }
                        },
                    )
                }

                composable(Routes.HISTORY) {
                    HistoryScreen(
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

            // Outside the NavHost: a completed session should be able to surface over
            // whatever screen happens to be showing.
            CompletionSheet(
                onStartNext = {
                    navController.navigate(Routes.TIMER) {
                        popUpTo(Routes.TODAY) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onTakeBreak = { minutes -> navController.navigate(Routes.breakScreen(minutes)) },
            )

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

    private data class TabSpec(val route: String, val label: (Strings) -> String, val icon: ImageVector)

    private val TABS = listOf(
        TabSpec(Routes.TODAY, { it.tabToday }, Icons.Outlined.Checklist),
        TabSpec(Routes.HISTORY, { it.tabHistory }, Icons.Outlined.PhotoLibrary),
        TabSpec(Routes.SETTINGS, { it.tabSettings }, Icons.Outlined.Settings),
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
            val strings = LocalStrings.current
            TABS.forEach { tab ->
                val selected = currentRoute == tab.route
                val label = tab.label(strings)
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigate(tab.route) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = label,
                        tint = if (selected) FocusColors.AccentBlue else FocusColors.Muted2,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        label,
                        color = if (selected) FocusColors.AccentBlue else FocusColors.Muted2,
                        fontSize = 10.sp,
                    )
                }
            }
    }
}
