# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically.

## Unreleased

### Neu
- Dein Elektroauto: Unter Einstellungen > Kraftstoff > Dein Elektroauto > „Dein Fahrzeug“ wählst du dein Auto aus einer offenen Fahrzeugliste (Open EV Data), die beim ersten Öffnen einmal heruntergeladen wird. Horizon Nav schlägt das passende Modell aus dem Text des Akku-Widgets vor (z. B. „ID.7 Pro“), die genaue Variante bestätigst du selbst oder suchst sie von Hand. Mit Akkugröße und Ladekurve des Autos und der Leistung der Ladesäule werden die Ladezeiten an den Ladestopps genau.
- Bevorzugte Ladeanbieter: Unter Einstellungen > Kraftstoff > Dein Elektroauto wählst du im Menü „Bevorzugte Ladeanbieter“ deine Ladenetze, z. B. IONITY, EnBW oder Tesla Supercharger. Die Ladestopps nutzen dann ihre Säulen, solange die Fahrt dadurch höchstens 10 Minuten länger dauert (unter „Umweg für bevorzugte Anbieter“ auf 5 bis 20 Minuten einstellbar) und keinen zusätzlichen Stopp braucht. Die Liste der Stopps zeigt den Anbieter jeder Säule und markiert bevorzugte mit ★.
- Ladeleistung und Betreiber online: Fehlen bei einer Ladesäule Leistung oder Betreiber, sucht die App sie online. In Deutschland nutzt sie das Ladesäulenregister der Bundesnetzagentur, ohne Schlüssel. Weltweit nutzt sie Open Charge Map, wenn du unter Einstellungen > Kraftstoff > Dein Elektroauto einen eigenen kostenlosen Schlüssel einträgst. Schnelllader aus dem Register, die TomTom nicht kennt, kommen als weitere Kandidaten in die Planung. Bei „Alle“ ergänzt das Register auch die Leistung normaler Ladesäulen. Mit „Ladesäulen-Daten prüfen“ testest du den Open-Charge-Map-Schlüssel und siehst, was die letzte Abfrage gefunden hat. Ladedaten: Bundesnetzagentur.de (CC BY 4.0), Open Charge Map (CC BY-SA 4.0).
- Schnelllader für Ladestopps: Unter Einstellungen > Kraftstoff wählst du, welche Ladesäulen als Stopp infrage kommen: alle, ab 50, ab 150 oder ab 300 kW (Standard ab 50 kW). Säulen ohne bekannte Leistung nimmt die Planung nur, wenn keine passende in Reichweite ist. Die Liste der Stopps zeigt die Leistung jeder Säule.
- Reichweite für Ladestopps: Horizon Nav liest die Restreichweite aus dem Widget deiner Auto-App (z. B. „212 km“) und zeigt sie über der Geschwindigkeit. Die Route plant Ladestopps automatisch so, dass du am Ziel mit der eingestellten Reichweite ankommst (Standard 50 km, unter Einstellungen > Kraftstoff änderbar). Jeder Stopp wird nur so weit geladen, wie es bis zum nächsten Stopp reicht. An jeder Ladesäule bleiben mindestens 30 km Reserve (einstellbar), falls die Säule belegt ist.
- Akku auf der Karte: Bei Elektroautos steht der Ladestand (aus dem Widget oder direkt vom Auto) über der Geschwindigkeit, mit Alter ab 10 Minuten. Unter 20 % wird er gelb, unter 10 % rot.
- Akkustand direkt vom Auto: Über Android Auto liest Horizon Nav den Ladestand direkt vom Fahrzeug, wenn es ihn teilt (beim ersten Mal fragt Android Auto um Erlaubnis). Das Widget dient dann nur noch als Ersatz.
- Ladestand aus dem Widget deiner Auto-App: Unter Einstellungen > Kraftstoff > Dein Elektroauto > „Ladestand aus Widget“ das Widget deiner Auto-App wählen. Horizon Nav liest daraus den Prozentwert, sobald sich das Widget aktualisiert, und nutzt ihn für den Akku bei Ankunft und Ladestopps.
- Deutsche Sprachbefehle: Tippe aufs Mikrofon und sag z. B. „Bring mich nach Hause“, „Fahr nach München“, „Nächste Tankstelle“, „Günstige Tankstelle“, „Wann komme ich an?“, „Ton aus“, „Navigation beenden“, „Melde Blitzer“ oder „Autobahnen vermeiden“. Englische Befehle funktionieren weiterhin.
- Eigener Standard-Zoom: Lege fest, wie nah die Karte ohne Route zeigt. Mit + und − zoomen und „Als Standard verwenden“ tippen, oder den Regler unter Einstellungen > Kartenansicht nutzen, wo „Zurücksetzen“ ihn wiederherstellt. Der Auto-Zoom zoomt bei Tempo weiter heraus und an Kreuzungen hinein, ausgehend von deiner Einstellung.
- Verkehrsübersicht im Stand: Nach 30 Sekunden Stillstand an der Ampel oder im Stau zeigt die Karte den Verkehr um dich herum von oben und kehrt zurück, sobald du losfährst. Standardmäßig an, abschaltbar unter Einstellungen > Auf der Karte.
- Wo du geparkt hast: Wenn Android Auto sich trennt, merkt sich das Handy, wo das Auto steht. Eine Karte „Dein Auto“ zeigt, wie lange her und wie weit weg, mit Fußweg-Button und Auto-Pin auf der Karte – für 2 Tage oder bis du wieder im Auto sitzt.
- Ziele vom Handy senden: Handy einmal mit dem Radio koppeln (Einstellungen > Orte & Fahrten), dann „An Auto senden“ im Routenbildschirm des Handys. Das Radio bietet das Ziel beim nächsten Einschalten (innerhalb von 12 Stunden) an und startet es nach ein paar Sekunden. Die Ziele laufen verschlüsselt mit deinem Kopplungscode über einen kostenlosen Nachrichtendienst.
- Spenden: Horizon Nav ist kostenlos und werbefrei. Unterstützen kannst du es jetzt unter Einstellungen > Über > Spenden, per QR-Code fürs Handy oder PayPal-Button. Etwa einen Tag nach der Installation fragt einmalig eine Karte danach (nie während der Fahrt).
- 2D / 3D in Android Auto: Ein 2D/3D-Button bei den Android-Auto-Buttons, wie in der App.
- Straßennamen ansagen: Neue Einstellung unter Einstellungen > Ton. Ausgeschaltet hörst du nur die Richtung, z. B. „In 200 Metern links abbiegen“.
- Bioethanol (E85) als Kraftstoff: Die Tankstellenliste zeigt dann Stationen mit E85, in Frankreich mit Preisen.
- Live-Spritpreise: Frankreich und Österreich automatisch, Deutschland mit einem kostenlosen Tankerkönig-Schlüssel (Einstellungen > Kraftstoff). Preise im landesüblichen Format, z. B. 1,789 €.
- Maut nur in bestimmten Ländern meiden: Unter Einstellungen > Routen. Eine Route, die dort Maut kosten würde, wird ohne Mautstraßen geplant.
- Größe des Straßennamens: Ein Regler für den Namen der Straße unter deinem Pfeil (Einstellungen > Anzeige, für Android Auto bei den Größen des Autobildschirms).
- Fahrzeugtyp für den Pfeil: Limousine, Kompaktwagen, Kombi, SUV, Pickup, Transporter oder Sportwagen, in jeder Farbe (Einstellungen > Dein Pfeil).
- Freie Straßen in Grün: Neue Option unter Einstellungen > Karte (standardmäßig aus) zeigt fließenden Verkehr auf Hauptstraßen grün, zusätzlich zu Gelb und Rot für Stau.
- Stoppschilder: Stoppschilder auf deiner Route erscheinen auf der Karte, mit kurzem Ton vor jedem, wo OpenStreetMap sie kennt.
- Farbe der Alternativroute: Einstellbar, standardmäßig ein helles Blaugrau, das sich besser von grauen Straßen abhebt.
- Zuhause und Arbeit: Lassen sich in den Einstellungen festlegen oder ändern, und Favoriten können umbenannt oder zu Zuhause/Arbeit gemacht werden.
- Einfachere Zwischenstopps: Buttons im Stopp-Fenster fügen mit einem Tipp die beste Tankstelle, Ladesäule, Kaffee, Essen, Raststätte oder Toilette entlang der Route hinzu, dazu deine gespeicherten Orte. Langes Drücken auf die Karte während der Fahrt fügt einen Stopp hinzu.
- Elektroautos: Akkustand und gewünschten Mindest-Akku bei Ankunft in den Routenoptionen eingeben. Ein Ladestopp wird vorgeschlagen, wenn die Fahrt einen braucht, die Fahrtkarte zeigt den Akku bei Ankunft und einen Button für Ladesäulen voraus, und die Eco-Route nutzt TomToms E-Routing.

