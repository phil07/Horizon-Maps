## Unreleased

### Verbessert
- Fahrtkarte: Der farbige Fortschrittsbalken hat runde Enden.
  EN: Trip card: the coloured progress bar has rounded ends.
- Letzte Orte unter der Suche: Alle zuletzt genutzten Orte stehen in der Liste, die scrollt. Der Schalter „Alle“ / „Weniger“ entfällt.
  EN: Recent places under the search bar: all of them are in the list, which scrolls. The "All" / "Less" switch is gone.
- Scheinwerferkegel unter dem 3D-Auto: Der Kegel beginnt unter dem 3D-Auto und ragt nicht mehr über sein Dach. Er flackert nicht mehr. Auf einer Route geht er an, sobald du losfährst, und bleibt bis zum Ziel an, auch wenn du an Ampeln hältst. Ohne Route zeigt er sich, während du fährst. (Dank an liamstears)
  EN: Headlight beam under the 3D car: the beam starts under the 3D car and no longer shines over its roof, and it no longer flickers. On a route it comes on once you first move off and stays on to the end, even when you stop at lights; with no route it shows while you are moving. (Thanks to liamstears)

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
