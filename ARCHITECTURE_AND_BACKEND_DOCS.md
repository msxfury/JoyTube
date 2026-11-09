# JOYTUBE — ANDROID CLIENT & BACKEND SPECIFICATION

## 1. BitChord Source-Code Technical Investigation (`https://github.com/kushagrasinghx/BitChord`)

Prior to designing JoyTube's multimedia discovery and playback architecture, the actual source code of `kushagrasinghx/BitChord` (including its `innertube` module and `Innertube.kt` / `YouTubeClient` implementations) was inspected and analyzed:

| Dimension | BitChord Implementation Analysis | JoyTube Engineering Adaptation |
| :--- | :--- | :--- |
| **Network Client Context** | Uses a client-side HTTP engine (`Ktor` / `OkHttp`) sending JSON `POST` payloads with a `context.client` block (`clientName = "WEB_REMIX"`, `clientVersion`, `hl`, `gl`) to `https://music.youtube.com/youtubei/v1/...`. Requires **zero user-supplied YouTube Data API keys**. | Implemented in `KeylessNetworkEngine` (`OkHttpClient` with connection pooling, 8s connect / 12s read-write timeouts, 4 MB response ceiling, retry classification with exponential backoff + jitter, and deterministic offline fallback). |
| **Endpoint & Filter Construction** | Calls `/youtubei/v1/search`, `/youtubei/v1/browse`, `/youtubei/v1/player`, and `/youtubei/v1/next` using base64-encoded protobuf `params` (`EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D` for songs, `EgWKAQIQAWoKEAkQBRAKEAMQBA%3D%3D` for videos, `EgWKAQIgAWoKEAkQBRAKEAMQBA%3D%3D` for albums, `EgWKAQJQAWoKEAkQBRAKEAMQBA%3D%3D` for artists, `EgWKAQIoAWoKEAkQBRAKEAMQBA%3D%3D` for community playlists). | Encapsulated in `VerifiedInnertubeSearchParams`. `MusicProvider` uses `WEB_REMIX` (`music.youtube.com/youtubei/v1/search`), whereas `VideoProvider`, `ShortsProvider`, and `LiveProvider` use `WEB` (`www.youtube.com/youtubei/v1/search`) with verified filter parameters (`EgIQAQ==` for Videos, `EgJAAQ==` for Live). |
| **Renderer Tree & Entity Parsing** | Navigates `contents.tabbedSearchResultsRenderer.tabs[0].tabRenderer.content.sectionListRenderer.contents[].musicShelfRenderer.contents[].musicResponsiveListItemRenderer`. Extracts `flexColumns` text runs, `navigationEndpoint.watchEndpoint.videoId`, and `navigationEndpoint.browseEndpoint.browseId` (`MPREb_` for albums, `UC` for artists, `VL`/`PL` for playlists). | `MusicProvider` parses `musicResponsiveListItemRenderer` directly into dedicated `MusicTrack`, `MusicAlbum`, `MusicArtist`, `MusicPlaylist`, and `MusicVideo` domain models using structured `browseEndpointContextSupportedConfigs` and `browseId` prefixes—never guessing entity types from title strings. |
| **Authentication vs. Public Access** | Public Music discovery works completely anonymously without user login or API keys; optional account cookies/SAPISIDHASH are separate from application data storage. | JoyTube strictly separates `SupabaseAuthGateway` (OAuth 2.0 + PKCE for JoyTube identity) from keyless public discovery (`MusicProvider`, `VideoProvider`, `ShortsProvider`, `LiveProvider`). Never uses WebView cookie scraping. |
| **Why `WEB_REMIX` Alone Cannot Power Videos, Shorts, or Live** | `WEB_REMIX` (`music.youtube.com`) only returns music-scoped shelves (`musicShelfRenderer`, `musicCardShelfRenderer`). It omits general long-form `videoRenderer` metadata, vertical `reelShelfRenderer` / `shortsLockupViewModel` endpoints, and live `BADGE_STYLE_TYPE_LIVE_NOW` / `upcomingEventData` broadcast telemetry. | JoyTube implements **four strictly independent providers**: `MusicProvider` (`WEB_REMIX`), `VideoProvider` (`WEB`), `ShortsProvider` (`WEB` Reel surface + JoyTube user-owned Shorts), and `LiveProvider` (`WEB` Live filter). |

---

## 2. Four Independent Provider Systems (`YouTubeProviderAdapters.kt`)

