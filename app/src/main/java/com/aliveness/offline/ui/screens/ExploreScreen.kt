package com.aliveness.offline.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aliveness.offline.MainViewModel
import com.aliveness.offline.data.SortOption
import com.aliveness.offline.data.Source
import com.aliveness.offline.data.VideoItem
import com.aliveness.offline.data.formatSize
import com.aliveness.offline.data.toFolders
import com.aliveness.offline.ui.components.EmptyState
import com.aliveness.offline.ui.components.ScreenTitle
import com.aliveness.offline.ui.components.SkeletonRows
import com.aliveness.offline.ui.components.SortViewControls
import com.aliveness.offline.ui.components.VideoGridList
import com.aliveness.offline.ui.components.VideoMenuHost
import com.aliveness.offline.ui.components.VideoThumb
import com.aliveness.offline.ui.components.pressable
import com.aliveness.offline.ui.components.thumbBorder
import com.aliveness.offline.ui.theme.Dimens

private enum class Category(val label: String) {
    All("All"), Reels("Reels"), Long("Long videos"), Recent("Recently added"),
    Folders("Folders"), Collections("Collections"),
}

@Composable
fun ExploreScreen(
    vm: MainViewModel,
    onPlay: (VideoItem, String) -> Unit,
    onOpenList: (String) -> Unit,
) {
    val items by vm.items.collectAsState()
    val links by vm.links.collectAsState()
    val collections by vm.collections.collectAsState()
    val loaded by vm.loaded.collectAsState()

    var catName by rememberSaveable { mutableStateOf(Category.All.name) }
    var sortName by rememberSaveable { mutableStateOf(SortOption.Newest.name) }
    var asGrid by rememberSaveable { mutableStateOf(true) }
    var menu by remember { mutableStateOf<VideoItem?>(null) }
    val cat = Category.valueOf(catName)
    val sort = SortOption.valueOf(sortName)

    val source = when (cat) {
        Category.All -> Source.ALL
        Category.Reels -> Source.REELS
        Category.Long -> Source.LONG
        Category.Recent -> Source.ADDED
        else -> Source.ALL
    }
    val videos = remember(items, cat, sort) {
        val base = Source.resolve(items, source, emptyList())
        val limited = if (cat == Category.Recent) base.take(60) else base
        sort.apply(limited)
    }

    val header: @Composable () -> Unit = {
        Column {
            ScreenTitle("Explore")
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dimens.PageGutter),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Category.values().toList()) { c ->
                    FilterChip(selected = c == cat, onClick = { catName = c.name }, label = { Text(c.label) })
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }

    when (cat) {
        Category.Folders, Category.Collections -> {
            val folders = remember(items) { items.toFolders() }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    bottom = Dimens.NavBarSpace,
                ),
            ) {
                item { header() }
                if (cat == Category.Folders) {
                    if (folders.isEmpty()) item { EmptyState(Icons.Rounded.Folder, "No folders", "Scan your videos to see the folders they live in.") }
                    items(folders, key = { it.path }) { f ->
                        NavRow(
                            title = f.name,
                            subtitle = "${f.count} videos · ${formatSize(f.sizeBytes)}",
                            cover = f.cover,
                            onClick = { onOpenList(Source.folder(f.path)) },
                        )
                    }
                } else {
                    if (collections.isEmpty()) item {
                        EmptyState(Icons.Rounded.PlaylistPlay, "No collections", "Create playlists from the Library tab, or long-press a video and choose “Add to playlist”.")
                    }
                    items(collections, key = { it.id }) { c ->
                        val ids = links.filter { it.collectionId == c.id }.map { it.videoId }
                        val cover = ids.firstNotNullOfOrNull { id -> items.firstOrNull { it.id == id } }
                        NavRow(c.name, "${ids.size} videos", cover) { onOpenList(Source.collection(c.id)) }
                    }
                }
            }
        }
        else -> {
            if (!loaded) {
                Column { header(); SkeletonRows() }
            } else {
                VideoGridList(
                    videos = videos, asGrid = asGrid, portraitCards = cat == Category.Reels,
                    onOpen = { onPlay(it, source) }, onMenu = { menu = it },
                    modifier = Modifier.fillMaxSize(),
                    header = {
                        Column {
                            header()
                            Row(
                                Modifier.fillMaxWidth().padding(start = Dimens.PageGutter, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${videos.size} videos · ${sort.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                SortViewControls(sort, { sortName = (it ?: SortOption.Newest).name }, asGrid, { asGrid = !asGrid })
                            }
                        }
                    },
                    empty = { EmptyState(Icons.Rounded.VideoLibrary, "Nothing here", "No videos match this category yet.") },
                )
            }
        }
    }
    VideoMenuHost(menu, { menu = null }, vm, onPlay = { onPlay(it, source) })
}

@Composable
internal fun NavRow(title: String, subtitle: String, cover: VideoItem?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressable(onClick, null, 0.985f)
            .padding(horizontal = Dimens.PageGutter, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shape = RoundedCornerShape(Dimens.ThumbRadius + 3.dp)
        Box(
            Modifier.size(width = 84.dp, height = 56.dp).clip(shape).thumbBorder(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (cover != null) VideoThumb(cover, Modifier.fillMaxSize())
            else Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
