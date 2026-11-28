## Unreleased

### Fixed
- Android Auto Vollbild: Ein Tipp auf die Karte schickt den Bildschirm wieder an das Auto, damit die Knöpfe von Android Auto zurückkommen, die bei manchen Autos verschwunden blieben (dank joebGer19, #455). Bitte sag Bescheid, ob es hilft.
  EN: Android Auto full-screen: a tap on the map sends the screen to the car again, so Android Auto's own buttons come back; they stayed hidden on some cars (thanks to joebGer19, #455). Please tell me if it helps.

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
