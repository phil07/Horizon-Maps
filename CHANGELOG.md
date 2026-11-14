# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Under each entry, an indented line "  EN: Headline: what it does" gives its English for the app in English (GitHub and Google Play get the German only). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Neu
- Kreisel einstellen: Unter Abbiegekarte & Spuren > Kreisel lassen sich die Abstände einstellen, zwischen denen die Richtung eines Kreisels gemessen wird: vor der Einfahrt sowie Anfang und Ende nach dem Ausgang. Standard: 10 m vor der Einfahrt, 20 bis 40 m nach dem Ausgang.
  EN: Set the roundabout: under Turn map & lanes > Roundabout you can set the distances over which a roundabout's direction is measured: before the entry, and the start and end after the exit. Default: 10 m before the entry, 20 to 40 m after the exit.

### Behoben
- Kreisel-Winkel: Die Ausfahrt wird nicht mehr direkt am Ausgang gemessen, wo der Bogen des Kreisels noch nachwirkt. Dadurch stimmt der Winkel der grünen Abbiegekarte besser mit dem Verlauf der Karte überein.
  EN: Roundabout angle: the exit is no longer measured right at the exit, where the roundabout's bend still counts. The green turn card's angle now matches the map's line better.

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
