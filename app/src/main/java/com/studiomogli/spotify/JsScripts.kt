package com.studiomogli.spotify

object JsScripts {

    /**
     * Injected at document_start on open.spotify.com:
     *  - Page Visibility API spoof (keeps screen-off playback alive)
     *  - Fetch/XHR ad-slot JSON pruning for Spotify spclient ad endpoints
     */
    val DOCUMENT_START_SCRIPT: String = """
        (function() {
            var host = (location && location.hostname ? location.hostname : '').toLowerCase();
            if (host.indexOf('spotify.com') === -1) return;
            if (window.__spBgDocStartInstalled) return;
            window.__spBgDocStartInstalled = true;

            // 1. Page Visibility API Spoof
            try {
                Object.defineProperty(document, 'hidden', {
                    get: function() { return false; },
                    configurable: true
                });
                Object.defineProperty(document, 'webkitHidden', {
                    get: function() { return false; },
                    configurable: true
                });
                Object.defineProperty(document, 'visibilityState', {
                    get: function() { return 'visible'; },
                    configurable: true
                });
                Object.defineProperty(document, 'webkitVisibilityState', {
                    get: function() { return 'visible'; },
                    configurable: true
                });
            } catch (e) {}

            var blockedEvents = ['visibilitychange', 'webkitvisibilitychange', 'pagehide', 'freeze'];
            blockedEvents.forEach(function(evtName) {
                window.addEventListener(evtName, function(e) {
                    e.stopImmediatePropagation();
                }, true);
                document.addEventListener(evtName, function(e) {
                    e.stopImmediatePropagation();
                }, true);
            });

            // 2. Intercept fetch() calls to Spotify ad-logic / ads endpoints
            function isSpotifyAdApi(url) {
                if (!url || typeof url !== 'string') return false;
                var lower = url.toLowerCase();
                return lower.indexOf('/ads/') !== -1 ||
                       lower.indexOf('/ad-logic/') !== -1 ||
                       lower.indexOf('/ads-v') !== -1 ||
                       lower.indexOf('adeventtracker.spotify.com') !== -1;
            }

            if (typeof window.fetch === 'function') {
                var origFetch = window.fetch;
                window.fetch = function(input, init) {
                    var url = '';
                    try {
                        url = typeof input === 'string' ? input : (input && input.url ? input.url : '');
                    } catch (e) {}

                    if (isSpotifyAdApi(url)) {
                        return Promise.resolve(new Response('{}', {
                            status: 200,
                            headers: { 'Content-Type': 'application/json' }
                        }));
                    }
                    return origFetch.apply(this, arguments);
                };
            }
        })();
    """.trimIndent()

