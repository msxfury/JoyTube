/* JoyTube's isolated-world YouTube controls. Only runs on youtube.com pages. */
(function () {
  'use strict';

  const SETTINGS_KEY = 'joytubeYouTubeMediaSettings';
  const SETTINGS_EVENT = 'joytube-media-settings-v1';
  const STATUS_EVENT = 'joytube-media-status-v1';
  const DEFAULTS = { quality: 'auto', boost: 1 };
  const QUALITY_OPTIONS = [
    ['auto', 'YouTube Auto'],
    ['highres', 'Highest available'],
    ['hd2160', '2160p'],
    ['hd1440', '1440p'],
    ['hd1080', '1080p'],
    ['hd720', '720p'],
    ['large', '480p'],
    ['medium', '360p'],
    ['small', '240p']
  ];

  function clampSettings(value) {
    const allowedQuality = new Set(QUALITY_OPTIONS.map(([key]) => key));
    const quality = allowedQuality.has(value && value.quality) ? value.quality : DEFAULTS.quality;
    const boost = Number(value && value.boost);
    return {
      quality,
      boost: Number.isFinite(boost) ? Math.max(1, Math.min(2.5, boost)) : DEFAULTS.boost
    };
  }

  if (typeof document === 'undefined') {
    if (typeof module !== 'undefined' && module.exports) {
      module.exports = { clampSettings, QUALITY_OPTIONS };
    }
    return;
  }

  function fireSettings(settings) {
    document.dispatchEvent(new CustomEvent(SETTINGS_EVENT, {
      detail: JSON.stringify(clampSettings(settings)),
      bubbles: false
    }));
  }

  async function readSettings() {
    try {
      const stored = await browser.storage.local.get(SETTINGS_KEY);
      return clampSettings(stored[SETTINGS_KEY]);
    } catch (_) {
      return { ...DEFAULTS };
    }
  }

  async function saveSettings(settings) {
    const clean = clampSettings(settings);
    try { await browser.storage.local.set({ [SETTINGS_KEY]: clean }); } catch (_) { /* Current session still works. */ }
    fireSettings(clean);
    return clean;
  }

  function makePanel() {
    if (document.getElementById('joytube-media-tools-host')) return null;
    const host = document.createElement('div');
    host.id = 'joytube-media-tools-host';
    host.style.cssText = 'all:initial;position:fixed;z-index:2147483647;top:8px;right:8px;pointer-events:auto;';
    const shadow = host.attachShadow({ mode: 'open' });
    shadow.innerHTML = `
      <style>
        :host { all: initial; font-family: Roboto,Arial,sans-serif; color-scheme: dark; }
        * { box-sizing: border-box; }
        button, select, input { font: inherit; }
        .toggle { width:42px;height:42px;border:1px solid #ffffff40;border-radius:50%;background:#17191ee8;color:#fff;font-size:13px;font-weight:800;box-shadow:0 3px 14px #0008; }
        .panel { display:none;width:min(260px,calc(100vw - 24px));margin-top:8px;padding:14px;border:1px solid #ffffff30;border-radius:16px;background:#17191ef5;box-shadow:0 8px 28px #000a;color:#fff; }
        .panel.open { display:block; }
        .title { margin:0 0 12px;font-size:14px;font-weight:700; }
        .field { display:block;margin:10px 0 5px;color:#d8dce5;font-size:12px; }
        .row { display:flex;align-items:center;gap:10px; }
        input[type=range] { flex:1;min-width:0;accent-color:#fff; }
        select { width:100%;padding:9px;border:1px solid #ffffff38;border-radius:9px;background:#242832;color:#fff; }
        .value { min-width:46px;text-align:right;font-size:12px;font-variant-numeric:tabular-nums; }
        .status { min-height:14px;margin:8px 0 0;color:#b9c0cc;font-size:11px;line-height:1.35; }
        .hint { margin:7px 0 0;color:#939baa;font-size:10px;line-height:1.35; }
        button:focus-visible,select:focus-visible,input:focus-visible { outline:2px solid #fff;outline-offset:2px; }
      </style>
      <button class="toggle" type="button" aria-label="JoyTube media controls" aria-expanded="false"><svg aria-hidden="true" viewBox="0 0 24 24" width="21" height="21" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M11 5 6 9H3v6h3l5 4z"/><path d="M15.5 8.5a5 5 0 0 1 0 7M18.5 5.5a9 9 0 0 1 0 13"/></svg></button>
      <section class="panel" aria-label="JoyTube media controls">
        <p class="title">JoyTube media</p>
        <label class="field quality-control" for="jt-quality">Video quality</label>
        <select class="quality-control" id="jt-quality"></select>
        <label class="field" for="jt-boost">Volume boost</label>
        <div class="row"><input id="jt-boost" type="range" min="100" max="250" step="5" value="100"><output class="value">100%</output></div>
        <p class="status quality-status quality-control" role="status" aria-live="polite">Quality: YouTube Auto</p>
        <p class="status volume-status" role="status" aria-live="polite">Volume boost: 100%</p>
        <p class="hint">Quality can only use options YouTube makes available. Boost may be unavailable for some streams.</p>
      </section>`;
    document.documentElement.appendChild(host);

    const toggle = shadow.querySelector('.toggle');
    const panel = shadow.querySelector('.panel');
    const quality = shadow.querySelector('#jt-quality');
    const boost = shadow.querySelector('#jt-boost');
    const output = shadow.querySelector('output');
    const qualityStatus = shadow.querySelector('.quality-status');
    const volumeStatus = shadow.querySelector('.volume-status');
    if (location.hostname === 'music.youtube.com') {
      shadow.querySelectorAll('.quality-control').forEach(element => { element.hidden = true; });
    }
    for (const [key, label] of QUALITY_OPTIONS) {
      const option = document.createElement('option');
      option.value = key;
      option.textContent = label;
      quality.appendChild(option);
    }

    toggle.addEventListener('click', () => {
      const open = !panel.classList.contains('open');
      panel.classList.toggle('open', open);
      toggle.setAttribute('aria-expanded', String(open));
    });

    let settings = { ...DEFAULTS };
    quality.addEventListener('change', async () => {
      settings = await saveSettings({ ...settings, quality: quality.value });
      qualityStatus.textContent = quality.value === 'auto'
        ? 'YouTube Auto selected. Reload the video if a previous fixed quality remains.'
        : 'Applying YouTube’s available quality…';
    });
    boost.addEventListener('input', async () => {
      const value = Number(boost.value) / 100;
      output.value = `${boost.value}%`;
      output.textContent = `${boost.value}%`;
      settings = await saveSettings({ ...settings, boost: value });
    });

    document.addEventListener(STATUS_EVENT, event => {
      try {
        const message = JSON.parse(event.detail);
        if (message && typeof message.message === 'string') {
          const target = message.type === 'quality' ? qualityStatus : message.type === 'volume' ? volumeStatus : null;
          if (target) target.textContent = message.message;
        }
      } catch (_) { /* Ignore malformed page events. */ }
    });

    void readSettings().then(value => {
      settings = value;
      quality.value = value.quality;
      boost.value = String(Math.round(value.boost * 100));
      output.value = `${boost.value}%`;
      output.textContent = `${boost.value}%`;
      fireSettings(settings);
    });
    return host;
  }

  function mount() {
    if (!document.documentElement) {
      document.addEventListener('DOMContentLoaded', mount, { once: true });
      return;
    }
    makePanel();
  }

  mount();

})();
