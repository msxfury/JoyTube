# JoyTube GeckoView privacy filter

JoyTube uses Brave's official `adblock` Rust crate as a WebAssembly module inside its GeckoView built-in extension. The engine is instantiated only after the JoyTube privacy filter setting is enabled. The APK bundles a current EasyPrivacy list for first-use and offline use; when enabled, the extension checks for an update every four days and swaps in a newly built engine only after the downloaded list passes validation and the new engine initializes.

## Request path

1. `JoyTubeViewModel` persists the master switch locally and in the existing account settings.
2. `GeckoBrowserView` installs the built-in extension and sends the switch state through GeckoView's native messaging port before it loads the first page.
3. When disabled, the extension does not instantiate WebAssembly and registers no blocking listener.
4. When enabled, the extension loads the bundled or last-known-good EasyPrivacy list, then instantiates the Rust engine.
5. The extension's synchronous `webRequest.onBeforeRequest` listener asks the Rust engine for each request decision. The request callback does not wait on Android/JNI messaging.
6. A filter update is parsed into a new WASM instance; the extension swaps the new instance in only after successful initialization. An invalid, unavailable, or incomplete update leaves the previous engine active.
7. YouTube-owned destinations and requests originating from YouTube-owned domains bypass filtering in the Rust matcher. The separate YouTube player and playback code are not changed.

The matcher applies to GeckoView requests only. It does not block YouTube ads, modify YouTube playback, or provide cosmetic page-element hiding.

## Build and test

Requires Rust with `wasm32-unknown-unknown` installed:

```sh
rustup target add wasm32-unknown-unknown
privacy-filter/build-wasm.sh
cargo test --manifest-path privacy-filter/Cargo.toml
node privacy-filter/tests/wasm_integration.mjs
```

Android instrumentation tests verify GeckoView 145's blocking request behavior and that the packaged extension initializes its Rust engine and bundled list. Run the app's Android tests with the project's Gradle wrapper and an API 26+ emulator.

## Licensing and data attribution

The Rust engine is Brave's `adblock` crate, pinned to version `0.13.5`, under MPL-2.0. EasyPrivacy is maintained by the EasyList authors and is distributed under the EasyList repository's GPL-3.0-or-later or CC BY-SA 3.0-or-later terms. Its source URL and license notice are preserved in the bundled list header. See [EasyList's license page](https://easylist.to/pages/licence.html).
