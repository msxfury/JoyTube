use axum::{
    Router,
    body::Body,
    extract::{DefaultBodyLimit, Path, Query, State},
    http::{HeaderValue, Method, Request, StatusCode, header},
    response::{IntoResponse, Response},
    routing::{any, get},
};
use bytes::Bytes;
use reqwest::Client;
use std::{collections::HashMap, env, sync::Arc};

#[derive(Clone)]
struct AppState {
    client: Client,
    supabase_url: String,
    publishable_key: String,
}

const ALLOWED_TABLES: &[&str] = &[
    "profiles",
    "user_preferences",
    "imported_assets",
    "joytube_collections",
    "joytube_collection_items",
    "recent_imported_content",
];

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let supabase_url = env::var("SUPABASE_URL")?.trim_end_matches('/').to_owned();
    let publishable_key = env::var("SUPABASE_PUBLISHABLE_KEY")?;
    let bind_address = env::var("BIND_ADDRESS").unwrap_or_else(|_| "0.0.0.0:8080".into());

    let state = Arc::new(AppState {
        client: Client::builder().https_only(true).build()?,
        supabase_url,
        publishable_key,
    });

    let app = Router::new()
        .route("/health", get(|| async { "ok" }))
        .route("/v1/{table}", any(proxy_table))
        .layer(DefaultBodyLimit::max(1024 * 1024))
        .with_state(state);

    let listener = tokio::net::TcpListener::bind(&bind_address).await?;
    println!("JoyTube Rust API listening on {bind_address}");
    axum::serve(listener, app).await?;
    Ok(())
}

async fn proxy_table(
    State(state): State<Arc<AppState>>,
    Path(table): Path<String>,
    Query(query): Query<HashMap<String, String>>,
    request: Request<Body>,
) -> Response {
    if !ALLOWED_TABLES.contains(&table.as_str()) {
        return (StatusCode::NOT_FOUND, "Unknown JoyTube resource").into_response();
    }

    let (parts, body) = request.into_parts();
    let has_user_token = parts.headers.contains_key(header::AUTHORIZATION);
    let public_read = parts.method == Method::GET
        && matches!(
            table.as_str(),
            "joytube_collections" | "joytube_collection_items" | "imported_assets"
        );
    if !has_user_token && !public_read {
        return (StatusCode::UNAUTHORIZED, "Supabase user session required").into_response();
    }

    let body_bytes = match axum::body::to_bytes(body, 1024 * 1024).await {
        Ok(bytes) => bytes,
        Err(_) => return (StatusCode::PAYLOAD_TOO_LARGE, "Request body too large").into_response(),
    };

    let mut upstream = state
        .client
        .request(
            parts.method.clone(),
            format!("{}/rest/v1/{}", state.supabase_url, table),
        )
        .query(&query)
        .header("apikey", &state.publishable_key)
        .header(header::ACCEPT, "application/json");

    if let Some(auth) = parts.headers.get(header::AUTHORIZATION) {
        upstream = upstream.header(header::AUTHORIZATION, auth);
    }
    if let Some(content_type) = parts.headers.get(header::CONTENT_TYPE) {
        upstream = upstream.header(header::CONTENT_TYPE, content_type);
    }
    if let Some(prefer) = parts.headers.get("prefer") {
        upstream = upstream.header("prefer", prefer);
    }
    if !body_bytes.is_empty() {
        upstream = upstream.body(body_bytes);
    }

    match upstream.send().await {
        Ok(upstream_response) => {
            let status = StatusCode::from_u16(upstream_response.status().as_u16())
                .unwrap_or(StatusCode::BAD_GATEWAY);
            let content_type = upstream_response
                .headers()
                .get(header::CONTENT_TYPE)
                .cloned()
                .unwrap_or(HeaderValue::from_static("application/json"));
            let bytes = match upstream_response.bytes().await {
                Ok(bytes) => bytes,
                Err(_) => {
                    return (
                        StatusCode::BAD_GATEWAY,
                        "Supabase response could not be read",
                    )
                        .into_response();
                }
            };
            let mut response = Response::new(Body::from(Bytes::from(bytes)));
            *response.status_mut() = status;
            response
                .headers_mut()
                .insert(header::CONTENT_TYPE, content_type);
            response
        }
        Err(_) => (StatusCode::BAD_GATEWAY, "Supabase request failed").into_response(),
    }
}
