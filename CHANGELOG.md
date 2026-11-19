## Unreleased

### Neu
- Spanien: Live-Meldungen der DGT. Unfälle, Baustellen, Sperrungen, Gefahren und die vernetzten V16-Warnbaken liegengebliebener Autos erscheinen auf der Karte im Umkreis von 10 km und werden auf deiner Route angesagt wie die TomTom-Meldungen. Kostenlos, ohne Schlüssel, auch ohne TomTom-Schlüssel. Was TomTom schon meldet, kommt nicht doppelt. Katalonien und das Baskenland veröffentlichen ihre Meldungen getrennt und sind noch nicht dabei. Danke an franciscoserranoadt.
  EN: Spain: live incidents from the DGT. Accidents, roadworks, closures, hazards and the connected V16 beacons of broken-down cars show on the map within 10 km and are announced on your route like TomTom's. Free, no key, and they work without a TomTom key too. What TomTom already reports isn't repeated. Catalonia and the Basque Country publish theirs separately and aren't included yet. Thanks to franciscoserranoadt.

## 1.0.246 — 2026-10-28

### Improved in-app updates
- Show the installed and available app versions in the update prompt.
- Display release notes in the update prompt when the release provides them.
- Show download progress as a percentage and downloaded/total megabytes.
- Improve update failure messages, including checksum verification failures, timeouts, and cancellations.
- Only open the APK in a browser when the device has no download service, avoiding a browser fallback that could bypass checksum verification after other download failures.
- Keep update detection working if `version.json` is missing or malformed by falling back to the version number in the release title or notes.

## Historical versions

Earlier builds were published by replacing assets on the single `app` release. Their original release notes and assets may not be recoverable from GitHub Releases. The new versioned-release history starts with the first successful publish using this workflow.
