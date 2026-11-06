# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Behoben
- Doppelte Verkehrsabfragen: Mit Android Auto haben Handy und Auto die Verkehrsmeldungen beide alle 3 Minuten bei TomTom abgefragt. Jetzt fragt nur noch das Auto, solange die Handy-App im Hintergrund ist.
- Sperrung ohne Antwort: Fand die Neuberechnung um eine Sperrung keine Route (kein Netz), wurde diese Sperrung nie wieder umfahren. Jetzt versucht es die App bei der nächsten Verkehrsprüfung erneut.
- Live-Alternativen im Stand: Bei einer Pause mit laufender Route wird nur noch alle 6 Minuten gesucht, nicht alle 2 (spart Abfragen bei TomTom und Mapbox).
- Umweltzonen: Eine leere Antwort des Kartenservers hat die bekannten Zonen für 30 Tage gelöscht. Jetzt bleiben sie und die App fragt am nächsten Tag erneut.

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
