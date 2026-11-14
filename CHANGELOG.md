# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Under each entry, an indented line "  EN: Headline: what it does" gives its English for the app in English (GitHub and Google Play get the German only). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Behoben
- Kreisel-Ausfahrt nach Zahl der Arme: Die grüne Abbiegekarte verteilt die Arme eines Kreisels jetzt gleichmäßig und setzt die Ausfahrt nach ihrer Nummer, z. B. bei vier Armen die 3. Ausfahrt drei Viertel herum, also links. Die Arme kommen aus OpenStreetMap; die beiden Fahrbahnen einer geteilten Straße zählen als einer, Wege und Zufahrten wie zu einer Tankstelle gar nicht. Passt die Zahl nicht zur Ausfahrt, misst die App wie bisher.
  EN: Roundabout exit by its number of arms: the green turn card now spaces a roundabout's arms evenly and places the exit by its number; with four arms the third exit is three quarters round, a left. The arms come from OpenStreetMap; the two carriageways of a divided road count as one, paths and access roads such as to a petrol station not at all. Where the count doesn't fit the exit, the app measures as before.

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
