'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { hostIsYouTube, isYouTubeRequest } = require('../../app/src/main/assets/joytube-privacy-filter/background.js');

test('YouTube page and media hosts are exempt from privacy filtering', () => {
  for (const host of [
    'm.youtube.com',
    'www.youtube-nocookie.com',
    'youtu.be',
    'i.ytimg.com',
    'rr.googlevideo.com',
    'youtube.googleapis.com'
  ]) {
    assert.equal(hostIsYouTube(host), true, `${host} should be exempt`);
  }
});

test('requests initiated by a YouTube page remain exempt, including third-party hosts', () => {
  assert.equal(isYouTubeRequest({
    url: 'https://tracker.example/pixel',
    documentUrl: 'https://m.youtube.com/watch?v=abc',
    originUrl: 'https://m.youtube.com'
  }), true);
});

test('ordinary browsing requests remain eligible for the EasyPrivacy engine', () => {
  assert.equal(isYouTubeRequest({
    url: 'https://www.google-analytics.com/analytics.js',
    documentUrl: 'https://example.org/article',
    originUrl: 'https://example.org'
  }), false);
  assert.equal(hostIsYouTube('youtube.com.attacker.example'), false);
});
