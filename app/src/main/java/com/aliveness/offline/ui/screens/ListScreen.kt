package com.aliveness.offline.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aliveness.offline.MainViewModel
import com.aliveness.offline.data.SortOption
import com.aliveness.offline.data.Source
import com.aliveness.offline.data.VideoItem
import com.aliveness.offline.ui.components.EmptyState
import com.aliveness.offline.ui.components.NameDialog
import com.aliveness.offline.ui.components.SkeletonRows
import com.aliveness.offline.ui.components.SortViewControls
import com.aliveness.offline.ui.components.SubTopBar
import com.aliveness.offline.ui.components.VideoGridList
import com.aliveness.offline.ui.components.VideoMenuHost

/** Any named list of videos: a shelf's "See all", a folder, a library row or a custom playlist. */
@Composable
fun ListScreen(
    source: String,
    vm: MainViewModel,
    onBack: () -> Unit,
    onPlay: (VideoItem, String) -> Unit,
) {
    val items by vm.items.collectAsState()
    val links by vm.links.collectAsState()
    val collections by vm.collections.collectAsState()
    val loaded by vm.loaded.collectAsState()

    var sortName by rememberSaveable { mutableStateOf<String?>(null) }
    var asGrid by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf<VideoItem?>(null) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }

    val collectionId = source.takeIf { it.startsWith("col|") }?.removePrefix("col|")?.toLongOrNull()
    val title = Source.title(source, collections)
    val sort = sortName?.let { SortOption.valueOf(it) }
    val videos = remember(items, links, source, sort) {
        val base = Source.resolve(items, source, links)
        sort?.apply(base) ?: base
    }
    val portrait = source == Source.REELS || source == Source.FAV_REELS

    if (renaming && collectionId != null) {
        NameDialog("Rename playlist", title, "Save",
            onConfirm = { vm.renameCollection(collectionId, it); renaming = false },
            onDismiss = { renaming = false })
    }
    if (deleting && collectionId != null) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("Delete playlist?") },
            text = { Text("“$title” will be removed. Your videos stay on the phone.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteCollection(collectionId); deleting = false; onBack() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancel") } },
        )
    }

    Column(Modifier.fillMaxSize()) {
        SubTopBar(title, onBack) {
            SortViewControls(
                sort = sort, onSort = { sortName = it?.name },
                asGrid = asGrid, onToggleGrid = { asGrid = !asGrid },
                showDefault = collectionId != null,
            )
            if (collectionId != null) {
                IconButton(onClick = { overflow = true }) { Icon(Icons.Rounded.MoreVert, "Playlist options") }
                DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = { overflow = false; renaming = true })
                    DropdownMenuItem(text = { Text("Delete playlist") }, leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                        onClick = { overflow = false; deleting = true })
                }
            }
        }
        if (!loaded) {
            SkeletonRows()
        } else {
            VideoGridList(
                videos = videos, asGrid = asGrid, portraitCards = portrait,
                onOpen = { onPlay(it, source) }, onMenu = { menu = it },
                modifier = Modifier.fillMaxSize(),
                bottomPadding = 24.dp,
                topPadding = 0.dp,
                header = if (videos.isNotEmpty()) ({
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        androidx.compose.material3.Button(onClick = { onPlay(videos.first(), source) }) {
                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Play all · ${videos.size}")
                        }
                    }
                }) else null,
                empty = {
                    EmptyState(
                        Icons.Rounded.VideoLibrary, "Nothing here yet",
                        if (collectionId != null) "Long-press any video and choose “Add to playlist”."
                        else "Videos that match will show up here.",
                    )
                },
            )
        }
    }
    VideoMenuHost(menu, { menu = null }, vm, onPlay = { onPlay(it, source) }, collectionId = collectionId)
}
