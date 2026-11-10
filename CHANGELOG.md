# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Under each entry, an indented line "  EN: Headline: what it does" gives its English for the app in English (GitHub and Google Play get the German only). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Neu
- Knöpfe auf der Fahrtkarte: Unter Einstellungen > Android Auto lassen sich Übersicht, Andere Routen, Ton und Beenden ein- und ausschalten, und die Knöpfe in drei Größen wählen. Weniger Knöpfe machen die Karte schmaler.
  EN: Trip card buttons: Under Settings > Android Auto you can turn Overview, Other routes, Sound and End on or off, and pick the buttons' size (three sizes). Fewer buttons make the card narrower.

### Neu
- Einfachere Routen bevorzugen: Unter Einstellungen › Routen › Routenauswahl. Eine Route, die nur etwa eine Minute langsamer ist, aber deutlich weniger Abbiegungen hat, wird zuerst gewählt und heißt jetzt Einfachste (vorher Kürzeste). Ausgeschaltet startet die App immer auf der schnellsten Route.
  EN: Prefer simpler routes: Under Settings › Routes › Route choice. A route only about a minute slower but with far fewer turns is picked first, and is now labelled Simplest (it used to say Shortest). Switched off, the app always starts on the fastest route.

- Tempolimit-Abstimmung: Stimmen Mapbox und OpenStreetMap überein, zählt das jetzt einmal statt doppelt.
  EN: Speed limit vote: When Mapbox and OpenStreetMap agree, it now counts once instead of twice.

- Mehrere Fahrzeuge: Jedes Auto (über sein Bluetooth-Gerät) behält seinen Kraftstoff, die E-Auto-Daten und die aus Fahrten gelernten Werte. Verbindet sich ein bekanntes Auto, wird es automatisch gewählt. Ein unbekanntes Gerät fragt Einstellungen › Android Auto › Fahrzeuge.
  EN: Several cars: Each car (by its Bluetooth device) keeps its fuel type, electric car data and the values learned from its trips. A known car is picked automatically when it connects. For an unknown device, Settings › Android Auto › Vehicles asks which car it is.

### Behoben
- Absturz beim Antippen eines Ortes auf der Karte: Ein Ort ohne Namen in Mapbox' Daten schloss die App. Namenlose Orte werden jetzt ignoriert, und andere Fehler bei einem Kartentipp können die App nicht mehr schließen.
  EN: Crash tapping a place on the map: A place with no name in Mapbox's data closed the app. Nameless places are now ignored, and other errors in a map tap can no longer close the app.
- Spurbild unter der Abbiegekarte auf Android Auto: Auf einem kurzen Bildschirm lief es unter die Fahrtkarte. Es wird jetzt passend zum freien Platz kleiner und entfällt, wenn selbst ein kleines nicht passt. Die Spuren stehen trotzdem in der Abbiegekarte.
  EN: Lane picture under the turn card on Android Auto: On a short screen it ran under the trip card. It now shrinks to the free room and is left out when even a small one wouldn't fit. The lanes still show in the turn card.
- Abbiegung am Ende einer Slip Road: Die Anzeige zählt jetzt bis zur Stelle herunter, an der die Slip Road die Hauptstraße verlässt („Ausfahrt links nehmen“), und erst danach bis zum Abbiegen. Vorher zählte sie bis zum Ende der Slip Road.
  EN: Turn at the end of a slip road: The display now counts down to where the slip road leaves the main road ("take the exit on the left"), and only then to the turn. It used to count down to the end of the slip road.

### Verbessert
- An Auto senden schickt die ganze Fahrt: Aus den Routenoptionen gehen jetzt auch deine Stopps und der gewählte Weg mit, nicht nur das Ziel. Die Karte im Auto zeigt, wie viele Stopps kommen.
  EN: Send to car sends the whole trip: From the route options it now sends your stops and the way you picked too, not just the destination. The car's card says how many stops are coming.

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
