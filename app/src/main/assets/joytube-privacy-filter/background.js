'use strict';

const FILTER_URL = 'https://easylist.to/easylist/easyprivacy.txt';
const FILTER_ASSET = 'easyprivacy.txt';
const UPDATE_AFTER_MS = 4 * 24 * 60 * 60 * 1000;
const MIN_FILTER_BYTES = 50000;
const NATIVE_APP = 'com.example.joytube.privacy';

let enabled = false;
let engine = null;
let wasmModulePromise = null;
let updateTimer = null;
let generation = 0;
let requestListenerRegistered = false;

function validList(text) {
  return typeof text === 'string' &&
    text.length >= MIN_FILTER_BYTES &&
    /^! Title: EasyPrivacy$/m.test(text) &&
    /^! Version: .+$/m.test(text);
}

async function getWasmModule() {
  if (!wasmModulePromise) {
    wasmModulePromise = fetch(browser.runtime.getURL('adblock.wasm'))
      .then(response => {
        if (!response.ok) throw new Error(`WASM asset request failed: ${response.status}`);
        return response.arrayBuffer();
      })
      .then(bytes => WebAssembly.compile(bytes));
  }
  return wasmModulePromise;
}

async function createEngine(listText) {
  const module = await getWasmModule();
  const { exports } = await WebAssembly.instantiate(module);
  const encoder = new TextEncoder();
  const bytes = encoder.encode(listText);
  const ptr = exports.joytube_alloc(bytes.length);
  new Uint8Array(exports.memory.buffer, ptr, bytes.length).set(bytes);
  let initialized;
  try {
    initialized = exports.joytube_init_filter_list(ptr, bytes.length);
  } finally {
    exports.joytube_dealloc(ptr, bytes.length);
  }
  if (initialized !== 1) throw new Error('EasyPrivacy could not be parsed by adblock-rust');

  const encode = value => encoder.encode(value);
  const withBytes = (value, run) => {
    const buffer = encode(value);
    const dataPtr = exports.joytube_alloc(buffer.length);
    new Uint8Array(exports.memory.buffer, dataPtr, buffer.length).set(buffer);
    try {
      return run(dataPtr, buffer.length);
    } finally {
      exports.joytube_dealloc(dataPtr, buffer.length);
    }
  };

  return {
    check(details) {
      const source = details.documentUrl || details.originUrl || details.url;
      const type = details.type || 'other';
      return withBytes(details.url, (urlPtr, urlLen) =>
        withBytes(source, (sourcePtr, sourceLen) =>
          withBytes(type, (typePtr, typeLen) =>
            exports.joytube_should_block(
              urlPtr, urlLen, sourcePtr, sourceLen, typePtr, typeLen
            ) === 1
          )
        )
      );
    }
  };
}

function onBeforeRequest(details) {
  const activeEngine = engine;
  if (!enabled || !activeEngine) return {};
  try {
    return activeEngine.check(details) ? { cancel: true } : {};
  } catch (_) {
    // A malformed request must not break navigation.
    return {};
  }
}

function setListenerActive(shouldListen) {
  if (shouldListen && !requestListenerRegistered) {
    browser.webRequest.onBeforeRequest.addListener(
      onBeforeRequest,
      { urls: ['http://*/*', 'https://*/*'] },
      ['blocking']
    );
    if (!browser.webRequest.onBeforeRequest.hasListener(onBeforeRequest)) {
      throw new Error('GeckoView did not register the blocking listener');
    }
    requestListenerRegistered = true;
  } else if (!shouldListen && requestListenerRegistered) {
    browser.webRequest.onBeforeRequest.removeListener(onBeforeRequest);
    requestListenerRegistered = false;
  }
}

async function loadInitialList() {
  const stored = await browser.storage.local.get(['easyPrivacyText']);
  if (validList(stored.easyPrivacyText)) return stored.easyPrivacyText;
  const response = await fetch(browser.runtime.getURL(FILTER_ASSET));
  if (!response.ok) throw new Error(`Bundled EasyPrivacy list unavailable: ${response.status}`);
  const bundled = await response.text();
  if (!validList(bundled)) throw new Error('Bundled EasyPrivacy list failed validation');
  return bundled;
}

async function refreshFilterList(currentGeneration) {
  try {
    const response = await fetch(FILTER_URL, { cache: 'no-store' });
    if (!response.ok) throw new Error(`EasyPrivacy update returned ${response.status}`);
    const candidateText = await response.text();
    if (!validList(candidateText) || !enabled || currentGeneration !== generation) return;

    // Build a separate engine first; the active instance keeps handling requests until
    // the replacement is fully parsed and initialized.
    const candidateEngine = await createEngine(candidateText);
    if (!enabled || currentGeneration !== generation) return;
    await browser.storage.local.set({
      easyPrivacyText: candidateText,
      easyPrivacyUpdatedAt: Date.now()
    });
    engine = candidateEngine;
  } catch (_) {
    // Keep the bundled or last known-good engine when the network or update fails.
  }
}

async function enableFiltering() {
  const currentGeneration = ++generation;
  clearTimeout(updateTimer);
  engine = null;
  wasmModulePromise = null;
  const listText = await loadInitialList();
  const candidateEngine = await createEngine(listText);
  if (!enabled || currentGeneration !== generation) return;
  engine = candidateEngine;
  setListenerActive(true);

  const stored = await browser.storage.local.get(['easyPrivacyUpdatedAt']);
  const elapsed = Date.now() - (stored.easyPrivacyUpdatedAt || 0);
  if (elapsed >= UPDATE_AFTER_MS) {
    void refreshFilterList(currentGeneration);
  } else {
    updateTimer = setTimeout(() => void refreshFilterList(currentGeneration), UPDATE_AFTER_MS - elapsed);
  }
}

async function configureFiltering(shouldEnable) {
  enabled = shouldEnable === true;
  if (!enabled) {
    generation += 1;
    clearTimeout(updateTimer);
    setListenerActive(false);
    engine = null;
    wasmModulePromise = null;
    return;
  }
  await enableFiltering();
}

try {
  const nativePort = browser.runtime.connectNative(NATIVE_APP);
  nativePort.onMessage.addListener(async message => {
    if (message?.type !== 'configure') return;
    const requestId = Number.isInteger(message.requestId) ? message.requestId : -1;
    try {
      await configureFiltering(message.enabled);
      nativePort.postMessage({
        type: 'ready',
        requestId,
        enabled,
        ok: !enabled || (engine !== null && requestListenerRegistered)
      });
    } catch (_) {
      enabled = false;
      setListenerActive(false);
      engine = null;
      nativePort.postMessage({ type: 'ready', requestId, enabled: false, ok: false });
    }
  });
} catch (_) {
  // Filtering remains off unless JoyTube explicitly enables it through GeckoView.
}
