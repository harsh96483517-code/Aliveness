package com.aliveness.offline.data

/**
 * A "source" is a string naming a list of videos (a shelf, a folder, a collection). Screens pass it
 * around as a nav argument, and the Reels/Player queue is resolved from it once on entry.
 */
object Source {
    const val ALL = "all"
    const val REELS = "reels"
    const val LONG = "long"
    const val FAV = "fav"
    const val FAV_REELS = "favreels"
    const val LATER = "later"
    const val WATCHED = "watched"
    const val CONTINUE = "continue"
    const val ADDED = "added"
    const val PLAYED = "played"
    const val DOWNLOADS = "downloads"

    fun folder(path: String) = "folder|$path"
    fun collection(id: Long) = "col|$id"

    fun title(source: String, collections: List<CollectionEntity>): String = when {
        source == ALL -> "All Videos"
        source == REELS -> "All Reels"
        source == LONG -> "Long Videos"
        source == FAV -> "Favorites"
        source == FAV_REELS -> "Favorite Reels"
        source == LATER -> "Watch Later"
        source == WATCHED -> "Recently Watched"
        source == CONTINUE -> "Continue Watching"
        source == ADDED -> "Recently Added"
        source == PLAYED -> "Most Played"
        source == DOWNLOADS -> "Downloads"
        source.startsWith("folder|") -> source.removePrefix("folder|").trimEnd('/').substringAfterLast('/')
            .ifBlank { "Internal storage" }
        source.startsWith("col|") -> collections.firstOrNull { "col|${it.id}" == source }?.name ?: "Playlist"
        else -> "Videos"
    }

    fun resolve(
        items: List<VideoItem>,
        source: String,
        links: List<CollectionItemEntity>,
    ): List<VideoItem> = when {
        source == ALL -> items
        source == REELS -> items.filter { it.isReel }
        source == LONG -> items.filter { it.isLong }
        source == FAV -> items.filter { it.favorite }
        source == FAV_REELS -> items.filter { it.favorite && it.isReel }
        source == LATER -> items.filter { it.watchLater }
        source == WATCHED -> items.filter { it.lastWatchedAt > 0 }.sortedByDescending { it.lastWatchedAt }
        source == CONTINUE -> items.filter { it.inProgress }.sortedByDescending { it.lastWatchedAt }
        source == ADDED -> items.sortedByDescending { it.dateAddedSec }
        source == PLAYED -> items.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
        source == DOWNLOADS -> items.filter { it.isDownload }
        source.startsWith("folder|") -> {
            val path = source.removePrefix("folder|")
            items.filter { it.folderPath == path }
        }
        source.startsWith("col|") -> {
            val id = source.removePrefix("col|").toLongOrNull()
            val order = links.filter { it.collectionId == id }.map { it.videoId }
            val byId = items.associateBy { it.id }
            order.mapNotNull { byId[it] }
        }
        else -> items
    }
}

enum class SortOption(val label: String) {
    Newest("Newest"), Oldest("Oldest"), Name("Name"), Size("Size"), Duration("Duration");

    fun apply(list: List<VideoItem>): List<VideoItem> = when (this) {
        Newest -> list.sortedByDescending { it.dateAddedSec }
        Oldest -> list.sortedBy { it.dateAddedSec }
        Name -> list.sortedBy { it.title.lowercase() }
        Size -> list.sortedByDescending { it.sizeBytes }
        Duration -> list.sortedByDescending { it.durationMs }
    }
}

data class FolderInfo(val path: String, val name: String, val count: Int, val sizeBytes: Long, val cover: VideoItem)

fun List<VideoItem>.toFolders(): List<FolderInfo> =
    groupBy { it.folderPath }.map { (path, vids) ->
        FolderInfo(path, vids.first().folderName, vids.size, vids.sumOf { it.sizeBytes }, vids.maxBy { it.dateAddedSec })
    }.sortedBy { it.name.lowercase() }
