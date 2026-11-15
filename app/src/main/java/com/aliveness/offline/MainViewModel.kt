package com.aliveness.offline

import android.app.Application
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aliveness.offline.data.AppSettings
import com.aliveness.offline.data.CollectionEntity
import com.aliveness.offline.data.CollectionItemEntity
import com.aliveness.offline.data.RecentSearchEntity
import com.aliveness.offline.data.Source
import com.aliveness.offline.data.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = (app as AlivenessApp).graph
    private val repo = graph.repo
    private val store = graph.settings

    val settings: StateFlow<AppSettings> =
        store.flow.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val items: StateFlow<List<VideoItem>> =
        repo.visible.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val everything: StateFlow<List<VideoItem>> =
        repo.everything.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val collections: StateFlow<List<CollectionEntity>> =
        repo.collections.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val links: StateFlow<List<CollectionItemEntity>> =
        repo.links.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val recentSearches: StateFlow<List<RecentSearchEntity>> =
        repo.recentSearches.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val scanning: StateFlow<Boolean> = repo.scanning

    /** False until the database has answered once, so "empty" and "still loading" look different. */
    val loaded = MutableStateFlow(false)

    init {
        viewModelScope.launch { repo.visible.first(); loaded.value = true }
    }

    fun refresh() { viewModelScope.launch { repo.refresh() } }

    fun toggleFavorite(id: Long) { viewModelScope.launch { repo.toggleFavorite(id) } }
    fun toggleWatchLater(id: Long) { viewModelScope.launch { repo.toggleWatchLater(id) } }
    fun markStarted(id: Long) { viewModelScope.launch { repo.markStarted(id) } }
    fun savePosition(id: Long, pos: Long, dur: Long) { viewModelScope.launch { repo.savePosition(id, pos, dur) } }

    fun createCollection(name: String, thenAdd: Long? = null) {
        viewModelScope.launch {
            val id = repo.createCollection(name)
            if (thenAdd != null) repo.addToCollection(id, thenAdd)
        }
    }
    fun renameCollection(id: Long, name: String) { viewModelScope.launch { repo.renameCollection(id, name) } }
    fun deleteCollection(id: Long) { viewModelScope.launch { repo.deleteCollection(id) } }
    fun addToCollection(cid: Long, vid: Long) { viewModelScope.launch { repo.addToCollection(cid, vid) } }
    fun removeFromCollection(cid: Long, vid: Long) { viewModelScope.launch { repo.removeFromCollection(cid, vid) } }

    fun rememberSearch(q: String) { viewModelScope.launch { repo.rememberSearch(q) } }
    fun forgetSearch(r: RecentSearchEntity) { viewModelScope.launch { repo.forgetSearch(r) } }
    fun clearSearches() { viewModelScope.launch { repo.clearSearches() } }

    fun <T> setSetting(key: Preferences.Key<T>, value: T) { viewModelScope.launch { store.set(key, value) } }
    fun setFolderHidden(path: String, hidden: Boolean) { viewModelScope.launch { store.setHidden(path, hidden) } }

    /** Snapshot of a source's videos at this instant. Players keep this list, so it stays stable. */
    fun queue(source: String): List<VideoItem> = Source.resolve(items.value, source, links.value)
}
