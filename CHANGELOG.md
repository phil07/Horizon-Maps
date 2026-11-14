# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Under each entry, an indented line "  EN: Headline: what it does" gives its English for the app in English (GitHub and Google Play get the German only). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Behoben
- Suche mit TomTom-Schlüssel: Lehnt TomTom die klassische Suche für den Schlüssel ab (neuere Schlüssel können nur die neuen „Orbis“-APIs mitbringen), probiert die App dieselbe Suche jetzt bei TomTom Orbis. Geschäfte, Orte und Adressen werden wieder gefunden, nicht nur Postleitzahlen. Lehnt TomTom beides ab, sagt die App einmal warum, statt still dünne Ergebnisse zu zeigen.
  EN: Searching with a TomTom key: if TomTom's classic search refuses the key (newer keys can come with only the newer "Orbis" APIs), the same search is now tried on TomTom's Orbis service. Shops, places and addresses are found again, not only postcodes. If TomTom refuses both, the app now says why once, instead of quietly showing thin results.

### Neu
- Warnung vor scharfen Kurven: Kurven, die man langsamer nehmen muss, sind auf der Route gelb-schwarz gestreift mit schwarzen Pfeilen. Etwa 9 Sekunden davor erscheint ein gelbes Schild „Scharfe Kurve · mit 40 km/h nehmen“ mit Gong, nur wenn du schneller fährst. Aus der Form der Route berechnet, Abbiegungen zählen nicht. Einstellungen › Warnungen › Scharfe Kurven.
  EN: Sharp bend warnings: bends that need slowing for are striped yellow with black chevrons on the route, and about 9 seconds before each a yellow chevron sign says "Sharp bend · take it at 25 mph" with a chime (only when you're going faster than that). Worked out from the route's shape; junction turns don't count. Settings › Warnings › Sharp bends.
- Scheinwerferkegel bei Nacht: Nach Einbruch der Dunkelheit liegt ein weicher Lichtkegel vor deinem Pfeil auf der Straße, solange du fährst, bei höherem Tempo länger. Einstellungen › Pfeil › Scheinwerferkegel bei Nacht.
  EN: Headlight beam: after dark, a soft cone of light lies on the road ahead of your arrow while you're moving, longer at speed. Settings › Your arrow › Headlight beam at night.
- Ziel bei der Ankunft beleuchtet: Das Gebäude bleibt beleuchtet, solange die Ankunftskarte da ist, der Eingang (die Seite, die am nächsten zu deinem Halteplatz liegt) bekommt eine Markierung, und eine gepunktete Linie zeigt den Fußweg vom vorgeschlagenen Parkplatz. Einstellungen › Ankunft › Ziel beleuchten.
  EN: Destination lit up on arrival: the building stays lit while the arrival card is up, its entrance (the side nearest where you pulled up) gets a marker, and a dotted line shows the walk from the suggested parking. Settings › Arrival › Light up your destination.

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
