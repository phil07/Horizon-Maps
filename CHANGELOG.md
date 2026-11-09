# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Under each entry, an indented line "  EN: Headline: what it does" gives its English for the app in English (GitHub and Google Play get the German only). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Neu
- Öffnungszeiten: Suchergebnisse, „Alle Standorte“ und die Ort-Karte zeigen „Geöffnet bis …“ oder „Geschlossen · öffnet …“, wenn TomTom die Zeiten kennt.
  EN: Opening times: Search results, "See all locations" and the place card show "Open until …" or "Closed · opens …" when TomTom knows the times.
- Freien Punkt antippen: Ein Tipp auf eine freie Stelle der Handy-Karte zeigt die Adresse mit „Los“ und „Als Stopp“. Ein zweiter Tipp schließt die Karte. Nicht während der Zielführung.
  EN: Tap a free spot: A tap on a free spot of the phone's map shows its address with Go and Add as stop. A second tap puts the card away. Not while being guided.
- Schlagworte für Favoriten: Favoriten können ein eigenes Schlagwort bekommen (z. B. „Kunden“) und lassen sich danach filtern. Es passen jetzt 60 statt 20 Favoriten.
  EN: Tags for favourites: Favourites can get a tag of your own (e.g. "customers") and be filtered by it. There is now room for 60 instead of 20.

### Verbessert
- Pausenerinnerung: Ist sie eingeschaltet, werden nach zwei Stunden auf einer Route die Raststätten entlang der Strecke angeboten.
  EN: Break reminder: When it is on, the rest areas along the route are offered after two hours of driving.
- Android Auto: Die Schnellsuche (Tanken, Essen, Laden …) zeigt Markenlogos, die Suche Öffnungszeiten.
  EN: Android Auto: The quick search (fuel, food, charging …) shows brand logos, the search shows opening times.

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
