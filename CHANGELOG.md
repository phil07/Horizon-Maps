## Unreleased

### Improved
- Fehlerberichte enthalten jetzt, was die letzten fünf Routen beendet hat: welcher Bildschirm, ob die Fahrt nach Hause, zur Arbeit oder woandershin ging (nie der Ort selbst) und welcher Teil der App sie beendet hat. Damit lässt sich eine Route finden, die sich von selbst abbricht. Danke an carlb051.
  EN: Problem reports now include what ended the last five routes: which screen, whether it went to Home, Work or elsewhere (never the place itself) and which part of the app ended it, to track down a route that cancels itself. Thanks to carlb051.
- Immer Nacht bei Tageslicht: Die Karte bleibt dunkel, aber Sterne, leuchtende Straßenlaternen und der Scheinwerferkegel kommen erst, wenn es draußen wirklich dunkel ist. Danke an bijindas.
  EN: Always night in daylight: the map stays dark, but the stars, lit street lamps and headlight beam only come on once it's actually dark outside. Thanks to bijindas.

### Fixed
- Absturz beim Antippen eines Suchtreffers, während oben in der Liste „Alle Standorte anzeigen“ stand. Danke an DJSatan.
  EN: Crash when tapping a search result while "See all locations" was showing at the top of the list. Thanks to DJSatan.

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
