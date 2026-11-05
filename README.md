# Zmusic

A local music player for Android with a liquid-glass look.
Kotlin, Jetpack Compose and Media3. No images are loaded, so scrolling stays light.

## Build without a PC
1. Push this folder to a GitHub repo (branch `main`).
2. Open the repo's **Actions** tab. The *Build Zmusic APK* workflow runs on every push.
3. Download **Zmusic-release-apk** from the finished run (the fallback one is slower, only use it if release is missing).
4. Unzip it and install the `.apk` inside (it is named Zmusic-release.apk).

Release and debug builds use the same signing key, so they install over each other.

## Audio extraction
The Settings screen can hand an HTTP(S) URL to yt-dlp running in Termux. Install/configure Termux, then run `pkg install python ffmpeg`, `python -m pip install -U yt-dlp`, and `termux-setup-storage`. In Termux settings, enable **Allow external apps** (and grant the app the RUN_COMMAND permission prompt). Extraction uses `yt-dlp -x --audio-format mp3 --audio-quality 0` and saves the result to your shared Music folder. Only download content you have permission to use.

## Smoothness
- The app asks Android for the screen's highest refresh rate (120 Hz on the Tecno Spark 30C).
- If it still feels like 60 fps: Settings > Display > Refresh rate > High, and turn off Battery saver.
