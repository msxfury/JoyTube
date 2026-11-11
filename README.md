<div align="center">

# JoyTube for Android

**Browse YouTube from inside JoyTube and keep your own media library in one Android app.**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![GeckoView](https://img.shields.io/badge/Browser-GeckoView-FF7139?logo=firefoxbrowser&logoColor=white)](https://mozilla.github.io/geckoview/)
[![Version](https://img.shields.io/badge/Version-1.0.6-blue)](#release-information)
[![GitHub](https://img.shields.io/badge/GitHub-thecodencoffee%2FJoyTube-181717?logo=github)](https://github.com/thecodencoffee/JoyTube)

**Maintainer: [thecodencoffee](https://github.com/thecodencoffee)** · [Latest releases](https://github.com/thecodencoffee/JoyTube/releases/latest)

</div>

JoyTube is an Android media browsing and personal-library project built with Kotlin and Jetpack Compose. Its YouTube Home, Shorts, and Search screens load YouTube's mobile website inside GeckoView. JoyTube also provides a separate account library for user-imported media.

> JoyTube is an independent project, not an official YouTube app. YouTube pages and playback remain subject to YouTube's availability, restrictions, and ads. JoyTube does not provide YouTube Premium features or remove YouTube ads.

## Release information

| Item | Value |
| --- | --- |
| App version | **1.0.6** |
| Android version code | `7` |
| Minimum Android | Android 8.0 (API 26) |
| Target Android | API 36 |
| Maintainer | [thecodencoffee](https://github.com/thecodencoffee) |

## Features

| Feature | What it does | How it works | Status and limits |
| --- | --- | --- | --- |
| YouTube Home inside JoyTube | Shows YouTube's mobile homepage in the Home tab. | GeckoView loads `https://m.youtube.com` within the app shell. JoyTube's branded header and floating navigation remain available. | Uses YouTube's website directly. Network access and YouTube's own availability, sign-in, playback, and page behavior apply. |
| YouTube Shorts inside JoyTube | Opens YouTube Shorts from JoyTube's Shorts tab. | GeckoView loads `https://m.youtube.com/shorts` so the page's own gestures and controls handle Shorts browsing. | Depends on YouTube's mobile website and device/network compatibility. |
| YouTube Search inside JoyTube | Opens YouTube's own search page from the top-right search control or Search tab. | GeckoView loads YouTube's mobile results page; queries and result interactions are handled by YouTube's page. | JoyTube does not proxy the search or guarantee YouTube results. |
| YouTube quality preference | Offers Auto, highest available, and resolution choices on YouTube video pages. | A built-in GeckoView content script requests the chosen tier from YouTube's player and falls back to a tier the player reports as available. | YouTube owns the stream and available quality tiers. The player hook is undocumented and may change. Choosing Auto may require reloading a video after a fixed tier was requested. No Premium tier is unlocked and no source quality is created or enhanced. |
| YouTube volume boost | Adds a volume control for HTML video and audio on YouTube and YouTube Music. | Uses a Web Audio gain stage and compressor, up to 250%, when the active media stream can safely be routed. | Some streams do not allow Web Audio routing and remain at normal YouTube volume. Strong boosts can still distort audio. |
| Compact JoyTube header | Keeps the JoyTube wordmark at top left and YouTube search at top right. | Uses a shorter in-app header and removes the separate account avatar action. | Account remains accessible from the bottom dock; when the dock is hidden, tap the JoyTube wordmark to return to Account. |
| Hide JoyTube navigation | Hides the floating JoyTube dock so YouTube's own page controls have more room. | The setting is under Settings → Application and syncs with JoyTube account settings. The YouTube viewport reserves space for the dock when visible. | Can be turned back on from Settings; with the dock hidden, tap the JoyTube wordmark to open Account. |
| Account destinations | Provides New Imported Joy's, settings, collections, and personal library access. | Account no longer lists History or Browser destinations; browser controls remain in the Browser feature. | Browser and History screens still exist in the app but are not Account shortcuts. |
| GeckoView browser | Displays web pages entered or opened in the built-in browser; links requesting a new window stay in the current JoyTube browser page. | Uses a shared GeckoView runtime and an in-app browser session. JoyTube's Back, Forward, and Refresh controls use the current Gecko session history. | Implemented for web browsing. Individual sites may ignore the dark preference or behave differently from Firefox. |
| Optional privacy filtering | Filters matching tracker network requests in the GeckoView browser. | Brave's official Rust `adblock` engine (version 0.13.5) is compiled to WebAssembly and runs in a built-in GeckoView extension with the EasyPrivacy list. The Rust engine and request listener initialize only when the setting is enabled. | Off by default. Filtering applies to GeckoView only; YouTube-owned domains and requests from YouTube pages are exempt. It does not remove YouTube ads, hide page elements, or affect the separate YouTube player. |
| Filter-list refresh | Keeps the bundled EasyPrivacy rules current. | When filtering is enabled, downloads are validated and parsed into a new engine before swapping it in; failures retain the previous working list. | Checks every four days while enabled. Requires network access for updates; the bundled list supports first use and offline startup. |
| New Imported Joy's | Lists publicly published videos, Shorts, and music from JoyTube accounts. | Reads public `imported_assets` rows that have a storage path and opens the corresponding public Supabase Storage file in GeckoView. | Read-only public feed. It requires public records, uploaded objects, and the Supabase migration/policies. This release does not include a complete upload/publish workflow. |
| Google sign-in | Signs the user into a JoyTube account. | Android Credential Manager obtains Google identity information; Supabase Auth establishes the JoyTube session. | Requires correct Google Cloud OAuth and Supabase provider configuration. It does not request YouTube API access or link subscriptions. |
| Google profile | Shows the account name, email, and profile photo in the JoyTube profile screen. | Uses the Google profile fields available in the signed-in Supabase session. | Available after sign-in when Google provides the fields; a JoyTube avatar fallback is used if no photo URL is available. |
| App update notice | Notifies signed-in users when a newer Android APK release is available. | Checks the latest stable GitHub release immediately after sign-in and every 30 minutes while the app process is running. It compares release and installed versions and requires an APK asset. | A dismissed release is remembered on-device. The Update button opens the APK link; Android handles installation. Checks may be delayed when Android stops or restricts the app. |
| Local media import | Adds files selected by the user to the JoyTube library. | Uses Android's file picker and keeps the selected file reference on that device. | Available for selected local files and supported file types. File access can depend on the original Android permission grant. |
| Local media playback | Plays supported imported files. | Uses Android Media3 and device-provided decoders. | Works only for formats/codecs supported by the device. This does not make YouTube streams downloadable. |
| Account and library metadata | Stores JoyTube profile/settings and private library metadata in Supabase when configured. | The Android app calls Supabase Auth and database APIs directly; database row-level security enforces user access. Public Joys use public metadata and files in the configured storage bucket. | Requires the database migrations, provider configuration, network access, and correct RLS policies. Local imports are not automatically uploaded or made public. |
| Theme and app information | Offers JoyTube theme settings and in-app About and Terms/Privacy screens. | Native Compose screens display app settings and descriptions of current behavior. | Screens are included; cloud persistence depends on Supabase configuration. |
| Rust API service | Provides an optional Rust/Axum Supabase proxy scaffold. | Source exists separately from the Android app. | Not deployed and not connected to the Android client. |

The media controls are opened from the floating speaker button over the YouTube page. They run only on YouTube domains. Quality controls are hidden on YouTube Music, where the page controls audio playback. The selected quality and boost are stored in GeckoView's local extension storage; they are not uploaded to Supabase. The controls do not intercept requests or change YouTube advertising behavior.

### Android release APKs

Release builds produce separate APKs for ARM64, ARMv7, and x86_64 devices. Install the APK matching the device architecture; there is no universal APK. For a Samsung Galaxy S21, use the ARM64 APK. GeckoView includes a large native browser engine, while the privacy-filter and media-control assets add browser features without separate native libraries per CPU architecture.

### What the YouTube browser does not do

The GeckoView browser displays YouTube's website inside JoyTube. It does not turn JoyTube into an official YouTube client, grant API access to subscriptions or watch history, guarantee every video or live stream will play, bypass paid features, or block YouTube ads. Google sign-in for a JoyTube account is separate from sign-in on YouTube's website; YouTube may apply its own embedded-browser sign-in restrictions.

## Short roadmap

| Stage | Focus | Status |
| --- | --- | --- |
| Current Android release (1.0.6) | YouTube Home, Shorts, and Search inside GeckoView; compact header and hideable JoyTube dock; public New Imported Joy's feed; optional Rust/EasyPrivacy filtering | Included in the Android source. The public feed is read-only and requires media uploaded and published to Supabase Storage. YouTube page behavior depends on YouTube, device, and network availability. |
| Next | Improve search and browser reliability across Android devices | Planned; depends on upstream YouTube website behavior. |
| Future | Evaluate a native iOS client | Planned; not included in this Android repository. |

## Privacy and account boundaries

JoyTube uses Google sign-in to establish a JoyTube account through Supabase. It does not request YouTube scopes or store Google passwords, YouTube subscriptions, YouTube watch history, likes, dislikes, or comments. Imported and downloaded media files are intended to remain on the device; cloud features concern account and library metadata when configured.

## Limitations

- YouTube's mobile website controls playback eligibility, ads, captions, and available quality.
- Google sign-in to JoyTube does not automatically sign in to YouTube inside GeckoView or link YouTube subscriptions.
- The New Imported Joy's tab reads public uploaded media; the app does not yet provide a complete public upload/publish workflow.
- Privacy filtering runs only in GeckoView and adds memory use while enabled; YouTube pages and media hosts are exempt to avoid disrupting playback.
- JoyTube's browser controls can request a quality tier YouTube exposes and boost compatible HTML media through Web Audio. YouTube still chooses the actual stream and may ignore a quality request; Web Audio may be unavailable for a stream. Neither control extracts media, unlocks paid quality, or changes ad requests.
- Android 8.0 (API 26) is the minimum supported version for the pinned GeckoView 145 dependency.
- A 30-minute update interval applies while the app is running; Android does not guarantee exact background timing.
- The Rust API is not deployed. Supabase features require the project's provider settings, migrations, and row-level security policies.
- This project does not promise zero lag, low battery use on every device, or full YouTube feature parity.

## Maintainer

Created and maintained by **[thecodencoffee](https://github.com/thecodencoffee)**. See the [JoyTube repository](https://github.com/thecodencoffee/JoyTube) for source code and [releases](https://github.com/thecodencoffee/JoyTube/releases) for Android builds.
