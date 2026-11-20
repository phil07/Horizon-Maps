## Unreleased

### Improved
- Eine Pfeilgröße: Der Pfeil hatte zwei Größen-Regler, „Pfeil“ und „Bildschirm & Größen“, die sich miteinander multipliziert haben. Jetzt gibt es nur noch „Pfeilgröße“. Wer beide verändert hatte, bekommt die verrechnete Größe, der Pfeil bleibt also so groß wie vorher.
  EN: One arrow size: the arrow had two size sliders, "Arrow" and "Screen & sizes", which multiplied each other. There's now just "Arrow size". If you had changed both, they are combined, so your arrow stays the size it was.
- Kartenquelle zeigt nur, was funktioniert: OpenStreetMap immer, Mapbox sobald ein Mapbox-Schlüssel eingetragen ist, TomTom sobald ein TomTom-Schlüssel eingetragen ist.
  EN: Map source only offers what works: OpenStreetMap always, Mapbox once a Mapbox key is entered, TomTom once a TomTom key is entered.
- Der Auto-Start-Countdown wird nur angezeigt, wenn der Auto-Start eingeschaltet ist.
  EN: The auto-start countdown only shows when auto-start is on.

### Fixed
- Suche mit Mapbox: Unter Schlüssel & Konten › Nutzung sucht Mapbox jetzt auch wirklich mit Mapbox. Die eigene Einstellung „Suchen mit“, die davon abweichen konnte, ist entfernt.
  EN: Search with Mapbox: under Keys & accounts › Nutzung, Mapbox now searches with Mapbox too. The separate "Search with" setting, which could disagree with it, is gone.

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
