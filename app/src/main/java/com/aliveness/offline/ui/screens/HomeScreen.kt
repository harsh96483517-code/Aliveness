package com.aliveness.offline.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aliveness.offline.MainViewModel
import com.aliveness.offline.data.Source
import com.aliveness.offline.data.VideoItem
import com.aliveness.offline.ui.components.EmptyState
import com.aliveness.offline.ui.components.FeaturedCard
import com.aliveness.offline.ui.components.ScreenTitle
import com.aliveness.offline.ui.components.SkeletonShelf
import com.aliveness.offline.ui.components.VideoMenuHost
import com.aliveness.offline.ui.components.VideoShelf
import com.aliveness.offline.ui.theme.Dimens

private data class ShelfDef(val title: String, val source: String, val portrait: Boolean)

private val Shelves = listOf(
    ShelfDef("Recently Watched", Source.WATCHED, false),
    ShelfDef("Continue Watching", Source.CONTINUE, false),
    ShelfDef("Recently Added", Source.ADDED, false),
    ShelfDef("All Reels", Source.REELS, true),
    ShelfDef("Favorite Reels", Source.FAV_REELS, true),
    ShelfDef("Most Played", Source.PLAYED, false),
    ShelfDef("Watch Later", Source.LATER, false),
    ShelfDef("Recently Downloaded", Source.DOWNLOADS, false),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onPlay: (VideoItem, String) -> Unit,
    onSeeAll: (String) -> Unit,
    onSettings: () -> Unit,
) {
    val items by vm.items.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val scanning by vm.scanning.collectAsState()
    var menu by remember { mutableStateOf<VideoItem?>(null) }

    val shelves = remember(items) {
        Shelves.map { it to Source.resolve(items, it.source, emptyList()) }.filter { it.second.isNotEmpty() }
    }
    val featured = remember(items) {
        items.filter { it.inProgress }.maxByOrNull { it.lastWatchedAt }?.let { it to "Continue watching" }
            ?: items.firstOrNull()?.let { it to "Latest" }
    }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    PullToRefreshBox(isRefreshing = scanning, onRefresh = { vm.refresh() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = top, bottom = Dimens.NavBarSpace),
        ) {
            item {
                ScreenTitle("Home") {
                    IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Settings") }
                }
            }
            when {
                !loaded || (items.isEmpty() && scanning) -> {
                    item { SkeletonShelf() }
                    item { SkeletonShelf(portrait = true) }
                    item { SkeletonShelf() }
                }
                items.isEmpty() -> item {
                    EmptyState(
                        icon = Icons.Rounded.VideoLibrary,
                        title = "No videos yet",
                        message = "Aliveness plays the videos and Reels stored on this phone. Pull down or tap below to scan.",
                        actionLabel = "Scan videos",
                        onAction = { vm.refresh() },
                    )
                }
                else -> {
                    featured?.let { (v, label) ->
                        item {
                            FeaturedCard(
                                v, label,
                                onClick = { onPlay(v, if (v.isReel) Source.REELS else Source.ALL) },
                                onLongClick = { menu = v },
                                modifier = Modifier.padding(horizontal = Dimens.PageGutter).padding(bottom = 26.dp),
                            )
                        }
                    }
                    shelves.forEach { (def, videos) ->
                        item(key = def.source) {
                            VideoShelf(
                                title = def.title, videos = videos, portrait = def.portrait,
                                onSeeAll = { onSeeAll(def.source) },
                                onOpen = { onPlay(it, def.source) },
                                onMenu = { menu = it },
                            )
                        }
                    }
                }
            }
        }
    }
    VideoMenuHost(menu, { menu = null }, vm, onPlay = { onPlay(it, Source.ALL) })
}
