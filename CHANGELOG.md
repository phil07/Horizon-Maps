## Unreleased

### Verbessert
- Ladeplanung unterwegs: Es werden nur noch die Ladesäulen auf dem Rest der Route abgefragt, nicht mehr die der ganzen Strecke. Das spart Datenabrufe und lässt mehr Platz für die Säulen, die noch vor dir liegen.
  EN: Charging plan on the way: only the chargers on the rest of the route are looked up, no longer those of the whole trip. That saves data and leaves more room for the chargers still ahead of you.
- Leistungsanzeige (Einstellungen › Leistungsanzeige): zeigt jetzt auch, wie lange die App für jede neue Position rechnet, und wie oft das Android-Auto-Bild neu aufgebaut wird.
  EN: Performance meter (Settings › Performance meter): now also shows how long the app works on each new position, and how often the Android Auto screen is laid out again.

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
