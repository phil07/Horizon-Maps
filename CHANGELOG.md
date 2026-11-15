## Unreleased

### Verbessert
- Kartenvorschau in den Einstellungen auf dem Handy: Auf den Kartenseiten (Kartenstil, Auf der Karte, Pfeil) bleibt die Live-Karte oben stehen, während du in den Einstellungen scrollst. So siehst du jede Änderung, sobald du sie machst.
  EN: Map preview in Settings on a phone: on the map pages (Map style, Light & 3D, On the map, Your arrow) the live preview now stays at the top while you scroll the settings, as it does in landscape, so you can see each change as you make it

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