### Verbessert
- Verkehr in der Umgebung: Bleibt jetzt während der Navigation auf der Karte (er verschwand früher beim Routenstart). Unter Einstellungen > Karte lässt er sich auf die Route beschränken.
- Flüssigere Karte: Die Karte ist nicht mehr zusätzlich auf 30 Bilder pro Sekunde begrenzt, was sie ruckeln ließ.
- Schnellerer Zoom: Die Karte zoomt während der Fahrt nicht mehr viel zu langsam (bis zu 10 Sekunden vor einer Abbiegung) und startet nicht mehr doppelt so weit herausgezoomt wie gedacht.
- Automatische Update-Suche: Der Schalter „Selbst nach Updates suchen“ funktioniert wieder, und Updates und Kartendaten werden höchstens alle halbe Stunde geprüft statt bei jedem Öffnen.
- Neue-Version-Karte: Zeigt die Änderungen sofort, auch ohne Verbindung, weil die Hinweise in der App stecken. Jede Änderung hat eine eigene Karte mit Symbol, Überschrift und kurzer Beschreibung, sortiert nach Neu, Verbessert und Behoben. Im Querformat verteilt sie sich über den Bildschirm.
- Ansagen: Neue Einstellung „Sprachkanal“ (Einstellungen > Ton) für Radios, bei denen Töne zu hören sind, die Stimme aber nicht. Eine klare Meldung, wenn keine Sprachausgabe oder Sprache installiert ist, und Warnungen schneiden eine Ansage nicht mehr mittendrin ab.
- Ausfahrten: Außerhalb von Großbritannien und Irland heißen Autobahnausfahrten jetzt „Ausfahrt 12“.
- Sicherer Start: Stürzt die App mehrmals hintereinander beim Start ab (z. B. durch den Grafiktreiber), startet sie mit reduzierten 3D-Effekten und sagt das auch.
- Mapbox-Schlüssel: Kann die Karte ohne gültigen Schlüssel nicht laden, erklärt ein Hinweis das und lässt dich einen neuen einfügen, statt einer leeren Karte mit Sternen. Leerzeichen und unsichtbare Zeichen werden aus eingefügten Schlüsseln entfernt.
- TomTom-Schlüssel: Ein TomTom-Schlüssel schaltet die Karte nicht mehr auf TomToms flache Karte um, auf der 3D-Gebäude fehlten. Es ist kein TomTom-Schlüssel mehr in die App eingebaut; trage deinen eigenen in den Einstellungen ein, wenn du die TomTom-Suche nutzt.
- Satellitenansicht: Funktioniert jetzt auch mit der heruntergeladenen Karte und bleibt nachts sichtbar, statt fast schwarz zu werden.
- Pfeil in der Mitte der freien Karte: Ohne Navigation sitzt dein Pfeil mittig zwischen Suchfeld und Buttons statt dahinter.
- Aufgeräumte Kartenseite: Zuhause und Arbeit als zwei Kacheln mit Fahrzeit, Tankstelle, Laden, Essen, Kaffee und Parken als fünf runde Buttons. „Gespeichert & zuletzt“ zeigt drei Orte und „Alle“ für den Rest.
- Weniger Buttons beim Fahren: Nur Kompass und Kartenoptionen bleiben sichtbar. Zoom erscheint beim Berühren der Karte oder im Stand, Sprache und Ton sind auf der Fahrtkarte.
- Kartenoptionen neu gestaltet: Standard oder Satellit als zwei Bildkarten, ein schneller Auto/Tag/Nacht-Schalter und die Kartenebenen als Kacheln.
- Einstellungen neu sortiert: 15 kleinere Bereiche in vier Gruppen (Karte, Fahren, Du, Gerät), Android Auto mit eigenem Bereich. Die Suche findet weiterhin jede Einstellung.
- 3D-Modelle für alle Fahrzeugtypen: Kompaktwagen, Kombi, SUV, Pickup, Transporter und Sportwagen haben jetzt eigene 3D-Modelle.
- Navigationspfeil-Größe im Auto: Die Einstellung ändert jetzt den Pfeil auf der Karte statt der Abbiegekarte.
- Linkslenker: Der Tacho sitzt wieder unten links, damit die Abbiegekarte ihn nicht verdeckt. Das Ausfahrtsbanner wiederholt nicht mehr, was das Schild auf der Abbiegekarte schon zeigt.
- Android Auto, aufgeräumter Bildschirm: Gefahren voraus stehen unten auf der Abbiegekarte, der Ton-Button ist auf der Fahrtkarte, die Fahrtkarte hat einen schmaleren Fortschrittsbalken und größere Schrift, und bei weit entfernter Abbiegung schrumpft die Abbiegekarte auf eine Zeile.
- Android Auto, Kartenseite wie in der App: Zuhause- und Arbeit-Kacheln mit Fahrzeit, fünf runde Buttons und „Gespeichert & zuletzt“ mit „Alle“ für den Rest.
- Android Auto, Pfeil und Route: Pfeil und Routenlinie sind etwas größer und heben sich besser von Gebäuden ab.
- Android Auto, Tempolimit-Schild: Sitzt direkt am Bildschirmrand, wenn die Android-Auto-Buttons ausgeblendet sind, sonst daneben.
- Android Auto, Bäume: Die Karte wird fast so nah wie auf dem Handy gezeichnet, sodass auch 3D-Bäume erscheinen. Die Kartenschrift ist dadurch etwas kleiner.
- Spurführung näher an echtem 3D: Wo die echten Fahrstreifen bekannt sind, leuchten die Spuren 150 m vor der Kreuzung auf, die Routenlinie liegt in deiner Spur, und eine kleine Spurblase vor deinem Pfeil zeigt die richtigen Spuren. Sonst erscheint ein Spurschild über der Straße wie eine Autobahnbrücke.
- Spurdaten von Mapbox auch auf TomTom-Routen: Mit Mapbox-Schlüssel bekommt eine TomTom-Route im Hintergrund die Spurdaten von Mapbox. Abschaltbar unter Einstellungen > Abbiegekarte.

