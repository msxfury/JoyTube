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

  function isVideoRoute(pathname) {
    return /^\/(?:watch|shorts(?:\/|$)|live(?:\/|$)|embed\/)/.test(pathname || '');
  }

  if (typeof document === 'undefined') {
    if (typeof module !== 'undefined' && module.exports) {
      module.exports = { clampSettings, QUALITY_OPTIONS, isVideoRoute };
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
        .toolbar { display:flex;gap:7px; }
        .tool { display:grid;place-items:center;width:42px;height:42px;border:1px solid #ffffff40;border-radius:50%;background:#17191ee8;color:#fff;box-shadow:0 3px 14px #0008; }
        .tool svg { width:21px;height:21px; }
        .panel { display:none;width:min(260px,calc(100vw - 24px));margin-top:8px;padding:14px;border:1px solid #ffffff30;border-radius:16px;background:#17191ef5;box-shadow:0 8px 28px #000a;color:#fff; }
        .panel.open { display:block; }
        .panel [hidden] { display:none !important; }
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
      <div class="toolbar" role="toolbar" aria-label="JoyTube video controls">
        <button class="tool volume-button" type="button" aria-label="Volume boost" title="Volume boost"><svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M11 5 6 9H3v6h3l5 4z"/><path d="M15.5 8.5a5 5 0 0 1 0 7M18.5 5.5a9 9 0 0 1 0 13"/></svg></button>
        <button class="tool quality-button" type="button" aria-label="Video quality" title="Video quality"><svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M4 6h16M4 12h16M4 18h16"/><path d="M9 4v4m7 2v4m-4 2v4"/></svg></button>
        <button class="tool download-button" type="button" aria-label="Download unavailable for YouTube playback" title="YouTube does not provide JoyTube a downloadable media file"><svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3v12m-5-5 5 5 5-5"/><path d="M5 16v4h14v-4"/></svg></button>
      </div>
      <section class="panel" aria-label="JoyTube media controls">
        <p class="title">JoyTube media</p>
        <div class="quality-section">
          <label class="field" for="jt-quality">Video quality</label>
          <select id="jt-quality"></select>
          <p class="status quality-status" role="status" aria-live="polite">Quality: YouTube Auto</p>
        </div>
        <div class="volume-section">
          <label class="field" for="jt-boost">Volume boost</label>
          <div class="row"><input id="jt-boost" type="range" min="100" max="250" step="5" value="100"><output class="value">100%</output></div>
          <p class="status volume-status" role="status" aria-live="polite">Volume boost: 100%</p>
        </div>
        <p class="status download-status" role="status" aria-live="polite" hidden></p>
        <p class="hint">Quality uses options exposed by YouTube. Boost is limited to streams GeckoView can route through Web Audio. JoyTube cannot download YouTube playback.</p>
      </section>`;
    document.documentElement.appendChild(host);

    const toolbar = shadow.querySelector('.toolbar');
    const volumeButton = shadow.querySelector('.volume-button');
    const qualityButton = shadow.querySelector('.quality-button');
    const downloadButton = shadow.querySelector('.download-button');
    const panel = shadow.querySelector('.panel');
    const qualitySection = shadow.querySelector('.quality-section');
    const volumeSection = shadow.querySelector('.volume-section');
    const downloadStatus = shadow.querySelector('.download-status');
    const quality = shadow.querySelector('#jt-quality');
    const boost = shadow.querySelector('#jt-boost');
    const output = shadow.querySelector('output');
    const qualityStatus = shadow.querySelector('.quality-status');
    const volumeStatus = shadow.querySelector('.volume-status');
    if (location.hostname === 'music.youtube.com') {
      qualityButton.hidden = true;
      qualitySection.hidden = true;
    }
    for (const [key, label] of QUALITY_OPTIONS) {
      const option = document.createElement('option');
      option.value = key;
      option.textContent = label;
      quality.appendChild(option);
    }

    function showPanel(section) {
      panel.classList.add('open');
      qualitySection.hidden = section !== 'quality' || location.hostname === 'music.youtube.com';
      volumeSection.hidden = section !== 'volume';
      downloadStatus.hidden = true;
    }
    volumeButton.addEventListener('click', () => showPanel('volume'));
    qualityButton.addEventListener('click', () => showPanel('quality'));
    downloadButton.addEventListener('click', () => {
      panel.classList.add('open');
      qualitySection.hidden = true;
      volumeSection.hidden = true;
      downloadStatus.hidden = false;
      downloadStatus.textContent = 'YouTube does not provide JoyTube a downloadable media file. Import media you have rights to into JoyTube to use its device download tools.';
    });

    function updateVisibility() {
      const hasVideo = Boolean(document.querySelector('#movie_player video, ytd-player video, ytd-reel-video-renderer[is-active] video, video.html5-main-video'));
      toolbar.hidden = !isVideoRoute(location.pathname) || !hasVideo;
      if (toolbar.hidden) panel.classList.remove('open');
    }
    const pageObserver = new MutationObserver(updateVisibility);
    pageObserver.observe(document.documentElement, { childList: true, subtree: true });
    document.addEventListener('yt-navigate-finish', updateVisibility);
    document.addEventListener('loadedmetadata', updateVisibility, true);
    document.addEventListener('playing', updateVisibility, true);
    updateVisibility();

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
