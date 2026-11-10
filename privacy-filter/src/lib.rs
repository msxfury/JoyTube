use adblock::{
    engine::Engine,
    lists::{FilterSet, ParseOptions, RuleTypes},
    request::Request,
};
use std::{cell::RefCell, slice, str};

thread_local! {
    static ENGINE: RefCell<Option<Engine>> = const { RefCell::new(None) };
}

fn bytes_to_str(bytes: &[u8]) -> Option<&str> {
    str::from_utf8(bytes).ok()
}

fn is_youtube_host(host: &str) -> bool {
    let host = host.trim_end_matches('.').to_ascii_lowercase();
    [
        "youtube.com",
        "youtube-nocookie.com",
        "youtu.be",
        "ytimg.com",
        "googlevideo.com",
        "youtubei.googleapis.com",
    ]
    .iter()
    .any(|domain| host == *domain || host.ends_with(&format!(".{domain}")))
}

fn should_block(engine: &Engine, url: &str, source_url: &str, request_type: &str) -> bool {
    let Some(destination) = adblock::url_parser::parse_url(url) else {
        return false;
    };
    if is_youtube_host(destination.hostname()) {
        return false;
    }
    if adblock::url_parser::parse_url(source_url)
        .is_some_and(|source| is_youtube_host(source.hostname()))
    {
        return false;
    }

    let normalized_type = match request_type {
        "main_frame" => "document",
        "sub_frame" => "subdocument",
        other => other,
    };
    let Ok(request) = Request::new(url, source_url, normalized_type, "GET") else {
        return false;
    };
    engine.check_network_request(&request).should_block()
}

#[no_mangle]
pub extern "C" fn joytube_alloc(len: usize) -> *mut u8 {
    let buffer = vec![0_u8; len].into_boxed_slice();
    Box::into_raw(buffer) as *mut u8
}

#[no_mangle]
pub unsafe extern "C" fn joytube_dealloc(ptr: *mut u8, len: usize) {
    if !ptr.is_null() {
        let slice = std::ptr::slice_from_raw_parts_mut(ptr, len);
        drop(Box::from_raw(slice));
    }
}

#[no_mangle]
pub unsafe extern "C" fn joytube_init_filter_list(ptr: *const u8, len: usize) -> i32 {
    if ptr.is_null() || len == 0 {
        return 0;
    }
    let Some(list_text) = bytes_to_str(slice::from_raw_parts(ptr, len)) else {
        return 0;
    };
    if list_text.len() < 64 || !list_text.contains("EasyPrivacy") {
        return 0;
    }
    let mut filter_set = FilterSet::new(false);
    filter_set.add_filter_list(
        list_text.to_owned(),
        ParseOptions {
            rule_types: RuleTypes::NetworkOnly,
            ..ParseOptions::default()
        },
    );
    let engine = Engine::new_with_filter_set(filter_set);
    ENGINE.with(|current| *current.borrow_mut() = Some(engine));
    1
}

#[no_mangle]
pub unsafe extern "C" fn joytube_should_block(
    url_ptr: *const u8,
    url_len: usize,
    source_ptr: *const u8,
    source_len: usize,
    type_ptr: *const u8,
    type_len: usize,
) -> i32 {
    if url_ptr.is_null() || source_ptr.is_null() || type_ptr.is_null() {
        return 0;
    }
    let Some(url) = bytes_to_str(slice::from_raw_parts(url_ptr, url_len)) else {
        return 0;
    };
    let Some(source) = bytes_to_str(slice::from_raw_parts(source_ptr, source_len)) else {
        return 0;
    };
    let Some(request_type) = bytes_to_str(slice::from_raw_parts(type_ptr, type_len)) else {
        return 0;
    };
    ENGINE.with(|current| {
        current
            .borrow()
            .as_ref()
            .is_some_and(|engine| should_block(engine, url, source, request_type)) as i32
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    fn test_engine() -> Engine {
        let mut filters = FilterSet::new(false);
        filters.add_filter_list(
            "! EasyPrivacy test list\n||tracker.example^\n@@||tracker.example/allowed^\n"
                .to_owned(),
            ParseOptions {
                rule_types: RuleTypes::NetworkOnly,
                ..ParseOptions::default()
            },
        );
        Engine::new_with_filter_set(filters)
    }

    #[test]
    fn matches_tracker_and_honors_exceptions() {
        let engine = test_engine();
        assert!(should_block(
            &engine,
            "https://tracker.example/pixel",
            "https://publisher.example/",
            "image"
        ));
        assert!(!should_block(
            &engine,
            "https://tracker.example/allowed?id=1",
            "https://publisher.example/",
            "image"
        ));
    }

    #[test]
    fn youtube_destination_and_source_are_exempt() {
        let engine = test_engine();
        assert!(!should_block(
            &engine,
            "https://tracker.example/pixel",
            "https://m.youtube.com/watch?v=abc",
            "image"
        ));
        assert!(!should_block(
            &engine,
            "https://googlevideo.com/videoplayback",
            "https://publisher.example/",
            "media"
        ));
    }

    #[test]
    fn youtube_suffix_lookalikes_are_not_exempt() {
        assert!(is_youtube_host("m.youtube.com"));
        assert!(!is_youtube_host("youtube.com.evil.example"));
        assert!(!is_youtube_host("evil-youtube.com"));
    }
}
