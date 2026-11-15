@file:OptIn(ExperimentalMaterial3Api::class, androidx.media3.common.util.UnstableApi::class)

package com.aliveness.offline.ui.screens

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import com.aliveness.offline.MainViewModel
import com.aliveness.offline.data.VideoItem
import com.aliveness.offline.data.formatDuration
import com.aliveness.offline.player.RESIZE_FIT
import com.aliveness.offline.player.SpeedSteps
import com.aliveness.offline.player.VideoSurface
import com.aliveness.offline.player.rememberExoPlayer
import com.aliveness.offline.player.rememberPlayerUi
import com.aliveness.offline.player.rememberPosition
import com.aliveness.offline.player.speedLabel
import com.aliveness.offline.ui.components.ImmersiveMode
import com.aliveness.offline.ui.components.KeepScreenOn
import com.aliveness.offline.ui.components.OrientationLock
import com.aliveness.offline.ui.components.VideoMenuHost
import com.aliveness.offline.ui.components.VideoThumb
import com.aliveness.offline.ui.components.currentBrightness
import com.aliveness.offline.ui.components.findActivity
import com.aliveness.offline.ui.components.setWindowBrightness
import com.aliveness.offline.ui.components.shareVideo
import com.aliveness.offline.ui.theme.AccentRed
import com.aliveness.offline.ui.theme.Dimens
import kotlinx.coroutines.delay
import java.util.Locale

private enum class PlayerSheet { Speed, Audio, Subtitles }

private data class Hud(val icon: ImageVector?, val text: String)

