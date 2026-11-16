## Unreleased

### Verbessert
- Die Suche zeigt Vorschläge schneller: Orte aus dem Netz erscheinen, sobald sie da sind. Geschäfte und Ketten in der Nähe kommen dazu, wenn sie gefunden sind, statt alles auf sie warten zu lassen.
  EN: Search shows suggestions sooner: places from the online search appear as soon as they arrive. Shops and chains nearby join them when found, instead of everything waiting for them.

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
