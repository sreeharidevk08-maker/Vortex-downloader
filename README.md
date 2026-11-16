# VORTEX Downloader (Android)

A clean, ad-free Android Video & Audio Downloader built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Room**, **DataStore**, **WorkManager**, **OkHttp**, and **Android Media3**.

## Features Implemented
- **Authorized Source Analysis (`AuthorizedSourceResolver`)**:
  - Supports **Internet Archive** (`archive.org/details/<id>`) via its public metadata API and **direct HTTPS media streams** (`.mp4`, `.mp3`, `.m4a`, `.webm`, `.wav`, `.ogg`, `.flac`).
  - Explicitly blocks and explains restricted/DRM-protected sources (e.g., YouTube, Netflix, Spotify) without bypassing access controls.
- **Video & Audio Downloads**:
  - Displays only genuinely available qualities (`2160p` down to `144p`) and audio bitrates (`320 / 256 / 192 kbps`, never artificially upscaled).
  - Real-time HTTP streaming with `Range` pause/resume, speed, ETA, duplicate prevention, SHA-256 verification, and Scoped Storage output to `Movies/VORTEX` and `Music/VORTEX`.
  - Native `MediaExtractor` + `MediaMuxer` (`MediaProcessor`) for muxing separate video+audio tracks or extracting audio tracks with separate conversion progress.
- **Frosted-Glass UI**:
  - Deep midnight black, dark charcoal, frosted white, soft violet, electric blue, and subtle cyan palette with Dark, Light, and System themes.
  - 4-tab `FloatingGlassNavBar`: **Home**, **Downloads**, **Library**, **Settings**.
  - Built-in **Media3 ExoPlayer** modal with seek bar, volume, playback speed (`0.75x`–`2.0x`), fullscreen video, and `VortexPlaybackService` for background audio.
- **Zero Ads & Zero Trackers**: No ad SDKs, no analytics SDKs, no hardcoded secrets.

## Building Debug & Release APKs
Because the project uses Android Gradle Plugin 8.7.3 and Android SDK 35 (`minSdk = 29` / Android 10+):

1. Open the `VortexDownloader/` folder in **Android Studio** (Ladybug or newer) so Android Studio syncs Gradle and downloads dependencies from Google Maven / Maven Central.
2. Or build from the command line (with `ANDROID_HOME` and Gradle 8.9+ configured):
   ```bash
   gradle wrapper --gradle-version 8.9
   ./gradlew testDebugUnitTest
   ./gradlew assembleDebug
   ./gradlew assembleRelease
   ```
3. Outputs:
   - Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
   - Release APK (unsigned until configured with your keystore in `signingConfigs`): `app/build/outputs/apk/release/app-release-unsigned.apk`
4. Install on an Android 10+ device via ADB:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
