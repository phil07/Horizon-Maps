## Unreleased

### Neu
- Was ist neu, jederzeit: Unter Einstellungen › Über & Sicherung gibt es jetzt einen Knopf, der die Neuerungen der installierten Version wieder einblendet. (Dank an leedank93)
  EN: What's new, any time: under Settings › About & backup there is now a button that brings back the changes of the installed version. (Thanks to leedank93)

### Behoben
- Pfeil auf der falschen Fahrbahn: Ohne laufende Route konnte der Pfeil auf die Gegenfahrbahn springen, wenn das GPS zur Mitte der Straße wanderte. Er bleibt jetzt auf der Seite, auf der du fährst, auch bevor die Tempolimit-Straßen geladen sind. (Dank an leedank93)
  EN: Arrow on the wrong side of a divided road: with no route running, the arrow could jump to the other carriageway when the GPS drifted towards the middle. It now keeps to the side you are driving on, even before the speed-limit roads have loaded. (Thanks to leedank93)

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
