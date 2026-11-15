package com.aliveness.offline.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Media metadata discovered through MediaStore. Rebuilt on every library scan. */
@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: Long,
    val uri: String,
    val title: String,
    val fileName: String,
    val folderPath: String,
    val folderName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val width: Int,
    val height: Int,
    val mime: String,
)

/** Everything the user did with a video. Kept apart so a rescan never wipes it. */
@Entity(tableName = "video_state")
data class VideoStateEntity(
    @PrimaryKey val videoId: Long,
    val favorite: Boolean = false,
    val watchLater: Boolean = false,
    val positionMs: Long = 0,
    val lastWatchedAt: Long = 0,
    val playCount: Int = 0,
)

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "collection_items", primaryKeys = ["collectionId", "videoId"])
data class CollectionItemEntity(
    val collectionId: Long,
    val videoId: Long,
    val addedAt: Long,
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val text: String,
    val at: Long,
)

/** A video joined with the user's state, as the UI sees it. */
data class VideoItem(
    val id: Long,
    val uri: String,
    val title: String,
    val fileName: String,
    val folderPath: String,
    val folderName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val width: Int,
    val height: Int,
    val mime: String,
    val favorite: Boolean,
    val watchLater: Boolean,
    val positionMs: Long,
    val lastWatchedAt: Long,
    val playCount: Int,
) {
    val isPortrait: Boolean get() = height > width && width > 0

    /** A Reel is a short, portrait clip. When dimensions are unknown, short is enough. */
    val isReel: Boolean
        get() = if (width > 0 && height > 0) isPortrait && durationMs <= REEL_MAX_MS
        else durationMs in 1..REEL_FALLBACK_MS
    val isLong: Boolean get() = durationMs >= LONG_MIN_MS
    val extension: String get() = fileName.substringAfterLast('.', "").uppercase()
    val inProgress: Boolean
        get() = positionMs > 5_000 && durationMs > 0 && positionMs < durationMs - 5_000
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val isDownload: Boolean get() = folderPath.contains("download", ignoreCase = true)

    companion object {
        const val REEL_MAX_MS = 3 * 60_000L
        const val REEL_FALLBACK_MS = 90_000L
        const val LONG_MIN_MS = 10 * 60_000L
    }
}
