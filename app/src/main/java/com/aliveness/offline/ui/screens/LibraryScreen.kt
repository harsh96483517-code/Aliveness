package com.aliveness.offline.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aliveness.offline.MainViewModel
import com.aliveness.offline.data.Source
import com.aliveness.offline.ui.components.NameDialog
import com.aliveness.offline.ui.components.ScreenTitle
import com.aliveness.offline.ui.components.pressable
import com.aliveness.offline.ui.theme.AccentRed
import com.aliveness.offline.ui.theme.Dimens

private data class LibEntry(val icon: ImageVector, val title: String, val source: String?)

private val Entries = listOf(
    LibEntry(Icons.Rounded.VideoLibrary, "All Videos", Source.ALL),
    LibEntry(Icons.Rounded.MovieCreation, "Reels", Source.REELS),
    LibEntry(Icons.Rounded.Favorite, "Favorites", Source.FAV),
    LibEntry(Icons.Rounded.Schedule, "Watch Later", Source.LATER),
    LibEntry(Icons.Rounded.History, "Recently Watched", Source.WATCHED),
    LibEntry(Icons.Rounded.Download, "Downloads", Source.DOWNLOADS),
    LibEntry(Icons.Rounded.Folder, "Folders", null),
)

@Composable
fun LibraryScreen(
    vm: MainViewModel,
    onOpenList: (String) -> Unit,
    onOpenFolders: () -> Unit,
) {
    val items by vm.items.collectAsState()
    val links by vm.links.collectAsState()
    val collections by vm.collections.collectAsState()
    var creating by remember { mutableStateOf(false) }

    if (creating) {
        NameDialog("New playlist", "", "Create",
            onConfirm = { vm.createCollection(it); creating = false },
            onDismiss = { creating = false })
    }

    val counts = remember(items) {
        Entries.mapNotNull { e -> e.source?.let { it to Source.resolve(items, it, emptyList()).size } }.toMap()
    }
    val folderCount = remember(items) { items.map { it.folderPath }.distinct().size }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            bottom = Dimens.NavBarSpace,
        ),
    ) {
        item { ScreenTitle("Library") }
        items(Entries, key = { it.title }) { e ->
            val count = if (e.source == null) folderCount else counts[e.source] ?: 0
            LibraryRow(e.icon, e.title, count) {
                if (e.source == null) onOpenFolders() else onOpenList(e.source)
            }
            HorizontalDivider(Modifier.padding(start = 64.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = Dimens.PageGutter, end = 8.dp, top = 28.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Playlists", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, "New playlist") }
            }
        }
        if (collections.isEmpty()) {
            item {
                Text(
                    "Create playlists to group videos your way.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Dimens.PageGutter, vertical = 8.dp),
                )
            }
        }
        items(collections, key = { "c${it.id}" }) { c ->
            val ids = links.filter { it.collectionId == c.id }.map { it.videoId }
            val cover = ids.firstNotNullOfOrNull { id -> items.firstOrNull { it.id == id } }
            NavRow(c.name, "${ids.size} videos", cover) { onOpenList(Source.collection(c.id)) }
        }
    }
}

@Composable
private fun LibraryRow(icon: ImageVector, title: String, count: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressable(onClick, null, 0.99f)
            .padding(horizontal = Dimens.PageGutter, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = AccentRed, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
