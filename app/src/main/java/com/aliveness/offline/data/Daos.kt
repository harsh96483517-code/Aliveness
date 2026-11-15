package com.aliveness.offline.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

private const val ITEM_SELECT = """
SELECT v.id AS id, v.uri AS uri, v.title AS title, v.fileName AS fileName,
       v.folderPath AS folderPath, v.folderName AS folderName, v.durationMs AS durationMs,
       v.sizeBytes AS sizeBytes, v.dateAddedSec AS dateAddedSec, v.width AS width,
       v.height AS height, v.mime AS mime,
       COALESCE(s.favorite, 0) AS favorite, COALESCE(s.watchLater, 0) AS watchLater,
       COALESCE(s.positionMs, 0) AS positionMs, COALESCE(s.lastWatchedAt, 0) AS lastWatchedAt,
       COALESCE(s.playCount, 0) AS playCount
FROM videos v LEFT JOIN video_state s ON s.videoId = v.id
ORDER BY v.dateAddedSec DESC
"""

@Dao
interface VideoDao {
    @Query(ITEM_SELECT)
    fun observeItems(): Flow<List<VideoItem>>

    @Upsert
    suspend fun upsertVideos(list: List<VideoEntity>)

    @Query("SELECT id FROM videos")
    suspend fun allIds(): List<Long>

    @Query("DELETE FROM videos WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM video_state WHERE videoId = :id")
    suspend fun getState(id: Long): VideoStateEntity?

    @Upsert
    suspend fun upsertState(state: VideoStateEntity)
}

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    fun observeCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collection_items ORDER BY addedAt DESC")
    fun observeLinks(): Flow<List<CollectionItemEntity>>

    @Insert
    suspend fun insert(c: CollectionEntity): Long

    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM collection_items WHERE collectionId = :id")
    suspend fun clearItems(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addItem(item: CollectionItemEntity)

    @Query("DELETE FROM collection_items WHERE collectionId = :cid AND videoId = :vid")
    suspend fun removeItem(cid: Long, vid: Long)

    @Query("DELETE FROM collection_items WHERE videoId IN (:ids)")
    suspend fun removeVideos(ids: List<Long>)
}

@Dao
interface SearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY at DESC LIMIT 12")
    fun observeRecent(): Flow<List<RecentSearchEntity>>

    @Upsert
    suspend fun upsert(r: RecentSearchEntity)

    @Delete
    suspend fun delete(r: RecentSearchEntity)

    @Query("DELETE FROM recent_searches")
    suspend fun clear()
}
