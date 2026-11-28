## Unreleased

### Improved
- Schrift des Fahrerdisplays: standardmäßig größer, und in den Android-Auto-Einstellungen gibt es „Schrift des Fahrerdisplays“ (Klein, Normal, Groß, Sehr groß).
  EN: Driver's display writing is bigger by default, and Android Auto settings have a "Driver's display text size" (Smaller, Normal, Larger, Largest).
- Handy-Akku am Tacho: größere Zahl. Grün beim Laden, gelb unter 30 %, rot unter 15 %.
  EN: The phone battery badge on the car screen has a bigger number: green while charging, amber under 30 %, red under 15 %.
- Kreuzungszoom ist auch in den Android-Auto-Einstellungen (dank joel941703-hash, #451).
  EN: Junction zoom is also in the Android Auto settings (thanks to joel941703-hash, #451).

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
