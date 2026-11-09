# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Under each entry, an indented line "  EN: Headline: what it does" gives its English for the app in English (GitHub and Google Play get the German only). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Neu
- Ort antippen: Ein angetippter Ort auf der Handy-Karte zeigt eine Karte mit Name, Adresse, „Los“ und „Als Stopp“, auch über der Routenauswahl. Langes Drücken fährt weiterhin direkt hin.
  EN: Tap a place: A place tapped on the phone's map shows a card with its name, address, Go and Add as stop, also on top of the route options. Holding still goes there straight away.

### Verbessert
- Sprache: Nur deutsch eingestellte Handys bekommen Deutsch, alle anderen Englisch.
  EN: Language: Only phones set to German get German, all others English.
- Logos: Schärfer, da zuerst das große Symbol der Marken-Website geladen wird.
  EN: Logos: Sharper, as the brand website's large icon is loaded first.
- Alle Standorte: Fahrzeiten kommen ohne Mapbox-Token von TomTom (für die acht nächsten Filialen).
  EN: See all locations: Without a Mapbox token, drive times come from TomTom (for the eight nearest branches).
- Eigenes Design: Akzentfarben Gold, Orange und Rosa sind etwas dunkler, damit weiße Schrift darauf lesbar bleibt.
  EN: Own design: The accent colours gold, orange and pink are a little darker so white text stays readable.

### Behoben
- Markierter Ort: Ein lange gedrückter Kartenpunkt heißt auf Deutsch jetzt „Markierter Ort“ statt „Dropped pin“.
  EN: Dropped pin: A spot held on the map is now called "Markierter Ort" in German.
- Suche nach Art (Tanken, Essen, Parken …): Ergebnisse kommen jetzt in der Sprache der App.
  EN: Search by kind (fuel, food, parking …): Results now come in the app's language.

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
