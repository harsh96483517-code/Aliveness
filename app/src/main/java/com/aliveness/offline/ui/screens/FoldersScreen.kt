package com.aliveness.offline.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.aliveness.offline.MainViewModel
import com.aliveness.offline.data.Source
import com.aliveness.offline.data.formatSize
import com.aliveness.offline.data.toFolders
import com.aliveness.offline.ui.components.EmptyState
import com.aliveness.offline.ui.components.SubTopBar
import com.aliveness.offline.ui.theme.Dimens

/** Every folder that holds video, including hidden ones, so they can be brought back. */
@Composable
fun FoldersScreen(vm: MainViewModel, onBack: () -> Unit, onOpenList: (String) -> Unit) {
    val everything by vm.everything.collectAsState()
    val settings by vm.settings.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val folders = remember(everything) { everything.toFolders() }

    Column(Modifier.fillMaxSize()) {
        SubTopBar("Folders", onBack) {
            IconButton(onClick = { vm.refresh() }, enabled = !scanning) { Icon(Icons.Rounded.Refresh, "Refresh library") }
        }
        if (folders.isEmpty()) {
            EmptyState(Icons.Rounded.Folder, "No folders", "Scan your library to list folders with videos.",
                actionLabel = "Scan videos", onAction = { vm.refresh() })
        }
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
            item {
                if (folders.isNotEmpty()) Text(
                    "Hide a folder to keep its videos out of Home, Explore, Library and Search. Files are never touched.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Dimens.PageGutter, vertical = 4.dp),
                )
            }
            items(folders, key = { it.path }) { f ->
                val hidden = f.path in settings.hiddenFolders
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box_(Modifier.weight(1f).alpha(if (hidden) 0.45f else 1f)) {
                        NavRow(
                            title = f.name,
                            subtitle = (if (hidden) "Hidden · " else "") + "${f.count} videos · ${formatSize(f.sizeBytes)}",
                            cover = f.cover,
                            onClick = { if (!hidden) onOpenList(Source.folder(f.path)) },
                        )
                    }
                    IconButton(onClick = { vm.setFolderHidden(f.path, !hidden) }) {
                        Icon(if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            if (hidden) "Show folder" else "Hide folder")
                    }
                }
            }
        }
    }
}

@Composable
private fun Box_(modifier: Modifier, content: @Composable () -> Unit) =
    androidx.compose.foundation.layout.Box(modifier) { content() }
