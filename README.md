# VYBE 🎵

**A Next-Generation Music Player & Local Media Hub with AI Playlist Vibe Matching.**

Built with **Jetpack Compose**, **AndroidX Media3 (ExoPlayer)**, and an **Offline 15-Dimensional Vector AI Recommendation Engine**.

---

## ✨ Features

- 📁 **Local Device Storage Playback**: Directly browse, search, and play audio files stored on your Android device with automatic album art resolution via `MediaStore`.
- 🎶 **Local & Mixed Playlists**: Create custom playlists with device audio files or mix local and streaming music together.
- 🧠 **AI Playlist Vibe Match Engine**: 
  - Analyzes the acoustic embedding centroid of any playlist using an offline database of **170,000+ tracks**.
  - Calculates Cosine Similarity in 15-dimensional latent music feature space.
  - Generates recommended tracks matching the playlist's exact tempo, energy, and mood with a single tap.
- ⚡ **Dual-Player Seamless Crossfade**: Powered by dual ExoPlayer instances (`playerA` + `playerB`) for gapless transitions with zero audio latency.
- 📜 **Real-Time Word-by-Word Synced Lyrics**: Apple Music-style spring physics, dynamic canvas animations, and KuGou/LrcLib dual lyric integration.
- 🎨 **Dynamic Kawarp Mesh Gradient**: Liquid visualizer background shifting with album colors using `dev.chrisbanes.haze` blur.
- 🎧 **Hardware DSP & Dolby Atmos**: Probes hardware codecs (`audio/eac3`, `audio/ac4`) and interfaces with system equalizers and OEM sound engines.

---

## 🛠️ Tech Stack & Architecture

- **UI**: 100% Jetpack Compose with Material 3 & Navigation Compose.
- **Audio Service**: AndroidX Media3 `MediaSessionService` + ExoPlayer.
- **Dependency Injection**: Dagger Hilt.
- **Database**: Android Room + SQLite Vector Embedding DB.
- **Target SDK**: Android 16 (API 36), Min SDK 24.

---

## 🚀 Getting Started in Android Studio

1. Open **Android Studio**.
2. Select **Open** and choose the `VYBE` directory.
3. Sync Gradle and build the project (`Build -> Make Project`).
4. Run on an Android device or emulator running Android 7.0 (API 24) or higher.

---

## 📦 Pushing to Your GitHub

To push this repository to your GitHub account (`muhammadfadilah-lab`):

```bash
git init
git add .
git commit -m "Initial commit: VYBE Music Player with Local Playback & AI Vibe Match"
git branch -M main
git remote add origin https://github.com/muhammadfadilah-lab/VYBE.git
git push -u origin main
```
