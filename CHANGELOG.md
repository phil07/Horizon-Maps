## Unreleased

### Neu
- Autogas (LPG) als Kraftstoff: Preise gibt es in Frankreich. Sonst zeigt die Liste die Tankstellen, die in OpenStreetMap als Autogas-Verkauf eingetragen sind. (Dank an TheDMAI)
  EN: LPG (Autogas) as a fuel: prices show in France. Elsewhere the list shows the stations mapped in OpenStreetMap as selling LPG. (Thanks to TheDMAI)

### Behoben
- Satellitenansicht auf dem Autodisplay: Das Autodisplay zeigt die Satellitenansicht, wenn sie eingeschaltet ist, statt immer die gezeichnete Karte. Fehlerberichte nennen jetzt auch, ob der Satellit an ist, und den letzten Fehler der Karte.
  EN: Satellite view on Android Auto: the car screen shows satellite view when it is switched on, instead of always the drawn map. Feedback reports now also say whether satellite is on and the last error the map gave.

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
