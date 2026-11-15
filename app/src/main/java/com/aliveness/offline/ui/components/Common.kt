package com.aliveness.offline.ui.components

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aliveness.offline.data.VideoItem
import com.aliveness.offline.data.formatDuration
import com.aliveness.offline.data.formatSize
import com.aliveness.offline.ui.theme.AccentRed
import com.aliveness.offline.ui.theme.Dimens
import com.aliveness.offline.ui.theme.LocalThumbPx

/** A frame pulled straight from the video file, decoded at the size the settings ask for. */
@Composable
fun VideoThumb(video: VideoItem, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val px = LocalThumbPx.current
    val request = remember(video.id, px) {
        ImageRequest.Builder(ctx)
            .data(Uri.parse(video.uri))
            .size(px)
            // ~10% in (capped at 1 s) dodges the black first frame many clips start with.
            .setParameter("coil#video_frame_micros", minOf(1_000_000L, video.durationMs * 100))
            .crossfade(true)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

fun Modifier.thumbBorder(shape: androidx.compose.ui.graphics.Shape): Modifier = composed {
    border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), shape)
}

/** Tap + long-press with the small press-in scale the reference cards use. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pressable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    pressedScale: Float = 0.97f,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick,
        )
}

fun Modifier.shimmer(): Modifier = composed {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "x",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val hi = MaterialTheme.colorScheme.outline
    drawBehind {
        val start = size.width * (x * 3f - 1f)
        drawRect(Brush.horizontalGradient(listOf(base, hi, base), startX = start, endX = start + size.width))
    }
}

@Composable
fun DurationBadge(durationMs: Long, modifier: Modifier = Modifier) {
    Text(
        text = formatDuration(durationMs),
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.W600,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun ProgressStrip(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(3.dp).background(Color.Black.copy(alpha = 0.4f))) {
        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Color.White))
    }
}

/** The shelf/grid card. [width] null fills the cell (grids); otherwise a fixed shelf width. */
@Composable
fun VideoCard(
    video: VideoItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = 220.dp,
    portrait: Boolean = false,
) {
    val sized = if (width != null) modifier.width(width) else modifier.fillMaxWidth()
    Column(sized.pressable(onClick, onLongClick)) {
        val shape = RoundedCornerShape(Dimens.SmallRadius)
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(if (portrait) 9f / 14f else 16f / 9f)
                .clip(shape)
                .thumbBorder(shape),
        ) {
            VideoThumb(video, Modifier.fillMaxSize())
            DurationBadge(video.durationMs, Modifier.align(Alignment.BottomEnd).padding(6.dp))
            if (video.favorite) {
                Icon(
                    Icons.Rounded.Favorite, null, tint = AccentRed,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp),
                )
            }
            if (video.inProgress) ProgressStrip(video.progress, Modifier.align(Alignment.BottomStart))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            video.title, style = MaterialTheme.typography.titleSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${video.folderName} · ${formatSize(video.sizeBytes)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun FeaturedCard(
    video: VideoItem,
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Dimens.CardRadius)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .clip(shape)
            .thumbBorder(shape)
            .pressable(onClick, onLongClick, pressedScale = 0.985f),
    ) {
        VideoThumb(video, Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)), startY = 120f),
            ),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(label.uppercase(), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.W700, letterSpacing = 1.sp)
            Text(video.title, color = Color.White, style = MaterialTheme.typography.titleLarge,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.clip(CircleShape).background(Color.White).padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    if (video.inProgress) "Resume" else "Play",
                    color = Color.Black, style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        DurationBadge(video.durationMs, Modifier.align(Alignment.TopEnd).padding(12.dp))
        if (video.inProgress) ProgressStrip(video.progress, Modifier.align(Alignment.BottomStart))
    }
}

@Composable
fun ShelfHeader(title: String, onSeeAll: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .let { if (onSeeAll != null) it.clickable(onClick = onSeeAll) else it }
            .padding(horizontal = Dimens.PageGutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        if (onSeeAll != null) {
            Icon(Icons.Rounded.ChevronRight, "See all", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun VideoShelf(
    title: String,
    videos: List<VideoItem>,
    portrait: Boolean,
    onSeeAll: () -> Unit,
    onOpen: (VideoItem) -> Unit,
    onMenu: (VideoItem) -> Unit,
) {
    Column(Modifier.padding(bottom = 26.dp)) {
        ShelfHeader(title, onSeeAll)
        LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.PageGutter),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ShelfGap),
        ) {
            items(videos.take(20), key = { it.id }) { v ->
                VideoCard(
                    video = v, onClick = { onOpen(v) }, onLongClick = { onMenu(v) },
                    width = if (portrait) 132.dp else 220.dp, portrait = portrait,
                )
            }
        }
    }
}

@Composable
fun SkeletonShelf(portrait: Boolean = false) {
    Column(Modifier.padding(bottom = 26.dp)) {
        Box(
            Modifier.padding(horizontal = Dimens.PageGutter, vertical = 12.dp)
                .width(150.dp).height(22.dp).clip(RoundedCornerShape(6.dp)).shimmer(),
        )
        Row(
            Modifier.padding(horizontal = Dimens.PageGutter),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ShelfGap),
        ) {
            repeat(3) {
                Box(
                    Modifier.width(if (portrait) 132.dp else 220.dp)
                        .aspectRatio(if (portrait) 9f / 14f else 16f / 9f)
                        .clip(RoundedCornerShape(Dimens.SmallRadius)).shimmer(),
                )
            }
        }
    }
}

@Composable
fun SkeletonRows(count: Int = 6) {
    Column {
        repeat(count) {
            Row(Modifier.padding(horizontal = Dimens.PageGutter, vertical = 6.dp)) {
                Box(Modifier.width(96.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(7.dp)).shimmer())
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.width(180.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                    Box(Modifier.width(110.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            message, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Text(actionLabel) }
        }
    }
}

@Composable
fun VideoRow(
    video: VideoItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().pressable(onClick, onLongClick, 0.985f)
            .padding(horizontal = Dimens.PageGutter, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shape = RoundedCornerShape(Dimens.ThumbRadius)
        Box(Modifier.width(96.dp).aspectRatio(16f / 9f).clip(shape).thumbBorder(shape)) {
            VideoThumb(video, Modifier.fillMaxSize())
            DurationBadge(video.durationMs, Modifier.align(Alignment.BottomEnd).padding(4.dp))
            if (video.inProgress) ProgressStrip(video.progress, Modifier.align(Alignment.BottomStart))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(video.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                "${video.folderName} · ${formatSize(video.sizeBytes)} · ${video.extension}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (video.favorite) {
            Icon(Icons.Rounded.Favorite, null, tint = AccentRed, modifier = Modifier.padding(start = 8.dp).size(16.dp))
        }
        trailing?.invoke()
    }
}

@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(com.aliveness.offline.R.drawable.ic_launcher_art),
        contentDescription = "Aliveness",
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.28f)),
    )
}

/** Logo + app name across the top of Home. */
@Composable
fun BrandHeader(modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = Dimens.PageGutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppLogo(44.dp)
        Spacer(Modifier.width(12.dp))
        Text("Aliveness", style = MaterialTheme.typography.displayLarge, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = Dimens.PageGutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.displayLarge, modifier = Modifier.weight(1f))
        if (trailing != null) trailing() else AppLogo(34.dp)
    }
}
