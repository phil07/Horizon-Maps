# Datenschutzerklärung für Horizon Nav

Stand: 1. November 2026

> **Bitte vor der Veröffentlichung ausfüllen:** Name und Kontaktadresse unter „Verantwortlicher“. Ohne erreichbare Kontaktangabe akzeptiert Google Play die Erklärung nicht.

## Verantwortlicher

[Vorname Nachname]
[E-Mail-Adresse für Datenschutzanfragen]

## Grundsatz

Horizon Nav ist eine Navigations-App ohne eigenes Benutzerkonto, ohne Werbung und ohne Analyse- oder Tracking-Dienste. Einstellungen, Favoriten, Fahrten und eigene Meldungen werden nur auf deinem Gerät gespeichert. Damit die Navigation funktioniert, schickt die App aber Anfragen an die unten genannten Dienste. Dabei werden je nach Funktion dein ungefährer oder genauer Standort, ein Suchbegriff oder eine Route übertragen, außerdem technisch bedingt deine IP-Adresse.

## Berechtigungen

- **Standort (genau und ungefähr):** für Kartenposition, Routenführung, Warnungen und die Suche in der Nähe. Während einer Navigation nutzt die App den Standort auch, wenn sie im Hintergrund läuft; dafür zeigt Android eine dauerhafte Benachrichtigung.
- **Benachrichtigungen:** für die Navigations-Benachrichtigung.
- **Android Auto (Navigation, Kartenfläche, Ladestand):** um die App im Auto anzuzeigen. Den Ladestand deines Elektroautos liest die App nur, wenn du das in Android Auto erlaubst; er bleibt auf dem Gerät.
- **Internet:** für Karten, Routen und die unten genannten Dienste.

## Dienste, mit denen die App Daten austauscht

| Dienst | Wofür | Was übertragen wird |
| --- | --- | --- |
| Mapbox (Mapbox Inc., USA) | Kartendarstellung, Suche, Routen, Verkehr | Kartenausschnitt, Suchbegriffe, Start, Ziel und Zwischenziele; das Mapbox-Kartenmodul sendet außerdem anonyme Nutzungs- und Standortdaten an Mapbox (Telemetrie) |
| TomTom (nur mit eigenem Schlüssel) | Routen, Verkehr, Blitzer, Ladesäulen | Start, Ziel, Route, Position |
| OpenStreetMap-Dienste: Overpass-Server (overpass-api.de, overpass.kumi.systems, overpass.private.coffee), Nominatim, Photon (komoot) | Tempolimits, Orte in der Nähe, Adresssuche | Position oder Kartenausschnitt, Suchbegriffe |
| Bundesnetzagentur (Ladesäulenregister, über ArcGIS von Esri) | Leistung und Betreiber von Ladesäulen | Route bzw. Bereich entlang der Route |
| Open Charge Map (nur mit eigenem Schlüssel) | Ladesäulen weltweit | Route bzw. Bereich entlang der Route |
| Open-Meteo, RainViewer | Wetter und Regenradar | Position bzw. Kartenausschnitt |
| Tankerkönig (nur mit eigenem Schlüssel), E-Control (Österreich), economie.gouv.fr (Frankreich), UK Fuel Finder und postcodes.io (Großbritannien) | Kraftstoffpreise | Position bzw. Umkreis |
| Meldungs-Server von Horizon Nav (Cloudflare Workers, Cloudflare Inc.) | Geteilte Meldungen anderer Fahrer (Stau, Polizei, Unfall usw.) | Siehe Abschnitt „Geteilte Meldungen“ |
| ntfy.sh (nur wenn du Handy und Radio koppelst) | Ziele vom Handy ans Radio senden | Das Ziel, verschlüsselt mit deinem Kopplungscode |
| GitHub (GitHub Inc., USA) | Offline-Pakete, Fahrzeugliste (Open EV Data), Versionshinweise | Nur der Abruf der Dateien |
| GeoNames | Ortsnamen für die Offline-Suche | Nur der Abruf der Dateien |

Die Anbieter verarbeiten die Daten nach ihren eigenen Datenschutzbestimmungen. Einige sitzen in den USA; dorthin werden Daten dann auf Grundlage der jeweiligen Garantien des Anbieters (z. B. Standardvertragsklauseln, EU-US Data Privacy Framework) übermittelt.

## Geteilte Meldungen

Wenn „Meldungen teilen“ eingeschaltet ist (Standard), sendet die App deine Meldungen an den Meldungs-Server: die Art der Meldung, den Ort, die Zeit und eine zufällige Geräte-Kennung, die auf deinem Gerät erzeugt wird und nichts über dich verrät. Abstimmungen („Noch da“ / „Weg“) werden mit derselben Kennung gesendet. Um Missbrauch zu begrenzen, zählt der Server Anfragen pro Kennung und pro IP-Adresse; diese Zähler werden nach spätestens zwei Stunden gelöscht. Meldungen werden nach ihrer Gültigkeit (30 Minuten bis 4 Stunden) unsichtbar und spätestens eine Stunde danach gelöscht. Andere Fahrer sehen nur Art, Ort und Zeit einer Meldung, nicht die Kennung. Ist „Meldungen teilen“ aus, baut die App für Meldungen keine Verbindung zum Server auf.

## Fehlerberichte

Die App sendet keine Absturzberichte automatisch. Wenn du in den Einstellungen selbst ein Problem meldest, öffnet die App ein vorausgefülltes Formular auf GitHub; was du dort absendest, bestimmst du selbst.

## Speicherdauer und Löschen

Alle lokal gespeicherten Daten löschst du, indem du die App-Daten in den Android-Einstellungen löschst oder die App deinstallierst. Deine aktiven Meldungen löschst du unter Einstellungen > Warnungen > Deine Meldungen > „Löschen“.

## Deine Rechte

Du hast nach der DSGVO das Recht auf Auskunft, Berichtigung, Löschung, Einschränkung der Verarbeitung, Datenübertragbarkeit und Widerspruch. Wende dich dazu an die oben genannte Kontaktadresse. Außerdem kannst du dich bei einer Datenschutz-Aufsichtsbehörde beschweren.

## Rechtsgrundlage

Die Verarbeitung erfolgt, um die von dir genutzten Funktionen bereitzustellen (Art. 6 Abs. 1 lit. b DSGVO), und beim Meldungs-Server zusätzlich zum Schutz vor Missbrauch (berechtigtes Interesse, Art. 6 Abs. 1 lit. f DSGVO).
