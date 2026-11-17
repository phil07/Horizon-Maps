## Unreleased

### Neu
- Such-Taste auf der Fahrtkarte (Android Auto): Ein Tipp öffnet die Suche. Ein Ort, den du während der Fahrt wählst, wird gefragt: „Als Stopp“ oder „Hierhin“. Ausschaltbar unter Einstellungen › Android Auto › Knöpfe auf der Fahrtkarte. (Dank an yurividal)
  EN: Search button on the trip card (Android Auto): a tap opens the search. A place you pick while driving is asked about: "Add as a stop" or "Go here". Can be turned off under Settings › Android Auto › Buttons on the trip card. (Thanks to yurividal)
- Verknüpfungen auf dem Autodisplay: Die Reihe unter der Suche wie auf dem Handy, mit deinen Orten, Favoriten und Tracks sowie den Knöpfen „Meine Orte“ und „Meine Tracks“. (Dank an bijindas)
  EN: Shortcuts on Android Auto: the row under the search bar as on the phone, with your places, favourites and tracks and the "My places" and "My tracks" buttons. (Thanks to bijindas)

### Verbessert
- Such-Knöpfe einzeln ausschalten: Tanken, Laden, Essen, Kaffee, Parken, Stellplätze, Entsorgung und Autogas lassen sich einzeln unter Einstellungen › Orte & Fahrten › Verknüpfungen ein- und ausschalten. Ausgeschaltete kommen nicht von allein zurück.
  EN: Find buttons can be turned off one by one: fuel, charging, food, coffee, parking, pitches, water & waste and LPG, under Settings › Places & trips › Shortcuts. Turned-off ones don't come back on their own.

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
