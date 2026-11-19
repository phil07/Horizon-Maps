## Unreleased

### Fixed
- Android Auto: Während der Fahrt liegen die Buttons oben links auf der Karte nicht mehr übereinander. Der 2D-Knopf ist dort weggefallen, die Tonstufe bleibt. 2D und 3D stellst du weiter in den Einstellungen oder auf dem Telefon um.
  EN: On Android Auto, the buttons at the top left of the map no longer overlap while driving. The 2D button is gone from there, the voice button stays. You still switch 2D and 3D in Settings or on the phone.

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
