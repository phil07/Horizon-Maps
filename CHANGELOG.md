## Unreleased

### New
- Android Auto: ein Pin-Knopf am Ende der Routenleiste auf der Fahrtkarte öffnet die Stopp-Karte (Tanken, Laden, Essen, Kaffee entlang der Route), um einen Stopp in einem Tipp hinzuzufügen.
  EN: Android Auto: a pin button at the end of the route bar on the trip card opens the stops card (fuel, charging, food, coffee along the route), to add a stop in one tap.

### Improved
- Scharfe Kurve: Das Schild „Mit X nehmen“ bleibt bis zum Ende der Kurve stehen, mindestens 5 Sekunden.
  EN: Sharp bend: the "take it at X" sign stays until the end of the bend, at least 5 seconds.
- Ankunftszeit: Die Restzeit wird über etwa eine halbe Minute geglättet, damit ein schwankender Verkehrswert die Ankunft nicht um mehrere Minuten springen lässt.
  EN: Arrival time: the time left is smoothed over about half a minute, so a traffic figure that flips does not swing the arrival by several minutes.
- Android Auto: Lange Namen in der Suche brechen auf eine zweite Zeile um, statt abgeschnitten zu werden.
  EN: Android Auto: long names in search wrap onto a second line instead of being cut short.
- Kompakter Tacho auf dem Autodisplay folgt Tag und Nacht, statt immer schwarz zu sein.
  EN: The compact speedometer on the car screen follows day and night instead of always being black.
- Eigenes Bild als Standortpfeil: Ein weißer Hintergrund am Rand wird durchsichtig (abschaltbar unter Einstellungen › Pfeil, „Weißen Hintergrund entfernen“).
  EN: Your own locator picture: a white background at its edge becomes see-through (can be switched off under Settings › Arrow, "Remove white background").

### Fixed
- Warnbanner haben Platz für eine dritte Zeile.
  EN: Warning banners have room for a third line.
- Neu zentrieren im Stand gibt wieder die 3D-Ansicht: Die flache Verkehrsansicht kommt erst nach weiteren 30 Sekunden Stillstand zurück.
  EN: Re-centring while stopped gives the 3D view again: the flat traffic view only returns after another 30 seconds stopped.

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
