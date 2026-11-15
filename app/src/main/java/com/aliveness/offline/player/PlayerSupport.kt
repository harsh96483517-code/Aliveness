package com.aliveness.offline.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

/** One ExoPlayer per screen that needs one: created on entry, released on exit, paused on ON_STOP. */
@Composable
fun rememberExoPlayer(speed: Float): ExoPlayer {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { it.playbackParameters = PlaybackParameters(speed) }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, player) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) player.pause() }
        owner.lifecycle.addObserver(obs)
        onDispose {
            owner.lifecycle.removeObserver(obs)
            player.release()
        }
    }
    return player
}

/** Player events mirrored into Compose state. */
@Stable
class PlayerUi(private val player: Player) {
    var isPlaying by mutableStateOf(player.isPlaying)
    var playbackState by mutableIntStateOf(player.playbackState)
    var error by mutableStateOf<PlaybackException?>(null)
    var tracks by mutableStateOf(player.currentTracks)
    var repeatMode by mutableIntStateOf(player.repeatMode)
    var speed by mutableStateOf(player.playbackParameters.speed)
    var mediaId by mutableStateOf(player.currentMediaItem?.mediaId)
    var firstFrame by mutableStateOf(false)
    var onTransition: ((MediaItem?) -> Unit)? = null

    val isBuffering get() = playbackState == Player.STATE_BUFFERING
    val ended get() = playbackState == Player.STATE_ENDED

    val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) { this@PlayerUi.isPlaying = isPlaying }
        override fun onPlaybackStateChanged(playbackState: Int) {
            this@PlayerUi.playbackState = playbackState
            if (playbackState != Player.STATE_IDLE) error = null
        }
        override fun onPlayerError(error: PlaybackException) { this@PlayerUi.error = error }
        override fun onTracksChanged(tracks: Tracks) { this@PlayerUi.tracks = tracks }
        override fun onRepeatModeChanged(repeatMode: Int) { this@PlayerUi.repeatMode = repeatMode }
        override fun onPlaybackParametersChanged(p: PlaybackParameters) { speed = p.speed }
        override fun onRenderedFirstFrame() { firstFrame = true }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaId = mediaItem?.mediaId
            firstFrame = false
            error = null
            onTransition?.invoke(mediaItem)
        }
    }
}

@Composable
fun rememberPlayerUi(player: Player): PlayerUi {
    val ui = remember(player) { PlayerUi(player) }
    DisposableEffect(player) {
        player.addListener(ui.listener)
        onDispose { player.removeListener(ui.listener) }
    }
    return ui
}

/** (position, duration) in ms, polled while [active]. Duration is 0 until known. */
@Composable
fun rememberPosition(player: Player, active: Boolean = true, intervalMs: Long = 250): State<Pair<Long, Long>> =
    produceState(initialValue = 0L to 0L, player, active) {
        while (active) {
            val d = player.duration
            value = player.currentPosition to (if (d == C.TIME_UNSET) 0L else d)
            delay(intervalMs)
        }
    }

val SpeedSteps = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

fun speedLabel(s: Float): String =
    if (s % 1f == 0f) "${s.toInt()}x" else "${"%.2f".format(s).trimEnd('0')}x"
