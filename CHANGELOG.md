## Unreleased

### Behoben
- Durchschnittsgeschwindigkeits-Kontrollen außerhalb Großbritanniens: Das Limit einer Kontrolle wurde immer als britisch gelesen, aus „60“ (km/h) wurden 95 km/h. Es wird jetzt in den Einheiten des Landes gelesen, in dem die Kontrolle liegt, und die Karte zeigt nie ein Limit über dem eigenen Schild der Straße.
  EN: Average speed check limits outside the UK: a check's limit was always read as British, so one mapped as "60" (km/h) showed as 95 km/h. It's now read in the units of the country the check is in, and the card never shows a limit higher than the road's own signed limit.
- Verschwinden aus Android Auto nach einem Update: Ein Update aus der App heraus bittet Androids Installationsprogramm jetzt, Google Play als Installationsquelle einzutragen. Android Auto prüft das, bevor es eine App listet, die nicht aus dem Play Store stammt. Vorher konnte ein Update in der App Horizon Nav unbemerkt aus Android Auto nehmen (gefunden von marcocebrian). Problemberichte enthalten jetzt diese Installationsangabe.
  EN: Disappearing from Android Auto after an update: updating from inside the app now asks Android's installer to record Google Play as the installer, which is what Android Auto checks before listing an app from outside the Play Store. Before, an in-app update could quietly take Horizon Nav out of Android Auto (found by marcocebrian). Problem reports now include this install record.

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
