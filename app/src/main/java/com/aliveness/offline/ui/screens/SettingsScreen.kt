package com.aliveness.offline.ui.screens

import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aliveness.offline.BuildConfig
import com.aliveness.offline.MainViewModel
import com.aliveness.offline.data.Keys
import com.aliveness.offline.data.ThemeMode
import com.aliveness.offline.data.formatSize
import com.aliveness.offline.data.toFolders
import com.aliveness.offline.player.SpeedSteps
import com.aliveness.offline.player.speedLabel
import com.aliveness.offline.ui.components.SubTopBar
import com.aliveness.offline.ui.theme.Dimens
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit, onFolders: () -> Unit) {
    val s by vm.settings.collectAsState()
    val everything by vm.everything.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val folders = remember(everything) { everything.toFolders() }
    val total = remember(everything) { everything.sumOf { it.sizeBytes } }
    val dynamicOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val free = remember(everything) {
        runCatching {
            val st = StatFs(Environment.getDataDirectory().path)
            formatSize(st.availableBytes) + " free of " + formatSize(st.totalBytes)
        }.getOrDefault("Unavailable")
    }

    Column(Modifier.fillMaxSize()) {
        SubTopBar("Settings", onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            item { Header("Appearance") }
            item {
                Choice("Theme", listOf("System", "Light", "Dark"), s.themeMode.ordinal) {
                    vm.setSetting(Keys.theme, it)
                }
            }
            item {
                SwitchRow(
                    "Dynamic color",
                    if (dynamicOk) "Use your wallpaper colors" else "Needs Android 12 or newer",
                    s.dynamicColor && dynamicOk, enabled = dynamicOk,
                ) { vm.setSetting(Keys.dynamic, it) }
            }

            item { Header("Dock") }
            item { SwitchRow("Reels button in dock", "Open the Reels viewer straight from the bottom bar", s.dockReels) { vm.setSetting(Keys.dockReels, it) } }
            item { SwitchRow("Liquid glass drag", "Press and slide along the dock: the glass lens pops up and follows your finger", s.liquidDock) { vm.setSetting(Keys.liquidDock, it) } }
            item { SwitchRow("Dock haptics", "Small tick when the lens moves to another tab", s.dockHaptics, enabled = s.liquidDock) { vm.setSetting(Keys.dockHaptics, it) } }

            item { Choice("Dock size", listOf("Compact", "Large"), if (s.dockLarge) 1 else 0) { vm.setSetting(Keys.dockLarge, it == 1) } }
            item { Choice("Dock glass", listOf("Clear", "Frosted", "Solid"), s.dockGlass) { vm.setSetting(Keys.dockGlass, it) } }

            item { Header("Reels") }
            item { SwitchRow("Start muted", "Reels open with sound off", s.reelsMuted) { vm.setSetting(Keys.reelsMuted, it) } }
            item { SwitchRow("Show caption", "Video name under the folder name", s.reelsCaption) { vm.setSetting(Keys.reelsCaption, it) } }
            item { SwitchRow("Show progress bar", "Thin scrub bar along the bottom", s.reelsProgress) { vm.setSetting(Keys.reelsProgress, it) } }

            item { Header("Security") }
            item { AppLockRow(vm, s.appLock) }

            item { Header("Playback") }
            item { SwitchRow("Autoplay", "Start playing as soon as a video opens", s.autoplay) { vm.setSetting(Keys.autoplay, it) } }
            item { SwitchRow("Auto-play next Reel", "Move on when a Reel ends", s.autoNext) { vm.setSetting(Keys.autoNext, it) } }
            item { SwitchRow("Resume where you left off", "Remember playback position", s.resumePlayback) { vm.setSetting(Keys.resume, it) } }
            item {
                SwitchRow("Gesture controls", "Swipe for brightness and volume, double-tap to seek", s.gestures) {
                    vm.setSetting(Keys.gestures, it)
                }
            }
            item {
                Choice("Default playback speed", SpeedSteps.map { speedLabel(it) },
                    SpeedSteps.indexOf(s.defaultSpeed).coerceAtLeast(0)) { vm.setSetting(Keys.speed, SpeedSteps[it]) }
            }
            item {
                Choice("Reels fit", listOf("Fill screen (crop)", "Fit (letterbox)"), if (s.reelsCrop) 0 else 1) {
                    vm.setSetting(Keys.reelsCrop, it == 0)
                }
            }

            item { Header("Library") }
            item {
                Choice("Thumbnail quality", listOf("Low", "Balanced", "High"), s.thumbQuality) {
                    vm.setSetting(Keys.thumbQuality, it)
                }
            }
            item {
                ActionRow(
                    "Folder exclusions",
                    if (s.hiddenFolders.isEmpty()) "No hidden folders" else "${s.hiddenFolders.size} hidden",
                    onFolders,
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = !scanning) { vm.refresh() }
                        .padding(horizontal = Dimens.PageGutter, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Refresh media library", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (scanning) "Scanning…"
                            else if (s.lastScanAt == 0L) "Not scanned yet"
                            else "Last scan " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(s.lastScanAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (scanning) CircularProgressIndicator(Modifier.padding(4.dp), strokeWidth = 2.dp)
                }
            }

            item { Header("Storage") }
            item { Info("Videos", everything.size.toString()) }
            item { Info("Library size", formatSize(total)) }
            item { Info("Folders", folders.size.toString()) }
            item { Info("Device storage", free) }

            item { Header("Updates") }
            item { UpdateSection() }

            item { Header("About Aliveness") }
            item { Info("Version", BuildConfig.VERSION_NAME) }
            item { Info("Network", "Videos play fully offline. The internet is used only when you tap “Check for updates”. No accounts, no tracking.") }
            item {
                Info(
                    "Licence & credits",
                    "Aliveness is free software under the GNU GPL v3. Its interface design and structure are adapted from " +
                        "BitChord (github.com/kushagrasinghx/BitChord, GPL-3.0). It is not affiliated with BitChord.",
                )
            }
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text, style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(start = Dimens.PageGutter, end = Dimens.PageGutter, top = 26.dp, bottom = 6.dp),
    )
}

@Composable
internal fun SwitchRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = Dimens.PageGutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun Choice(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = Dimens.PageGutter))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.PageGutter, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(options.size) { i ->
                FilterChip(selected = i == selected, onClick = { onSelect(i) }, label = { Text(options[i]) })
            }
        }
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = Dimens.PageGutter, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Info(title: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Dimens.PageGutter, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
