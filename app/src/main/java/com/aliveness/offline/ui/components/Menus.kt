package com.aliveness.offline.ui.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliveness.offline.MainViewModel
import com.aliveness.offline.data.VideoItem
import com.aliveness.offline.data.formatDate
import com.aliveness.offline.data.formatDuration
import com.aliveness.offline.data.formatSize
import com.aliveness.offline.ui.theme.AccentRed
import com.aliveness.offline.ui.theme.Dimens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

fun shareVideo(context: Context, video: VideoItem) {
    val uri = Uri.parse(video.uri)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = video.mime.ifBlank { "video/*" }
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(video.title, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share video"))
}

data class NavTab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

/**
 * Floating frosted-glass dock. Press anywhere on it and a glass lens pops up under your finger; slide to
 * another tab and let go to open it. A plain tap works the same way. With [liquid] off the lens is a flat pill.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassNavBar(
    tabs: List<NavTab>,
    selectedRoute: String?,
    onSelect: (NavTab) -> Unit,
    haze: HazeState,
    modifier: Modifier = Modifier,
    liquid: Boolean = true,
    haptics: Boolean = true,
    large: Boolean = true,
    glass: Int = 0,
) {
    val container = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline
    val barShape = RoundedCornerShape(32.dp)
    val lensShape = RoundedCornerShape(28.dp)
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val currentOnSelect by rememberUpdatedState(onSelect)
    val barH = if (large) 78.dp else 64.dp
    val fill = when (glass) { 0 -> 0.22f; 1 -> 0.55f; else -> 0.94f }
    val selectedIndex = tabs.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)

    BoxWithConstraints(modifier.padding(horizontal = if (large) 16.dp else 24.dp).fillMaxWidth().height(barH)) {
        val barW = with(density) { maxWidth.toPx() }
        val tabW = barW / tabs.size
        var pressed by remember { mutableStateOf(false) }
        var fingerX by remember { mutableFloatStateOf(0f) }

        val targetCenter = if (pressed) fingerX.coerceIn(tabW / 2, barW - tabW / 2) else tabW * (selectedIndex + 0.5f)
        val center by animateFloatAsState(
            targetValue = targetCenter,
            animationSpec = if (pressed) spring(stiffness = Spring.StiffnessHigh)
            else spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
            label = "lensX",
        )
        val pop by animateFloatAsState(
            targetValue = if (pressed && liquid) 1f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "pop",
        )
        val activeIndex = (center / tabW).toInt().coerceIn(0, tabs.size - 1)

        LaunchedEffect(activeIndex, pressed) {
            if (pressed && haptics && liquid) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        // The glass bar itself.
        Box(
            Modifier.matchParentSize().clip(barShape)
                .hazeEffect(haze, if (glass == 0) HazeMaterials.ultraThin(container) else HazeMaterials.regular(container))
                .background(container.copy(alpha = fill))
                .border(0.5.dp, outline.copy(alpha = 0.7f), barShape),
        )

        // The lens: sits above the bar so it can swell past its edges while pressed.
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((center - tabW / 2 + 0f).roundToInt(), 0) }
                .width(with(density) { tabW.toDp() })
                .height(barH - 12.dp)
                .graphicsLayer {
                    val sc = 1f + 0.34f * pop
                    scaleX = sc; scaleY = 1f + 0.42f * pop
                    translationY = -10.dp.toPx() * pop
                }
                .clip(lensShape)
                .hazeEffect(haze, HazeMaterials.ultraThin(container))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f + 0.16f * pop),
                            Color.White.copy(alpha = 0.03f + 0.05f * pop),
                        ),
                    ),
                )
                .border(
                    width = (0.6f + 0.6f * pop).dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.35f + 0.4f * pop), Color.White.copy(alpha = 0.06f)),
                    ),
                    shape = lensShape,
                ),
        )

        // Icons and labels.
        Row(Modifier.matchParentSize()) {
            tabs.forEachIndexed { i, tab ->
                val active = if (pressed) i == activeIndex else tab.route == selectedRoute
                val tint by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "tabTint",
                )
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(if (active) tab.selectedIcon else tab.icon, tab.label, tint = tint, modifier = Modifier.size(if (large) 28.dp else 24.dp))
                    Text(tab.label, color = tint, fontSize = if (large) 11.sp else 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.W600)
                }
            }
        }

        // One gesture for tap and drag: press pops the lens, slide moves it, release opens the tab under it.
        Box(
            Modifier.matchParentSize().pointerInput(tabs.size, barW) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    pressed = true
                    fingerX = down.position.x
                    var last = down.position.x
                    do {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id }
                        if (ch != null) {
                            last = ch.position.x
                            fingerX = last
                            ch.consume()
                        }
                    } while (ev.changes.any { it.pressed })
                    pressed = false
                    val idx = (last / tabW).toInt().coerceIn(0, tabs.size - 1)
                    currentOnSelect(tabs[idx])
                }
            },
        )
    }
}

@Composable
fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(60) },
                singleLine = true, label = { Text("Name") },
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }, enabled = text.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, tint: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = Dimens.PageGutter, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

/**
 * The long-press context menu. [target] is what was pressed; the sheet reads the live copy from the
 * database so Favorite/Watch later toggle visibly. [collectionId] adds "Remove from playlist".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoMenuHost(
    target: VideoItem?,
    onDismiss: () -> Unit,
    vm: MainViewModel,
    onPlay: (VideoItem) -> Unit,
    collectionId: Long? = null,
) {
    if (target == null) return
    val context = LocalContext.current
    val everything by vm.everything.collectAsState()
    val collections by vm.collections.collectAsState()
    val links by vm.links.collectAsState()
    val live = everything.firstOrNull { it.id == target.id } ?: target

    var picker by remember(target.id) { mutableStateOf(false) }
    var info by remember(target.id) { mutableStateOf(false) }
    var creating by remember(target.id) { mutableStateOf(false) }

    if (creating) {
        NameDialog(
            title = "New playlist", initial = "", confirmLabel = "Create",
            onConfirm = { vm.createCollection(it, thenAdd = live.id); creating = false; onDismiss() },
            onDismiss = { creating = false },
        )
    }
    if (info) {
        AlertDialog(
            onDismissRequest = { info = false },
            title = { Text("Video info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoLine("Name", live.fileName)
                    InfoLine("Folder", live.folderPath.ifBlank { "Internal storage" })
                    InfoLine("Duration", formatDuration(live.durationMs))
                    InfoLine("Size", formatSize(live.sizeBytes))
                    if (live.width > 0) InfoLine("Resolution", "${live.width} × ${live.height}")
                    InfoLine("Format", live.mime.ifBlank { live.extension })
                    InfoLine("Added", formatDate(live.dateAddedSec))
                    InfoLine("Plays", live.playCount.toString())
                }
            },
            confirmButton = { TextButton(onClick = { info = false }) { Text("Close") } },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Dimens.PageGutter, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val shape = RoundedCornerShape(Dimens.ThumbRadius)
                VideoThumb(live, Modifier.width(80.dp).height(46.dp).clip(shape))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(live.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${live.folderName} · ${formatDuration(live.durationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            if (!picker) {
                SheetAction(Icons.Rounded.PlayArrow, "Play") { onDismiss(); onPlay(live) }
                SheetAction(
                    if (live.favorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                    if (live.favorite) "Remove from Favorites" else "Add to Favorites",
                    tint = if (live.favorite) AccentRed else MaterialTheme.colorScheme.onSurface,
                ) { vm.toggleFavorite(live.id) }
                SheetAction(
                    if (live.watchLater) Icons.Rounded.Schedule else Icons.Outlined.Schedule,
                    if (live.watchLater) "Remove from Watch Later" else "Watch Later",
                ) { vm.toggleWatchLater(live.id) }
                SheetAction(Icons.Outlined.PlaylistAdd, "Add to playlist") { picker = true }
                if (collectionId != null) {
                    SheetAction(Icons.Rounded.RemoveCircleOutline, "Remove from this playlist") {
                        vm.removeFromCollection(collectionId, live.id); onDismiss()
                    }
                }
                SheetAction(Icons.Outlined.Share, "Share") { onDismiss(); shareVideo(context, live) }
                SheetAction(Icons.Outlined.Info, "Video info") { info = true }
            } else {
                SheetAction(Icons.Rounded.Add, "New playlist…") { creating = true }
                LazyColumn {
                    items(collections, key = { it.id }) { c ->
                        val inside = links.any { it.collectionId == c.id && it.videoId == live.id }
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    if (inside) vm.removeFromCollection(c.id, live.id) else vm.addToCollection(c.id, live.id)
                                }
                                .padding(horizontal = Dimens.PageGutter, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(c.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            if (inside) Icon(Icons.Rounded.Check, "In playlist")
                        }
                    }
                }
                if (collections.isEmpty()) {
                    Text(
                        "No playlists yet.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Dimens.PageGutter, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
