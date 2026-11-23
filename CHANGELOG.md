## Unreleased

### New
- Meine Routen: Auf der Ankunftskarte speichert „Als meine Route speichern“ die Straßen, die du wirklich gefahren bist (auf dem Handy mit Namen, auf Android Auto als „Zuhause → Arbeit“). Fährst du wieder von dort nach dort, steht sie in der Routenwahl zuerst und ausgewählt als „Deine Route“, mit dem Vergleich zur schnellsten. Mehrere für dieselbe Fahrt: Die zur jetzigen Uhrzeit passende kommt zuerst. Auf ihr führt eine Umleitung zurück auf die Route, und es kommen keine Hinweise auf schnellere Routen. Einstellungen › Orte & Fahrten › Meine Routen zum Anzeigen, Umbenennen, Löschen und um zu wählen, ob sie angeboten, sofort gestartet oder nicht angeboten wird. Sie sind auch im Backup.
  EN: My routes: on the arrival card "Save as my route" keeps the roads you really took (named on the phone, as "Home → Work" on Android Auto). Next time you go from there to there it's first and picked in the route choice as "Your route", with how it compares with the fastest. Several for the same trip: the one for this time of day comes first. Driving it, a reroute brings you back onto it and no faster routes are offered. Settings › Places & trips › My routes to show, rename or delete them and choose whether each is offered, started straight away or not offered. They're in backups too.
- Tempolimit-Töne einzeln: Einstellungen › Warnungen › Tempolimit-Töne schaltet nur Töne und Stimme zum Tempolimit aus (zu schnell, Wechsel). Blitzer, Gefahren und andere Warnungen bleiben.
  EN: Speed limit sounds on their own: Settings › Alerts › Speed limit sounds turns off just the speed limit's beeps and voice (over the limit, and when it changes); cameras, hazards and other alerts still sound.
- Das Pop-up beim Tempowechsel lässt sich abschalten (Einstellungen › Warnungen). Das Schild am Tacho pulsiert weiter.
  EN: The pop-up when the limit changes can be switched off (Settings › Alerts); the sign by the speedometer still pulses.

### Fixed
- Nachts dimmen dimmt jetzt wirklich: die Hälfte der aktuellen Bildschirmhelligkeit, nie heller. Bisher waren es feste 50 %, heller als die meisten Bildschirme nachts.
  EN: Dim the screen at night now really dims: half of what the screen is at, never brighter. It used to be a fixed 50 %, brighter than most screens at night.
- Kein Regen auf der Karte mehr bei Sonne: Klarer Himmel zählt als trocken, und für Regen braucht es mehr als einen Hauch in den Wetterdaten.
  EN: No more rain on the map in sunshine: a clear sky counts as dry, and rain needs more than a trace in the weather figures.
- Eine Fahrt, die beim Halt unterwegs von selbst endete, wird 3 Stunden lang zum Fortsetzen angeboten: „Fahrt fortsetzen“ auf dem Handy, auf Android Auto eine Karte „Weiter nach …“, die nach 10 Sekunden von selbst startet (außer du tippst „Nicht jetzt“), und ein Knopf „Weiter“ in der Android-Auto-Leiste.
  EN: A trip that ended by itself while you were stopped on the way is offered again for 3 hours: "Resume trip" on the phone, and on Android Auto a "Carry on to …" card that starts by itself after 10 seconds unless you tap "Not now", plus a "Carry on" button in Android Auto's own row.

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
