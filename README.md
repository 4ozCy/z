# Zmusic

A local music player for Android with a liquid-glass look.
Kotlin, Jetpack Compose and Media3. No images are loaded, so scrolling stays light.

## Build without a PC
1. Push this folder to a GitHub repo (branch `main`).
2. Open the repo's **Actions** tab. The *Build Zmusic APK* workflow runs on every push.
3. Download **Zmusic-release-apk** from the finished run (the fallback one is slower, only use it if release is missing).
4. Unzip it and install the `.apk` inside (it is named Zmusic-release.apk).

Release and debug builds use the same signing key, so they install over each other.

## Smoothness
- The app asks Android for the screen's highest refresh rate (120 Hz on the Tecno Spark 30C).
- If it still feels like 60 fps: Settings > Display > Refresh rate > High, and turn off Battery saver.
