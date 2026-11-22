package com.studiomogli.spotify

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * Layer 1 Spotify Network Ad Blocker.
 * Drops Spotify ad servers, ad-logic endpoints, and third-party ad networks while keeping
 * Spotify's audio CDN (scdn.co, akamaized.net), Widevine DRM license calls, and login hosts intact.
 */
object AdBlocker {

    private val SAFE_HOST_SUFFIXES = listOf(
        "scdn.co",
        "spotifycdn.com",
        "accounts.spotify.com",
        "login.spotify.com",
        "accounts.google.com",
        "myaccount.google.com",
        "gds.google.com",
        "ssl.gstatic.com"
    )

    private val BLOCKED_HOST_SUFFIXES = listOf(
        "adeventtracker.spotify.com",
        "audio-sp-iad.spotifycdn.net",
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "adnxs.com",
        "pubmatic.com",
        "rubiconproject.com",
        "openx.net",
        "moatads.com",
        "scorecardresearch.com",
        "google-analytics.com",
        "googletagmanager.com"
    )

    private val BLOCKED_PATH_SUBSTRINGS = listOf(
        "/ads/",
        "/ad-logic/",
        "/ads-v1/",
        "/ads-v2/",
        "/sponsored-content/",
        "/gabo-receiver-service/",
        "/pagead/",
        "/ptracking",
        "ad_type="
    )

    fun isAdRequest(request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val host = uri.host?.lowercase() ?: return false

        if (SAFE_HOST_SUFFIXES.any { host == it || host.endsWith(".$it") }) {
            return false
        }

        if (BLOCKED_HOST_SUFFIXES.any { host == it || host.endsWith(".$it") }) {
            return true
        }

        val fullUrl = uri.toString().lowercase()
        // Never block Widevine DRM license or playback token endpoints
        if (fullUrl.contains("widevine") || fullUrl.contains("license") || fullUrl.contains("melody")) {
            return false
        }

        return BLOCKED_PATH_SUBSTRINGS.any { fullUrl.contains(it) }
    }

    fun intercept(request: WebResourceRequest?): WebResourceResponse? {
        if (!isAdRequest(request)) return null
        return WebResourceResponse(
            "application/json",
            "utf-8",
            200,
            "OK",
            mapOf("Cache-Control" to "no-store"),
            ByteArrayInputStream("{}".toByteArray())
        )
    }
}
