## Unreleased

### Neu
- Android Auto: deine gespeicherten Orte unter der Suchleiste: Zuhause und Arbeit kommen Favoriten und Sternorte dazu, so viele wie passen, mit Alle für den Rest. Die Liste darunter zeigt jetzt nur noch zuletzt besuchte Orte, damit nichts doppelt erscheint.
  EN: Android Auto: your saved places under the search bar: Home and Work are joined by your starred places and favourites, as many as fit, with All for the rest. The list below now shows recent places only, so nothing appears twice.

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
