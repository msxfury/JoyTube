# JoyTube for Android

**JoyTube** is an Android media discovery and personal-library project by **MSX_Fury**. The Android client is written in Kotlin with Jetpack Compose. A small Rust service and a Supabase schema are included for backend development.

> **Project status:** This repository is an actively developed prototype, not a production-ready YouTube replacement. Some discovery paths use undocumented YouTube web interfaces and may stop working without notice. Playback uses YouTube's embedded player and remains subject to YouTube's availability, ads, and access rules. A YouTube Data API key is not included.

## Contents

- [Project at a glance](#project-at-a-glance)
- [Feature status](#feature-status)
- [Architecture](#architecture)
- [Search and provider behavior](#search-and-provider-behavior)
- [Playback behavior](#playback-behavior)
- [Supabase schema and ownership model](#supabase-schema-and-ownership-model)
- [Build the Android app](#build-the-android-app)
- [Configure Supabase and Google sign-in](#configure-supabase-and-google-sign-in)
- [Build and sign a release APK](#build-and-sign-a-release-apk)
- [Run the Rust API](#run-the-rust-api)
- [Data and privacy](#data-and-privacy)
- [Known limitations](#known-limitations)
- [Test scope](#test-scope)
- [Repository layout](#repository-layout)
- [Development conventions](#development-conventions)
- [Appendix A: Android component map](#appendix-a-android-component-map)
- [Appendix B: Search and provider engineering notes](#appendix-b-search-and-provider-engineering-notes)
- [Appendix C: Network behavior reference](#appendix-c-network-behavior-reference)
- [Appendix D: Supabase schema field reference](#appendix-d-supabase-schema-field-reference)
- [Appendix E: Authentication and session lifecycle](#appendix-e-authentication-and-session-lifecycle)
- [Appendix F: Rust service request reference](#appendix-f-rust-service-request-reference)
- [Appendix G: Build and release runbooks](#appendix-g-build-and-release-runbooks)
- [Appendix H: Test strategy and coverage boundaries](#appendix-h-test-strategy-and-coverage-boundaries)
- [Appendix I: Troubleshooting guide](#appendix-i-troubleshooting-guide)
- [Appendix J: Security and privacy review checklist](#appendix-j-security-and-privacy-review-checklist)
- [Appendix K: Product semantics and terminology](#appendix-k-product-semantics-and-terminology)
- [Appendix L: Change-impact guide](#appendix-l-change-impact-guide)
- [Appendix M: Suggested issue templates](#appendix-m-suggested-issue-templates)
- [Appendix N: Release handoff template](#appendix-n-release-handoff-template)
- [Appendix O: Glossary](#appendix-o-glossary)
- [Appendix P: Maintainer completion checklist](#appendix-p-maintainer-completion-checklist)
- [Contributing](#contributing)

## Project at a glance

| Property | Current repository value |
| --- | --- |
| Product | JoyTube for Android |
| Maintainer | MSX_Fury |
| Android application ID | `com.aistudio.joytube.xkvpzr` |
| Current Android version | `1.0.1` (`versionCode` `2`) |
| Android minimum SDK | API 24 |
| Android target SDK | API 36 |
| Android implementation | Kotlin, Jetpack Compose, Material 3 |
| Android networking | OkHttp and Kotlin coroutines |
| Auth and cloud data | Supabase Auth and PostgREST, called directly by Android |
| Database | PostgreSQL migration with row-level security policies |
| Optional backend | Rust 2024 edition, Axum, Tokio, Reqwest |
| YouTube Data API key | Not configured or required by current client code |

Values are defined in `app/build.gradle.kts`, `gradle/libs.versions.toml`, and the Rust manifests. Increment the Android `versionCode` for every distributable update.

## Feature status

This table describes source-level behavior, not a production guarantee. A screen, setting, or model existing in the repository does not by itself mean its complete end-to-end service is ready.

| Feature | Current implementation | Status and practical boundary |
| --- | --- | --- |
| Android interface | Native Compose screens, navigation, shared components, Material 3 styles | **UI implemented.** Some screens use illustrative or local UI state and do not imply a complete service. |
| Glass / normal theme | Client theme state and repository calls for `glass` or `normal` | **Partial.** Cloud persistence requires sign-in, migration, policies, and network access. |
| Video search | Provider sends requests to YouTube's undocumented `WEB` search surface and parses selected renderer fields | **Prototype.** Upstream behavior can change, throttle, or stop. |
| Shorts search | Reel surface parser distinguishes verified signals from heuristic candidates | **Prototype.** Depends on undocumented fields; not every short-duration result is a confirmed Short. |
| Live search | Parser derives active, upcoming, and completed states from response metadata | **Prototype.** Fields may be absent or stale; not a guaranteed live directory. |
| Music search | Provider uses the undocumented YouTube Music `WEB_REMIX` surface | **Prototype.** Not a supported catalog API or a guarantee of track availability. |
| Combined search | Coordinator can fan out providers, merge/rank results, track pagination/cache state, and discard stale generations | **Implemented in client code.** Cannot guarantee complete results or stable upstream availability. |
| Network resilience | OkHttp connection pool, timeouts, response size limits, transient retry policy, coroutine cancellation | **Implemented in client code.** These controls do not stabilize an undocumented upstream. |
| Embedded playback | Android WebView loads YouTube's embedded player for supported video IDs | **Implemented for supported embeds.** Ads, restrictions, availability, controls, and quality are governed by YouTube. |
| Playback queue / mini-player state | Process-local coordinator for selected item, queue operations, and presentation state | **Partial.** This is not a standalone streaming engine. |
| Captions | Caption preference can request English captions in the embed URL | **Limited.** Depends on available caption tracks; no download, translation, or transcription pipeline. |
| Quality selection | Quality preference/menu exists in the UI | **Not an exact resolution control.** The embedded player determines actual playback quality. |
| Google sign-in | Credential Manager's explicit Sign in with Google option obtains a Google ID token; app exchanges it with Supabase and encrypts its Supabase session using Android Keystore | **Code implemented; configuration and real-device validation required.** OAuth client IDs, package, and signing fingerprints must match. |
| Release update notice | Checks the public `msxfury/JoyTube` latest-release endpoint at sign-in and every six hours while signed in | **APK-asset based.** Shows “New Update Available!” only when a newer stable release includes an `.apk`; Update opens the GitHub APK download URL. It does not silently install updates. |
| JoyTube profile | Supabase Auth identity and profile upsert operations | **Partial.** Depends on provider setup, deployed schema, and RLS. |
| Preferences | Theme and JSON setting reads/writes through the Supabase repository | **Partial.** A displayed setting may not have a complete runtime effect. |
| Collections / favourites | Repository operations for collections, visibility, membership, and favourite flags | **Partial.** Validate each UI path against the deployed project. |
| Imported media | Android document picker retains access to a selected device URI; item metadata syncs to JoyTube | **Implemented for selected local files.** The URI and access grant remain device-local; they are not synced to another device. YouTube origins are rejected. |
| Local playback | Media3 ExoPlayer handles selected local content URIs; YouTube results use the embedded player | **Implemented for formats supported by the installed Media3 decoders.** Availability depends on the selected file and retained Android URI grant. |
| Download controls | Copies a selected local document into app-managed Movies storage and verifies the output exists and is non-empty | **Implemented for selected local files.** No arbitrary URL or YouTube stream downloads; pause/resume and background transfer are not supported. |
| Device-local download boundary | Downloaded bytes are stored in app-managed device storage, with local path and exact byte count in app state | **Device-local.** Download list state currently resets when the process restarts; no cloud media upload occurs. |
| Ad blocking | Browser/settings controls expose selected preferences | **Not system-wide.** Cannot block ads in every app or remove ads from YouTube's embedded player. |
| Rust API | Axum proxy with table allowlist and caller-token forwarding | **Standalone scaffold.** Not deployed and not called by Android. |
| Supabase data security | Migration includes RLS policies on the defined public tables | **Schema included.** Must be applied and independently checked in the target project. |

Do not infer that a download button downloads media, a quality menu forces resolution, a captions switch guarantees a track, or a browser preference is a device-wide ad blocker. JoyTube does not promise full YouTube feature parity.

## Architecture

```text
Android app (Kotlin / Compose)
  ├── UI and playback coordination
  ├── discovery adapters ──> YouTube web search surfaces (unofficial)
  ├── embedded player ────> YouTube player
  └── Google identity ────> Supabase Auth / PostgREST

Rust Axum API (optional, currently not called by Android)
  └── forwards caller-authenticated requests ──> Supabase PostgREST

Supabase
  ├── Auth users
  └── PostgreSQL tables protected by row-level security
```

The Android app currently calls Supabase directly for its auth and data operations. `backend-rust/` is a separate service scaffold and is not deployed or wired into the Android client.

### Android request and state flow

1. Compose UI events update `JoyTubeViewModel` state and navigation.
2. Search queries are normalized and debounced before provider execution.
3. A generation identifier prevents stale search results from replacing a newer query's state.
4. Provider outputs are normalized and ranked before being exposed to the UI.
5. Selecting a compatible YouTube result loads the embedded player; the client does not extract media streams.
6. Authenticated library operations send the Supabase user token to PostgREST. Database RLS is the ownership boundary.
7. Playback coordination and much UI state are process-local. They are not an offline database or durable download queue.

### Main code components

| File / package | Responsibility |
| --- | --- |
| `MainActivity.kt` | Activity entry point and Compose setup. |
| `viewmodel/JoyTubeViewModel.kt` | Screen state, navigation, search, playback, and user actions. |
| `backend/search/UnifiedSearchCoordinator.kt` | Provider coordination, ranking, cache, rate limiting, and stale-response handling. |
| `backend/providers/YouTubeProviderAdapters.kt` | Video, Shorts, live, and music result parsing. |
| `backend/network/KeylessNetworkEngine.kt` | HTTP calls, limits, timeout/retry handling, and parsing bounds. |
| `backend/playback/GlobalPlaybackCoordinator.kt` | JoyTube playback selection, queue, and presentation state. |
| `ui/components/YouTubeEmbedPlayer.kt` | WebView wrapper around YouTube's embedded player. |
| `backend/auth/SupabaseGoogleAuthManager.kt` | Google token acquisition, Supabase exchange, and local session storage. |
| `backend/data/SupabaseJoyTubeRepository.kt` | Direct Supabase REST operations. |
| `supabase/migrations/*.sql` | Tables, functions, grants, indexes, triggers, and RLS policies. |
| `backend-rust/src/main.rs` | Optional standalone Axum/PostgREST proxy. |

## Search and provider behavior

The provider implementations issue keyless HTTP requests with YouTube web-client contexts. “Keyless” means no YouTube Data API key is used; it does **not** mean that the surface is official, stable, unrestricted, or guaranteed.

| Provider | Client context | Intended result | Parser signals used in current code |
| --- | --- | --- | --- |
| `VideoProvider` | YouTube `WEB` | Standard videos | `videoRenderer` and related title, channel, duration, thumbnail, and optional count fields. |
| `ShortsProvider` | YouTube `WEB` | Reel/Short results and candidates | Explicit reel/Shorts renderer endpoints; heuristic candidates remain distinct. |
| `LiveProvider` | YouTube `WEB` | Active, upcoming, completed broadcasts | Live badges/overlays, upcoming-event data, and streamed/published text. |
| `MusicProvider` | YouTube Music `WEB_REMIX` | Tracks, albums, artists, playlists, music videos | Music renderer structure plus browse and video identifiers. |

In `All` mode, providers can run concurrently and partial provider failure can leave other results usable. A single-provider mode is designed to invoke only the selected adapter. Ranking considers query relevance, mode intent, freshness, and popularity; missing popularity differs from an explicit zero. Pagination and request controls exist in code but cannot make the upstream response format a stable contract.

Endpoint paths, client versions, renderer shapes, protobuf filters, and headers are implementation details for an undocumented surface. Upstream changes may yield incomplete results or stop a feature without an app-side code change.

## Playback behavior

`YouTubeEmbedPlayer` validates an expected 11-character video ID and loads `https://www.youtube.com/embed/{id}` in Android `WebView`. It does not extract direct video or audio URLs. Playback requires network access and can be affected by embed settings, restrictions, removed/private videos, region or age restrictions, WebView availability, and YouTube-side changes.

The embed owns playback controls and quality behavior. JoyTube's quality preference is not proof that a chosen resolution is delivered. When captions are enabled, the URL requests English captions; a video may lack that track. The app does not create, translate, download, or persist captions. No guarantee is made about ad-free playback, Premium-only features, background playback, every livestream, or parity with the official YouTube application.

## Supabase schema and ownership model

The migration at `supabase/migrations/20261106000001_joytube_schema_and_rls.sql` defines:

| Object | Purpose | Policy intent |
| --- | --- | --- |
| Supabase Auth `auth.users` | Supabase UUID and email identity | Managed by Supabase Auth; `profiles` references the UUID. |
| `public.profiles` | Username, display name, avatar URL, timestamps | Owner-scoped read/write. |
| `public.user_preferences` | JSON settings and `glass`/`normal` theme | Owner-scoped read/write. |
| `public.imported_assets` | Item title/kind, favourite flag, aggregate counts, timestamps | Owner writes; public read path can expose metadata referenced by public collections. |
| `public.joytube_collections` | Name, description, visibility, favourite flag | Owner writes; public collection metadata can be read publicly. |
| `public.joytube_collection_items` | Collection membership and ordering | Owner writes after checking collection/item ownership; public memberships can be read. |
| `public.recent_imported_content` | Owner's recent item references | Owner-scoped read/write. |
| `record_imported_asset_view(uuid)` | Increment an owned item's view count | Caller-privilege function with owner check. |
| `record_imported_asset_download(uuid)` | Increment an owned item's download count | Caller-privilege function with owner check. |

RLS is enabled for all six `public` tables. Update policies use both `USING` and `WITH CHECK`; collection membership policies verify referenced rows. These policies apply only after the migration is applied and API roles have access. Verify the deployed project instead of assuming its live state matches this file.

The schema stores metadata, not media bytes or local content URIs. Selected document URI pointers are stored in app-private local preferences and joined to cloud item metadata after sign-in; they are meaningful only on the device that granted access. Local files are played with Media3 when supported, and copies made from selected local documents remain in app-managed device storage. URL-based media ingestion, transcoding, background transfer, pause/resume, and cross-device local-file sync are not implemented. YouTube audiovisual sources remain excluded from import/download.

## Build the Android app

### Real local-media verification

The device instrumentation suite exercises the production `LocalMediaFileOperations` code through a test `content://` provider. It generates a 64 MiB fixture at runtime and checks the copied file's exact length and SHA-256, copies a real two-second H.264/AAC MP4 and waits for Media3 to reach `STATE_READY` with a positive duration, and verifies that empty, forged-signature, and unsupported MIME inputs fail rather than being reported as completed downloads. The large binary fixture is generated in the emulator cache so it does not add 64 MiB to the repository.

Run both the JVM and connected-device suites with:

```bash
./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest
```

The connected suite requires an Android emulator or device with enough free storage for temporary 64 MiB source and output files. Passing it verifies local file copying and local MP4 decoder preparation on that test environment. It does not verify picker behavior across every document provider, device codec support for every media format, Google OAuth, live Supabase RLS, or YouTube embed availability.

### Requirements

- Android Studio with Android SDK platform 36 and a compatible Android Gradle Plugin installation.
- JDK supported by the Gradle wrapper (the project has been built with its supplied JDK in the development environment).
- Android device or emulator for installation and device tests.
- Network access for resolving dependencies on an initial Gradle build.

From the repository root:

```bash
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

To run local unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

Instrumented tests require a running emulator or connected Android device:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Install a debug APK to a connected device with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If Gradle cannot locate the SDK, set `ANDROID_HOME`/`ANDROID_SDK_ROOT` or create a machine-local `local.properties` with `sdk.dir=...`. Do not commit `local.properties`.

## Configure Supabase and Google sign-in

The schema migration is in `supabase/migrations/`. Apply it to the intended Supabase project before using cloud profile and library features.

For Google identity sign-in:

1. Enable Google as a Supabase Auth provider and configure it with the Google **Web application** OAuth client ID and client secret.
2. Configure the Google consent screen and OAuth clients in the same Google Cloud project.
3. Create an **Android** OAuth client using application ID `com.aistudio.joytube.xkvpzr` and the SHA-1 certificate fingerprint for the APK's signing key.
4. Keep the Web client ID in the Android `GOOGLE_WEB_CLIENT_ID` configuration. The Android client is matched by package name and certificate; it does not replace the Web client ID used to request the Google ID token.

In the Supabase Google provider configuration, include the Web client ID first, followed by the Android client ID, separated by a comma. Keep the Google client secret only in the Supabase provider configuration. The Android app uses `GetSignInWithGoogleOption` for its explicit sign-in button; the selected account's Google ID token is exchanged with Supabase Auth.

The current Supabase URL, publishable key, and Google Web client ID are set in `app/build.gradle.kts` as public client configuration. Never put a Supabase secret/service-role key or Google OAuth client secret in the Android app or this repository. Move environment-specific public values into a controlled build configuration before creating separate staging and production builds.

### Configure the Android OAuth client

The Android OAuth registration must use the exact application ID and SHA-1 fingerprint for the signing certificate on the phone:

```text
Application ID: com.aistudio.joytube.xkvpzr
```

Debug and release builds use different signing certificates, so register the relevant fingerprint for each distribution path. For a release keystore:

```bash
keytool -list -v -keystore "$KEYSTORE_PATH" -alias upload
```

The Android OAuth client is separate from `GOOGLE_WEB_CLIENT_ID`. The app passes the Web client ID to Credential Manager when requesting the Google ID token, then exchanges that token with Supabase Auth. A successful compile does not verify OAuth setup.

## Build and sign a release APK

The release build uses the ignored local `keystore.properties` file when present, or the environment variables below. Keep the keystore and local properties backed up securely and never commit them:

```bash
export KEYSTORE_PATH="$PWD/my-upload-key-v2.jks"
export STORE_PASSWORD="<keystore-password>"
export KEY_PASSWORD="<key-password>"
./gradlew clean :app:assembleRelease
```

The APK output is `app/build/outputs/apk/release/app-release.apk`. Future APKs intended to update an installation signed with this new key must use this same key. The replacement key cannot update older installs signed with the previous key; those installs must be uninstalled first, which removes their local app data. This app is distributed as direct APKs, so Play Console upload-key reset does not apply.

The release build currently has minification disabled. After a successful build, make a versioned copy if needed:

```bash
cp app/build/outputs/apk/release/app-release.apk \
  app/build/outputs/apk/release/JoyTube-Android-v1.0.1.apk
```

Update `versionName` when the human-readable release label changes and increment `versionCode` for each distributable update.

To get the SHA-1 fingerprint used for the Android OAuth client:

```bash
keytool -list -v -keystore "$KEYSTORE_PATH" -alias upload
```

## Run the Rust API

The Rust service is a separate Axum application. It forwards the caller's Supabase bearer token to PostgREST and does not use a service-role key.

```bash
cd backend-rust
export SUPABASE_URL="https://your-project.supabase.co"
export SUPABASE_PUBLISHABLE_KEY="your-publishable-key"
export BIND_ADDRESS="127.0.0.1:8080"
cargo run --release
```

The default bind address is `0.0.0.0:8080`. `GET /health` is the health endpoint; database routes use `/v1/{table}` and require `Authorization: Bearer <Supabase access token>`. Do not expose the service on the public internet without HTTPS, operational controls, and an appropriate CORS policy. Android does not currently call this service.

The Rust binary reads `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, and optional `BIND_ADDRESS` from its process environment; copying `.env.example` to `.env` does not automatically load those values. Provide them through a shell, container, or process manager. The service has a table allowlist and forwards the caller's authorization header so database RLS remains responsible for row access. Treat the current scaffold as not production-hardened.

## Data and privacy

The Supabase migration is designed to store JoyTube account metadata and user-created library metadata, including profiles, preferences, collections, favourites, imported-item metadata, and view/download counters. Row-level security policies restrict writes and private records by owner.

Downloaded media bytes are intended to remain on the user's device. The schema does not define storage for YouTube credentials, YouTube subscriptions, watch history, likes/dislikes, comments, or temporary playback state. Google identity sign-in does not grant YouTube account access.

This describes the intended schema and source behavior; verify the deployed Supabase project and the current app flows before using real account data.

### Data boundary summary

| Category | Intended location / handling |
| --- | --- |
| JoyTube user ID and email | Supabase Auth identity. |
| Username, display name, profile URL | `public.profiles`. |
| Theme and settings | `public.user_preferences`. |
| Collections and membership | Supabase metadata tables, subject to RLS. |
| Imported-item metadata and aggregate counts | Supabase metadata, not file backups. |
| Downloaded media bytes | Intended to remain on device; real media download code is not currently present. |
| Google ID token | Exchanged with Supabase Auth; not intended to be stored as JoyTube account data. |
| YouTube credentials, subscriptions, watch history, likes/dislikes, comments, temporary playback state | Not in the JoyTube schema or intended persistence model. |

Google identity sign-in does not grant YouTube account access. The auth manager does not request YouTube scopes. A publishable Supabase key and OAuth client ID are client configuration values, but must still be paired with correct RLS and provider settings.

## Known limitations

- YouTube discovery uses undocumented web client interfaces, not a supported YouTube API. Those interfaces can change, throttle requests, or stop responding.
- Embedded playback follows YouTube's playback behavior, restrictions, and ad experience. JoyTube cannot promise complete YouTube feature parity or ad-free playback.
- The Rust service is source code only: it has not been deployed and is not yet integrated with Android.
- Do not assume YouTube media downloading, arbitrary quality selection, captions, or every import flow is production-ready. The repository does not establish a supported way to download arbitrary YouTube media.
- The app has not been validated across all Android devices, network conditions, and account configurations. No guarantee of zero lag, overheating, or battery impact is possible.

## Repository layout

```text
app/                         Android application source, resources, and tests
backend-rust/                Rust/Axum API service source
supabase/migrations/         PostgreSQL schema and row-level security policies
gradle/                      Gradle wrapper and version catalog
ARCHITECTURE_AND_BACKEND_DOCS.md
                             Architecture notes and backend design details
```

## Test scope

Run JVM tests with:

```bash
./gradlew :app:testDebugUnitTest
```

The JVM suite includes provider parsing against sanitized fixtures, search coordination and ranking, playback coordinator state, auth/data separation, local file validation rules, and import/download boundaries. These code-level checks do not prove that current YouTube endpoints work, OAuth is configured, a deployed Supabase project has the expected schema, or a media file is downloaded.

Instrumented tests require an emulator or connected device:

```bash
./gradlew :app:connectedDebugAndroidTest
```

The current instrumented suite checks application package context and selected shared-header controls. It is not a broad end-to-end suite. Successful release assembly proves compilation and packaging in that build environment; it does not certify live network behavior, device coverage, battery use, performance, or store readiness.

## Development conventions

- Keep JoyTube account metadata separate from transient provider and playback data.
- Use user-scoped Supabase JWTs for private records. RLS, not client filtering, is the authorization boundary.
- Never add Supabase secret/service-role keys or OAuth client secrets to a mobile client.
- Treat provider response fields as optional; counts, dates, durations, and images may be absent.
- Keep network responses bounded and coroutine work cancellable. Avoid unbounded retry loops.
- Do not represent metadata-only imports or UI progress as completed media transfer.
- Avoid logging tokens, private account data, signing materials, or downloaded media.
- Keep build outputs, SDK paths, local environment files, debug credentials, and upload keystores out of Git.

## Engineering reference appendices

The appendices below expand on the source-level implementation for maintainers who want to trace a feature from its UI entry point through model, service, and persistence code. File names are paths beneath `app/src/main/java/com/example/` unless otherwise stated.

### Appendix A: Android component map

#### A.1 Application and activity layer

1. `MainActivity.kt` is the Android activity entry point.
2. The activity creates the Compose content hierarchy.
3. The activity uses the JoyTube theme definitions in the UI theme package.
4. The screen tree is written with Jetpack Compose rather than XML layouts.
5. The app namespace is `com.example` in the Android Gradle configuration.
6. The installable package identifier is `com.aistudio.joytube.xkvpzr`.
7. Keep the namespace and application ID distinction clear when adding resources or OAuth registrations.
8. Resource names live under `app/src/main/res/`.
9. Drawable launcher assets are under `res/drawable/`.
10. Density-specific launcher images are under `res/mipmap-*`.
11. Theme and color resources also include XML definitions under `res/values/`.
12. Font resources are stored under `res/font/`.
13. Backup and data extraction rules are under `res/xml/`.
14. Android permissions and application declarations are in `app/src/main/AndroidManifest.xml`.
15. Review the manifest whenever adding a new platform capability.
16. Do not assume a permission exists merely because a screen contains a related control.
17. Do not add storage permissions if the platform picker can grant temporary scoped access.
18. Avoid putting credentials or private application configuration in manifest metadata.

#### A.2 State and event layer

1. `viewmodel/JoyTubeViewModel.kt` owns the main `JoyTubeUiState` flow.
2. UI events are exposed through methods on the ViewModel.
3. Search input is stored in UI state before provider work begins.
4. Search text updates cancel the pending debounce job.
5. A blank normalized query clears the active result response.
6. Nonblank text waits for the configured debounce interval.
7. Explicit search submission bypasses the typing debounce.
8. Filter changes may initiate a new provider search for the active query.
9. Search generation IDs are used to discard stale results.
10. This guards presentation state against out-of-order provider completions.
11. It does not cancel every possible upstream socket operation by itself.
12. Provider adapters and coroutines still need to respect cancellation.
13. UI simulation modes are state controls, not backend availability checks.
14. The ViewModel can display empty, loading, offline, or error representations.
15. A simulated offline state does not disable the device's network interface.
16. A visible loading state does not prove a network request is active.
17. Error feedback is surfaced through the UI state.
18. Do not place raw auth tokens in UI state.
19. Do not log complete UI state objects if they may include private profile data.
20. Keep durable identity data in the auth session layer, not in transient screen variables.

#### A.3 Navigation and screen composition

1. Primary navigation is represented by JoyTube route and tab model types.
2. Secondary screens may be pushed onto a process-local back stack.
3. Overlay state is separate from the route stack.
4. A player route can select an owned library item or a current search result.
5. Collection screens display JoyTube-owned item relationships.
6. Import screens collect metadata in a draft before confirmation.
7. Account and settings screens present sign-in and preference state.
8. Browser screens use an Android WebView component.
9. Shared branding and button components are in `ui/components/JoyTubeComponents.kt`.
10. Artwork presets and artwork helpers are in `ui/components/AtmosphericArtwork.kt`.
11. Icon types and vector drawing helpers are in `ui/icons/JoyTubeIcons.kt`.
12. Player surfaces are composed in `ui/screens/PlayerAndCategoryScreens.kt`.
13. Import and library surfaces are in `ui/screens/ImportAndLibraryScreens.kt`.
14. Browser and settings screens are in `ui/screens/BrowserAndSettingsScreens.kt`.
15. Shared overlays and sheets are in `ui/screens/OverlaysAndSheets.kt`.
16. Primary navigation surfaces are in `ui/screens/PrimaryScreens.kt`.
17. Avoid implementing a second copy of shared controls inside an individual screen.
18. Keep accessible labels and stable test tags when changing frequently-tested controls.
19. Compose previews are helpful for appearance but do not validate persistence.
20. Screenshot references are design inputs, not runtime UI contracts.

#### A.4 Domain models

1. JoyTube entities are defined under `model/JoyTubeModels.kt`.
2. UI-facing media types are distinct from provider-specific parser nodes.
3. Collection visibility is represented by the app's visibility model.
4. Provider result types can include nullable metadata.
5. Nullable view counts must remain nullable when the source omits them.
6. Do not convert missing upstream values into fabricated zeros.
7. A provider result ID may be an upstream video or browse identifier.
8. JoyTube database row IDs are UUIDs generated by PostgreSQL.
9. Do not confuse a YouTube video ID with a Supabase imported-asset UUID.
10. A search result can be playable in the embedded player without being a JoyTube import.
11. A JoyTube import record does not guarantee that the corresponding media bytes are available.
12. `MediaType.Short` identifies a JoyTube category, not a verified YouTube reel on its own.
13. Collection membership is stored separately from the collection and asset records.
14. Ownership fields must be derived from the signed-in session where appropriate.
15. Do not trust a display name or username as an authorization key.
16. The Supabase Auth UUID is the ownership identifier.
17. Usernames are presentation metadata and may be changed subject to database constraints.
18. Public visibility applies to collection metadata and related allowed reads in the migration.
19. Public visibility does not make an arbitrary device file available to other users.
20. Preserve model boundaries when adding new upstream or cloud fields.

#### A.5 Theme and design system

1. JoyTube theme colors are defined in `ui/theme/Color.kt`.
2. Compose theme setup is defined in `ui/theme/Theme.kt`.
3. Typography styles are defined in `ui/theme/Type.kt`.
4. Glass and normal theme state can be represented by the user preference record.
5. The current theme label should not be confused with Android system dark mode.
6. A saved theme may fail to sync if Auth or PostgREST is unavailable.
7. Local UI feedback can update before the cloud write finishes.
8. Cloud write failures should be presented as failures rather than silently implied success.
9. Avoid making screen layouts depend on one device's exact pixel dimensions.
10. Use Compose density-independent units for layout values.
11. Keep player, browser, and system surfaces legible in both theme settings.
12. Test dialogs and bottom sheets at small phone widths.
13. Ensure text can wrap in translated or longer content.
14. Avoid low-contrast text over artwork.
15. Decorative artwork should not be treated as user-provided media.
16. Keep image loading and image placeholders independent from authorization.
17. Avoid blocking the main thread with image decoding or network I/O.
18. Reuse shared radii, colors, and type scales where possible.
19. UI polish must not imply that an underlying action is fully implemented.
20. Label unavailable actions honestly in any production-facing update.

### Appendix B: Search and provider engineering notes

#### B.1 Query lifecycle

1. Raw text is held by `JoyTubeViewModel` as the user types.
2. `QueryNormalizationPipeline` transforms input before provider execution.
3. A blank normalized string does not dispatch a provider request.
4. Typing updates replace a pending delayed job.
5. The debounce reduces request frequency during rapid typing.
6. Submitting a query can initiate work immediately.
7. The active search filter maps to a `UnifiedSearchMode`.
8. Category and web routes may have specialized UI behavior outside provider search.
9. The coordinator assigns a generation number to each logical search.
10. Results from a prior generation should not replace newer results.
11. Search state contains loading and result information for rendering.
12. Provider reports indicate outcomes for individual adapters.
13. Partial provider failure is represented separately from total search failure.
14. A successful result from one adapter does not imply all adapters succeeded.
15. Empty results can represent no matches, a parser mismatch, a network issue, or throttling.
16. UI should not describe every empty result as definitively “no videos found.”
17. Provider error categories help distinguish likely transient conditions.
18. Search ranking is deterministic only relative to the current input and metadata.
19. Ranking is not a statement about the objective quality of a video.
20. Do not persist search results as a watch history.

#### B.2 Provider fan-out

1. `VideoProvider` handles standard video-oriented responses.
2. `ShortsProvider` handles explicit reel/Short signals and candidate results.
3. `LiveProvider` extracts broadcast lifecycle metadata.
4. `MusicProvider` parses music-oriented entities.
5. `All` mode may run the provider adapters concurrently.
6. A selected specific mode should call only its relevant adapter.
7. Concurrency shortens aggregate wait time only when upstream requests finish normally.
8. Concurrent calls can increase pressure on rate-limited upstream endpoints.
9. A per-provider timeout prevents a slow adapter from indefinitely holding the coordinator.
10. A supervisor scope allows other adapters to complete when one fails.
11. Provider results are normalized into common search result representations.
12. Deduplication should preserve the richest useful result where identifiers match.
13. Result identity must not be inferred only from titles.
14. Music album browse IDs are not playable video IDs.
15. Artist browse IDs are not playable video IDs.
16. Playlist browse IDs are not guaranteed to map to a direct media stream.
17. An album result should be opened as a container rather than played as a track.
18. A track result may include a YouTube video ID for embedded playback.
19. A live result may represent an upcoming event rather than a live stream now.
20. A completed broadcast is a video-on-demand result, not an active stream.

#### B.3 Result parsing and integrity

1. Provider payloads are parsed as JSON-like renderer trees.
2. Renderer paths can change without notice.
3. Parser traversal should tolerate missing keys and absent arrays.
4. A malformed response should not crash the whole application process.
5. The parser should preserve optionality in domain models.
6. A missing duration is not equivalent to zero seconds.
7. A missing view count is not equivalent to zero views.
8. A missing thumbnail should allow a local placeholder.
9. A missing channel name should not fabricate an account name.
10. Relative publication strings are display text, not authoritative timestamps.
11. Upcoming event timestamps are only as accurate as the upstream response.
12. Active-live classification should use structured live signals, not title keywords.
13. “Live” in a title is not sufficient proof of an active broadcast.
14. A short duration alone is not sufficient proof that a standard video is a Short.
15. Explicit reel navigation signals are stronger than duration heuristics.
16. A heuristic result should remain visibly or programmatically distinguishable.
17. Parser fixtures should be sanitized and contain no account-specific data.
18. Fixture tests validate expected parser behavior for known shapes only.
19. Fixture tests do not validate current upstream behavior.
20. When upstream structure changes, capture only safe and permitted diagnostic examples.

#### B.4 Ranking inputs

1. Query relevance evaluates overlap between the normalized query and result metadata.
2. Mode intent rewards results associated with the selected category.
3. Freshness scoring uses different assumptions for live, music, and video content.
4. Active live results receive stronger time relevance than completed broadcasts.
5. Upcoming events can be ranked as time-sensitive without being marked active.
6. Music catalog content should not necessarily be penalized like a recent video upload.
7. Popularity may be absent from some provider entities.
8. The implementation distinguishes missing popularity from an explicit count of zero.
9. Missing data should not be silently fabricated to make ranking appear complete.
10. Composite ranking weights should remain normalized to the expected range.
11. Any ranking weight change should be accompanied by focused unit assertions.
12. Sorting ties should not produce crashes or invalid list states.
13. Ranking output is not an official YouTube ordering.
14. Search results may differ from YouTube's own application.
15. Ranking can change as upstream metadata changes.

### Appendix C: Network behavior reference

#### C.1 Shared client settings

1. The shared HTTP engine is `backend/network/KeylessNetworkEngine.kt`.
2. The current default connection timeout is 8 seconds.
3. The current default read/request timeout is 12 seconds.
4. The default response size ceiling is 4 MiB.
5. The default retry attempt budget is two retries after an initial attempt, subject to policy implementation.
6. OkHttp uses a connection pool with up to five idle connections.
7. The pool's configured keep-alive duration is five minutes.
8. Automatic OkHttp connection retry is disabled where retry is managed explicitly.
9. Retryable categories include selected transient network and server failures.
10. Authorization failures are not treated as transient retry candidates.
11. Invalid-parameter failures are not treated as transient retry candidates.
12. Unsupported operations are not treated as transient retry candidates.
13. Malformed responses are not automatically repaired by retrying indefinitely.
14. Coroutine cancellation is checked while network work is in progress.
15. Backoff uses an exponential delay and jitter policy.
16. A 429 response is represented as rate limiting.
17. A large response should be rejected instead of consumed without a bound.
18. A timeout is a failure outcome, not proof that the remote server is down.
19. Network diagnostic messages should be sanitized.
20. Do not include raw request authorization headers in diagnostics.

#### C.2 Request construction

1. Provider requests include a web-client context object.
2. The context includes a client name and version string.
3. The request builder can include a query.
4. The request builder can include a provider-specific `params` value.
5. The request builder can include continuation state.
6. The request builder can include a browse identifier.
7. Language and geographic hints are request parameters in the client context.
8. A request may include browser-like origin and referer headers.
9. The request may include YouTube client name/version headers.
10. These values are implementation details for an undocumented interface.
11. They should not be described as a public YouTube API contract.
12. Do not add Google OAuth credentials to a YouTube discovery request.
13. JoyTube identity authentication is separate from provider discovery.
14. Requests should avoid sending unnecessary personal data.
15. Search text is transmitted to the external search provider when live calls are enabled.

#### C.3 Failure interpretation

1. DNS failure indicates name resolution failed from the current environment.
2. Connection timeout indicates a connection was not established within the configured limit.
3. Read timeout indicates the response did not complete within the configured limit.
4. HTTP 429 indicates the remote endpoint rejected or limited the request rate.
5. HTTP 5xx responses may be transient but can persist.
6. HTTP 4xx responses may indicate unsupported request shape or access restrictions.
7. A successful HTTP status can still contain an unexpected response structure.
8. Unexpected JSON structure may result in empty or partial parsed results.
9. An empty parsed list does not distinguish every transport and parser condition.
10. Retry success does not imply future request reliability.
11. Repeated client retries can worsen upstream rate pressure.
12. Use bounded retries and backoff.
13. Do not spin on a failed continuation token.
14. Detect repeated continuation state before requesting the next page.
15. Stop pagination when no new identifiers are produced.
16. Expose a useful UI message when all providers fail.
17. Preserve partial successful providers when another provider fails.
18. Avoid displaying raw server stack traces to end users.
19. Store only sanitized observability information.
20. Avoid recording search text as personal history unless a separate feature and policy explicitly require it.

### Appendix D: Supabase schema field reference

This appendix summarizes the migration in this repository. It does not certify a remote project's live schema.

#### D.1 `public.profiles`

1. `id` is the primary key.
2. `id` references `auth.users(id)`.
3. Auth user deletion cascades to the profile row.
4. `username` is required.
5. `username` is unique.
6. The username length constraint is 3 through 32 characters.
7. The username pattern allows letters, digits, and underscores.
8. `display_name` is required.
9. Display name length is constrained to 1 through 80 characters.
10. `avatar_url` is optional text metadata.
11. `created_at` defaults to the current UTC time.
12. `updated_at` defaults to the current UTC time.
13. An update trigger refreshes `updated_at`.
14. Authenticated users can select their own profile row.
15. Authenticated users can insert a row only with their own Auth UUID.
16. Authenticated users can update only their own profile row.
17. Authenticated users can delete only their own profile row.
18. The email remains in Supabase Auth rather than duplicated in this public profile table.
19. Do not use the username as a stable ownership identifier.
20. Do not use editable profile metadata as an authorization claim.

#### D.2 `public.user_preferences`

1. `user_id` is the primary key.
2. `user_id` references `auth.users(id)`.
3. Auth user deletion cascades to preferences.
4. `settings` is a non-null JSONB object by default.
5. `theme` defaults to `glass`.
6. Theme values are constrained to `glass` and `normal`.
7. `updated_at` defaults to UTC time.
8. RLS is enabled.
9. A user may select only their own preference row.
10. A user may insert only a row tied to their own Auth UUID.
11. A user may update only their own preference row.
12. A user may delete only their own preference row.
13. Settings JSON is not intended to contain authentication secrets.
14. Settings should be versioned or validated before adding app behavior that depends on a new key.
15. Unknown settings should be handled without crashing the app.
16. Theme changes can fail to sync independently from local UI state.
17. Do not assume an absent preferences row has been created remotely.
18. Auth initialization currently attempts to ensure a preferences row exists.
19. Database migrations should preserve the accepted theme constraint.
20. Keep unrelated large user content out of the settings JSON field.

#### D.3 `public.imported_assets`

1. `id` is a generated UUID primary key.
2. `owner_id` is a required Auth UUID reference.
3. Auth user deletion cascades to owned imported assets.
4. `title` is required and trimmed length must be between 1 and 200 characters.
5. `media_kind` is constrained to `video`, `short`, `music`, or `live`.
6. `is_favourite` defaults to false.
7. `view_count` is a nonnegative bigint defaulting to zero.
8. `download_count` is a nonnegative bigint defaulting to zero.
9. `last_viewed_at` is optional.
10. `imported_at` defaults to UTC time.
11. `created_at` defaults to UTC time.
12. `updated_at` defaults to UTC time.
13. An update trigger refreshes `updated_at`.
14. The table does not define a media byte column.
15. The table does not define a persistent device URI column.
16. The table does not have its own visibility column in this migration.
17. Public reads can be allowed when an asset is referenced by a public collection.
18. Owner-scoped insert and update policies compare `owner_id` with `auth.uid()`.
19. The migration creates an owner index for this table.
20. Do not interpret counters as verified YouTube view or download statistics.

#### D.4 `public.joytube_collections`

1. `id` is a generated UUID primary key.
2. `owner_id` is a required Auth UUID reference.
3. Auth user deletion cascades to collections.
4. `name` is required and trimmed length must be between 1 and 80 characters.
5. `description` defaults to an empty string.
6. Description length is limited to 500 characters.
7. `visibility` defaults to `private`.
8. Visibility values are constrained to `public` and `private`.
9. `is_favourite` defaults to false.
10. `created_at` and `updated_at` default to UTC time.
11. An update trigger refreshes `updated_at`.
12. Owners can select their own collections.
13. Public collections can be selected through the public read policy.
14. Owners can insert, update, and delete their collection rows.
15. A public collection is still owned by its creator.
16. Public visibility does not authorize edits by other users.
17. The migration creates an index on `owner_id`.
18. The client repository currently writes name, visibility, and favourite state.
19. The client repository's current update method does not persist description edits.
20. Confirm description editing behavior before claiming it works end to end.

#### D.5 `public.joytube_collection_items`

1. `id` is a generated UUID primary key.
2. `collection_id` references a JoyTube collection.
3. `owner_id` references a Supabase Auth user.
4. `imported_asset_id` references an imported asset.
5. `position_index` is a nonnegative integer defaulting to zero.
6. A collection can include a given asset only once.
7. Deleting the collection cascades to its membership rows.
8. Deleting the referenced asset cascades to its membership rows.
9. The row's owner is expected to match the authenticated user for writes.
10. Insert policy checks that the collection belongs to the caller.
11. Insert policy checks that the asset belongs to the caller.
12. Update policy repeats ownership checks with `WITH CHECK`.
13. Owners can delete their membership rows.
14. Memberships in public collections can be selected under the public policy.
15. The table has indexes for owner, asset, and collection lookups as declared in migration.
16. Collection membership is metadata, not media storage.
17. A position update must remain nonnegative.
18. Duplicate attach attempts are handled with an upsert preference in the client.
19. Public collection reads do not automatically disclose private collection rows.
20. Validate join ownership if this schema is changed.

#### D.6 `public.recent_imported_content`

1. `id` is a generated UUID primary key.
2. `owner_id` references a Supabase Auth user.
3. `imported_asset_id` references an imported asset.
4. `imported_at` defaults to current UTC time.
5. A unique constraint prevents duplicate owner/asset pairs.
6. RLS is enabled.
7. Reads are restricted to the owner.
8. Inserts check that the asset belongs to the caller.
9. Updates are owner-scoped.
10. Deletes are owner-scoped.
11. An index supports owner and descending import-time queries.
12. This table tracks recent imports, not YouTube watch history.
13. Opening a provider search result should not write a recent import row.
14. Imported content recency is distinct from playback recency.
15. The client can upsert by owner and asset pair.

#### D.7 Counter functions and triggers

1. `set_updated_at()` is a trigger helper for selected tables.
2. The helper sets `updated_at` to UTC time.
3. `record_imported_asset_view(uuid)` increments `view_count` for a caller-owned asset.
4. The view-count function updates `last_viewed_at`.
5. `record_imported_asset_download(uuid)` increments `download_count` for a caller-owned asset.
6. Both counter functions use caller privileges rather than `SECURITY DEFINER`.
7. Both functions set a controlled search path.
8. Both functions raise an error if the asset is not found or not owned by the caller.
9. Execute permission is revoked from public and anon roles.
10. Execute permission is granted to authenticated users.
11. The migration does not make these counters authoritative analytics.
12. Counters represent JoyTube events recorded by the client.
13. A UI event can be recorded even when the corresponding underlying file action is incomplete.
14. Do not call a download counter “downloaded bytes” or “completed files.”
15. If event accuracy matters, use a transaction tied to verified operation completion.
16. Idempotency should be considered before adding retries to counters.
17. A retry after an ambiguous timeout can increment twice.
18. Add an idempotency key or event table if exactly-once semantics become a requirement.
19. Review grants after changing function signatures.
20. Re-run database security review when policies or functions change.

### Appendix E: Authentication and session lifecycle

1. JoyTube sign-in begins from an Android foreground activity.
2. Credential Manager is invoked from the main thread with that activity.
3. The app builds a Google ID credential option.
4. Authorized-account filtering is disabled so the user can choose another account.
5. The request uses the configured Google Web client ID as the server client ID.
6. A random nonce is generated for the request.
7. The nonce value is hashed before it is sent to Google.
8. Google returns an ID token through the credential result.
9. The app sends the token to Supabase Auth's ID-token exchange endpoint.
10. Supabase's publishable key is sent as the public API key.
11. The app does not request YouTube API scopes in this flow.
12. The ID token is exchanged rather than treated as the durable JoyTube session.
13. The resulting Supabase user UUID identifies the JoyTube account.
14. Email and user metadata are read from the Supabase Auth response.
15. The client ensures a profile record exists after successful authentication.
16. The client ensures a preferences record exists after successful authentication.
17. The Supabase access token is needed for authenticated REST operations.
18. The refresh token is used to renew the Supabase session when needed.
19. Local session material is encrypted using Android Keystore-backed cryptography.
20. Keystore material is device-local and should not be copied to another device.
21. Session expiry is evaluated before protected operations.
22. A failed refresh can clear local session state and require sign-in again.
23. Sign-out attempts to revoke/logout the Supabase session.
24. Local session data is cleared during sign-out.
25. A sign-in button does not prove that Google Cloud OAuth is configured correctly.
26. A Web client ID alone does not register the Android package/signing certificate.
27. Android OAuth registration includes the application ID and certificate SHA-1.
28. A debug installation and release installation can have different SHA-1 values.
29. OAuth consent-screen publication or test-user settings may affect account eligibility.
30. Supabase's Google provider must accept the ID token audience used by the app.
31. Do not log ID tokens, access tokens, or refresh tokens.
32. Do not place an OAuth client secret in `BuildConfig`.
33. Do not include a Google password field in JoyTube sign-in.
34. Do not ask users to enter their YouTube password into JoyTube.
35. Troubleshoot auth in order: app package, signing SHA-1, Web client ID, Google provider, Supabase project, then network.
36. Preserve the server-side Supabase Auth boundary for identity exchange.

### Appendix F: Rust service request reference

#### F.1 Runtime configuration

1. The Rust package is named `joytube-backend`.
2. The Rust crate uses edition 2024.
3. The application entry point is `backend-rust/src/main.rs`.
4. Axum provides the HTTP router.
5. Tokio provides the async runtime and TCP listener.
6. Reqwest provides the HTTPS client to Supabase.
7. The HTTP client is configured to require HTTPS for upstream calls.
8. `SUPABASE_URL` is required at process startup.
9. `SUPABASE_PUBLISHABLE_KEY` is required at process startup.
10. `BIND_ADDRESS` is optional.
11. The default bind address in source is `0.0.0.0:8080`.
12. Binding to all interfaces exposes the service on reachable interfaces.
13. Use a loopback address for local development unless external access is required.
14. The service does not load a dotenv file in `main.rs`.
15. Environment values must be injected by the process environment or deployment platform.
16. The repository's `.env.example` is a template, not a secret store.
17. Keep the Supabase publishable key in deployment configuration.
18. Never configure a Supabase secret/service-role key for this proxy.
19. Do not expose debug output or credentials in startup logs.
20. Production deployment requires a maintained runtime and operational monitoring.

#### F.2 Route behavior

1. `GET /health` returns a short health response.
2. `/v1/{table}` accepts methods through the Axum `any` route.
3. The allowed table names are an explicit source-code list.
4. Unknown table names return not found.
5. Query parameters are collected and forwarded to PostgREST.
6. Request methods are forwarded to the upstream table route.
7. Request bodies are bounded to one mebibyte by the router layer.
8. The body is read with the same one-mebibyte maximum in the handler.
9. Content type is forwarded when supplied.
10. The caller authorization header is forwarded when supplied.
11. The publishable key is sent as the PostgREST `apikey` header.
12. The response content type is forwarded when available.
13. The upstream response status is returned to the caller where valid.
14. Upstream request errors return a gateway error response.
15. Upstream response read errors return a gateway error response.
16. The proxy does not create its own user identity.
17. The proxy does not replace Postgres row-level security.
18. The proxy does not validate JWT claims locally in this implementation.
19. RLS and Supabase Auth remain responsible for caller identity and authorization.
20. Review all allowed table names before adding new resources.

#### F.3 Public-read exception in source

1. The proxy recognizes public reads only for selected metadata tables.
2. The current public-read list includes collections.
3. The current public-read list includes collection items.
4. The current public-read list includes imported assets.
5. A public-read request must use GET to qualify for the exception.
6. Other requests require an authorization header.
7. The proxy's header-presence check is not JWT validation.
8. Supabase should reject invalid or insufficient user tokens.
9. Supabase RLS remains necessary even when a request includes a bearer token.
10. Anonymous table access should remain restricted by grants and RLS.
11. Do not add user preferences to the public-read allowlist.
12. Do not add profile rows to the public-read allowlist without a privacy design.
13. Public collection reads must not reveal private membership data.
14. Review query forwarding for filters that could broaden data exposure.
15. Test unauthorized writes and private reads before deployment.

### Appendix G: Build and release runbooks

#### G.1 Local setup checklist

1. Check out the intended repository branch.
2. Confirm `git status` contains no unexpected local changes.
3. Install or select a JDK compatible with the checked-in Gradle wrapper.
4. Install Android SDK platform 36 and compatible build tools.
5. Configure SDK discovery with environment variables or local properties.
6. Keep machine-specific SDK paths out of source control.
7. Allow Gradle to resolve pinned plugins and dependencies.
8. Review `gradle/libs.versions.toml` before updating dependencies.
9. Review Gradle wrapper distribution before changing Gradle versions.
10. Do not delete wrapper files when changing Android Studio settings.
11. Build a debug variant before attempting a release variant.
12. Confirm the expected package name in the produced artifact.
13. Run relevant unit tests when implementation changes are made.
14. Use an emulator for instrumented tests.
15. Use a real phone for Google OAuth and WebView behavior checks.
16. Do not consider simulator behavior sufficient for all network and media conditions.
17. Keep environment-specific credentials outside the repository.
18. Confirm repository status after the build.
19. Do not stage `build/` outputs unless explicitly preparing an artifact distribution.
20. Keep APK delivery separate from source commits unless the project intentionally publishes binaries.

#### G.2 Debug build checklist

1. Run `./gradlew :app:assembleDebug` from the repository root.
2. Confirm Gradle exits successfully.
3. Inspect the debug APK timestamp and size.
4. Confirm the output path is `app/build/outputs/apk/debug/app-debug.apk`.
5. Install with `adb install -r` on a test device.
6. Launch the app from the Android launcher.
7. Check startup and navigation without signing in.
8. Check sign-in only with a configured test account.
9. Check the device has a supported WebView implementation before testing playback.
10. Check network request errors without logging sensitive headers.
11. Avoid testing with real sensitive accounts unless necessary.
12. Capture logs with user identifiers and tokens removed.
13. Verify release-specific OAuth separately from debug OAuth.
14. Remove any test account or test content created during verification.
15. Record the device and OS version with bug results.

#### G.3 Release signing checklist

1. Confirm `versionName` reflects the intended release label.
2. Confirm `versionCode` is greater than previously distributed builds.
3. Confirm the release application ID is correct.
4. Confirm the release build type uses the release signing configuration.
5. Confirm `KEYSTORE_PATH` points to the intended upload keystore.
6. Confirm the alias matches `upload` in the current Gradle configuration.
7. Supply store and key passwords through a secure environment.
8. Never echo passwords to build logs.
9. Never commit keystore files.
10. Never paste signing passwords into a public issue.
11. Keep a secure backup of the keystore.
12. Keep the release SHA-1 fingerprint available for OAuth configuration.
13. Build with `./gradlew clean :app:assembleRelease`.
14. Inspect the generated release APK path.
15. Verify the APK signature with Android `apksigner`.
16. Inspect the package name and version with Android build tools.
17. Test install on a device before distributing the artifact.
18. Check update behavior against an installation signed by the same key.
19. Do not expect an app signed with a different key to update in place.
20. Archive the artifact with its checksum and release notes.
21. Do not publish debug-signed artifacts as production releases.
22. Do not infer store readiness from local release assembly.

#### G.4 Versioning checklist

1. `versionName` is the human-readable version.
2. `versionCode` is the monotonically increasing Android update number.
3. Both values are currently declared in `app/build.gradle.kts`.
4. Keep the APK filename consistent with `versionName` when distributing files manually.
5. Avoid reusing a version code for a different build intended as an update.
6. Record source commit and checksum alongside a distributed APK.
7. Keep the signing identity stable across updates.
8. If the signing key changes, plan migration before distribution.
9. Separate development and production configuration where possible.
10. Document release-specific OAuth fingerprints.

### Appendix H: Test strategy and coverage boundaries

#### H.1 JVM test responsibilities

1. JVM tests run without an Android emulator for the host-side test sources.
2. The test source includes provider parser tests using fixtures.
3. Provider fixtures cover selected renderer structures.
4. Tests verify omitted view counts remain null in selected cases.
5. Tests verify Shorts classification does not use duration alone in one case.
6. Tests verify live classification uses metadata rather than a title keyword in selected fixtures.
7. Search coordinator tests verify adapter selection in chosen modes.
8. Search coordinator tests exercise a simulated failing adapter.
9. Ranking tests cover normalized weights and popularity distinctions.
10. Playback tests cover coordinator selection and rejection of a nonplayable container.
11. Auth surface tests verify no YouTube scope API was added to the manager surface.
12. Import tests exercise ownership and source policy boundaries.
13. File signature tests cover a selected media signature path.
14. Tests are only as broad as their assertions and fixtures.
15. A passing test does not certify every provider response shape.
16. A passing parser test does not make the protocol supported.
17. A passing security unit test does not prove deployed RLS correctness.
18. A passing fake download boundary test does not prove file transfer.
19. A passing session unit test does not validate Google Cloud console configuration.
20. Add regression tests for behavior that can be deterministically reproduced.

#### H.2 Instrumented test responsibilities

1. Instrumented tests require Android runtime instrumentation.
2. The current test uses an AndroidJUnit4 runner.
3. The current test checks the package context against application configuration.
4. The current test renders the shared brand header.
5. It verifies selected search and profile controls exist.
6. It verifies a removed design-spec chip remains absent.
7. It does not exercise real Google sign-in.
8. It does not exercise Supabase network writes.
9. It does not exercise live YouTube search.
10. It does not exercise real video playback.
11. It does not test background playback.
12. It does not test actual device file downloads.
13. Instrumentation success is limited to the device or emulator used.
14. Capture Android version and emulator image when reporting results.
15. Do not use test account credentials in screenshots.

#### H.3 Manual integration matrix

| Scenario | Environment needed | What to observe | What a pass does not prove |
| --- | --- | --- | --- |
| App cold start | Device or emulator | Launch, initial route, crash-free startup | Long-run stability on all devices. |
| Google sign-in | Real OAuth configuration and eligible Google account | Credential picker, token exchange, profile result | YouTube account linkage. |
| Supabase profile read | Deployed project with migration and signed-in session | Profile load and error handling | Correctness of every RLS policy. |
| Search videos | Network and reachable upstream | Result parsing and UI state | Stable future protocol behavior. |
| Search music | Network and reachable music surface | Music entity parsing | Catalog completeness. |
| Search Shorts | Network and reel metadata | Explicit/heuristic classifications | Correct classification of every candidate. |
| Search live | Network and live metadata | Active/upcoming/completed status display | Guaranteed stream availability. |
| Embedded playback | Publicly embeddable test video | WebView load and player behavior | Ads, quality, or all-account support. |
| Caption request | Video with known English captions | Caption visibility in embed | Caption availability for other videos. |
| Collection write | Deployed schema and authenticated session | Database row and UI response | Public exposure configuration unless tested separately. |
| Private collection access | Two separate test accounts | Owner access and nonowner denial | Full security without broader adversarial checks. |
| Public collection read | Public visibility and anonymous client | Intended read works | No unrelated data leakage. |
| Local picker selection | Device with media | URI selection and import draft | Durable file copy or playback readiness. |
| Download control | Current prototype | UI status entry only | Any actual media file creation. |
| Rust health endpoint | Locally running Rust process | HTTP health response | Proxy authorization or production readiness. |
| Rust table route | Local service and Supabase project | User token forwarding and RLS outcome | Deployment TLS, monitoring, or scaling. |

### Appendix I: Troubleshooting guide

#### I.1 Gradle cannot find Java

1. Confirm a Java executable is available to the shell.
2. Confirm `JAVA_HOME` points to the JDK directory, not its `bin` directory.
3. Put `$JAVA_HOME/bin` on `PATH` when invoking Gradle.
4. Use a JDK compatible with the Gradle wrapper and Android Gradle Plugin.
5. Restart Android Studio after changing its Gradle JDK setting.
6. Do not commit an absolute machine-local JDK path.

#### I.2 Gradle cannot find Android SDK

1. Confirm the SDK is installed in Android Studio SDK Manager.
2. Confirm platform API 36 is installed.
3. Confirm required build tools are installed.
4. Set `ANDROID_HOME` or `ANDROID_SDK_ROOT` to the SDK root.
5. Alternatively create `local.properties` with `sdk.dir=/absolute/path/to/sdk`.
6. Do not commit `local.properties`.
7. Retry Gradle after correcting SDK discovery.
8. Do not delete project source to resolve an SDK discovery error.

#### I.3 Google sign-in does not open or returns an error

1. Confirm Google is enabled as a Supabase Auth provider.
2. Confirm the Supabase provider has the intended Web OAuth client credentials.
3. Confirm `GOOGLE_WEB_CLIENT_ID` uses the correct Web client ID.
4. Confirm Android OAuth uses the exact application ID.
5. Confirm the Android OAuth client has the SHA-1 for the installed APK's signing certificate.
6. Distinguish debug APK certificate from release APK certificate.
7. Confirm the Google account is allowed by consent-screen testing settings if applicable.
8. Confirm the Android device can reach Google and Supabase endpoints.
9. Check sanitized app logs for a high-level error category.
10. Never include the ID token in a bug report.
11. Do not add YouTube scopes to resolve a JoyTube identity sign-in error.
12. Rebuild only if the app's Web client ID or package configuration changed.

#### I.3.a Credential Manager reports no Google credentials

1. The app's visible control is an explicit Google sign-in button.
2. Version `1.0.1` uses Credential Manager's dedicated Sign in with Google option rather than requesting only a previously authorized credential.
3. Install the updated APK; the older release uses the previous credential request.
4. Confirm Google Play services is installed and updated on the Android device.
5. Confirm at least one Google account can be selected or added in Android system settings.
6. Retry after updating Google Play services and restarting JoyTube.
7. Confirm the installed APK's application ID is `com.aistudio.joytube.xkvpzr`.
8. Confirm the Android OAuth client uses the SHA-1 fingerprint for the exact installed APK.
9. Confirm the OAuth clients and the app's Web client ID are from the same Google Cloud project.
10. In Supabase Auth's Google provider, list the Web client ID first and the Android client ID after it, comma-separated.
11. Keep the Google OAuth client secret in Supabase; do not add it to the APK.
12. Credential Manager errors occur before the app exchanges a token with Supabase, so changing database RLS will not fix that specific error.
13. If an account picker appears but Supabase rejects the sign-in, check the provider client-ID list and Supabase Auth logs.
14. Do not send Google passwords, ID tokens, or Supabase session tokens in screenshots or bug reports.

#### I.4 Sign-in succeeds but profile or library sync fails

1. Confirm the migration is applied to the configured Supabase project.
2. Confirm the build points to the intended project URL.
3. Confirm the publishable key corresponds to that project.
4. Confirm the user has a valid Supabase session.
5. Check that all expected tables are exposed through the Supabase Data API.
6. Check table grants in addition to RLS policies.
7. Check the caller's row ownership predicate.
8. Check required unique constraints for upsert conflict targets.
9. Check the database error without sharing user tokens.
10. Do not disable RLS as a quick fix.
11. Do not grant unrestricted table writes to anonymous users.
12. Validate the request owner UUID is the authenticated UUID.

#### I.5 Search returns no items or intermittent errors

1. Confirm the device has working internet access.
2. Try a common non-sensitive query to distinguish a blank query from network failure.
3. Check the active search mode and filter.
4. Check whether all providers failed or only one provider failed.
5. Look for rate-limit or timeout errors in sanitized diagnostics.
6. Do not assume empty parsed results mean the query has no matches.
7. Do not hammer retry actions against a rate-limited upstream.
8. Re-test the parser fixture if an upstream response shape appears to have changed.
9. Do not introduce a YouTube API key unless the architecture and provider contract are deliberately changed.
10. Report provider, mode, time, app version, and sanitized error category.

#### I.6 Embedded playback fails

1. Confirm the result contains a valid video ID.
2. Confirm the video permits embedding.
3. Confirm the video is available to the account and region.
4. Confirm Android System WebView or the relevant WebView provider is updated.
5. Confirm the device can reach YouTube domains.
6. Try a known publicly embeddable test video.
7. Distinguish player restrictions from JoyTube search parser failures.
8. Do not attempt to extract a direct media URL as a playback workaround.
9. Do not promise ad-free playback or arbitrary quality selection.
10. Capture only safe diagnostics; do not log cookies or account tokens.

#### I.7 Import and download controls appear to work but file is missing

1. Downloads copy the selected local document into app-managed Movies storage; the app does not download YouTube streams or arbitrary remote URLs.
2. The current copy operation has no pause/resume or persistent background worker.
3. Download list entries are currently process-local even though the copied file remains in app storage.
4. An imported file can become inaccessible if its original provider URI grant is revoked or the document is deleted.
5. URL metadata import is not a playable-media import path.
6. Do not treat a database download count as proof that bytes were saved.
7. Do not use YouTube-origin sources for downloads.
8. The local copy is complete only after a nonempty output file is verified.
9. Downloads are app-managed local files and are deleted when their entry is removed.
10. A future remote transfer feature needs an authorized source, cancellation, retry, safe destination selection, integrity checks, and persistent job state.

#### I.8 Rust service does not start

1. Confirm Rust and Cargo are installed.
2. Confirm `SUPABASE_URL` is set in the process environment.
3. Confirm `SUPABASE_PUBLISHABLE_KEY` is set in the process environment.
4. Confirm the bind address is available.
5. Confirm no other process is using the selected port.
6. Remember that the binary does not load `.env` in current source.
7. Confirm upstream TLS certificates are available to the runtime.
8. Do not switch off HTTPS enforcement to hide certificate problems.
9. Check startup errors without printing secret values.
10. Test `/health` locally before exercising PostgREST routes.

#### I.9 Git or release credential issue

1. Confirm the remote URL points to the intended GitHub repository.
2. Confirm the local branch and upstream branch names.
3. Authenticate using the user's approved Git credential manager or GitHub CLI.
4. Do not paste a token into a command that may be retained in shell history.
5. Do not place a token in the remote URL.
6. Confirm commit author identity separately from push authentication.
7. GitHub commit attribution depends on a verified author email or account association.
8. GitHub “pushed by” attribution depends on the authenticated push account.
9. A local author name does not authenticate a push.
10. Keep signing credentials distinct from GitHub credentials.

### Appendix J: Security and privacy review checklist

#### J.1 Android client review

1. Confirm no Supabase secret/service-role key is included in the app.
2. Confirm no Google OAuth client secret is included in the app.
3. Confirm Google ID tokens are not logged.
4. Confirm Supabase access and refresh tokens are not logged.
5. Confirm session storage uses Android Keystore-backed encryption.
6. Confirm sign-out clears local session state.
7. Confirm URL import validation rejects prohibited YouTube hosts.
8. Confirm user-provided URLs are parsed safely before network use.
9. Confirm WebView file access remains disabled unless a documented requirement changes it.
10. Confirm content access is not enabled without need.
11. Confirm embedded playback only loads expected YouTube player URLs.
12. Confirm WebView lifecycle destroys player resources when the component leaves.
13. Confirm no app feature silently collects YouTube history.
14. Confirm provider requests contain only the search query and required request metadata.
15. Confirm telemetry does not collect full account or media data unexpectedly.
16. Confirm imported file access is scoped and revocable.
17. Confirm private data is not included in screenshots or crash logs.
18. Confirm debug-only controls are not accidentally exposed in a release build.
19. Confirm the release manifest does not include unnecessary permissions.
20. Confirm backup behavior matches the intended local session policy.

#### J.2 Database review

1. Confirm RLS is enabled on every exposed public table.
2. Confirm SELECT policies match public/private semantics.
3. Confirm INSERT policies enforce `auth.uid()` ownership.
4. Confirm UPDATE policies include ownership in both `USING` and `WITH CHECK`.
5. Confirm DELETE policies enforce ownership.
6. Confirm public reads cannot expose private collection contents.
7. Confirm collection membership cannot reference another user's private assets.
8. Confirm profile email is not duplicated into a public table by accident.
9. Confirm service role is not used from Android.
10. Confirm anonymous grants are limited to the intended reads.
11. Confirm function execute grants are limited to intended roles.
12. Confirm functions use an appropriate privilege model.
13. Confirm search paths are controlled in database functions.
14. Confirm delete cascades match retention expectations.
15. Confirm indexes support actual query filters and ordering.
16. Confirm aggregate counters cannot be incremented by arbitrary anonymous callers.
17. Confirm Data API exposure settings align with table grants.
18. Confirm migrations are applied to the intended project.
19. Confirm staging and production projects are not confused.
20. Re-run security review after schema or policy changes.

#### J.3 Rust service review

1. Confirm the table allowlist is explicit and minimal.
2. Confirm caller bearer tokens are forwarded without being logged.
3. Confirm untrusted headers are not used to establish identity.
4. Confirm service-role credentials are absent.
5. Confirm request and response sizes remain bounded.
6. Confirm upstream TLS verification remains enabled.
7. Confirm public reads are limited to intended GET routes.
8. Confirm RLS is enabled for every proxied table.
9. Confirm CORS is restrictive for browser clients.
10. Confirm deployment has TLS termination.
11. Confirm error responses do not leak secrets or upstream internals.
12. Confirm logs redact bearer tokens and personal data.
13. Confirm rate limits and concurrency controls exist before public deployment.
14. Confirm health checks do not reveal sensitive configuration.
15. Confirm dependency updates are reviewed and locked.
16. Confirm process shutdown and restart behavior are operationally tested.
17. Confirm public network binding is intentional.
18. Confirm proxy host is not being treated as a replacement for database authorization.

### Appendix K: Product semantics and terminology

1. **JoyTube identity** means a Supabase Auth user record used by the app.
2. **Google sign-in** means Google identity proof exchanged for a Supabase session.
3. **YouTube account access** would require separate authorization and is not implemented by this sign-in flow.
4. **Provider result** means parsed metadata returned by a discovery adapter.
5. **Search result** does not mean the media has been imported into JoyTube.
6. **Imported asset** means a JoyTube metadata record associated with a user's library.
7. **Imported asset** does not necessarily mean the media file has been copied or uploaded.
8. **YouTube video ID** is an upstream identifier, not a JoyTube database UUID.
9. **Collection** means a JoyTube-owned group of imported asset references.
10. **Public collection** means collection metadata is readable under the migration's public policy.
11. **Private collection** means the owner is the intended reader and writer.
12. **Recent import** means a recent metadata import, not watch history.
13. **View count** means a JoyTube counter event for an imported asset.
14. **Download count** means a JoyTube counter event, not a verified count of saved bytes.
15. **Local download** describes the intended storage boundary for first-party supported files.
16. **Download UI** is the current presentation and progress state, not a working YouTube downloader.
17. **Embedded playback** means YouTube's own player is rendered inside JoyTube.
18. **Playback coordinator** means JoyTube tracks selection and presentation state around the embed.
19. **Caption preference** means a request to show a caption track if one is available.
20. **Quality preference** means a UI value, not a promise of a delivered resolution.
21. **Keyless search** means no YouTube Data API key is configured.
22. **Keyless search** does not imply an official or stable protocol.
23. **RLS** means database row-level security policies evaluated by Postgres.
24. **Publishable key** means a client-visible Supabase API key, not a user identity token.
25. **Bearer token** means the user's Supabase access token in an authenticated request.
26. **Release APK** means an Android package built with the release variant.
27. **Release APK** does not automatically mean Play Store-ready.
28. **Upload key** means the local signing key configured by the release build.
29. **Commit author** means the Git metadata identity attached to a commit.
30. **Pushed by** means the GitHub account that authenticated the remote push.

### Appendix L: Change-impact guide

1. Changing `applicationId` requires checking OAuth package registrations.
2. Changing the release signing key requires a new Android OAuth SHA-1 registration.
3. Changing the Google Web client ID requires checking both Android BuildConfig and Supabase provider configuration.
4. Changing the Supabase URL requires checking every environment-specific build.
5. Changing a publishable key requires checking the matching Supabase project.
6. Changing a table column requires updating the migration and relevant repository mapping.
7. Changing a policy requires checking owner and public-access scenarios.
8. Changing collection visibility requires checking select policy semantics.
9. Changing a foreign key requires checking delete cascades and client cleanup.
10. Changing a unique constraint requires checking PostgREST upsert conflict targets.
11. Changing a JSON settings key requires backwards-compatible parsing.
12. Changing the provider response parser requires fixture coverage.
13. Changing provider request parameters can affect upstream response shape.
14. Changing retry behavior can affect rate-limit pressure and latency.
15. Changing coroutine scope ownership can affect cancellation.
16. Changing the player URL can affect embed behavior and captions.
17. Changing WebView settings can affect security and media playback.
18. Changing media import semantics requires revisiting local file permissions and retention.
19. Changing download semantics requires real file lifecycle and failure behavior.
20. Changing counter calls requires checking event idempotency.
21. Changing release minification requires runtime smoke tests and keep-rule review.
22. Changing Gradle plugin versions requires reviewing compatibility and build output.
23. Changing Rust dependency versions requires reviewing the lockfile.
24. Changing the Rust route allowlist requires RLS and privacy review.
25. Changing public reads requires an explicit disclosure decision.
26. Changing a feature label requires checking whether the implementation supports the claim.
27. Changing README status statements should follow code changes, not precede them.
28. Changing user-data retention requires updating privacy and migration docs.
29. Changing auth session handling requires device-level testing.
30. Changing signing procedures requires preserving the existing update key.

### Appendix M: Suggested issue templates

#### M.1 Bug report information

1. Short title describing the observed failure.
2. JoyTube version name.
3. Android version and API level.
4. Device make and model.
5. Whether the app is debug or release signed.
6. Screen or route where the issue occurred.
7. Steps to reproduce from a clean app launch.
8. Expected behavior.
9. Actual behavior.
10. Whether the issue is repeatable.
11. Network type and whether VPN/proxy is in use.
12. Search mode or media type if relevant.
13. Sanitized error message.
14. Whether another provider or route works.
15. Whether the issue occurs after reinstall.
16. Whether the issue occurs in both debug and release builds.
17. Whether the Supabase project migration is applied.
18. Whether OAuth fingerprint matches installed APK.
19. Remove email addresses if not needed for diagnosis.
20. Remove tokens, passwords, cookies, and signing material.

#### M.2 Feature request information

1. Describe the user problem, not only a proposed button.
2. Identify which user role needs the behavior.
3. Describe the expected data ownership boundary.
4. State whether the feature requires network access.
5. State whether the feature writes Supabase data.
6. State whether the feature stores local files.
7. Identify any data that must not be retained.
8. Describe empty, offline, and permission-denied states.
9. Describe how completion or failure should be communicated.
10. Note whether the change affects Google or Supabase configuration.
11. Note whether the change affects package/signing identity.
12. Avoid requesting storage or account permissions without a clear need.
13. Identify how success would be tested.
14. Identify expected behavior on small-screen Android devices.
15. Avoid claims that imply guaranteed upstream service availability.

### Appendix N: Release handoff template

1. Product name: JoyTube for Android.
2. Version name: record the value from the built artifact.
3. Version code: record the integer from the built artifact.
4. Source commit: record the Git commit hash.
5. Artifact filename: record the APK name.
6. Artifact checksum: record SHA-256.
7. Signing certificate fingerprint: record the intended release SHA-1.
8. Build command: record the exact Gradle task.
9. Build environment: record operating system and JDK version.
10. Android SDK version: record installed platform/build tools.
11. Test commands: list only commands actually run.
12. Test result: record pass/fail and any skipped environments.
13. Device smoke test: identify the device and Android version.
14. OAuth test: record whether sign-in was tested on the release-signed artifact.
15. Supabase target: identify staging or production without disclosing secrets.
16. Migration revision: record which migration was applied.
17. Known limitations: list behavior not implemented or unverified.
18. Signing key custodian: identify who can maintain the key, without publishing credentials.
19. Distribution destination: state how the artifact is being delivered.
20. Rollback plan: identify the previous supported artifact and migration compatibility.

## Appendix O: Glossary

| Term | Meaning in this repository |
| --- | --- |
| Android application ID | Package identifier used by Android installation and Android OAuth configuration. |
| Android Keystore | Device-protected key facility used by the auth session manager. |
| API 24 | Minimum Android SDK configured for the current app. |
| API 36 | Target Android SDK configured for the current app. |
| Axum | Rust web framework used by the optional API scaffold. |
| Bearer token | Authorization token sent with a protected request. |
| Browse ID | Upstream identifier for a browsable music or channel entity. |
| BuildConfig | Generated Android class containing configured build values. |
| Cache entry | Temporary in-memory search response associated with a query/mode key. |
| Cancellation | Signal that obsolete coroutine work should stop promptly. |
| Caption track | Subtitle/caption data served by the embedded player when available. |
| Collection | JoyTube metadata grouping of imported asset references. |
| Compose | Declarative UI toolkit used by the Android client. |
| Continuation token | Upstream pagination state used to request another result page. |
| Credential Manager | Android API used to obtain a Google credential. |
| Download count | JoyTube aggregate counter, not proof that a file was stored. |
| Embedded player | YouTube player displayed inside a JoyTube WebView. |
| Freshness score | Ranking input based on content recency or live state. |
| Google ID token | Google-signed identity token exchanged with Supabase Auth. |
| Innertube | Informal name for internal YouTube web client request surfaces used here. |
| JoyTube profile | Application-owned profile row linked to a Supabase Auth user. |
| JWT | Signed token format used for Supabase sessions. |
| Keyless request | Request that does not use a YouTube Data API key. |
| Local URI | Android-scoped reference to content selected from device storage. |
| Media kind | JoyTube metadata classification: video, short, music, or live. |
| Metadata | Descriptive information stored independently from media bytes. |
| OAuth client ID | Public identifier for an OAuth client; different client types have different uses. |
| Owner ID | Supabase Auth UUID used to scope JoyTube-owned data. |
| PostgREST | REST interface to Supabase PostgreSQL tables and functions. |
| Publishable key | Supabase client API key that can be embedded in a public client with RLS. |
| Rate limiter | Client-side request control intended to limit request pressure. |
| Release APK | Android application package built from the release build type. |
| RLS | PostgreSQL row-level security policies. |
| Rust edition 2024 | Rust language edition declared by the backend manifest. |
| SHA-1 fingerprint | Signing certificate fingerprint used in Android OAuth registration. |
| Short candidate | Result that may be a Short but lacks the strongest explicit reel signal. |
| Supabase Auth UUID | Stable user identifier used for data ownership. |
| Supabase session | Supabase access/refresh token pair retained by the client session manager. |
| Theme setting | User preference selecting `glass` or `normal`. |
| View count | JoyTube counter event for an owned imported asset. |
| WEB client context | Unofficial client identity used by the current video/Short/live adapters. |
| WEB_REMIX context | Unofficial client identity used by the current music adapter. |
| WebView | Android browser component used for embedded playback and browser UI. |
| Version code | Monotonically increasing Android package update integer. |
| Version name | Human-readable Android release label. |

## Appendix P: Maintainer completion checklist

1. State whether the requested behavior is actually implemented.
2. Distinguish source-level code from deployed services.
3. Distinguish metadata records from media files.
4. Distinguish JoyTube identity from Google and YouTube account access.
5. Identify whether the flow requires Google Cloud setup.
6. Identify whether the flow requires Supabase migration or provider setup.
7. Identify whether a device-level action was tested.
8. State the exact build and test commands that were run.
9. Do not report tests that were not run.
10. Do not promise lag-free behavior or battery outcomes.
11. Do not promise complete YouTube compatibility.
12. Do not describe local UI progress as a completed backend operation.
13. Do not commit private signing materials.
14. Do not push under a user's identity without their configured Git credentials.
15. Keep the public README aligned with the actual repository state.
16. Review the feature-status table after each major implementation change.
17. Update OAuth package/fingerprint instructions after signing changes.
18. Update schema documentation when the migration changes.
19. Update API docs when Rust routes change.
20. Keep this document navigable with a table of contents.

## Contributing

Issues and pull requests are welcome. Include the Android version/device, steps to reproduce, and relevant sanitized logs for bugs. Do not attach OAuth secrets, Supabase secret keys, private account data, or downloaded media.

Helpful bug reports include device model, Android version, app version, reproducible steps, network conditions, and sanitized error logs. Remove tokens, account identifiers, private URLs, and other sensitive values before sharing logs.

## Maintainer

**MSX_Fury**
