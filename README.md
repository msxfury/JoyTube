<div align="center">

# JoyTube for Android

**Browse YouTube from inside JoyTube and keep your own media library in one Android app.**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![GeckoView](https://img.shields.io/badge/Browser-GeckoView-FF7139?logo=firefoxbrowser&logoColor=white)](https://mozilla.github.io/geckoview/)
[![Version](https://img.shields.io/badge/Version-1.0.3-blue)](#release-information)

**Maintainer: [msxfury](https://github.com/msxfury)**

</div>

JoyTube is an Android media browsing and personal-library project. The app is built with Kotlin and Jetpack Compose. Its built-in browser uses GeckoView, and its YouTube embed uses Android WebView.

> JoyTube is an independent project, not an official YouTube app. YouTube pages and playback remain subject to YouTube's availability, restrictions, and ads. JoyTube does not provide YouTube Premium features or remove YouTube ads.

## Release information

| Item | Value |
| --- | --- |
| App version | **1.0.3** |
| Android version code | `4` |
| Minimum Android | Android 8.0 (API 26) |
| Target Android | API 36 |
| Maintainer | [msxfury](https://github.com/msxfury) |

## Features

| Feature | What it does | How it works | Status and limits |
| --- | --- | --- | --- |
| YouTube inside JoyTube | Opens YouTube's mobile website from the JoyTube browser screen. | GeckoView loads `https://m.youtube.com` inside JoyTube, keeping browsing in the app. | Browser access is implemented. YouTube controls its website, sign-in flows, playback availability, and page behavior. |
| GeckoView browser | Displays web pages entered or opened in the built-in browser; links requesting a new window stay in the current JoyTube browser page. | Uses a shared GeckoView runtime and an in-app browser session. JoyTube's Back, Forward, and Refresh controls use the current Gecko session history. | Implemented for web browsing. Individual sites may ignore the dark preference or behave differently from Firefox. |
| Optional privacy filtering | Filters matching tracker network requests in the GeckoView browser. | Brave's official Rust `adblock` engine (version 0.13.5) is compiled to WebAssembly and runs in a built-in GeckoView extension with the EasyPrivacy list. The Rust engine and request listener initialize only when the setting is enabled. | Off by default. Filtering applies to GeckoView only; YouTube-owned domains and requests from YouTube pages are exempt. It does not remove YouTube ads, hide page elements, or affect the separate YouTube player. |
| Filter-list refresh | Keeps the bundled EasyPrivacy rules current. | When filtering is enabled, downloads are validated and parsed into a new engine before swapping it in; failures retain the previous working list. | Checks every four days while enabled. Requires network access for updates; the bundled list supports first use and offline startup. |
| YouTube search and discovery | Searches for videos, Shorts, live streams, and music. | The app requests public YouTube/YouTube Music web search surfaces and parses returned results. No YouTube Data API key is used. | Experimental. These are undocumented interfaces; results can be incomplete, throttled, or stop working if YouTube changes them. |
| Embedded YouTube playback | Plays a compatible YouTube result inside JoyTube. | Android WebView loads YouTube's embedded player for the selected video ID. | Depends on YouTube embed permissions, network, device WebView, and regional or account restrictions. Ads and available quality remain controlled by YouTube. |
| Google sign-in | Signs the user into a JoyTube account. | Android Credential Manager obtains Google identity information; Supabase Auth establishes the JoyTube session. | Requires correct Google Cloud OAuth and Supabase provider configuration. It does not request YouTube API access or link subscriptions. |
| Google profile | Shows the account name, email, and profile photo in the JoyTube profile screen. | Uses the Google profile fields available in the signed-in Supabase session. | Available after sign-in when Google provides the fields; a JoyTube avatar fallback is used if no photo URL is available. |
| App update notice | Notifies signed-in users when a newer Android APK release is available. | Checks the latest stable GitHub release immediately after sign-in and every 30 minutes while the app process is running. It compares release and installed versions and requires an APK asset. | A dismissed release is remembered on-device. The Update button opens the APK link; Android handles installation. Checks may be delayed when Android stops or restricts the app. |
| Local media import | Adds files selected by the user to the JoyTube library. | Uses Android's file picker and keeps the selected file reference on that device. | Available for selected local files and supported file types. File access can depend on the original Android permission grant. |
| Local media playback | Plays supported imported files. | Uses Android Media3 and device-provided decoders. | Works only for formats/codecs supported by the device. This does not make YouTube streams downloadable. |
| Account and library metadata | Stores JoyTube profile/settings and library metadata in Supabase when configured. | The Android app calls Supabase Auth and database APIs directly; database row-level security is expected to enforce user ownership. | Requires the database migrations, provider configuration, network access, and correct RLS policies. Media files are not uploaded as part of this metadata sync. |
| Theme and app information | Offers JoyTube theme settings and in-app About and Terms/Privacy screens. | Native Compose screens display app settings and descriptions of current behavior. | Screens are included; cloud persistence depends on Supabase configuration. |
| Rust API service | Provides an optional Rust/Axum Supabase proxy scaffold. | Source exists separately from the Android app. | Not deployed and not connected to the Android client. |

### Android release APKs

Release builds produce separate APKs for ARM64, ARMv7, and x86_64 devices. Install the APK matching the device architecture; there is no universal APK. GeckoView includes a large native browser engine, while the privacy-filter assets add about 1.16 MB compressed to each APK. The Rust filter itself is WebAssembly, so it does not add separate Rust native libraries for each CPU architecture.

### What the YouTube browser does not do

The GeckoView browser displays YouTube's website. It does not turn JoyTube into an official YouTube client, grant access to subscriptions or watch history, guarantee every video or live stream will play, bypass paid features, or block YouTube ads.

## Short roadmap

| Stage | Focus | Status |
| --- | --- | --- |
| Current Android release (1.0.3) | GeckoView browser, optional Rust/EasyPrivacy filtering, YouTube mobile website access, Google profile display, and release notices | Included in the Android source. Google sign-in and YouTube playback still depend on the configured providers, device, and YouTube availability. |
| Next | Improve search and browser reliability across Android devices | Planned; depends on upstream YouTube website behavior. |
| Future | Evaluate a native iOS client | Planned; not included in this Android repository. |

## Privacy and account boundaries

JoyTube uses Google sign-in to establish a JoyTube account through Supabase. It does not request YouTube scopes or store Google passwords, YouTube subscriptions, YouTube watch history, likes, dislikes, or comments. Imported and downloaded media files are intended to remain on the device; cloud features concern account and library metadata when configured.

## Limitations

- YouTube search discovery uses undocumented web interfaces and may be incomplete or unavailable.
- YouTube's embedded player controls playback eligibility, ads, captions, and available quality.
- Privacy filtering runs only in GeckoView and adds memory use while enabled; it does not filter YouTube traffic.
- Android 8.0 (API 26) is the minimum supported version for the pinned GeckoView 145 dependency.
- A 30-minute update interval applies while the app is running; Android does not guarantee exact background timing.
- The Rust API is not deployed. Supabase features require the project's provider settings, migrations, and row-level security policies.
- This project does not promise zero lag, low battery use on every device, or full YouTube feature parity.

## Maintainer

Created and maintained by **[msxfury](https://github.com/msxfury)**.
