package com.aliveness.offline.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class VideoRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsStore,
) {
    private val videos = db.videoDao()
    private val collectionsDao = db.collectionDao()
    private val searchDao = db.searchDao()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning

    /** Every indexed video, including those in hidden folders (the Folders screen needs them). */
    val everything: Flow<List<VideoItem>> = videos.observeItems()

    /** What the rest of the app shows: hidden folders filtered out. */
    val visible: Flow<List<VideoItem>> = combine(
        videos.observeItems(),
        settings.flow.map { it.hiddenFolders }.distinctUntilChanged(),
    ) { items, hidden -> if (hidden.isEmpty()) items else items.filter { it.folderPath !in hidden } }

    val collections: Flow<List<CollectionEntity>> = collectionsDao.observeCollections()
    val links: Flow<List<CollectionItemEntity>> = collectionsDao.observeLinks()
    val recentSearches: Flow<List<RecentSearchEntity>> = searchDao.observeRecent()

    /** Re-reads MediaStore; user data (favorites, history, collections) is untouched. */
    suspend fun refresh() = withContext(Dispatchers.IO) {
        if (!_scanning.compareAndSet(false, true)) return@withContext
        try {
            val found = MediaScanner.scan(context)
            found.chunked(500).forEach { videos.upsertVideos(it) }
            val foundIds = found.mapTo(HashSet()) { it.id }
            val gone = videos.allIds().filter { it !in foundIds }
            gone.chunked(500).forEach { videos.deleteByIds(it) }
            settings.set(Keys.lastScan, System.currentTimeMillis())
        } finally {
            _scanning.value = false
        }
    }

    private suspend fun stateOf(id: Long) = videos.getState(id) ?: VideoStateEntity(videoId = id)

    suspend fun toggleFavorite(id: Long) = withContext(Dispatchers.IO) {
        val s = stateOf(id); videos.upsertState(s.copy(favorite = !s.favorite))
    }

    suspend fun toggleWatchLater(id: Long) = withContext(Dispatchers.IO) {
        val s = stateOf(id); videos.upsertState(s.copy(watchLater = !s.watchLater))
    }

    /** Called once when a video actually starts playing. */
    suspend fun markStarted(id: Long) = withContext(Dispatchers.IO) {
        val s = stateOf(id)
        videos.upsertState(s.copy(playCount = s.playCount + 1, lastWatchedAt = System.currentTimeMillis()))
    }

    suspend fun savePosition(id: Long, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        val s = stateOf(id)
        // A finished video starts over next time instead of resuming at its last second.
        val pos = if (durationMs > 0 && positionMs >= durationMs - 3_000) 0L else positionMs.coerceAtLeast(0)
        videos.upsertState(s.copy(positionMs = pos))
    }

    suspend fun createCollection(name: String): Long = withContext(Dispatchers.IO) {
        collectionsDao.insert(CollectionEntity(name = name.trim(), createdAt = System.currentTimeMillis()))
    }

    suspend fun renameCollection(id: Long, name: String) = withContext(Dispatchers.IO) {
        collectionsDao.rename(id, name.trim())
    }

    suspend fun deleteCollection(id: Long) = withContext(Dispatchers.IO) {
        collectionsDao.clearItems(id); collectionsDao.delete(id)
    }

    suspend fun addToCollection(collectionId: Long, videoId: Long) = withContext(Dispatchers.IO) {
        collectionsDao.addItem(CollectionItemEntity(collectionId, videoId, System.currentTimeMillis()))
    }

    suspend fun removeFromCollection(collectionId: Long, videoId: Long) = withContext(Dispatchers.IO) {
        collectionsDao.removeItem(collectionId, videoId)
    }

    suspend fun rememberSearch(text: String) = withContext(Dispatchers.IO) {
        if (text.isNotBlank()) searchDao.upsert(RecentSearchEntity(text.trim(), System.currentTimeMillis()))
    }

    suspend fun forgetSearch(r: RecentSearchEntity) = withContext(Dispatchers.IO) { searchDao.delete(r) }
    suspend fun clearSearches() = withContext(Dispatchers.IO) { searchDao.clear() }
}
