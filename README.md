# Spotify v1.1.0 — Made by Studio Mogli

Android wrapper for **Spotify Web (`open.spotify.com`)** built by **Studio Mogli** ([studiomogli.com](https://studiomogli.com)).

## What's New in v1.1.0
- **Mobile-Native UI Overhaul:** Locks the viewport to `device-width` (`loadWithOverviewMode = false`) so text and cards render at natural phone size instead of shrunk desktop scale.
- **Full-Width Main View + Library Toggle:** Collapses Spotify's desktop left sidebar by default so playlists and search take 100% width; tap **Library** in the top bar anytime to slide open your playlists.
- **Clean Mobile Mini-Player:** Strips desktop clutter (volume slider, Queue, Lyrics, Connect Device, Fullscreen, What's New, Friend Activity) and enlarges Play/Pause/Prev/Next controls.
- **Inertia Auto-Reload Ad Skipper:** Zero network-level blocking so Spotify's DRM and updates never break. Sleeps silently during music; the instant an ad hits (`MediaSession` metadata detection), it mutes, reloads the player (with a 12-second anti-loop cooldown), and automatically resumes playback.
