## Unreleased

### Neu
- Ankunftszeiten mit dem üblichen Verkehr: Routen aus der heruntergeladenen Karte (Gratisversion oder ohne Empfang) rechnen jetzt die Staus ein, die zu dieser Tageszeit üblich sind, mehr zu den Stoßzeiten werktags und in Städten als auf der Autobahn. Die Berechnung läuft auf dem Telefon, nichts wird gesendet.
  EN: Arrival times that know the usual traffic: routes planned from your downloaded map (the free version, or anywhere without signal) now allow for the hold-ups that are normal at that time of day, more in the weekday rush hours and in towns than on motorways. Worked out on your phone, with nothing sent anywhere.

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
