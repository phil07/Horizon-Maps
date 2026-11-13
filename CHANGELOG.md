# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Under each entry, an indented line "  EN: Headline: what it does" gives its English for the app in English (GitHub and Google Play get the German only). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Behoben
- Regen und Schnee auf Android Auto: Wettereffekte erscheinen jetzt auch auf dem Autodisplay (bisher nur auf dem Telefon), mit dem Dunst zum Horizont.
  EN: Rain and snow on Android Auto: weather effects now show on the car screen too (they were only ever drawn on the phone), along with the weather haze towards the horizon.
- Regeneffekt fehlte bei Regen: Die Anzeige richtete sich nach der Stundenzusammenfassung, die bei Schauern oft „bewölkt“ sagt. Jetzt zählt auch der Regen oder Schnee, der im aktuellen Viertelstundenfenster fällt. Die Abfrage läuft alle 10 statt alle 20 Minuten.
  EN: Rain effect missing while it rained: the effect went by the hour's weather summary, which often says "cloudy" during showers. It now also checks the rain and snow actually falling in the current quarter hour, and looks every 10 minutes instead of 20.

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