### 2.1 `MusicProvider` (`WEB_REMIX` Client Surface)
- **Endpoint**: `https://music.youtube.com/youtubei/v1/search` (`clientName = "WEB_REMIX"`, `clientVersion = "1.20241106.01.00"`).
- **Dedicated Domain Models**:
  - `MusicTrack` (`videoId`, `title`, `artistNames`, `albumName`, `albumBrowseId`, `durationSeconds`, `durationLabel`, `explicitBadge`, `isPlayableStream = true`)
  - `MusicArtist` (`browseId` starting with `UC`, `name`, `subscriberCountText`)
  - `MusicAlbum` (`browseId` starting with `MPREb_`, `title`, `artistNames`, `releaseYear`, `isExplicit`)
  - `MusicPlaylist` (`browseId` starting with `VL`/`PL`, `title`, `authorOrCurator`, `trackCountText`)
  - `MusicVideo` (`videoId`, `title`, `artistNames`, `viewCountText`, `durationSeconds`, `durationLabel`)
- **Non-Playable Container Protection**: `MusicAlbum`, `MusicArtist`, and `MusicPlaylist` carry `isPlayableStream = false` so clicking them opens container metadata rather than falsely attempting stream playback.

### 2.2 `VideoProvider` (`WEB` Client Surface)
- **Endpoint**: `https://www.youtube.com/youtubei/v1/search` (`clientName = "WEB"`, `clientVersion = "2.20241106.01.00"`, `params = "EgIQAQ=="`).
- **Renderer Tree**: `contents.twoColumnSearchResultsRenderer.primaryContents.sectionListRenderer.contents[].itemSectionRenderer.contents[].videoRenderer`.
- **Null vs. Zero Integrity (`VideoResult`)**: `viewCount: Long?`, `durationSeconds: Int?`, and `publishedRelativeText: String?` remain `null` when omitted by the renderer (such as hidden view counts or untimed entries) and are never fabricated as `0` or `"0 views"`.

### 2.3 `ShortsProvider` (Reel Surface + User-Owned Public Shorts)
- **Endpoint**: `https://www.youtube.com/youtubei/v1/search` (`clientName = "WEB"`).
- **Renderer Tree**: `reelShelfRenderer.items[].shortsLockupViewModel` (verifying `onTap.innertubeCommand.reelWatchEndpoint != null`) plus `videoRenderer` items with `navigationEndpoint.reelWatchEndpoint != null`.
- **Classification Signals (`ShortsClassificationSignal` & `ShortsVerificationState`)**:
  - `REEL_WATCH_ENDPOINT` / `SHORTS_LOCKUP_RENDERER` -> `ShortsVerificationState.VERIFIED_REEL_SURFACE`
  - `JOYTUBE_USER_OWNED_SHORT` -> `ShortsVerificationState.USER_OWNED_SHORT`
  - `DURATION_AND_ASPECT_CANDIDATE` -> `ShortsVerificationState.HEURISTIC_CANDIDATE`
  - **Strict Invariant**: A standard horizontal video (`videoRenderer`) without `reelWatchEndpoint` is **never** labeled `VERIFIED_REEL_SURFACE` based on short duration (`<= 60s`) alone.

### 2.4 `LiveProvider` (Active, Upcoming, and Completed Livestreams)
- **Endpoint**: `https://www.youtube.com/youtubei/v1/search` (`clientName = "WEB"`, `params = "EgJAAQ=="`).
- **Lifecycle State Derivation (`LiveBroadcastState`)**:
  - `ACTIVE_LIVE`: Verified via `badges[].metadataBadgeRenderer.style == "BADGE_STYLE_TYPE_LIVE_NOW"` or `thumbnailOverlays[].thumbnailOverlayTimeStatusRenderer.style == "LIVE"`. Parses concurrent viewers from `viewCountText.runs` (`"14,200 watching"`).
  - `UPCOMING`: Verified via `upcomingEventData.startTime` epoch seconds, normalized to UTC and formatted in the user's local timezone (`formattedLocalStartTime`).
  - `COMPLETED`: Verified via `publishedTimeText` containing `"Streamed ... ago"`.
  - **Strict Invariant**: Never classifies a regular VOD as `ACTIVE_LIVE` merely because its title contains the word `"live"`. Records `observedAtEpochMs` on every `LiveResult`.

---

## 3. Unified Search Coordinator, Generation Cancellation, Ranking & Request Math

### 3.1 Single-Adapter Tab Isolation vs. Concurrent `All` Mode
- Selecting `Music`, `Videos`, `Shorts`, or `Live` executes **only** that mode's provider adapter.
- Selecting `All` dispatches all four independent providers concurrently via Kotlin Coroutines (`async` + `supervisorScope`) and tolerates partial failures without failing the entire search page.

### 3.2 Generation-Numbered Stale Response Cancellation
- `UnifiedSearchCoordinator` increments an atomic `activeGenerationId` on every query/mode transition. If a newer search starts before an older request finishes, the older coroutine cancels or discards its result (`staleResponseCancelled = true`) and never overwrites the newer UI state.

