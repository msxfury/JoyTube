browser.webRequest.onBeforeRequest.addListener(
  details => details.url.endsWith("/blocked-resource") ? { cancel: true } : {},
  { urls: ["http://127.0.0.1/*"] },
  ["blocking"]
);
