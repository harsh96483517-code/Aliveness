package com.aliveness.offline.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aliveness.offline.BuildConfig
import com.aliveness.offline.data.UpdateInfo
import com.aliveness.offline.data.Updater
import com.aliveness.offline.data.formatSize
import com.aliveness.offline.ui.theme.Dimens
import kotlinx.coroutines.launch
import java.io.File

/** "Check for updates" – the only place in the app that touches the network, and only on tap. */
@Composable
fun UpdateSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = BuildConfig.GITHUB_REPO
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Tap to check GitHub for a newer version.") }
    var found by remember { mutableStateOf<UpdateInfo?>(null) }
    var progress by remember { mutableStateOf<Float?>(null) }
    var downloaded by remember { mutableStateOf<File?>(null) }

    Column(Modifier.fillMaxWidth().padding(horizontal = Dimens.PageGutter, vertical = 8.dp)) {
        Text("Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.titleMedium)
        Text(
            if (repo.isBlank()) "Updates are enabled in builds made by the GitHub workflow."
            else status,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
        )
        progress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) }

        if (repo.isNotBlank()) {
            val info = found
            if (info != null && info.versionCode > BuildConfig.VERSION_CODE) {
                Button(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        try {
                            val file = downloaded ?: Updater.download(context, info.apkUrl) { progress = it }.also { downloaded = it }
                            progress = null
                            status = if (Updater.install(context, file)) "Installer opened. Confirm to update."
                            else "Allow “Install unknown apps” for Aliveness, then tap Install again."
                        } catch (e: Exception) {
                            progress = null
                            status = "Download failed: ${e.message ?: "no connection"}"
                        } finally { busy = false }
                    }
                }) { Text("Download and install ${info.tag}") }
            } else {
                Button(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        status = "Checking…"
                        try {
                            val latest = Updater.latest(repo)
                            found = latest
                            downloaded = null
                            status = when {
                                latest == null -> "No release published yet."
                                latest.versionCode > BuildConfig.VERSION_CODE ->
                                    "Update available: ${latest.tag} (${formatSize(latest.sizeBytes)})" +
                                        latest.notes.trim().takeIf { it.isNotEmpty() }?.let { "\n${it.take(240)}" }.orEmpty()
                                else -> "You’re on the latest version."
                            }
                        } catch (e: Exception) {
                            status = "Couldn’t check: ${e.message ?: "no connection"}"
                        } finally { busy = false }
                    }
                }) { Text("Check for updates") }
            }
        }
    }
}
