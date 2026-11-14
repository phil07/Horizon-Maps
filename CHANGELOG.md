# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Under each entry, an indented line "  EN: Headline: what it does" gives its English for the app in English (GitHub and Google Play get the German only). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Behoben
- Kreisel in der Abbiegekarte ohne Kartendaten: Fehlen die Kreisel-Daten aus OpenStreetMap, legt die grüne Karte jetzt einen Kreis durch die Route auf dem Ring und liest Ein- und Ausfahrt 15 m außerhalb davon ab. Vorher zählte nur, wie weit die Route auf dem Ring dreht; das war zu wenig, sodass eine dritte Ausfahrt nach links wie geradeaus und geradeaus wie rechts aussah.
  EN: Roundabouts on the turn card without map data: when the roundabout data from OpenStreetMap is missing, the green card now lays a circle through the route on the ring and reads the way in and out 15 m outside it. Before, only how far the route turns on the ring counted; that fell short, so a third exit to the left looked like straight on and straight on like a right.

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
