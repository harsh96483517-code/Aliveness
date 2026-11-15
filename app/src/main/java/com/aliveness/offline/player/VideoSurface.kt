@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.aliveness.offline.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

const val RESIZE_FIT = AspectRatioFrameLayout.RESIZE_MODE_FIT
const val RESIZE_CROP = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

/** Media3's video surface with its own controller switched off: every control is Compose UI. */
@Composable
fun VideoSurface(exo: Player, resizeMode: Int, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                this.resizeMode = resizeMode
                this.player = exo
            }
        },
        update = { it.resizeMode = resizeMode },
        onRelease = { it.player = null },
        modifier = modifier,
    )
}
