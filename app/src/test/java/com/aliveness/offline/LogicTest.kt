package com.aliveness.offline

import com.aliveness.offline.data.MediaScanner
import com.aliveness.offline.data.Source
import com.aliveness.offline.data.SortOption
import com.aliveness.offline.data.VideoItem
import com.aliveness.offline.data.formatDuration
import com.aliveness.offline.data.formatSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    private fun v(id: Long, w: Int = 1080, h: Int = 1920, dur: Long = 30_000, fav: Boolean = false,
                  pos: Long = 0, folder: String = "Movies/", added: Long = id) = VideoItem(
        id, "content://v/$id", "t$id", "t$id.mp4", folder, folder.trimEnd('/'), dur, 1000, added, w, h,
        "video/mp4", fav, false, pos, 0, 0,
    )

    @Test fun reelIsShortAndPortrait() {
        assertTrue(v(1).isReel)
        assertFalse(v(2, w = 1920, h = 1080).isReel)
        assertFalse(v(3, dur = 10 * 60_000).isReel)
    }

    @Test fun inProgressIgnoresStartAndEnd() {
        assertFalse(v(1, pos = 2_000).inProgress)
        assertTrue(v(1, pos = 10_000).inProgress)
        assertFalse(v(1, pos = 28_000).inProgress)
    }

    @Test fun sourcesFilter() {
        val all = listOf(v(1, fav = true), v(2, w = 1920, h = 1080), v(3, folder = "Download/"))
        assertEquals(listOf(1L, 3L), Source.resolve(all, Source.REELS, emptyList()).map { it.id })
        assertEquals(listOf(1L), Source.resolve(all, Source.FAV, emptyList()).map { it.id })
        assertEquals(listOf(3L), Source.resolve(all, Source.DOWNLOADS, emptyList()).map { it.id })
        assertEquals(listOf(3L), Source.resolve(all, Source.folder("Download/"), emptyList()).map { it.id })
    }

    @Test fun sortOrders() {
        val l = listOf(v(1, added = 5), v(2, added = 9))
        assertEquals(listOf(2L, 1L), SortOption.Newest.apply(l).map { it.id })
        assertEquals(listOf(1L, 2L), SortOption.Oldest.apply(l).map { it.id })
    }

    @Test fun formatting() {
        assertEquals("1:05", formatDuration(65_000))
        assertEquals("1:01:01", formatDuration(3_661_000))
        assertEquals("2 MB", formatSize(2L shl 20))
    }

    @Test fun legacyFolderFromPath() {
        assertEquals("DCIM/Camera/", MediaScanner.folderOf("/storage/emulated/0/DCIM/Camera/a.mp4", false))
        assertEquals("Movies/", MediaScanner.folderOf("Movies/", true))
    }
}

class UpdaterTest {
    @Test fun versionFromTag() {
        assertEquals(17, com.aliveness.offline.data.Updater.versionCodeOf("v1.0.17"))
        assertEquals(0, com.aliveness.offline.data.Updater.versionCodeOf("latest"))
    }
}
