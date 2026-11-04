# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Verbessert
- Ladeplanung: Die Routen der Vorschau werden gleichzeitig statt nacheinander geplant und die Wartezeit ist deutlich kürzer. Der Plan der gewählten Route wird nicht noch einmal berechnet. Auch innerhalb einer Route laufen Höhenprofil, TomTom und die Ladesäulen-Register jetzt gleichzeitig.
- Startbildschirm: Das grüne Vorschlagsfeld mit der letzten oder vermuteten Route („Berlin · 5 h 49 min · Los“) entfällt.
- Elektro-Routenvorschau: Die Routenkarten erscheinen sofort. Zeit, Ankunft und Ladestopps stehen zuerst als grobe Schätzung („ca.“) da und werden ersetzt, sobald die echte Ladeplanung fertig ist. Der Start-Countdown wartet darauf.
- Gespeichert & zuletzt: Unter häufigen Zielen steht jetzt die Adresse statt „Meist gegen 12:02“, auch bei schon gespeicherten Zielen.


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
