package com.studiomogli.spotify

object JsScripts {

    /**
     * Injected at document_start on open.spotify.com:
     *  - Page Visibility API spoof (keeps screen-off playback alive)
     *  - Zero network/fetch tampering (Inertia Method: lets Spotify run 100% normally)
     */
    val DOCUMENT_START_SCRIPT: String = """
        (function() {
            var host = (location && location.hostname ? location.hostname : '').toLowerCase();
            if (host.indexOf('spotify.com') === -1) return;
            if (window.__spBgDocStartInstalled) return;
            window.__spBgDocStartInstalled = true;

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
        })();
    """.trimIndent()

    /**
     * Injected on page load for open.spotify.com:
     *  1. Mobile-Native UI Overhaul (device-width viewport, collapsible sidebar, clean bottom player bar)
     *  2. Inertia Auto-Reload Ad Skipper (sleeps during music; on ad hit -> mutes -> reloads -> auto-resumes)
     *  3. Lock-screen / Bluetooth remote control bridge
     *  4. Playback state & metadata sync to Android Foreground Service
     */
    val RUNTIME_AND_COSMETIC_SCRIPT: String = """
        (function() {
            var host = (location && location.hostname ? location.hostname : '').toLowerCase();
            if (host.indexOf('spotify.com') === -1) return;
            if (window.__spBgRuntimeInstalled) return;
            window.__spBgRuntimeInstalled = true;

            // --- 1. Enforce True Mobile Viewport Scaling ---
            function enforceMobileViewport() {
                try {
                    var head = document.head || document.documentElement;
                    if (!head) return;
                    var existing = document.querySelector('meta[name="viewport"]');
                    var content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover';
                    if (existing) {
                        existing.setAttribute('content', content);
                    } else {
                        var meta = document.createElement('meta');
                        meta.name = 'viewport';
                        meta.content = content;
                        head.appendChild(meta);
                    }
                } catch (e) {}
            }
            enforceMobileViewport();

            // --- 2. Mobile-Native CSS Overhaul ---
            function injectMobileSpotifyCss() {
                try {
                    if (document.getElementById('sp-mogli-mobile-css')) return;
                    var style = document.createElement('style');
                    style.id = 'sp-mogli-mobile-css';
                    style.textContent = [
                        '/* Prevent desktop min-width horizontal scroll */',
                        'html, body, #main, .Root, .Root__top-container {',
                        '    min-width: 0 !important;',
                        '    max-width: 100vw !important;',
                        '    overflow-x: hidden !important;',
                        '}',
                        '',
                        '/* Hide desktop-only clutter, promos, and right sidebar */',
                        'a[href*="/download"],',
                        'button[data-testid="upgrade-button"],',
                        'a[data-testid="upgrade-button"],',
                        '[data-testid="billboard-ad"],',
                        '[data-testid="hpto-parent-container"],',
                        '[data-testid="ad-slot-container"],',
                        '[aria-label="Install App"],',
                        '[aria-label="Explore Premium"],',
                        '[aria-label="What\'s New"],',
                        '[aria-label="Friend Activity"],',
                        '[data-testid="whats-new-feed-button"],',
                        '[data-testid="friend-activity-button"],',
                        '.Root__right-sidebar,',
                        'aside[aria-label="Now playing view"],',
                        '.Root__ads-container {',
                        '    display: none !important;',
                        '    visibility: hidden !important;',
                        '    width: 0 !important;',
                        '    height: 0 !important;',
                        '    pointer-events: none !important;',
                        '}',
                        '',
                        '/* Collapse Left Library Sidebar by default so Main View uses 100% width */',
                        'body:not(.sp-show-library) .Root__nav-bar,',
                        'body:not(.sp-show-library) #Desktop_LeftSidebar_Id,',
                        'body:not(.sp-show-library) [data-testid="left-sidebar"] {',
                        '    display: none !important;',
                        '    width: 0 !important;',
                        '    min-width: 0 !important;',
                        '}',
                        '',
                        '/* When Library toggle is active, show sidebar as a clean slide-over drawer */',
                        'body.sp-show-library .Root__nav-bar,',
                        'body.sp-show-library #Desktop_LeftSidebar_Id,',
                        'body.sp-show-library [data-testid="left-sidebar"] {',
                        '    display: flex !important;',
                        '    position: fixed !important;',
                        '    top: 0 !important;',
                        '    left: 0 !important;',
                        '    bottom: 76px !important;',
                        '    width: 82vw !important;',
                        '    max-width: 340px !important;',
                        '    z-index: 9998 !important;',
                        '    background: #121212 !important;',
                        '    box-shadow: 6px 0 24px rgba(0,0,0,0.85) !important;',
                        '}',
                        '',
                        '/* Make main view fill the full screen width cleanly */',
                        '.Root__top-container {',
                        '    display: flex !important;',
                        '    flex-direction: column !important;',
                        '    padding: 0 !important;',
                        '    gap: 0 !important;',
                        '}',
                        '.Root__main-view {',
                        '    width: 100% !important;',
                        '    max-width: 100vw !important;',
                        '    min-width: 0 !important;',
                        '    flex: 1 1 auto !important;',
                        '    border-radius: 0 !important;',
                        '}',
                        '.Root__globalNav {',
                        '    padding: 6px 8px !important;',
                        '    gap: 6px !important;',
                        '}',
                        '',
                        '/* Thumb-friendly Mobile Mini-Player Bar */',
                        '.Root__now-playing-bar {',
                        '    width: 100% !important;',
                        '    min-width: 0 !important;',
                        '    padding: 6px 10px !important;',
                        '    background: #0D0D0D !important;',
                        '    border-top: 1px solid #1F1F1F !important;',
                        '    z-index: 9999 !important;',
                        '}',
                        '.Root__now-playing-bar footer[data-testid="now-playing-bar"] {',
                        '    min-width: 0 !important;',
                        '    height: auto !important;',
                        '    min-height: 64px !important;',
                        '    padding: 4px 0 !important;',
                        '}',
                        '/* Hide desktop-only bottom-right buttons (volume, queue, lyrics, device, fullscreen) */',
                        '[data-testid="volume-bar"],',
                        '[data-testid="control-button-npv"],',
                        '[data-testid="lyrics-button"],',
                        '[data-testid="control-button-queue"],',
                        '[data-testid="device-picker-icon-button"],',
                        '[data-testid="fullscreen-mode-button"],',
                        '[data-testid="pip-toggle-button"],',
                        '[data-testid="now-playing-bar"] > div > div:last-child {',
                        '    display: none !important;',
                        '}',
                        '/* Enlarge core playback controls for mobile thumbs */',
                        'button[data-testid="control-button-playpause"] {',
                        '    transform: scale(1.22) !important;',
                        '    margin: 0 8px !important;',
                        '}',
                        'button[data-testid="control-button-skip-forward"],',
                        'button[data-testid="control-button-skip-back"] {',
                        '    transform: scale(1.12) !important;',
                        '}'
                    ].join('\n');
                    (document.head || document.documentElement).appendChild(style);
                } catch (e) {}
            }
            injectMobileSpotifyCss();

            // Expose Library Drawer Toggle for the Native Top Bar Button
            window.__spToggleLibrary = function() {
                try {
                    if (!document.body) return false;
                    var isOpen = document.body.classList.toggle('sp-show-library');
                    return isOpen;
                } catch (e) {
                    return false;
                }
            };

            // Auto-close Library drawer when user taps a playlist/item inside it
            document.addEventListener('click', function(e) {
                try {
                    if (!document.body || !document.body.classList.contains('sp-show-library')) return;
                    var sidebar = document.querySelector('.Root__nav-bar, #Desktop_LeftSidebar_Id, [data-testid="left-sidebar"]');
                    if (sidebar && !sidebar.contains(e.target)) {
                        document.body.classList.remove('sp-show-library');
                    }
                } catch (err) {}
            }, true);

            // --- 3. Inertia Auto-Reload Ad Skipper ---
            var RELOAD_COOLDOWN_MS = 12000;
            var adMuteActive = false;

            function isSpotifyAdPlaying() {
                try {
                    // Primary: Standard MediaSession API (survives Spotify UI redesigns)
                    if (navigator.mediaSession && navigator.mediaSession.metadata) {
                        var msTitle = (navigator.mediaSession.metadata.title || '').trim().toLowerCase();
                        if (msTitle === 'advertisement' || msTitle === 'spotify' || msTitle === 'sponsored') {
                            return true;
                        }
                    }
                    // Secondary DOM fallback
                    if (document.querySelector('[data-testid="context-item-info-ad-title"], [data-testid="ad-Companion-card"], a[data-testid="context-item-link"][href*="/ad/"]')) {
                        return true;
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

            function muteAllMedia(shouldMute) {
                try {
                    var mediaEls = document.querySelectorAll('audio, video');
                    for (var i = 0; i < mediaEls.length; i++) {
                        mediaEls[i].muted = shouldMute;
                    }
                } catch (e) {}
            }

            // If we just reloaded to skip an ad, automatically press Play once player is ready
            function resumeAfterInertiaReload() {
                try {
                    if (sessionStorage.getItem('__sp_resume_after_reload') !== '1') return;
                    sessionStorage.removeItem('__sp_resume_after_reload');

                    var attempts = 0;
                    var resumeTimer = setInterval(function() {
                        attempts++;
                        if (attempts > 30) {
                            clearInterval(resumeTimer);
                            return;
                        }
                        var playBtn = document.querySelector('[data-testid="control-button-playpause"]');
                        if (playBtn) {
                            var aria = (playBtn.getAttribute('aria-label') || '').toLowerCase();
                            if (aria.indexOf('play') !== -1) {
                                playBtn.click();
                                clearInterval(resumeTimer);
                            } else if (aria.indexOf('pause') !== -1) {
                                // Already playing
                                clearInterval(resumeTimer);
                            }
                        }
                    }, 500);
                } catch (e) {}
            }
            resumeAfterInertiaReload();

            function checkInertiaAdSkip() {
                try {
                    var adNow = isSpotifyAdPlaying();
                    if (adNow) {
                        // Step 1: Instantly mute so user hears zero ad audio
                        muteAllMedia(true);
                        adMuteActive = true;

                        // Step 2: Trigger clean Auto-Reload if outside cooldown window
                        var lastReload = parseInt(sessionStorage.getItem('__sp_last_ad_reload') || '0', 10);
                        var now = Date.now();
                        if (now - lastReload > RELOAD_COOLDOWN_MS) {
                            sessionStorage.setItem('__sp_last_ad_reload', String(now));
                            sessionStorage.setItem('__sp_resume_after_reload', '1');
                            location.reload();
                        }
                    } else if (adMuteActive) {
                        muteAllMedia(false);
                        adMuteActive = false;
                    }
                } catch (e) {}
            }

            setInterval(checkInertiaAdSkip, 600);

            // --- 4. Remote Control Bridge for Lock Screen & Bluetooth Buttons ---
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

            // --- 5. Sync Playback State & Track Metadata to Android Foreground Service ---
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
