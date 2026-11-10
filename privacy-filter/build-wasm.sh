#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
manifest="$repo_root/privacy-filter/Cargo.toml"
asset_dir="$repo_root/app/src/main/assets/joytube-privacy-filter"

if ! rustup target list --installed | grep -qx 'wasm32-unknown-unknown'; then
  echo "Missing Rust target wasm32-unknown-unknown. Install it with: rustup target add wasm32-unknown-unknown" >&2
  exit 1
fi

cargo build --manifest-path "$manifest" --target wasm32-unknown-unknown --release
cp "$repo_root/privacy-filter/target/wasm32-unknown-unknown/release/joytube_privacy_filter_wasm.wasm" \
  "$asset_dir/adblock.wasm"
