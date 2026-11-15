package com.aliveness.offline

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import android.content.pm.PackageManager
import com.aliveness.offline.ui.AlivenessRoot
import com.aliveness.offline.ui.components.EmptyState
import com.aliveness.offline.ui.theme.AlivenessTheme
import com.aliveness.offline.ui.theme.LocalThumbPx

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by vm.settings.collectAsState()
            AlivenessTheme(settings) {
                CompositionLocalProvider(LocalThumbPx provides settings.thumbPx) {
                    val view = LocalView.current
                    var immersive by remember { mutableStateOf(false) }
                    val darkBg = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    SideEffect {
                        // Dark status-bar icons only on a light page; video screens are always black.
                        WindowCompat.getInsetsController(window, view).apply {
                            isAppearanceLightStatusBars = !darkBg && !immersive
                            isAppearanceLightNavigationBars = !darkBg && !immersive
                        }
                    }
                    PermissionGate(vm) { AlivenessRoot(vm) { immersive = it } }
                }
            }
        }
    }
}

private val VideoPermission =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO
    else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
private fun PermissionGate(vm: MainViewModel, content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, VideoPermission) == PackageManager.PERMISSION_GRANTED)
    }
    var askedOnce by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it; askedOnce = true
    }
    LaunchedEffect(granted) { if (granted) vm.refresh() }

    if (granted) {
        content()
    } else {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EmptyState(
                icon = Icons.Rounded.VideoLibrary,
                title = "Allow access to your videos",
                message = "Aliveness only reads the videos on this phone so it can play them offline. Nothing leaves your device.",
                actionLabel = "Allow access",
                onAction = { launcher.launch(VideoPermission) },
            )
            if (askedOnce) {
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                }) { Text("Open app settings") }
            }
        }
    }
}
