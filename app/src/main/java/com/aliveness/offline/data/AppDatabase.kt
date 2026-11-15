package com.aliveness.offline.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VideoEntity::class, VideoStateEntity::class, CollectionEntity::class,
        CollectionItemEntity::class, RecentSearchEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun collectionDao(): CollectionDao
    abstract fun searchDao(): SearchDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "aliveness.db").build()
    }
}
