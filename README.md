# Spotify v1.0.0 — Made by Studio Mogli

Android wrapper for **Spotify Web (`open.spotify.com`)** built by **Studio Mogli** ([studiomogli.com](https://studiomogli.com)).

## Features
- **Widevine DRM Enabled:** Grants `RESOURCE_PROTECTED_MEDIA_ID` so encrypted Spotify audio streams play natively inside Android WebView.
- **On-Demand Playback & Unlimited Skips:** Uses an engine-matched Desktop Chrome UA to unlock on-demand song selection on free accounts.
- **Screen-Off Background Playback:** `BackgroundWebView` + Foreground Media Service + `PARTIAL_WAKE_LOCK` + lock-screen & Bluetooth controls.
- **Spotify Ad Filtering & Auto-Mute:** Blocks Spotify ad endpoints (`/ads/`, `/ad-logic/`, `adeventtracker.spotify.com`), hides "Upgrade / Install App" banners, and auto-mutes if an ad slips through.
- **Isolated Google Sign-In:** Email/password works directly on `accounts.spotify.com`, and "Continue with Google" routes into the clean `LoginActivity`.
- **Studio Mogli Trust Card, Refer & Earn 10%, and Auto-Hidden Update Button:** Checks `harekrishna1602/spotify-music` GitHub Releases silently on launch.

## Build via GitHub Actions
1. Create a new **Public** GitHub repository named `spotify-music`.
2. Upload all files from `Spotify-Android-v1.0.0.zip` (and make sure `.github/workflows/build-apk.yml` is added).
3. Wait ~2–3 minutes in the **Actions** tab. Your APK (`Spotify-v1.0.0-StudioMogli.apk`) will appear automatically under **Releases** and **Actions Artifacts**.
