# LyricWave — Android source project

A native Android local-audio player starter with animated playback artwork and synchronized `.lrc` lyrics.

## Included
- Select multiple local audio files through Android's file picker.
- Play/pause, previous/next, seek bar and elapsed time.
- Import a timed `.lrc` lyrics file.
- Current lyric and next lyric update during playback.
- Rotating record animation while audio plays.
- Media3 MediaSession provides standard media metadata and playback controls to Android where supported.

## Build an APK
1. Open this folder in Android Studio (recent version recommended).
2. Install Android SDK Platform 35 if Android Studio asks.
3. Let Gradle sync.
4. Connect your Android phone with USB debugging enabled, or choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Lyrics format
Import a timed `.lrc` file. Example:

```text
[00:00.00]First line of the song
[00:04.50]Second line appears at 4.5 seconds
[00:08.20]Third line appears at 8.2 seconds
```

The lyrics file is selected separately from the audio. This starter does not fetch copyrighted lyrics automatically.

## Android media card limitation
Android draws its standard lock-screen and notification media card. Third-party apps cannot reliably place custom animated karaoke lyrics inside that system card across all Android versions and phone manufacturers. This starter animates lyrics inside the app and exposes standard playback metadata and controls through MediaSession.

## Current scope
This is an Android Studio source project, not a precompiled APK. Playback currently lives in the activity; background playback service and a dedicated lyric notification are next-stage improvements.

## Build APK online with GitHub Actions
1. Open the repository on GitHub and select the **Actions** tab.
2. Open **Build LyricWave APK** and tap **Run workflow** (or push to `main` / `master` to trigger it).
3. Wait for the workflow to finish successfully.
4. Open the completed workflow run, scroll to **Artifacts**, and download `LyricWave-debug-apk`.
5. Extract the downloaded artifact ZIP. Install `app-debug.apk` on your Android phone. You may need to allow Chrome/Files to install unknown apps.

The workflow creates a debug APK for testing, not a Play Store release-signed APK.
