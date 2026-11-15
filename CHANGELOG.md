## Unreleased

### Verbessert
- Tacho-Seite gilt auch auf Android Auto: Die Einstellung „Tacho-Seite“ (Einstellungen › Beim Fahren) bestimmt jetzt auch auf dem Autodisplay, ob Tacho und Tempolimit in der unteren Ecke rechts oder links stehen. „Automatisch“ verhält sich wie früher. Das Limit-Schild steht auch dort immer zur Bildschirmmitte hin.
  EN: Speed side also applies on Android Auto: the "Speed side" setting (Settings › While driving) now also decides on the car screen whether the speed and limit sit in the bottom right or bottom left corner. "Automatic" behaves as before. The limit sign always faces the middle of the screen there too.

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
