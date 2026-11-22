package com.studiomogli.spotify

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse

/**
 * Inertia Mode Network Layer:
 * Intentionally lets all Spotify requests pass through untouched so Spotify's DRM,
 * spclient state machine, and future updates never detect network tampering or stall.
 * Ad handling is performed cleanly at playback time via the Auto-Reload Inertia Engine in JsScripts.kt.
 */
object AdBlocker {
    fun intercept(request: WebResourceRequest?): WebResourceResponse? = null
}
