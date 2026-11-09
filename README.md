# JoyTube Android

JoyTube is a native Android app built with Kotlin and Jetpack Compose. Open this
folder in Android Studio, let Gradle sync, then run the `app` configuration on an
Android emulator or device.

The Supabase migration in `supabase/migrations` defines JoyTube-owned account
metadata and collections. Downloaded media files belong on the device and are
not stored in Supabase. YouTube/Google account credentials and activity are not
part of JoyTube storage. No YouTube Data API key is required.

The current UI and provider code are prototypes; sign-in and some media flows
are simulated and must be connected to production services before release.

`backend-rust/` contains an Axum API proxy for authenticated metadata requests
to Supabase. It is not deployed or wired into the Android app yet; see its
README for required server environment variables. YouTube search currently
uses a keyless, undocumented client interface, while playback of discovered
video IDs uses YouTube's embedded player and its own ads and access rules.
