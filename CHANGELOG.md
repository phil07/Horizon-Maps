# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Neu
- Sprache wählen: Unter Einstellungen > Ton & Stimme > Sprache stellst du die App auf Deutsch oder Englisch, unabhängig von der Telefonsprache (ab Android 13). Das gilt für Bildschirme, Android Auto, Ansagen, Routenanweisungen und Suchergebnisse.
- Einstellungen auf Englisch: Alle Einstellungsseiten, Optionen, Beschreibungen und Buttons gibt es jetzt auch auf Englisch. Die Einstellungssuche findet Begriffe in beiden Sprachen.

### Behoben
- Verkehrsfarben: Ein Stau oder Unfall auf der Routenlinie bleibt nicht mehr rot, wenn er sich aufgelöst hat. Jede Aktualisierung beginnt beim Verkehr des Routendienstes und legt TomTom und Meldungen darüber.
- Verpasste Abbiegung: Sicherer gegen GPS-Abweichungen in der Stadt. Sie zählt nur, wenn du weiter als die GPS-Ungenauigkeit (mindestens 15 m) von der Route weg bist und deutlich anders fährst (über 90 Grad), damit das Heranfahren an eine Abbiegung keine neue Route auslöst.
- Postleitzahl-Bezirk: Ein Bezirk allein („M25“, „B1“) heißt oft auch wie eine Straße. Er steht jetzt an dritter Stelle, sodass bei „M25“ die Autobahn zuerst kommt.
- Routenübersicht in Android Auto: Die Route füllt jetzt den Bildschirm, statt nur das obere Drittel zu nutzen, und ist dadurch größer und besser lesbar.

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