    /**
     * Injected on page load for open.spotify.com:
     *  - Mobile-friendly CSS cleanup (hides desktop Upgrade/Install banners & oversized side panels)
     *  - Auto-mute fallback if an audio ad plays + suppresses ad titles on lock screen
     *  - Lock-screen / Bluetooth remote control bridge mapped to Spotify's data-testid buttons
     *  - Playback state & metadata sync to Android Foreground Service
     */
    val RUNTIME_AND_COSMETIC_SCRIPT: String = """
        (function() {
            var host = (location && location.hostname ? location.hostname : '').toLowerCase();
            if (host.indexOf('spotify.com') === -1) return;
            if (window.__spBgRuntimeInstalled) return;
            window.__spBgRuntimeInstalled = true;

            // 1. Mobile-Friendly & Cosmetic CSS Cleanup
            function injectSpotifyCss() {
                try {
                    if (document.getElementById('sp-mogli-css')) return;
                    var style = document.createElement('style');
                    style.id = 'sp-mogli-css';
                    style.textContent = [
                        '/* Hide Upgrade, Install App, and Promo Banners */',
                        'a[href*="/download"],',
                        'button[data-testid="upgrade-button"],',
                        'a[data-testid="upgrade-button"],',
                        '[data-testid="billboard-ad"],',
                        '[data-testid="hpto-parent-container"],',
                        '[data-testid="ad-slot-container"],',
                        '[aria-label="Install App"],',
                        '[aria-label="Explore Premium"],',
                        '.Root__ads-container {',
                        '    display: none !important;',
                        '    visibility: hidden !important;',
                        '    height: 0 !important;',
                        '    pointer-events: none !important;',
                        '}',
                        '/* Make Desktop Spotify UI cleaner on narrow mobile screens */',
                        '@media (max-width: 900px) {',
                        '    .Root__right-sidebar, aside[aria-label="Now playing view"] {',
                        '        display: none !important;',
                        '    }',
                        '    .Root__now-playing-bar {',
                        '        padding-left: 6px !important;',
                        '        padding-right: 6px !important;',
                        '    }',
                        '    button[data-testid="control-button-playpause"] {',
                        '        transform: scale(1.15);',
                        '    }',
                        '}'
                    ].join('\n');
                    (document.head || document.documentElement).appendChild(style);
                } catch (e) {}
            }
            injectSpotifyCss();

            // 2. Ad Detection & Auto-Mute Fallback
            var adMuteActive = false;

            function isSpotifyAdPlaying() {
                try {
                    if (document.querySelector('[data-testid="context-item-info-ad-title"], [data-testid="ad-Companion-card"], a[data-testid="context-item-link"][href*="/ad/"]')) {
                        return true;
                    }
                    if (navigator.mediaSession && navigator.mediaSession.metadata) {
                        var t = (navigator.mediaSession.metadata.title || '').toLowerCase();
                        if (t === 'advertisement' || t === 'spotify') {
                            return true;
                        }
                    }
                    var trackTitle = document.querySelector('[data-testid="context-item-info-title"]');
                    if (trackTitle) {
                        var text = (trackTitle.textContent || '').trim().toLowerCase();
                        if (text === 'advertisement' || text === 'sponsored') {
                            return true;
                        }
                    }
                } catch (e) {}
                return false;
            }

            function handleAdMuteFallback() {
                try {
                    var mediaEls = document.querySelectorAll('audio, video');
                    var muteBtn = document.querySelector('[data-testid="volume-bar-toggle-mute-button"]');
                    var adNow = isSpotifyAdPlaying();

                    if (adNow) {
                        for (var i = 0; i < mediaEls.length; i++) {
                            mediaEls[i].muted = true;
                        }
                        if (!adMuteActive) {
                            adMuteActive = true;
                            if (muteBtn) {
                                var label = (muteBtn.getAttribute('aria-label') || '').toLowerCase();
                                if (label.indexOf('mute') !== -1 && label.indexOf('unmute') === -1) {
                                    muteBtn.click();
                                }
                            }
                        }
                    } else if (adMuteActive) {
                        adMuteActive = false;
                        for (var j = 0; j < mediaEls.length; j++) {
                            mediaEls[j].muted = false;
                        }
                        if (muteBtn) {
                            var labelAfter = (muteBtn.getAttribute('aria-label') || '').toLowerCase();
                            if (labelAfter.indexOf('unmute') !== -1) {
                                muteBtn.click();
                            }
                        }
                    }
                } catch (e) {}
            }

            setInterval(handleAdMuteFallback, 600);

            // 3. Remote Control Bridge for Lock Screen & Bluetooth Buttons
            window.__spBgControl = function(action) {
                try {
                    var playPauseBtn = document.querySelector('[data-testid="control-button-playpause"]');
                    var nextBtn = document.querySelector('[data-testid="control-button-skip-forward"]');
                    var prevBtn = document.querySelector('[data-testid="control-button-skip-back"]');

                    if (action === 'toggle') {
                        if (playPauseBtn) playPauseBtn.click();
                    } else if (action === 'play') {
                        if (playPauseBtn) {
                            var lbl = (playPauseBtn.getAttribute('aria-label') || '').toLowerCase();
                            if (lbl.indexOf('play') !== -1) playPauseBtn.click();
                        }
                    } else if (action === 'pause') {
                        if (playPauseBtn) {
                            var lblP = (playPauseBtn.getAttribute('aria-label') || '').toLowerCase();
                            if (lblP.indexOf('pause') !== -1) playPauseBtn.click();
                        }
                    } else if (action === 'next') {
                        if (nextBtn) nextBtn.click();
                    } else if (action === 'prev') {
                        if (prevBtn) prevBtn.click();
                    }
                } catch (e) {}
            };

            // 4. Sync Playback State & Track Metadata to Android Foreground Service
            var lastState = '';
            function syncPlaybackState() {
                try {
                    if (!window.AndroidBridge || !window.AndroidBridge.onPlaybackStateChanged) return;
                    if (isSpotifyAdPlaying()) return;

                    var isPlaying = false;
                    if (navigator.mediaSession && navigator.mediaSession.playbackState) {
                        isPlaying = navigator.mediaSession.playbackState === 'playing';
                    }
                    if (!isPlaying) {
                        var playPauseBtn = document.querySelector('[data-testid="control-button-playpause"]');
                        if (playPauseBtn) {
                            var aria = (playPauseBtn.getAttribute('aria-label') || '').toLowerCase();
                            if (aria.indexOf('pause') !== -1) isPlaying = true;
                        }
                    }

                    var title = '';
                    var artist = '';

                    if (navigator.mediaSession && navigator.mediaSession.metadata) {
                        title = navigator.mediaSession.metadata.title || '';
                        artist = navigator.mediaSession.metadata.artist || '';
                    }

                    if (!title) {
                        var titleEl = document.querySelector('[data-testid="context-item-info-title"]');
                        if (titleEl) title = (titleEl.textContent || '').trim();
                    }
                    if (!artist) {
                        var artistEl = document.querySelector('[data-testid="context-item-info-subtitles"]');
                        if (artistEl) artist = (artistEl.textContent || '').trim();
                    }

                    if (!title) title = 'Spotify';
                    if (!artist) artist = isPlaying ? 'Playing in background' : 'Paused';

                    var signature = isPlaying + '|' + title + '|' + artist;
                    if (signature !== lastState) {
                        lastState = signature;
                        window.AndroidBridge.onPlaybackStateChanged(isPlaying, title, artist);
                    }
                } catch (e) {}
            }

            setInterval(syncPlaybackState, 1500);
            document.addEventListener('click', function() {
                setTimeout(syncPlaybackState, 300);
            }, true);
        })();
    """.trimIndent()
}
