## Unreleased

### Improved
- Akku: Auf dem Handy geht der Bildschirm wie üblich aus, solange du nur die Karte ansiehst. Er bleibt nur an, während eine Route geführt wird. Head Units bleiben wie bisher an.
  EN: Battery: on a phone the screen goes off as usual while you only look at the map, and stays on only while a route is guided. Head units stay on as before.

### Fixed
- Die Karte „nächste 3 km“ wird nicht mehr hoch: Die Angabe bleibt in einer Zeile, und ein langer Name wird stattdessen gekürzt.
  EN: The "next 3 km" hazard card no longer grows tall: the distance stays on one line, and a long name is cut short instead.

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