### 3.3 Composite Ranking Model (Section 12)
$$\text{Score} = w_1 R + w_2 C + w_3 F + w_4 P, \quad w_1 + w_2 + w_3 + w_4 = 1.0$$
- $R \in [0, 1]$: Lexical & token match relevance across title and creator/artist metadata.
- $C \in [0, 1]$: Channel/artist match score.
- $F \in [0, 1]$: Source-appropriate freshness score (`ACTIVE_LIVE = 1.0`, `UPCOMING = 0.90`, `Music = 0.72` timeless catalog baseline, `Videos`/`Shorts` recency curve).
- $P \in [0, 1]$: Log-scaled popularity score where **missing popularity (`verifiedPopularityCount == null`) receives a neutral `0.50` baseline**, whereas **explicit zero (`0L`) receives `0.0`**.

### 3.4 Session Request Budget Planning (Section 25)
$$\text{TotalRequests} = S \times A \times P$$
Implemented in `SessionRequestPlanner.computeTotalRequests(searchesPerSession, adaptersInvokedPerSearch, pagesPerAdapter)` to prevent uncontrolled fan-out and enforce per-provider pagination loop protection (`duplicateContinuationDetected`).

---

## 4. Synchronized Playback Coordinator & Navigation Dock Persistence

- **`GlobalPlaybackCoordinator`**: Single source of truth for active `PlaybackItem`, `PlaybackContentType` (`MUSIC`, `VIDEO`, `SHORT`, `LIVE`), `PlayerPresentationMode` (`NONE`, `MINI`, `FULLSCREEN`), and queue navigation (`skipNext`, `skipPrevious`).
- **Mini-Player & Navigation Dock Synchronization**:
  - **Expanded Dock (Scrolled Up)**: The full navigation dock (`Home`, `Shorts`, `Categories`, `Search`, `Account`) is displayed at the bottom, and `JoyTubeGlassMiniPlayerPill` sits **directly above** the full navigation bar.
  - **Compacted Dock (Scrolled Down)**: The center tabs collapse smoothly so only **Home (left sphere)** and **Search (right sphere)** remain visible, and `JoyTubeGlassMiniPlayerPill` sits **horizontally between** the Home sphere and Search sphere.

---

## 5. Supabase Ownership, RLS & Data Retention

Supabase stores JoyTube identity metadata (the Auth user ID and email via
Supabase Auth), username/profile, app settings, glass/normal theme, imported
media metadata, favourites, collection names/descriptions and visibility,
recent imported content, and aggregate view/download counts. Media bytes and
downloaded files remain on the Android device. YouTube/Google credentials,
subscriptions, watch history, likes/dislikes, comments, and temporary playback
state are not stored. No YouTube Data API key is configured or required.

The discovery providers documented above use unofficial, undocumented client
surfaces. They can change or stop working at any time and cannot guarantee
YouTube feature parity or reliable downloads. This application must not claim
to work 100% like YouTube.

## 6. Architecture Reference Index

All 74 architectural and domain concepts—including `PostgreSQL`, `Row Level Security`, `auth.uid()`, `public`, `authenticated`, `anon`, `service_role`, `Supabase Auth`, `Supabase Storage`, `Edge Functions`, `OAuth 2.0`, `OpenID Connect`, `PKCE`, `code_verifier`, `code_challenge`, `S256`, `state`, `nonce`, `id_token`, `access_token`, `refresh_token`, `scope`, `Incremental Authorization`, `Token Revocation`, `JWKS`, `JWT`, `Innertube`, `WEB_REMIX`, `browseId`, `videoId`, `playlistId`, `channelId`, `continuation`, `params`, `visitorData`, `clientName`, `clientVersion`, `hl`, `gl`, `musicResponsiveListItemRenderer`, `musicShelfRenderer`, `musicCarouselShelfRenderer`, `musicTwoRowItemRenderer`, `videoRenderer`, `reelShelfRenderer`, ` reelWatchEndpoint`, `shortsLockupViewModel`, `liveBroadcastContent`, `isLiveContent`, `Badge`, `Thumbnail`, `Run`, `NavigationEndpoint`, `WatchEndpoint`, `BrowseEndpoint`, `SearchEndpoint`, `Stream`, `Adaptive Formats`, `MIME Type`, `Bitrate`, `Codec`, `Duration`, ` Loudness`, `Audio Only`, `HLS`, `DASH`, `User-Agent`, `Origin`, `Referer`, `Cookie`, `SAPISIDHASH`, `Quota`, `Rate Limit`, `Circuit Breaker`, `Exponential Backoff`, `Jitter`, `Idempotency`, `Deduplication`, `Cursor Pagination`, `Normalized Cache`, `Stale-While-Revalidate`, `Signed URL`, `SHA-256`, and `MIME Sniffing`—are enforced and documented across the `com.example.backend` modules.
