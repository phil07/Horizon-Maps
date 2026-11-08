# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Neu
- Nächstes Tempolimit: Kommt auf der Route ein anderes Tempolimit, zeigt ein kleines Schild neben dem Limit, welches es ist und wie weit es noch ist (ab etwa 800 m).
- 3D-Fahnen: Eine hohe rote Fahne am Ziel und kleinere gelbe an deinen Stopps, über den Gebäuden, damit du sie aus der Ferne siehst. Ein/aus unter Einstellungen > Dein Pfeil & Routenlinie > Routenlinie.

### Behoben
- Verpasste Abbiegung: Der Pfeil folgt dir sofort, sobald du von der Route abweichst (vorher erst ab 40 m). Die neue Route kommt nach etwa 5 Sekunden.
- Einfachere Routen: Beim Planen wird eine Route mit deutlich weniger Abbiegungen gewählt, wenn die schnellste höchstens 1,5 Minuten schneller ist. Die schnellere bleibt als Option.
- Verkehr aus zwei Quellen: Auf einer Mapbox-Route mit TomTom-Schlüssel zählt jeder Abschnitt mit der stärkeren Verzögerung beider Dienste.
- Stau: Im Kriechen wird der Verkehr alle 2 Minuten statt 3 neu geholt, damit die Ankunftszeit schneller auf einen Stau reagiert.
- Tonsignal im Auto: Der kurze Warnton (Tempo, Blitzer voraus, Abbiegen) läuft jetzt auf dem Navigationskanal wie die Ansagen, also auch über Radio oder die Musik des Autos.
- Suche beim Fahren: Die Tastatur ist standardmäßig nicht mehr gesperrt. Wer die Sperre will, schaltet „Tastatur beim Fahren sperren“ in den Einstellungen wieder ein.
- Spuren bei zwei Ausfahrten dicht hintereinander: Die Spur leuchtet nur noch für die Abzweigung, an der die Route wirklich abbiegt, nicht schon für die erste.
- Spurhinweis an Kreiseln: Ohne Mapbox-Spurdaten gibt die App an Kreiseln keinen Spurhinweis mehr, statt einen falschen zu geben.
- Bordsteinseite: Mapbox-Routen bringen dich zur Seite des Ziels, auf der du aussteigst, statt über die Fahrbahn zu müssen. Ein/aus unter Einstellungen > Routen > Routenauswahl. Offline-Routen folgen noch nicht.
- Android Auto: Auf der Fahrtkarte gibt es jetzt die Knöpfe Übersicht (ganze Route) und Andere aus/an (die grauen Alternativrouten). Die Pfeilgröße im Auto geht bis 30 % statt 60 %, für kleine Displays.
- Blitzer aus in Android Auto: Der Schalter „Blitzer“ in den Auto-Einstellungen blendet jetzt auch die schon geladenen Blitzer auf der Karte aus (und beim Einschalten wieder ein). Vorher stoppte er nur die Warnungen.
- Kreisverkehr im Head-up-Display: Android Auto bekommt jetzt zusätzlich zur Ausfahrtsnummer den Ausfahrtswinkel. Das Head-up-Display im Auto zeigt dadurch geradeaus, erste, zweite oder dritte Ausfahrt im Kreisel an statt eines normalen Abbiegens.
- Ladestopps ohne Neustart: Werden unterwegs Ladestopps oder Stopps geplant, hinzugefügt oder entfernt, wird die Route von deiner Position aus geändert und die Navigation läuft weiter. Vorher begann sie von vorn mit Routenübersicht und wiederholten Ansagen. Android Auto und Handy ändern sie gleichzeitig. Findet die App keinen Weg über den Stopp, bleibt die Route, wie sie war.
- Route startet nicht mehr grundlos neu: Während der Fahrt hat die App die ganze Route neu berechnet, sobald die Restreichweite 5 km gefallen war (sie fällt ja beim Fahren). Das wiederholte Ansagen, setzte die Position zurück und verwarf die Akku-Schätzung. Jetzt prüft die App alle 3 Minuten, ob der Plan noch passt, und startet die Route nur neu, wenn sich die Ladestopps wirklich ändern.
- Ladestopp bei zu wenig Reichweite: Reicht die Reichweite (Auto-Wert oder Schätzung) nicht für Ziel plus eingestellte „Restreichweite am Ziel“, plant die App jetzt auch unterwegs einen Ladestopp ein (einmal pro Fahrt, mit Ansage). Vorher kam das nur beim Start der Route.

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
