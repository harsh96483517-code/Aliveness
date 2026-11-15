# Aliveness

Offline Reels and video player for Android. Reads the videos already on your phone (MediaStore), keeps
favourites, history, resume positions and playlists in a local Room database. No account, no server,
no INTERNET permission.

Interface design and structure are adapted from **BitChord** (https://github.com/kushagrasinghx/BitChord,
GPL-3.0). Aliveness is therefore also **GPL-3.0** (see `LICENSE`). It is not affiliated with BitChord.

## Build
Push to GitHub: the **Build Aliveness APK** workflow runs unit tests, builds the APK and publishes a GitHub Release. You
can also grab the APK from *Actions → Artifacts*. In the app: Settings → Updates → Check for updates (repo must be public).
Locally: `./gradlew :app:assembleDebug` (JDK 17, Android SDK 35).

## Stack
Kotlin 2.1, Jetpack Compose + Material 3, Media3 ExoPlayer, Room, DataStore, Coil (video frames), Haze (frosted glass), Navigation Compose.

## Map: BitChord → Aliveness
| BitChord | Aliveness |
|---|---|
| Home shelves of songs/albums | Home shelves of videos (Recently Watched, Continue, Reels, Favorites…) |
| Explore | Explore: categories, folders, collections, sort, grid/list |
| Library | Library: All, Reels, Favorites, Watch Later, Folders, playlists |
| Search | Offline search by name/folder/date/format with filters + recents |
| NowPlayingScreen | Video player + vertical Reels viewer |
| GlassNavBar (Haze) | GlassNavBar (Haze) |
| Music backends, accounts, lyrics | Removed |
