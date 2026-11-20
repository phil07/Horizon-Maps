## Unreleased

### New
- Android Auto: Sound-Knopf auf der Karte, wenn nicht navigiert, lässt sich unter Einstellungen › Android Auto › Auf der Karte ausschalten. Dann erscheint er nur während der Navigation, für mehr Platz.
  EN: Android Auto: the sound button on the map, when not navigating, can be switched off under Settings › Android Auto › On the map. Then it shows only while navigating, for more room.

### Improved
- Android Auto: Die Zeile „Dann …“ bricht bei einem langen Straßennamen auf eine zweite Zeile um, bevor sie abgeschnitten wird.
  EN: Android Auto: the "Then …" line wraps a long street name onto a second line before it is cut short.
- Android Auto: Sterne und Straßenlaternen bei Tag warten im Auto mit dunklem Bildschirm auf den Sonnenuntergang, wie auf dem Handy.
  EN: Android Auto: stars and street lamps by day wait for sunset with a car whose screen stays dark, as on the phone.
- Android Auto: Der Abbiegepfeil für das Armaturenbrett ist wieder der einfache Pfeil für jede Art von Abbiegung. Das Bild davon ließ die Abbiegung auf manchen Displays verschwinden.
  EN: Android Auto: the turn arrow for the dashboard is the plain arrow for each kind of turn again. The picture of it made the turn disappear on some displays.

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
