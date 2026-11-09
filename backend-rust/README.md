# JoyTube Rust API

Small Axum service for forwarding authenticated JoyTube metadata requests to
Supabase PostgREST. Supabase remains the database and enforces row level
security using the caller's bearer token. The service accepts only the tables
listed in `src/main.rs` and never uses a `service_role` key.

## Run

Copy `.env.example` into your deployment environment and set:

```sh
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_PUBLISHABLE_KEY=your-publishable-key
BIND_ADDRESS=0.0.0.0:8080
```

Then run `cargo run --release`. `GET /health` returns `ok`. Database routes use
`/v1/{table}` and require `Authorization: Bearer <Supabase user access token>`.
The caller JWT is forwarded so the migration's RLS rules remain in force.

Before production, deploy behind HTTPS and configure a restrictive CORS origin
for the app's web clients if needed. The Android app does not yet call this API;
it also needs real Supabase Auth, a deployed base URL, and the public app
configuration before account data syncs.
