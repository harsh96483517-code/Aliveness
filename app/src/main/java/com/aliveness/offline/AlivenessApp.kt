package com.aliveness.offline

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.aliveness.offline.data.AppDatabase
import com.aliveness.offline.data.SettingsStore
import com.aliveness.offline.data.VideoRepository

class AppGraph(app: Application) {
    val db = AppDatabase.create(app)
    val settings = SettingsStore(app)
    val repo = VideoRepository(app, db, settings)
}

class AlivenessApp : Application(), ImageLoaderFactory {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }

    /** Frames are decoded from the video files themselves and cached, so rescrolling is instant. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(VideoFrameDecoder.Factory()) }
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.18).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("thumbs")).maxSizeBytes(96L * 1024 * 1024).build() }
        .crossfade(true)
        .build()
}
