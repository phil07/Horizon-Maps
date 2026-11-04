# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Verbessert
- Ladeplanung unterwegs: Wird während der Fahrt neu geplant, rechnet die App mit dem aktuellen Akkustand: der letzte Wert vom Auto abzüglich des Verbrauchs seither, nach einem Ladestopp der dort geplante Ladestand, sonst der Startwert abzüglich des bisherigen Verbrauchs. Bisher galt der Akkustand vom Start.
- Ladeplanung unterwegs: Neue Stopps oder eine Neuplanung setzen Fahrzeit, Strecke und Verbrauch der laufenden Fahrt nicht mehr auf null zurück.
- Navigation starten: Die gewählte Route (z. B. „Kürzeste“) bleibt erhalten, wenn die Ladestopps beim Start eingebaut werden. Wird während der Ladeplanung gestartet, wartet die Navigation auf den Plan und startet dann mit den Stopps.
- Ladestopps: Eine Ladesäule gilt schon im Umkreis von 150 m als erreicht (Ladeparks sind größer). Fährst du an einem Ladestopp vorbei, plant die App für den neuen Weg neu, statt dich weit zurückzuschicken.
- Ohne Internet: Die Ladeplanung sagt „Offline: keine Ladeplanung“, statt fälschlich zu melden, dass keine Ladesäule erreichbar sei.
- Einstellungen > Kraftstoffpreise: Client ID, Client Secret, Preise und „Falschen Preis entdeckt?“ gehören zum britischen Preisdienst Fuel Finder und erscheinen nur noch in Großbritannien mit einem Verbrenner.
- Ladestopps nach Neuberechnung: Weichst du als Elektroauto deutlich von der Route ab, werden die Ladestopps für den neuen Weg neu geplant, wenn der Weg über die alten Säulen mehr als 5 Minuten länger wäre. Eigene Stopps bleiben. Reicht der Akku nach einem Umweg nicht mehr, wird ein Ladestopp eingeplant. Eine Meldung nennt die neuen Stopps.
- Routenvorschau beim Verbrenner: Der Tankstopp-Vorschlag entfällt. Zwischenstopps zum Laden werden nur noch für Elektroautos geplant. In den Einstellungen heißt der Schalter jetzt „Ladestopps auf langen Fahrten planen“ und erscheint nur beim Elektroauto.
- Elektro-Routenvorschau: Statt geschätzter Zeiten zeigt die Vorschau „Routen werden berechnet …“, bis die Ladeplanung für alle Routen fertig ist. Erst dann erscheinen die Routen zur Auswahl, mit Ladestopps und Ladepausen; die schnellste (samt Laden) ist vorgewählt.


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
