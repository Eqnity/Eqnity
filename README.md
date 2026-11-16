# EqnityMusic

A beautiful, modern Android music app that downloads music from YouTube (videos & Shorts) and plays it locally with a Spotify-like experience.

## Features

- 🎵 **Download from YouTube** - Paste any YouTube video or Shorts link to download audio
- 🎨 **Dynamic Album Art Gradients** - Beautiful gradients extracted from album artwork
- 🌙 **Dark Mode Only** - Carefully crafted dark theme following 60-3-10 color rule
- 📱 **Three Tab Layout** - Home, Playlists, Support
- 🎧 **Full Playback Controls** - Play, pause, skip, loop, shuffle
- 📂 **Playlist Management** - Create, rename, delete playlists; add/remove songs
- ☁️ **Background Downloads** - WorkManager-powered downloads with progress tracking
- 🎯 **Media3 Integration** - Professional media playback with foreground service
- 💾 **Room Database** - Local storage for tracks, playlists, and downloads

## Architecture

- **Language**: Kotlin
- **Architecture**: MVVM with Repository pattern
- **DI**: Hilt
- **Database**: Room
- **Networking**: Retrofit + OkHttp
- **Image Loading**: Coil
- **Media Playback**: Media3 (ExoPlayer)
- **Background Work**: WorkManager
- **UI**: Material3, ViewBinding, Navigation Component

## Color Scheme (60-3-10 Rule)

- **60% Primary Surface**: `#121212` (Main background)
- **30% Secondary Surface**: `#1A1A1A` (Cards, sheets)
- **10% Accent**: `#FA2D48` (Apple Music Red - Primary actions)

## Building

```bash
./gradlew assembleDebug
```

## Requirements

- Android Studio Koala or later
- JDK 17
- Android SDK 34
- Minimum API 24 (Android 7.0)

## License

Proprietary - Developed by Eqnity