### Behoben
- Absturz beim Start unter Android 16 behoben, ausgelöst durch die Abfrage des Akku-Widgets. Horizon Nav kann das Widget einer anderen App nicht selbst aktualisieren. Unter Einstellungen > Kraftstoff öffnet der Knopf „App öffnen“ die Auto-App, damit sie den Stand neu lädt.
- Ladestand aus dem Widget überschreibt nicht mehr deinen Zielwert „Ladung auf“. Prüfe ihn nach dem Update einmal, er steht eventuell auf dem alten Akkustand.
- Widget-Auswahl: Widgets anderer Apps (z. B. der Auto-App) erscheinen jetzt in der Liste für den Ladestand, auch wenn Android sie sonst ausblendet.
- Update nach Abbruch: Wurde die Installation eines Updates abgebrochen, meldete ein erneuter Versuch „Problem beim Parsen des Pakets“. Jeder Versuch lädt jetzt in eine neue Datei, und der Installer bekommt genau die geprüfte Datei.
- Spurführung: Routenlinie und Spuren rutschen an normalen Kreuzungen nicht mehr neben die Straße.
- Kreisverkehr-Pfeile: Zeigen jetzt die Richtung der echten Ausfahrt, statt sie aus der Ausfahrtnummer zu raten.
- Kreuzungen: Die Ansicht wird vor Abbiegungen flacher, statt so tief zu schwenken, dass der Horizont den Bildschirm füllt.
- Fehlende 3D-Gebäude: Der freie Bereich um deinen Pfeil schneidet keine Gebäude mehr weg, auch große Gebäude neben der Straße verschwinden nicht mehr beim Vorbeifahren.
- Farbe der Alternativroute: Eine Änderung wirkt jetzt sofort.
- Straßenname unter dem Pfeil: Springt an Kreuzungen nicht mehr kurz auf eine Nebenstraße, verschwindet auf Parkplätzen und zeigt nach einer namenlosen Auffahrt nicht mehr den Schildtext als Straßennamen. Er überdeckt auch nicht mehr die Richtungsanzeige unten.
- Suche nach Tankstellen, Parkplätzen & Co.: Nutzt jetzt die Kategoriesuche von Mapbox und findet die nächsten Orte zuverlässig, statt oft leer zu bleiben oder mit „HTTP 429“ zu scheitern.
- Zwischenstopp hinzufügen: Hängt nicht mehr fest. Eine Suche bei einem langsamen Server bricht nach 30 Sekunden ab.
- 2D / 3D wird gemerkt: Die Karte startet nicht mehr jedes Mal in 3D.
- Offline-Karten > Rund um Zuhause: Wird nicht mehr zur Seite gequetscht.
- Mapbox-Hinweis: Die Offline-Kartenseite verweist jetzt auf Einstellungen > Schlüssel & Konten.
- Verkehrsübersicht im Stand: Springt nicht mehr beim Warten an einer Abbiegung an, und „Dein Auto“ wird nur gespeichert, wenn das Auto steht.
- Neustarts mancher Radios: Startet das ganze Radio kurz nach dem Öffnen oder während der Fahrt neu, wechselt die App nach zwei Malen schrittweise auf schonenderes Zeichnen.
- Android Auto, schwarzer Bildschirm: Behoben, wenn Android Auto die App startet, bevor sie auf dem Handy geöffnet wurde (z. B. nach einem Update).
- Android Auto, Linkslenker: Dein Pfeil liegt nicht mehr unter den Karten, und das Tempolimit-Schild ragt nicht mehr über den rechten Rand.
- Android Auto, Tacho: Sitzt wieder unten rechts neben der Fahrtkarte statt mitten auf dem Bildschirm.
- Android Auto, breite Bildschirme: Die Fahrtkarte zieht sich nicht mehr über den ganzen Bildschirm.
- Updates mit AAEnabler: Wurde Horizon Nav mit AAEnabler installiert, speichert „Aktualisieren“ die neue Version im Download-Ordner, damit sie in Android Auto bleibt.
- Verkehrsbanner: Der Hinweis „Verkehr in deiner Nähe“ verschwindet nach 5 Sekunden und bleibt nicht mehr dauerhaft stehen.
- Verkehrsübersicht im Stand: Die lange Erklärung erscheint nicht mehr bei jedem Anhalten und Losfahren.
- Kategorien auf Deutsch: Tankstellen, Ladesäulen, Essen, Kaffee, Parkplätze, Toiletten und Raststätten heißen in der Suche, den Kartenoptionen und in Android Auto jetzt deutsch, ebenso Meldungen wie Gefahr, Panne und mobiler Blitzer.
- Alles auf Deutsch: Meldungen, Ansagen, Schaltflächen, Warnungen, Kartenoptionen, Offline-Karten, Update-Hinweise und Android Auto sind jetzt deutsch (auf englisch eingestellten Geräten bleibt es englisch).
- Neu-Karte nach mehreren Updates: Zeigt keine internen Zeichen wie „(1023, ###“ mehr, wenn du eine Version übersprungen hast.
- Einrichtung: Die Auswahl des Suchbereichs („Nur in Deutschland“ / „Überall“) wird wieder vollständig angezeigt.

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
