## Unreleased

### Added
- Einstellungen › Karte: Ein optionaler Hinweis zu den HD-Spurdaten. Er ist aus und erscheint nur im Demo-Modus. Die Spurführung selbst bleibt unverändert.
  EN: Settings › Map: an optional HD lane data note. It is off and only shows in Demo mode. The lane guidance itself is unchanged.

### Improved
- TomTom-Karte: Übersetzte Orts- und Straßennamen in der gewählten Sprache der App. Wo es keine Übersetzung gibt, bleibt der Originalname.
  EN: TomTom map labels: translated place and road names in the app's selected language. Where there is no translation, the original name stays.

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
