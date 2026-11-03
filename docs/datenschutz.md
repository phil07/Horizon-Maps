# Datenschutzerklärung für Horizon Nav

Stand: 3. November 2026


## Verantwortlicher

Philipp Fischer
iphilipp07@gmail.com

## Grundsatz

Horizon Nav ist eine Navigations-App ohne eigenes Benutzerkonto, ohne Werbung und ohne Analyse- oder Tracking-Dienste. Einstellungen, Favoriten, Fahrten und eigene Meldungen werden nur auf deinem Gerät gespeichert. Damit die Navigation funktioniert, schickt die App aber Anfragen an die unten genannten Dienste. Dabei werden je nach Funktion dein ungefährer oder genauer Standort, ein Suchbegriff oder eine Route übertragen, außerdem technisch bedingt deine IP-Adresse.

## Berechtigungen

- **Standort (genau und ungefähr):** für Kartenposition, Routenführung, Warnungen und die Suche in der Nähe. Während einer Navigation nutzt die App den Standort auch, wenn sie im Hintergrund läuft; dafür zeigt Android eine dauerhafte Benachrichtigung.
- **Benachrichtigungen:** für die Navigations-Benachrichtigung.
- **Android Auto (Navigation, Kartenfläche, Ladestand):** um die App im Auto anzuzeigen. Den Ladestand deines Elektroautos liest die App nur, wenn du das in Android Auto erlaubst, oder über dein Konto beim Autohersteller (siehe unten). Er bleibt auf dem Gerät.
- **Internet:** für Karten, Routen und die unten genannten Dienste.

## Dienste, mit denen die App Daten austauscht

| Dienst | Wofür | Was übertragen wird |
| --- | --- | --- |
| Mapbox (Mapbox Inc., USA) | Kartendarstellung, Suche, Routen, Verkehr | Kartenausschnitt, Suchbegriffe, Start, Ziel und Zwischenziele; das Mapbox-Kartenmodul sendet außerdem anonyme Nutzungs- und Standortdaten an Mapbox (Telemetrie) |
| TomTom (nur mit eigenem Schlüssel) | Routen, Verkehr, Blitzer, Ladesäulen | Start, Ziel, Route, Position |
| OpenStreetMap-Dienste: Overpass-Server (overpass-api.de, overpass.kumi.systems, overpass.private.coffee), Nominatim, Photon (komoot) | Tempolimits, Orte in der Nähe, Adresssuche | Position oder Kartenausschnitt, Suchbegriffe |
| Bundesnetzagentur (Ladesäulenregister, über ArcGIS von Esri) | Leistung und Betreiber von Ladesäulen | Route bzw. Bereich entlang der Route |
| Open Charge Map (nur mit eigenem Schlüssel) | Ladesäulen weltweit | Route bzw. Bereich entlang der Route |
| Rijkswaterstaat über PDOK (nur in den Niederlanden) | Tempolimits auf niederländischen Straßen | Ungefähre Position (ein kleines Kartenfeld um dich) |
| Open-Meteo, RainViewer | Wetter und Regenradar | Position bzw. Kartenausschnitt |
| Tankerkönig (nur mit eigenem Schlüssel), E-Control (Österreich), economie.gouv.fr (Frankreich), UK Fuel Finder und postcodes.io (Großbritannien) | Kraftstoffpreise | Position bzw. Umkreis |
| Meldungs-Server von Horizon Nav (Cloudflare Workers, Cloudflare Inc.) | Geteilte Meldungen anderer Fahrer (Stau, Polizei, Unfall usw.) | Siehe Abschnitt „Geteilte Meldungen“ |
| ntfy.sh (nur wenn du Handy und Radio koppelst) | Ziele vom Handy ans Radio senden | Das Ziel, verschlüsselt mit deinem Kopplungscode |
| GitHub (GitHub Inc., USA) | Offline-Pakete, Fahrzeugliste (Open EV Data), Versionshinweise | Nur der Abruf der Dateien |
| GeoNames | Ortsnamen für die Offline-Suche | Nur der Abruf der Dateien |
| BMW Group (Deutschland), Volvo Cars (Schweden), Smartcar Inc. (USA), nur wenn du das Konto einrichtest | Akkustand, Reichweite und Ladestatus deines Elektroautos | Deine Anmeldung bei dem Hersteller; die App fragt nur die Akkudaten ab. Siehe Abschnitt „Autohersteller-Konto“ |

Die Anbieter verarbeiten die Daten nach ihren eigenen Datenschutzbestimmungen. Einige sitzen in den USA; dorthin werden Daten dann auf Grundlage der jeweiligen Garantien des Anbieters (z. B. Standardvertragsklauseln, EU-US Data Privacy Framework) übermittelt.

## Autohersteller-Konto (Akkustand)

Wenn du unter Einstellungen > Kraftstoff ein Konto bei BMW oder Mini, Volvo oder Smartcar einrichtest, holt die App Akkustand, Reichweite und Ladestatus deines Elektroautos vom Hersteller. Dafür meldest du dich mit deinem eigenen Konto an und nutzt deine eigenen, kostenlosen Entwicklerschlüssel (Client-ID, bei Volvo außerdem den VCC-Schlüssel, bei Volvo und Smartcar das Secret). Die Anmeldung läuft über die Seite des Herstellers im Browser; die App erhält nur Zugangs- und Erneuerungstoken.

- **Gespeichert:** Deine Schlüssel und Tokens liegen verschlüsselt auf deinem Gerät (im Android-Schlüsselspeicher). Die Fahrzeugnummer (VIN) bleibt ebenfalls auf dem Gerät. Bei Smartcar sind außerdem deine Nutzer-ID und die Verbindungs-ID dort verschlüsselt gespeichert. Alles wird nicht in Sicherungen aufgenommen.
- **Wann abgerufen wird:** Nur wenn die App in Benutzung ist (geöffnet, eine Route läuft oder Android Auto ist verbunden) und einmal beim Öffnen. Die Abrufrate stellst du ein. Bei BMW gilt das Tageslimit von 50 Abrufen des Herstellers.
- **Wer die Daten bekommt:** Die Anfragen gehen direkt vom Gerät an den Hersteller bzw. an Smartcar, der sie an den Hersteller weiterleitet. Es gibt keinen Server der App dazwischen, und der Betreiber der App erhält diese Daten nicht.
- **Abmelden:** Beim Abmelden bittet die App Volvo, die Anmeldung zu beenden, und schaltet bei BMW den Datenzugang der App ab. Danach löscht sie die Tokens auf deinem Gerät. Bei BMW bleibt die Freigabe im My-BMW-Portal bestehen, bei Smartcar löscht die App die Verbindung über die Schnittstelle des Anbieters. Schlägt das fehl, entfernst du die App unter mySmartcar (my.smartcar.com). Das zeigt die App dir an.

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
