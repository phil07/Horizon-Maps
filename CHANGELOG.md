## Unreleased

### Neu
- Ortskarte mit mehr auf einen Blick: Tippst du auf ein Geschäft oder Suchergebnis, zeigt die Karte, ob es geöffnet ist, wie weit es ist, eine ungefähre Fahrzeit und die Ankunftszeit. Dazu Los, Als Stopp, Speichern, Anrufen, Website und Teilen. Anrufen und Website blenden sich während der Fahrt aus. (Dank an Dry-Secretary2456)
  EN: Place card with more at a glance: tap a shop or search result and the card shows whether it's open, how far it is, a rough drive time and the arrival time. Go, Add stop, Save, Call, Website and Share are there too. Call and Website hide while you're moving. (thanks to Dry-Secretary2456)

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
