const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const extensionRoot = path.resolve(__dirname, '../../app/src/main/assets/joytube-privacy-filter');
const { pickQuality, QUALITY_RANK } = require(path.join(extensionRoot, 'media-controls-main.js'));
const { clampSettings, QUALITY_OPTIONS } = require(path.join(extensionRoot, 'media-controls.js'));

test('media content scripts are restricted to YouTube and quality code runs in page world', () => {
  const manifest = JSON.parse(fs.readFileSync(path.join(extensionRoot, 'manifest.json'), 'utf8'));
  const scripts = manifest.content_scripts;
  assert.ok(scripts.some(script => script.world === 'MAIN' && script.matches.includes('*://*.youtube.com/*')));
  assert.ok(scripts.some(script => script.js.includes('media-controls.js') && script.matches.includes('*://*.youtube.com/*')));
  assert.equal(scripts.some(script => script.matches.includes('<all_urls>')), false);
});

test('quality selection uses the exact available level or best lower fallback', () => {
  assert.equal(pickQuality(['hd1080', 'hd720', 'large'], 'hd720'), 'hd720');
  assert.equal(pickQuality(['hd720', 'large'], 'hd1080'), 'hd720');
  assert.equal(pickQuality(['large', 'medium'], 'hd720'), 'large');
  assert.equal(pickQuality(['large', 'medium'], 'highres'), 'large');
  assert.equal(pickQuality([], 'hd1080'), null);
  assert.equal(pickQuality(['hd1080'], 'auto'), null);
  assert.deepEqual(QUALITY_RANK.slice(0, 4), ['highres', 'hd2160', 'hd1440', 'hd1080']);
});

test('stored media settings are clamped and invalid preferences fall back safely', () => {
  assert.deepEqual(clampSettings({ quality: 'hd720', boost: 1.75 }), { quality: 'hd720', boost: 1.75 });
  assert.deepEqual(clampSettings({ quality: 'premium-1080p', boost: 8 }), { quality: 'auto', boost: 2.5 });
  assert.deepEqual(clampSettings({ quality: 'auto', boost: -1 }), { quality: 'auto', boost: 1 });
  assert.ok(QUALITY_OPTIONS.some(([key]) => key === 'hd2160'));
  assert.equal(QUALITY_OPTIONS.some(([key]) => key.includes('premium')), false);
});
