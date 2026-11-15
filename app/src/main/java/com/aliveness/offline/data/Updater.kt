package com.aliveness.offline.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val versionCode: Int, val tag: String, val notes: String, val apkUrl: String, val sizeBytes: Long)

/** Self-update from the app's own GitHub Releases. Runs only when the user asks; nothing in the background. */
object Updater {

    /** Latest release, or null if the repo has no release with an APK yet. */
    suspend fun latest(repo: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val conn = URL("https://api.github.com/repos/$repo/releases/latest").openConnection() as HttpURLConnection
        try {
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            if (conn.responseCode == 404) return@withContext null
            if (conn.responseCode != 200) error("GitHub answered ${conn.responseCode}")
            parse(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    internal fun parse(json: String): UpdateInfo? {
        val o = JSONObject(json)
        val tag = o.optString("tag_name")
        val assets = o.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name").endsWith(".apk")) {
                return UpdateInfo(versionCodeOf(tag), tag, o.optString("body"), a.getString("browser_download_url"), a.optLong("size"))
            }
        }
        return null
    }

    /** Tags look like v1.0.17; the trailing number is the build number used as versionCode. */
    internal fun versionCodeOf(tag: String): Int = Regex("(\\d+)$").find(tag.trim())?.value?.toIntOrNull() ?: 0

    suspend fun download(context: Context, url: String, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val file = File(dir, "Aliveness.apk")
        val conn = URL(url).openConnection() as HttpURLConnection   // follows GitHub's redirect to the file host
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            if (conn.responseCode != 200) error("Download failed (${conn.responseCode})")
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) onProgress(done.toFloat() / total)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        file
    }

    /** Returns false if Android first needs the "install unknown apps" permission (settings opened). */
    fun install(context: Context, apk: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")),
            )
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
        return true
    }
}
