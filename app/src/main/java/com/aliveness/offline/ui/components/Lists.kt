package com.aliveness.offline.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aliveness.offline.data.SortOption
import com.aliveness.offline.data.VideoItem
import com.aliveness.offline.ui.theme.Dimens

/** Grid of cards or a list of rows, with an optional full-width header/empty block on top. */
@Composable
fun VideoGridList(
    videos: List<VideoItem>,
    asGrid: Boolean,
    portraitCards: Boolean,
    onOpen: (VideoItem) -> Unit,
    onMenu: (VideoItem) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    bottomPadding: androidx.compose.ui.unit.Dp = Dimens.NavBarSpace,
    topPadding: androidx.compose.ui.unit.Dp? = null,
    header: (@Composable () -> Unit)? = null,
    empty: (@Composable () -> Unit)? = null,
) {
    val top = topPadding ?: WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (asGrid) 2 else 1),
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(
            start = if (asGrid) Dimens.PageGutter else 0.dp,
            end = if (asGrid) Dimens.PageGutter else 0.dp,
            top = top, bottom = bottomPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(Dimens.ShelfGap),
        verticalArrangement = Arrangement.spacedBy(if (asGrid) 18.dp else 0.dp),
    ) {
        if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
        if (videos.isEmpty() && empty != null) item(span = { GridItemSpan(maxLineSpan) }) { empty() }
        items(videos, key = { it.id }) { v ->
            if (asGrid) {
                VideoCard(v, { onOpen(v) }, { onMenu(v) }, width = null, portrait = portraitCards)
            } else {
                VideoRow(v, { onOpen(v) }, { onMenu(v) })
            }
        }
    }
}

/** Sort dropdown + grid/list toggle, the pair of controls every browse screen shares. */
@Composable
fun SortViewControls(
    sort: SortOption?,
    onSort: (SortOption?) -> Unit,
    asGrid: Boolean,
    onToggleGrid: () -> Unit,
    showDefault: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { open = true }) { Icon(Icons.Rounded.Sort, "Sort") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (showDefault) {
                DropdownMenuItem(
                    text = { Text(if (sort == null) "• Playlist order" else "Playlist order") },
                    onClick = { onSort(null); open = false },
                )
            }
            SortOption.values().forEach { o ->
                DropdownMenuItem(
                    text = { Text(if (o == sort) "• ${o.label}" else o.label) },
                    onClick = { onSort(o); open = false },
                )
            }
        }
        IconButton(onClick = onToggleGrid) {
            Icon(if (asGrid) Icons.Rounded.ViewList else Icons.Rounded.GridView, if (asGrid) "List view" else "Grid view")
        }
    }
}

@Composable
fun SubTopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        Text(
            title, style = MaterialTheme.typography.titleLarge,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        actions()
    }
}