@Composable
fun PlayerScreen(source: String, startId: Long, vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    val settings by vm.settings.collectAsState()
    val everything by vm.everything.collectAsState()

    val queue = remember(source) { vm.queue(source) }
    if (queue.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val startIndex = remember { queue.indexOfFirst { it.id == startId }.coerceAtLeast(0) }

    val player = rememberExoPlayer(settings.defaultSpeed)
    val ui = rememberPlayerUi(player)
    val live = remember(everything) { everything.associateBy { it.id } }
    val currentId = ui.mediaId?.toLongOrNull() ?: queue[startIndex].id
    val current = live[currentId] ?: queue.firstOrNull { it.id == currentId } ?: queue[startIndex]
    val pos by rememberPosition(player)

    var fullscreen by remember { mutableStateOf(false) }
    var orientation by remember { mutableStateOf<Int?>(null) }
    var controls by remember { mutableStateOf(true) }
    var sheet by remember { mutableStateOf<PlayerSheet?>(null) }
    var menu by remember { mutableStateOf<VideoItem?>(null) }
    var hud by remember { mutableStateOf<Hud?>(null) }
    var hudTick by remember { mutableIntStateOf(0) }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val full = fullscreen || isLandscape

    val saveCurrent = remember(player) {
        {
            val id = player.currentMediaItem?.mediaId?.toLongOrNull()
            if (id != null) {
                val d = player.duration
                vm.savePosition(id, player.currentPosition, if (d == C.TIME_UNSET) 0L else d)
            }
        }
    }
    fun leave() { saveCurrent(); onBack() }
    BackHandler {
        if (fullscreen) { fullscreen = false; orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT } else leave()
    }
    fun toggleFullscreen() {
        if (full) { fullscreen = false; orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        else { fullscreen = true; orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE }
    }
    fun showHud(icon: ImageVector?, text: String) { hud = Hud(icon, text); hudTick++ }

    // Whole queue goes to ExoPlayer, so previous/next/auto-advance are native and gapless.
    LaunchedEffect(player) {
        val s = vm.settings.value
        val first = vm.everything.value.firstOrNull { it.id == queue[startIndex].id } ?: queue[startIndex]
        player.setMediaItems(
            queue.map { MediaItem.Builder().setUri(it.uri).setMediaId(it.id.toString()).build() },
            startIndex,
            if (s.resumePlayback && first.inProgress) first.positionMs else 0L,
        )
        player.prepare()
        player.playWhenReady = s.autoplay
        vm.markStarted(first.id)
    }
    DisposableEffectTransitions(ui, player, vm)
    LaunchedEffect(player) { while (true) { delay(3000); saveCurrent() } }
    LaunchedEffect(hudTick) { if (hudTick > 0) { delay(800); hud = null } }
    LaunchedEffect(orientation) {
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            delay(900); orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    LaunchedEffect(controls, ui.isPlaying, full) {
        if (full && controls && ui.isPlaying) { delay(3200); controls = false }
    }
    OrientationLock(orientation)
    ImmersiveMode(hidden = full)
    KeepScreenOn()

    // Gesture state for brightness / volume.
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var side by remember { mutableStateOf(0) } // -1 left (brightness), 1 right (volume)
    var level by remember { mutableStateOf(0f) }

    val videoArea: @Composable (Modifier) -> Unit = { mod ->
        Box(
            mod.background(Color.Black)
                .pointerInput(settings.gestures) {
                    detectTapGestures(
                        onTap = { controls = !controls },
                        onDoubleTap = { o ->
                            if (!settings.gestures) return@detectTapGestures
                            val forward = o.x > size.width / 2
                            player.seekTo((player.currentPosition + if (forward) 10_000 else -10_000).coerceAtLeast(0))
                            showHud(if (forward) Icons.Rounded.Forward10 else Icons.Rounded.Replay10, if (forward) "+10s" else "−10s")
                        },
                    )
                }
                .pointerInput(settings.gestures) {
                    if (!settings.gestures) return@pointerInput
                    detectVerticalDragGestures(
                        onDragStart = { o ->
                            side = if (o.x < size.width / 2) -1 else 1
                            level = if (side < 0) (activity?.currentBrightness() ?: 0.5f)
                            else audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() /
                                audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                        },
                        onVerticalDrag = { change, dy ->
                            change.consume()
                            level = (level - dy / size.height * 1.2f).coerceIn(0f, 1f)
                            if (side < 0) {
                                activity?.setWindowBrightness(level)
                                showHud(Icons.Rounded.BrightnessMedium, "${(level * 100).toInt()}%")
                            } else {
                                val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                audio.setStreamVolume(AudioManager.STREAM_MUSIC, (level * max).toInt(), 0)
                                showHud(Icons.Rounded.VolumeUp, "${(level * 100).toInt()}%")
                            }
                        },
                    )
                },
        ) {
            VideoSurface(player, RESIZE_FIT, Modifier.fillMaxSize())
            if (ui.isBuffering) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White, strokeWidth = 2.dp)
            hud?.let { h ->
                Row(
                    Modifier.align(Alignment.Center).clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.62f)).padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    h.icon?.let { Icon(it, null, tint = Color.White, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)) }
                    Text(h.text, color = Color.White, fontWeight = FontWeight.W700)
                }
            }
            ui.error?.let {
                Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.ErrorOutline, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Text("This video can’t be played", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text("Its format or codec isn’t supported on this phone.", color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall)
                    if (player.hasNextMediaItem()) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { player.seekToNextMediaItem(); player.prepare(); player.play() }) { Text("Skip to next") }
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (full) {
            videoArea(Modifier.fillMaxSize())
            AnimatedVisibility(controls, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
                FullscreenControls(
                    title = current.title, ui = ui, player = player, positionMs = pos.first, durationMs = pos.second,
                    onClose = { toggleFullscreen() }, onBack = { leave() },
                    onSheet = { sheet = it }, saveCurrent = saveCurrent,
                )
            }
        } else {
            // Blurred still of the current video behind everything, like the reference's artwork backdrop.
            VideoThumb(current, Modifier.fillMaxSize().blur(70.dp))
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.9f))),
                ),
            )
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = Dimens.PageGutter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { leave() }) {
                        Icon(Icons.Rounded.KeyboardArrowDown, "Close player", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Text("Now Playing", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(onClick = { menu = current }) { Icon(Icons.Rounded.MoreHoriz, "More options", tint = Color.White) }
                }
                Spacer(Modifier.height(8.dp))
                val ar = if (current.width > 0 && current.height > 0) current.width.toFloat() / current.height else 16f / 9f
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    val shape = RoundedCornerShape(Dimens.CardRadius)
                    if (ar < 1f) videoArea(Modifier.fillMaxSize().aspectRatio(ar).clip(shape))
                    else videoArea(Modifier.fillMaxWidth().aspectRatio(ar.coerceAtMost(2f)).clip(shape))
                }
                Spacer(Modifier.height(18.dp))
                Column(Modifier.fillMaxWidth()) {
                    Text(current.title, color = Color.White, style = MaterialTheme.typography.headlineMedium,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${current.folderName} · ${current.extension}", color = Color.White.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
                Spacer(Modifier.height(10.dp))
                SeekStrip(pos.first, pos.second, { f -> if (pos.second > 0) player.seekTo((pos.second * f).toLong()) })
                Row(Modifier.fillMaxWidth()) {
                    Text(formatDuration(pos.first), color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.weight(1f))
                    Text("-" + formatDuration((pos.second - pos.first).coerceAtLeast(0)), color = Color.White.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(8.dp))
                TransportRow(ui, player, saveCurrent, big = true)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    ChipButton(speedLabel(ui.speed)) { sheet = PlayerSheet.Speed }
                    SmallIcon(Icons.Rounded.Audiotrack, "Audio track") { sheet = PlayerSheet.Audio }
                    SmallIcon(Icons.Rounded.Subtitles, "Subtitles") { sheet = PlayerSheet.Subtitles }
                    SmallIcon(Icons.Rounded.Fullscreen, "Full screen") { toggleFullscreen() }
                    SmallIcon(
                        if (current.favorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder, "Favorite",
                        tint = if (current.favorite) AccentRed else Color.White,
                    ) { vm.toggleFavorite(current.id) }
                    SmallIcon(Icons.Outlined.Share, "Share") { shareVideo(context, current) }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    sheet?.let { which ->
        ModalBottomSheet(onDismissRequest = { sheet = null }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
                when (which) {
                    PlayerSheet.Speed -> {
                        SheetTitle("Playback speed")
                        SpeedSteps.forEach { sp ->
                            SheetOption(speedLabel(sp), ui.speed == sp) { player.setPlaybackSpeed(sp); sheet = null }
                        }
                    }
                    PlayerSheet.Audio -> TrackList("Audio track", C.TRACK_TYPE_AUDIO, ui, player, allowOff = false) { sheet = null }
                    PlayerSheet.Subtitles -> TrackList("Subtitles", C.TRACK_TYPE_TEXT, ui, player, allowOff = true) { sheet = null }
                }
            }
        }
    }
    VideoMenuHost(menu, { menu = null }, vm, onPlay = { })
}

/** Records a play when ExoPlayer moves to another item, and resumes that item's saved position. */
@Composable
private fun DisposableEffectTransitions(ui: com.aliveness.offline.player.PlayerUi, player: Player, vm: MainViewModel) {
    androidx.compose.runtime.DisposableEffect(ui, player) {
        ui.onTransition = { item ->
            val id = item?.mediaId?.toLongOrNull()
            if (id != null) {
                val v = vm.everything.value.firstOrNull { it.id == id }
                if (v != null) {
                    vm.markStarted(id)
                    if (vm.settings.value.resumePlayback && v.inProgress && player.currentPosition < 1_000) {
                        player.seekTo(v.positionMs)
                    }
                }
            }
        }
        onDispose { ui.onTransition = null }
    }
}

@Composable
private fun TransportRow(ui: com.aliveness.offline.player.PlayerUi, player: Player, saveCurrent: () -> Unit, big: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            player.repeatMode = when (ui.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }) {
            Icon(
                if (ui.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                "Repeat",
                tint = if (ui.repeatMode == Player.REPEAT_MODE_OFF) Color.White.copy(alpha = 0.45f) else Color.White,
            )
        }
        IconButton(onClick = { saveCurrent(); player.seekToPrevious() }, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(40.dp))
        }
        Box(
            Modifier.size(if (big) 72.dp else 60.dp).clip(CircleShape).background(Color.White)
                .clickable { if (ui.ended) { player.seekTo(0); player.play() } else if (ui.isPlaying) player.pause() else player.play() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (ui.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (ui.isPlaying) "Pause" else "Play", tint = Color.Black, modifier = Modifier.size(38.dp))
        }
        IconButton(
            onClick = { saveCurrent(); player.seekToNextMediaItem() },
            enabled = player.hasNextMediaItem(), modifier = Modifier.size(56.dp),
        ) {
            Icon(Icons.Rounded.SkipNext, "Next",
                tint = if (player.hasNextMediaItem()) Color.White else Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun FullscreenControls(
    title: String,
    ui: com.aliveness.offline.player.PlayerUi,
    player: Player,
    positionMs: Long,
    durationMs: Long,
    onClose: () -> Unit,
    onBack: () -> Unit,
    onSheet: (PlayerSheet) -> Unit,
    saveCurrent: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f))) {
        Row(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.KeyboardArrowDown, "Close player", tint = Color.White) }
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        Box(Modifier.align(Alignment.Center).fillMaxWidth(0.6f)) { TransportRow(ui, player, saveCurrent, big = false) }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp)) {
            SeekStrip(positionMs, durationMs, { f -> if (durationMs > 0) player.seekTo((durationMs * f).toLong()) })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${formatDuration(positionMs)} / ${formatDuration(durationMs)}", color = Color.White,
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                ChipButton(speedLabel(ui.speed)) { onSheet(PlayerSheet.Speed) }
                SmallIcon(Icons.Rounded.Audiotrack, "Audio track") { onSheet(PlayerSheet.Audio) }
                SmallIcon(Icons.Rounded.Subtitles, "Subtitles") { onSheet(PlayerSheet.Subtitles) }
                SmallIcon(Icons.Rounded.FullscreenExit, "Exit full screen") { onClose() }
            }
        }
    }
}

