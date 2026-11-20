## Unreleased

### Improved
- Ortsnamen und Straßennamen auf der Karte folgen der Sprache der App statt der des Telefons.
  EN: Place and road names on the map follow the app's language instead of the phone's.
- Die Musik wird beim Ansagen höchstens 90 Sekunden leiser gehalten, auch wenn eine Ansage nicht fertig gemeldet wird.
  EN: Music is held down during announcements for at most 90 seconds, even when an announcement never reports that it has finished.

### Fixed
- Falsche Einfrier-Meldungen: Wenn das Telefon schlief oder die App im Hintergrund pausiert war, konnte der Problembericht ein „Einfrieren“ melden, das nie war. Das wird jetzt ignoriert.
  EN: False freeze reports: when the phone slept or the app was paused in the background, the problem report could record a freeze that never happened. These are now ignored.

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
