@file:OptIn(ExperimentalFoundationApi::class, androidx.media3.common.util.UnstableApi::class)

package com.aliveness.offline.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.aliveness.offline.MainViewModel
import com.aliveness.offline.data.VideoItem
import com.aliveness.offline.data.formatDuration
import com.aliveness.offline.player.RESIZE_CROP
import com.aliveness.offline.player.RESIZE_FIT
import com.aliveness.offline.player.SpeedSteps
import com.aliveness.offline.player.VideoSurface
import com.aliveness.offline.player.rememberExoPlayer
import com.aliveness.offline.player.rememberPlayerUi
import com.aliveness.offline.player.rememberPosition
import com.aliveness.offline.player.speedLabel
import com.aliveness.offline.ui.components.AppLogo
import com.aliveness.offline.ui.components.KeepScreenOn
import com.aliveness.offline.ui.components.VideoMenuHost
import com.aliveness.offline.ui.components.VideoThumb
import com.aliveness.offline.ui.components.shareVideo
import com.aliveness.offline.ui.theme.AccentRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen vertical viewer. One ExoPlayer is bound to the settled page only; every other page
 * shows its still frame, so nothing off-screen is ever decoded.
 */
@Composable
fun ReelsScreen(source: String, startId: Long, vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by vm.settings.collectAsState()
    val everything by vm.everything.collectAsState()

    // The queue is captured once, so favouriting a clip never reshuffles what you are swiping through.
    val queue = remember(source) { vm.queue(source) }
    if (queue.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val startIndex = remember { queue.indexOfFirst { it.id == startId }.coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = startIndex) { queue.size }
    val settled = pagerState.settledPage

    val player = rememberExoPlayer(settings.defaultSpeed)
    val ui = rememberPlayerUi(player)
    val live = remember(everything) { everything.associateBy { it.id } }
    var crop by remember(settings.reelsCrop) { mutableStateOf(settings.reelsCrop) }
    var menu by remember { mutableStateOf<VideoItem?>(null) }
    var muted by remember(settings.reelsMuted) { mutableStateOf(settings.reelsMuted) }
    var heartTick by remember { mutableIntStateOf(0) }
    var showHeart by remember { mutableStateOf(false) }

    val saveCurrent = remember(player) {
        {
            val id = player.currentMediaItem?.mediaId?.toLongOrNull()
            if (id != null) {
                val d = player.duration
                vm.savePosition(id, player.currentPosition, if (d == C.TIME_UNSET) 0L else d)
            }
        }
    }
    BackHandler { saveCurrent(); onBack() }

    // Load whichever page the pager settles on.
    LaunchedEffect(settled) {
        saveCurrent()
        val v = queue[settled]
        val s = vm.settings.value
        val saved = vm.everything.value.firstOrNull { it.id == v.id } ?: v
        val item = MediaItem.Builder().setUri(v.uri).setMediaId(v.id.toString()).build()
        val start = if (s.resumePlayback && saved.inProgress) saved.positionMs else 0L
        player.setMediaItem(item, start)
        player.prepare()
        player.playWhenReady = s.autoplay
        vm.markStarted(v.id)
    }
    LaunchedEffect(settings.autoNext) {
        player.repeatMode = if (settings.autoNext) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
    }
    // Reel finished: glide to the next one, or loop the last.
    LaunchedEffect(ui.playbackState) {
        if (ui.playbackState == Player.STATE_ENDED && settings.autoNext) {
            if (settled < queue.lastIndex) pagerState.animateScrollToPage(settled + 1)
            else { player.seekTo(0); player.play() }
        }
    }
    LaunchedEffect(player) { while (true) { delay(3000); saveCurrent() } }
    LaunchedEffect(heartTick) {
        if (heartTick > 0) { showHeart = true; delay(650); showHeart = false }
    }
    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }
    KeepScreenOn()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0,
            key = { queue[it].id },
        ) { page ->
            val v = live[queue[page].id] ?: queue[page]
            val isCurrent = page == settled
            val currentV by rememberUpdatedState(v)
            val pos by rememberPosition(player, active = isCurrent)

            Box(Modifier.fillMaxSize()) {
                // Still frame: shown on every off-screen page and until the first video frame renders.
                androidx.compose.animation.AnimatedVisibility(
                    visible = !(isCurrent && ui.firstFrame),
                    enter = fadeIn(), exit = fadeOut(),
                ) { VideoThumb(v, Modifier.fillMaxSize()) }

                if (isCurrent) {
                    VideoSurface(player, if (crop) RESIZE_CROP else RESIZE_FIT, Modifier.fillMaxSize())
                }

                // Tap = pause/resume, double tap = favourite, long press = actions.
                Box(
                    Modifier.fillMaxSize().pointerInput(page, isCurrent) {
                        detectTapGestures(
                            onTap = { if (isCurrent) { if (player.isPlaying) player.pause() else player.play() } },
                            onDoubleTap = {
                                if (!currentV.favorite) vm.toggleFavorite(currentV.id)
                                heartTick++
                            },
                            onLongPress = { player.pause(); menu = currentV },
                        )
                    },
                )

                // Scrims for legibility.
                Box(
                    Modifier.fillMaxWidth().height(140.dp).align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent))),
                )
                Box(
                    Modifier.fillMaxWidth().height(260.dp).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)))),
                )

                if (isCurrent) {
                    if (ui.isBuffering) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White, strokeWidth = 2.dp)
                    AnimatedVisibility(
                        visible = !ui.isPlaying && ui.playbackState == Player.STATE_READY && ui.error == null,
                        modifier = Modifier.align(Alignment.Center),
                        enter = scaleIn(spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        Box(Modifier.size(76.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(44.dp))
                        }
                    }
                    ui.error?.let {
                        Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.ErrorOutline, null, tint = Color.White, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("This video can’t be played", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text("The format or codec isn’t supported on this phone.", color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(14.dp))
                            if (settled < queue.lastIndex) {
                                Button(onClick = { scope.launch { pagerState.animateScrollToPage(settled + 1) } }) { Text("Skip to next") }
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = showHeart,
                        modifier = Modifier.align(Alignment.Center),
                        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                        exit = scaleOut(targetScale = 1.3f) + fadeOut(),
                    ) { Icon(Icons.Rounded.Favorite, null, tint = AccentRed, modifier = Modifier.size(110.dp)) }
                }

                // Instagram-style author block: avatar + folder name, caption, audio line.
                Column(
                    Modifier.align(Alignment.BottomStart).navigationBarsPadding()
                        .padding(start = 14.dp, end = 84.dp, bottom = 34.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppLogo(34.dp, Modifier.border(1.5.dp, Color.White, RoundedCornerShape(10.dp)))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            v.folderName, color = Color.White, style = MaterialTheme.typography.titleSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (settings.reelsCaption) {
                        Spacer(Modifier.height(10.dp))
                        Text(v.title, color = Color.White, style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.3f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Original audio · ${formatDuration(v.durationMs)}", color = Color.White,
                            fontSize = 11.sp, maxLines = 1)
                    }
                }

                // Action rail.
                Column(
                    Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 8.dp, bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp),
                ) {
                    RailButton(
                        if (v.favorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        "Favorite", tint = if (v.favorite) AccentRed else Color.White,
                    ) { vm.toggleFavorite(v.id) }
                    RailButton(Icons.Outlined.Share, "Share") { shareVideo(context, v) }
                    RailButton(if (muted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp, if (muted) "Unmute" else "Mute") { muted = !muted }
                    RailText(speedLabel(ui.speed)) {
                        val i = SpeedSteps.indexOf(ui.speed).let { if (it < 0) SpeedSteps.indexOf(1f) else it }
                        player.setPlaybackSpeed(SpeedSteps[(i + 1) % SpeedSteps.size])
                    }
                    RailButton(if (crop) Icons.Rounded.FitScreen else Icons.Rounded.Crop, if (crop) "Fit" else "Fill") { crop = !crop }
                    RailButton(Icons.Rounded.MoreVert, "More") { player.pause(); menu = v }
                }

                if (isCurrent && settings.reelsProgress) {
                    SeekStrip(
                        positionMs = pos.first, durationMs = pos.second,
                        onSeek = { f -> if (pos.second > 0) player.seekTo((pos.second * f).toLong()) },
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
                    )
                }
            }
        }

        // Top bar floats above the pager.
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { saveCurrent(); onBack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White)
            }
            AppLogo(28.dp)
            Spacer(Modifier.width(10.dp))
            Text("Reels", color = Color.White, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text("${settled + 1} / ${queue.size}", color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 16.dp))
        }
    }

    VideoMenuHost(
        target = menu,
        onDismiss = { menu = null; player.play() },
        vm = vm,
        onPlay = { player.play() },
    )
}

@Composable
private fun RailButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.28f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = tint, modifier = Modifier.size(26.dp)) }
}

@Composable
private fun RailText(text: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.28f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.W700) }
}

/** Thin progress line you can tap or drag to scrub. */
@Composable
fun SeekStrip(positionMs: Long, durationMs: Long, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Box(
        modifier.fillMaxWidth().height(24.dp)
            .pointerInput(durationMs) { detectTapGestures { o -> onSeek((o.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.BottomStart,
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.25f)))
        Box(Modifier.fillMaxWidth(fraction).height(3.dp).background(Color.White))
    }
}
