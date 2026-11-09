# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Under each entry, an indented line "  EN: Headline: what it does" gives its English for the app in English (GitHub and Google Play get the German only). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Neu
- Alle Standorte anzeigen: Bei einer Suche nach einem Geschäft oder einer Kette (Lidl, Aldi, Starbucks …) mit mehreren Filialen in 25 km zeigt „Alle N Standorte anzeigen“ alle an. Sortiert nach Schnellste (Fahrzeit mit heutigem Verkehr), Nächste oder Auf meiner Route. Die Karte zeigt jede Filiale mit Logo und Fahrzeit. Antippen, dann „Los“ oder „Als Stopp“. „In diesem Bereich suchen“ sucht dort, wohin die Karte geschoben wurde. In Android Auto als Liste.
  EN: See all locations: Search for a shop or chain (Lidl, Aldi, Starbucks …) with several branches within 25 km and "See all N locations" shows them all, sorted by Quickest (drive time with today's traffic), Nearest or On my route. The map shows each with its logo and drive time. Tap one, then Go or Add as stop. "Search this area" looks again where you moved the map. On Android Auto it's a list.
- Logos in Suchergebnissen: Geschäfte und Marken zeigen ihr Logo (von ihrer Website oder aus einer eingebauten Liste bekannter Ketten). Logos bleiben nach dem ersten Mal auf dem Gerät; ohne Empfang erscheint die übliche Nadel.
  EN: Logos in search results: Shops and brands show their own logo (from their website or a built-in list of common chains). Logos are kept on the device after the first time; without a signal the usual pin shows.

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
