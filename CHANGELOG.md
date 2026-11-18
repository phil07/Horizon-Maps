## Unreleased

### Neu
- Großes Geschwindigkeitsschild: Eine neue Einstellung unter Beim Fahren tauscht die Anzeige. Das Tempolimit-Schild steht groß an der Stelle des Tachos, dein Tempo klein daneben. Dein Tempo wird bei der Grenze gelb und rot wie vorher. (Dank an ole1993)
  EN: Big speed limit sign: a new setting under While driving swaps them round. The limit sign is big where the speedometer was, and your speed is small beside it. Your speed still turns amber and red near and over the limit. (Thanks to ole1993)

### Verbessert
- Suche mit TomTom und Mapbox: Mit beiden fragt die Suche jetzt zugleich, und die Ergebnisse werden zusammengelegt. Ein Ort steht einmal in der Liste, der genaueste Treffer zuerst. Vorher wurde Mapbox nur gefragt, wenn TomTom weniger als vier Orte fand, so konnte eine nähere Straße gleichen Namens fehlen. (Dank an jvricardo1)
  EN: Search with both: with a TomTom key and a Mapbox token, search asks both at the same time and puts their results together. A place is listed once, the nearest exact match first. Before, Mapbox was only asked when TomTom found fewer than four places, so a nearer street of the same name could be missing. (Thanks to jvricardo1)

### Behoben
- Medienleiste: Der Songname erscheint jetzt bei mehr Musik-Apps. Manche schicken ihn an eine andere Stelle, und dann stand dort nur „Medien“. (Dank an bijindas)
  EN: Media bar: the song's name now shows with more music apps. Some send it in a different place, which left the bar saying just "Media". (Thanks to bijindas)
- Fehlerberichte enthalten keinen TomTom-Schlüssel und kein Mapbox-Token mehr. Sie werden überall unkenntlich gemacht, auch in der Adresse einer Karte, die nicht geladen hat. (Dank an zedje85)
  EN: Feedback reports no longer include your TomTom key or Mapbox token: they're blanked out wherever they appear, including in the address of a map that failed to load. (Thanks to zedje85)
- Ankunft: Stoppt man etwa 40 m vor dem Ziel, gilt man als angekommen, egal wie viel Route noch übrig ist. (Dank an Nubber99)
  EN: Arriving: stopping within about 40 m of your destination now counts as arriving, however much route is left. (Thanks to Nubber99)
- Sicherung: Wiederherstellen schließt die App nicht mehr, wenn auf dem Gerät die übliche Dateiauswahl fehlt. Es versucht die ältere Auswahl und sagt Bescheid, wenn beide fehlen. Das Sichern hat denselben Schutz. (Dank an franciscoserranoadt)
  EN: Backup: Restore no longer closes the app on a phone or unit where the usual file picker is missing. It tries the older picker, and says so if neither is there. Back up gets the same protection. (Thanks to franciscoserranoadt)

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
