package com.aliveness.offline.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aliveness.offline.MainViewModel
import com.aliveness.offline.data.Source
import com.aliveness.offline.data.VideoItem
import com.aliveness.offline.ui.components.GlassNavBar
import com.aliveness.offline.ui.components.NavTab
import com.aliveness.offline.ui.screens.ExploreScreen
import com.aliveness.offline.ui.screens.FoldersScreen
import com.aliveness.offline.ui.screens.HomeScreen
import com.aliveness.offline.ui.screens.LibraryScreen
import com.aliveness.offline.ui.screens.ListScreen
import com.aliveness.offline.ui.screens.PlayerScreen
import com.aliveness.offline.ui.screens.ReelsScreen
import com.aliveness.offline.ui.screens.SearchScreen
import com.aliveness.offline.ui.screens.SettingsScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

private const val REELS_TAB = "reels_tab"

private val Tabs = listOf(
    NavTab("home", "Home", Icons.Outlined.Home, Icons.Rounded.Home),
    NavTab("explore", "Explore", Icons.Outlined.Explore, Icons.Rounded.Explore),
    NavTab(REELS_TAB, "Reels", Icons.Outlined.PlayCircleOutline, Icons.Rounded.PlayCircle),
    NavTab("library", "Library", Icons.Outlined.VideoLibrary, Icons.Rounded.VideoLibrary),
    NavTab("search", "Search", Icons.Outlined.Search, Icons.Rounded.Search),
)

private fun NavController.openList(source: String) = navigate("list/${Uri.encode(source)}")

/** Shorts and Reels open the vertical viewer; everything else opens the full video player. */
private fun NavController.play(v: VideoItem, source: String) {
    val kind = if (v.isReel) "reels" else "player"
    navigate("$kind/${Uri.encode(source)}/${v.id}")
}

private fun isImmersive(route: String?) = route != null && (route.startsWith("reels") || route.startsWith("player"))

@Composable
fun AlivenessRoot(vm: MainViewModel, onImmersiveChanged: (Boolean) -> Unit) {
    val nav = rememberNavController()
    val haze = remember { HazeState() }
    val settings by vm.settings.collectAsState()
    val context = LocalContext.current
    val tabs = remember(settings.dockReels) { if (settings.dockReels) Tabs else Tabs.filter { it.route != REELS_TAB } }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route in tabs.map { it.route }
    val immersive = isImmersive(route)
    SideEffect { onImmersiveChanged(immersive) }

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when {
            isImmersive(targetState.destination.route) -> slideInVertically(tween(380)) { it } + fadeIn(tween(200))
            targetState.destination.route in Tabs.map { it.route } -> fadeIn(tween(220))
            else -> slideInHorizontally(tween(320)) { it / 8 } + fadeIn(tween(260))
        }
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isImmersive(targetState.destination.route)) fadeOut(tween(200)) else fadeOut(tween(180))
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { fadeIn(tween(220)) }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when {
            isImmersive(initialState.destination.route) -> slideOutVertically(tween(320)) { it } + fadeOut(tween(260))
            else -> slideOutHorizontally(tween(280)) { it / 8 } + fadeOut(tween(220))
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.fillMaxSize().hazeSource(haze),
            enterTransition = enter, exitTransition = exit,
            popEnterTransition = popEnter, popExitTransition = popExit,
        ) {
            composable("home") {
                HomeScreen(vm, onPlay = nav::play, onSeeAll = nav::openList, onSettings = { nav.navigate("settings") })
            }
            composable("explore") { ExploreScreen(vm, onPlay = nav::play, onOpenList = nav::openList) }
            composable("library") { LibraryScreen(vm, onOpenList = nav::openList, onOpenFolders = { nav.navigate("folders") }) }
            composable("search") { SearchScreen(vm, onPlay = nav::play) }
            composable("folders") { FoldersScreen(vm, onBack = { nav.popBackStack() }, onOpenList = nav::openList) }
            composable("settings") { SettingsScreen(vm, onBack = { nav.popBackStack() }, onFolders = { nav.navigate("folders") }) }
            composable("list/{source}", listOf(navArgument("source") { type = NavType.StringType })) { e ->
                ListScreen(e.arguments?.getString("source").orEmpty(), vm, onBack = { nav.popBackStack() }, onPlay = nav::play)
            }
            composable(
                "reels/{source}/{id}",
                listOf(navArgument("source") { type = NavType.StringType }, navArgument("id") { type = NavType.LongType }),
            ) { e ->
                ReelsScreen(e.arguments?.getString("source").orEmpty(), e.arguments?.getLong("id") ?: 0L, vm) { nav.popBackStack() }
            }
            composable(
                "player/{source}/{id}",
                listOf(navArgument("source") { type = NavType.StringType }, navArgument("id") { type = NavType.LongType }),
            ) { e ->
                PlayerScreen(e.arguments?.getString("source").orEmpty(), e.arguments?.getLong("id") ?: 0L, vm) { nav.popBackStack() }
            }
        }

        AnimatedVisibility(
            visible = showBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(280)) { it } + fadeIn(),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(),
        ) {
            GlassNavBar(
                tabs = tabs, selectedRoute = route, haze = haze,
                liquid = settings.liquidDock, haptics = settings.dockHaptics,
                large = settings.dockLarge, glass = settings.dockGlass,
                modifier = Modifier.navigationBarsPadding().padding(bottom = 12.dp),
                onSelect = { tab ->
                    if (tab.route == REELS_TAB) {
                        val reels = vm.queue(Source.REELS)
                        if (reels.isEmpty()) {
                            android.widget.Toast.makeText(context, "No Reels found yet. Portrait clips under 3 minutes show up here.", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            nav.navigate("reels/${Uri.encode(Source.REELS)}/${reels.first().id}")
                        }
                    } else if (tab.route != route) nav.navigate(tab.route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}
