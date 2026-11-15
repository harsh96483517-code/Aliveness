package com.aliveness.offline.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** Screen stays awake while a video screen is open. */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/** Hides the system bars (immersive full-screen) while [hidden]; restores them on exit. */
@Composable
fun ImmersiveMode(hidden: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(hidden) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (hidden) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

/** Locks to landscape/portrait for the full-screen toggle; unlocks again on exit. */
@Composable
fun OrientationLock(requested: Int?) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(requested) {
        if (requested != null) activity.requestedOrientation = requested
        onDispose { }
    }
    DisposableEffect(Unit) {
        onDispose { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
}

fun Activity.setWindowBrightness(value: Float) {
    val lp: WindowManager.LayoutParams = window.attributes
    lp.screenBrightness = value.coerceIn(0.02f, 1f)
    window.attributes = lp
}

fun Activity.currentBrightness(): Float {
    val b = window.attributes.screenBrightness
    if (b >= 0f) return b
    return runCatching {
        android.provider.Settings.System.getInt(contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255f
    }.getOrDefault(0.5f)
}