@Composable
private fun ChipButton(text: String, onClick: () -> Unit) {
    Text(
        text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.W700,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun SmallIcon(icon: ImageVector, label: String, tint: Color = Color.White, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icon, label, tint = tint) }
}

@Composable
private fun SheetTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = Dimens.PageGutter, vertical = 8.dp))
}

@Composable
private fun SheetOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Dimens.PageGutter, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Rounded.Check, "Selected")
    }
}

/** Audio / subtitle choices read from the tracks the file actually carries. */
@Composable
private fun TrackList(
    title: String,
    type: Int,
    ui: com.aliveness.offline.player.PlayerUi,
    player: Player,
    allowOff: Boolean,
    onDone: () -> Unit,
) {
    SheetTitle(title)
    val groups = ui.tracks.groups.filter { it.type == type && it.isSupported }
    val disabled = player.trackSelectionParameters.disabledTrackTypes.contains(type)
    if (groups.isEmpty()) {
        Text(
            if (type == C.TRACK_TYPE_TEXT) "This video has no embedded subtitles." else "This video has only one audio track.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.PageGutter, vertical = 8.dp),
        )
        return
    }
    if (allowOff) {
        SheetOption("Off", disabled) {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(type, true).build()
            onDone()
        }
    }
    groups.forEach { g ->
        for (i in 0 until g.length) {
            val f = g.getTrackFormat(i)
            val lang = f.language?.takeIf { it != "und" }?.let { Locale.forLanguageTag(it).displayLanguage }
            val name = f.label ?: lang ?: "Track ${i + 1}"
            val extra = if (type == C.TRACK_TYPE_AUDIO && f.channelCount > 0) " · ${f.channelCount}ch" else ""
            SheetOption(name + extra, g.isTrackSelected(i) && !disabled) {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(type, false)
                    .clearOverridesOfType(type)
                    .addOverride(TrackSelectionOverride(g.mediaTrackGroup, i))
                    .build()
                onDone()
            }
        }
    }
}
