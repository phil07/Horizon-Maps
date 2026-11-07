# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Verbessert
- Ansagen früher und kürzer: Die Hauptansage kommt etwa 30 Sekunden vor dem Abbiegen (in der Stadt rund 400 bis 500 m statt 250 bis 300 m). Sie nennt nur noch einen Ort vom Schild („links halten Richtung Frankfurt“), an der Kreuzung selbst kommt nur die Richtung („Links halten“). Eine neue Ansage unterbricht die vorige nicht mehr, sie wartet, bis diese zu Ende ist.
- Pfeil bei schmaler Karte: Teilt sich Android Auto den Bildschirm (z. B. mit Musik), sitzt der Standortpfeil jetzt über der Fahrtleiste statt dahinter.

### Behoben
- Gewählte Route bleibt: Hast du nicht die schnellste Route gewählt, hat Android Auto die Route neu berechnet und dich doch die schnellste fahren lassen. Jetzt übernimmt es genau die gewählte Route. Findet die App unterwegs eine schnellere, erscheint oben eine Karte zum Wechseln oder Behalten (handy und Android Auto), die nach 7 Sekunden verschwindet. Gewechselt wird nie von selbst.

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
