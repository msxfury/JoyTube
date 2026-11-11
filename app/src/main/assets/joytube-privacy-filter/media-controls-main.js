/*
 * YouTube quality-control bridge adapted from the approach in:
 * Leta Change YouTube Quality, Copyright (c) LetaLab, MIT License.
 * JoyTube adds no stream extraction, premium format access, or ad filtering.
 */
(function () {
  'use strict';

  const SETTINGS_EVENT = 'joytube-media-settings-v1';
  const STATUS_EVENT = 'joytube-media-status-v1';
  const QUALITY_RANK = ['highres', 'hd2160', 'hd1440', 'hd1080', 'hd720', 'large', 'medium', 'small', 'tiny'];
  const QUALITY_LABELS = {
    highres: '4320p', hd2160: '2160p', hd1440: '1440p', hd1080: '1080p',
    hd720: '720p', large: '480p', medium: '360p', small: '240p', tiny: '144p'
  };
  const audioGraphs = new WeakMap();
  let currentSettings = { quality: 'auto', boost: 1 };
  let audioContext = null;
  let qualityTimer = null;
  let lastQualityAttempt = '';

  function status(type, message) {
    document.dispatchEvent(new CustomEvent(STATUS_EVENT, {
      detail: JSON.stringify({ type, message }),
      bubbles: false
    }));
  }

  function parseSettings(detail) {
    try {
      const value = JSON.parse(detail);
      const quality = QUALITY_RANK.includes(value.quality) || value.quality === 'auto' ? value.quality : 'auto';
      const boost = Number(value.boost);
      return { quality, boost: Number.isFinite(boost) ? Math.max(1, Math.min(2.5, boost)) : 1 };
    } catch (_) {
      return null;
    }
  }

  function pickQuality(available, preferred) {
    if (!Array.isArray(available) || available.length === 0 || preferred === 'auto') return null;
    if (preferred === 'highres') return available[0];
    if (available.includes(preferred)) return preferred;
    const requestedIndex = QUALITY_RANK.indexOf(preferred);
    for (let index = requestedIndex + 1; index < QUALITY_RANK.length; index += 1) {
      if (available.includes(QUALITY_RANK[index])) return QUALITY_RANK[index];
    }
    return available[0];
  }

  if (typeof document === 'undefined') {
    if (typeof module !== 'undefined' && module.exports) {
      module.exports = { pickQuality, QUALITY_RANK, QUALITY_LABELS };
    }
    return;
  }

  function getPlayer() {
    return document.getElementById('movie_player') || document.querySelector('.html5-video-player');
  }

  function applyQuality() {
    const preferred = currentSettings.quality;
    if (preferred === 'auto') return false;
    const player = getPlayer();
    if (!player || typeof player.getAvailableQualityLevels !== 'function') return false;
    let available;
    try { available = player.getAvailableQualityLevels(); } catch (_) { return false; }
    const chosen = pickQuality(available, preferred);
    if (!chosen) return false;
    try {
      const canSetRange = typeof player.setPlaybackQualityRange === 'function';
      const canSetQuality = typeof player.setPlaybackQuality === 'function';
      if (!canSetRange && !canSetQuality) return false;
      if (canSetRange) player.setPlaybackQualityRange(chosen, chosen);
      if (canSetQuality) player.setPlaybackQuality(chosen);
      const label = QUALITY_LABELS[chosen] || chosen;
      const wanted = QUALITY_LABELS[preferred] || preferred;
      status('quality', chosen === preferred ? `Requested ${label}` : `Requested ${wanted}; using available ${label}`);
      return true;
    } catch (_) {
      return false;
    }
  }

  function scheduleQuality() {
    if (qualityTimer) clearInterval(qualityTimer);
    if (currentSettings.quality === 'auto') return;
    let attempts = 0;
    qualityTimer = setInterval(() => {
      attempts += 1;
      if (applyQuality() || attempts >= 20) {
        clearInterval(qualityTimer);
        qualityTimer = null;
        if (attempts >= 20) status('quality', 'YouTube did not expose a selectable quality for this video');
      }
    }, 400);
  }

  function getAudioContext() {
    if (audioContext) return audioContext;
    const AudioContextConstructor = window.AudioContext || window.webkitAudioContext;
    if (!AudioContextConstructor) return null;
    try {
      audioContext = new AudioContextConstructor();
      return audioContext;
    } catch (_) {
      return null;
    }
  }

  function safeForWebAudio(video) {
    const source = video.currentSrc || video.src;
    if (!source) return false;
    try {
      const parsed = new URL(source, location.href);
      if (parsed.origin === location.origin) return true;
      return video.crossOrigin === 'anonymous' || video.crossOrigin === 'use-credentials';
    } catch (_) {
      return false;
    }
  }

  async function graphFor(video) {
    const existing = audioGraphs.get(video);
    if (existing) return existing;
    if (!safeForWebAudio(video)) return null;
    const context = getAudioContext();
    if (!context) return null;
    try {
      const source = context.createMediaElementSource(video);
      const gain = context.createGain();
      const limiter = context.createDynamicsCompressor();
      limiter.threshold.value = -3;
      limiter.knee.value = 6;
      limiter.ratio.value = 12;
      limiter.attack.value = 0.003;
      limiter.release.value = 0.25;
      source.connect(gain);
      gain.connect(limiter);
      limiter.connect(context.destination);
      const graph = { gain };
      audioGraphs.set(video, graph);
      return graph;
    } catch (_) {
      return null;
    }
  }

  async function applyBoost() {
    const boost = currentSettings.boost;
    if (boost <= 1) {
      for (const video of document.querySelectorAll('video,audio')) {
        const graph = audioGraphs.get(video);
        if (graph) graph.gain.gain.setTargetAtTime(1, audioContext.currentTime, 0.04);
      }
      status('volume', 'Boost off (100%)');
      return;
    }
    const videos = Array.from(document.querySelectorAll('video,audio')).filter(video => !video.paused || video.readyState > 0);
    if (videos.length === 0) {
      status('volume', 'Start a YouTube video, then set the boost');
      return;
    }
    const context = getAudioContext();
    if (context && context.state === 'suspended') {
      try { await context.resume(); } catch (_) { /* Report below if still unavailable. */ }
    }
    if (!context || context.state !== 'running') {
      status('volume', 'Audio boost could not start; YouTube volume is unchanged');
      return;
    }
    let routed = 0;
    for (const video of videos) {
      const graph = await graphFor(video);
      if (!graph) continue;
      graph.gain.gain.setTargetAtTime(boost, audioContext.currentTime, 0.04);
      routed += 1;
    }
    if (routed > 0 && audioContext && audioContext.state === 'running') {
      status('volume', `Boost ${Math.round(boost * 100)}%`);
    } else {
      status('volume', 'Boost unavailable for this stream; YouTube volume is unchanged');
    }
  }

  function applySettings() {
    if (currentSettings.quality === 'auto') {
      status('quality', 'YouTube Auto selected; reload if a previously fixed quality remains');
    }
    scheduleQuality();
    void applyBoost();
  }

  document.addEventListener(SETTINGS_EVENT, event => {
    const settings = parseSettings(event.detail);
    if (!settings) return;
    currentSettings = settings;
    applySettings();
  });

  // A real pointer gesture is the safest time for Gecko to resume Web Audio.
  window.addEventListener('pointerdown', () => {
    if (currentSettings.boost <= 1) return;
    const context = getAudioContext();
    if (context && context.state === 'suspended') {
      void context.resume().then(() => void applyBoost());
    }
  }, true);

  document.addEventListener('yt-navigate-finish', () => {
    lastQualityAttempt = location.href;
    setTimeout(applySettings, 250);
  });
  document.addEventListener('yt-page-data-updated', () => {
    if (lastQualityAttempt !== location.href) {
      lastQualityAttempt = location.href;
      setTimeout(applySettings, 250);
    }
  });
  document.addEventListener('loadedmetadata', event => {
    if (event.target && ['VIDEO', 'AUDIO'].includes(event.target.tagName)) setTimeout(() => void applyBoost(), 100);
  }, true);
  document.addEventListener('fullscreenchange', () => setTimeout(scheduleQuality, 300));

})();
