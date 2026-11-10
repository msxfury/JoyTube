import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const wasmPath = new URL('../../app/src/main/assets/joytube-privacy-filter/adblock.wasm', import.meta.url);
const listPath = new URL('../../app/src/main/assets/joytube-privacy-filter/easyprivacy.txt', import.meta.url);
const { instance } = await WebAssembly.instantiate(await readFile(wasmPath));
const api = instance.exports;
const encode = (value) => new TextEncoder().encode(value);
const withBytes = (value, callback) => {
  const bytes = encode(value);
  const ptr = api.joytube_alloc(bytes.length);
  new Uint8Array(api.memory.buffer, ptr, bytes.length).set(bytes);
  try { return callback(ptr, bytes.length); }
  finally { api.joytube_dealloc(ptr, bytes.length); }
};
const list = await readFile(listPath, 'utf8');
assert.equal(withBytes(list, (ptr, len) => api.joytube_init_filter_list(ptr, len)), 1);
const check = (url, source, type = 'script') => withBytes(url, (urlPtr, urlLen) =>
  withBytes(source, (sourcePtr, sourceLen) => withBytes(type, (typePtr, typeLen) =>
    api.joytube_should_block(urlPtr, urlLen, sourcePtr, sourceLen, typePtr, typeLen))));
assert.equal(check('https://www.google-analytics.com/analytics.js', 'https://example.org/'), 1);
assert.equal(check('https://www.google-analytics.com/analytics.js', 'https://www.youtube.com/watch?v=x'), 0);
assert.equal(check('https://www.googlevideo.com/videoplayback', 'https://example.org/', 'media'), 0);
console.log('WASM integration passed: EasyPrivacy tracker rule, YouTube source exemption, YouTube media destination exemption.');
