# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Verbessert
- Akku bei Ankunft: wird nur noch alle 10 Sekunden oder 200 m neu gerechnet (und sofort bei neuem Akkustand, neuer Route oder anderen Ladestopps). Vorher rechnete die App jede Sekunde die ganze Restroute durch, auf Handy und Auto.
- Akku-Schätzung auf der Route: Kälte und Heizung zählen mit der Temperatur des jeweiligen Abschnitts, nicht mit dem Durchschnitt der ganzen Strecke. Fahrten in die Berge oder in die Kälte werden dadurch genauer geschätzt.
- Akku-Lernstand: wird mit Namen gespeichert. Ein späteres Update kann neue Werte ergänzen, ohne den gespeicherten Stand zu verwerfen.
- Akku-Schätzung auf dem Handy: Die Anzeige zeigt jetzt dieselbe Schätzung wie im Auto (mit „ca.“, wenn der letzte Wert alt ist).
- Akku-Schätzung: Die Rückgewinnung bergab wird nicht mehr angerechnet. Die Schätzung bleibt auch auf Bergstrecken vorsichtig. Die Anzeige „Rückgewinnung bergab“ bei den gelernten Werten entfällt.
- Einstellungen, Fahrtenbuch: Der Bereich startet zugeklappt. Die Kopfzeile mit Anzahl und Strecke klappt die Liste auf und zu. „Löschen“ steht jetzt im aufgeklappten Bereich.
- Akku-Schätzung auf der Route: Bei einer Lücke im GPS (zum Beispiel im Tunnel) zählt die App die Strecke entlang der Route statt der Luftlinie. Die Lernlogik bekommt dadurch auch diese Strecke.
- Akku-Schätzung nach Neustart: Der Lernstand (Bezugsakkustand, Strecke seit dem Wert, Verbrauch je Straßenart) wird gespeichert. Startet die App zwischen zwei Fahrten neu, geht er nicht verloren und wird bei der nächsten Fahrt weiterverwendet.
- Akku-Lernen: Ist ein Stück der Fahrt nicht messbar (GPS-Lücke ohne Route), wird aus dieser Strecke nicht gelernt, bis der nächste Bezugswert gesetzt ist.
- Akku-Lernen nach dem Laden: Steigt der Akkustand zwischen zwei Werten (Laden), wird die Fahrt davor nicht mehr gelernt. Bisher konnte das Lernen bei Ladung unter den Startwert zu optimistisch werden. Ein geplanter Ladestopp setzt den Bezugswert neu.
- Akku-Lernen beim Laden: Solange die App am Ladegerät Laden erkennt, wird nichts gelernt.
- Akku nach Neustart beim Laden: Der geplante Ladestand und der Stand seit dem Ladestopp bleiben nach einem Neustart der App erhalten, auch wenn er während der Ladung passiert.
- Laden erkennen: Steht das Auto im Stand nahe einer Ladesäule und ist der neue Akkustand höher als der vorige, zeigen Auto und Handy „lädt“. Die Schätzung folgt dann dem Widget. Die Anzeige endet, sobald das Auto losfährt.
- Akku-Schätzung ohne Route: Höhenunterschiede des GPS zählen ab 5 m Änderung (bei guter Genauigkeit), auf Bergfahrten und Abfahrten wie mit Route.
- Akku und Reichweite im Auto: Zwischen zwei Werten vom Widget rechnet die App den Verbrauch seit dem letzten Wert mit, jede Sekunde. Die Anzeige zeigt dann „ca.“, wenn der letzte echte Wert älter als 10 Minuten ist. Ein gleich gebliebener Widget-Wert gilt als veraltet, wenn das Auto seitdem mehr als 1 % verbraucht hat. Beim Stehen auf einer Route zählt nur Heizung oder Klimaanlage (nicht beim Laden), ohne Route sinkt die Schätzung im Stand nicht.
- Akku bei Ankunft: vorsichtiger geschätzt (10 % mehr Verbrauch) und das Modell lernt schneller aus der Fahrt.
- Parkplatz-Karte am Ziel: größere Schrift und ein größeres X zum Schließen.
- Android Auto während der Fahrt: Beenden, Ladesäulen-Filter (bei Elektroautos), Ton und Melden stehen dauerhaft links auf der Karte. Die Leiste oben von Android Auto bleibt dafür leer.

### Behoben
- Akkustand nach einer Stunde: Ist der letzte Wert älter als 60 Minuten, nimmt die Schätzung ihn weiter (bis 6 Stunden, mit „ca.“). Vorher sprang sie auf den Startwert vor der Fahrt (80 %), auch wenn der Akku viel leerer war.
- Akku-Schätzung ohne Route: Die Temperatur wird jetzt auch ohne Route geholt. Kälte und Heizung zählen immer mit.
- Nutzbare Batterie mit Komma: „64,5“ wurde als 645 gelesen (und auf 200 kWh begrenzt). Ein Komma gilt jetzt als Dezimalzeichen. Ein so falsch gespeicherter Wert muss einmal neu eingegeben werden.
- Gefahrene Strecke bei langsamer Fahrt: Unter etwa 20 km/h wurde die Strecke bis zur Hälfte zu lang gezählt (Akku-Schätzung und Fahrtenbuch). Jetzt stimmt sie.
- Akku-Lernen nach Bergfahrten: Die Rückgewinnung bergab ging als niedrigerer Verbrauch in die Werte je Straßenart ein. Die Schätzung in der Ebene wurde dadurch zu optimistisch. Jetzt wird sie beim Lernen herausgerechnet. Fahrten, die fast nur bergab gehen, werden nicht gelernt.
- Neuberechnung: Tippst du während einer Neuberechnung auf Beenden oder wählst ein neues Ziel, kommt die alte Route nicht mehr zurück.
- Akkustand nach Neustart der App: Der alte Wert aus dem Widget wird nicht mehr als neuer Wert übernommen. Vorher sprang die Schätzung zurück.
- Akkustand im Stand: Ein neuer Wert wird sofort übernommen, nicht erst beim Losfahren.
- Akkustand aus dem Widget: Ein Ladeziel („Ziel 80 %“) wird nie als Akkustand gelesen.
- GPS-Lücke auf der Route: Sie zählt nur, wenn beide Punkte auf der Route liegen (nicht auf einem Umweg).
- Akku-Schätzung: Jede gefahrene Strecke wurde doppelt abgezogen (einmal mit Temperatur, Wind und Höhe, einmal nur mit dem Grundverbrauch). Jetzt nur noch einmal.
- Ortung beim Start einer Navigation: Der Pfeil springt nicht mehr von bis zu 30 m Entfernung auf die Route, sondern rastet erst ein, wenn du wirklich auf ihr bist (15 m).
- Android Auto: Beenden, Ladesäulen-Filter und Ton stehen während der Fahrt als eigene Knopfspalte links auf der Karte und bleiben immer sichtbar. Melden bleibt in der Leiste oben.
- Absturz beim Start in Android Auto (1.0.1300): Wenn das Auto eine Kartenansicht mit zu vielen Schaltflächen ablehnt, bleibt die App jetzt offen und zeigt die Karte mit „Melden“. Die Ursache wird im Log protokolliert (HorizonCar).
- Absturz beim Start in Android Auto (1.0.1299): Die Navigationsleiste hatte fünf statt höchstens vier Schaltflächen. Bei Elektroautos steht der Ladesäulen-Filter jetzt an der Stelle von 2D/3D; 2D/3D bleibt auf dem Handy und in den Einstellungen.
- Akkustand und Reichweite in Android Auto: Die Werte aus dem Widget (oder vom Auto) werden sofort übernommen und auf dem Autobildschirm neu gezeichnet. Vorher blieb ein alter Wert stehen, bis man die Karte neu öffnete. Das Widget hat Vorrang, das Auto liefert den Wert nur, wenn das Widget keinen aktuellen hat.
- Akkustand im Auto: Das Widget wird während der Verbindung jede Minute aktualisiert (vorher alle 10 Minuten ohne Auto-Wert).
- Restkilometer: Fehlen sie im Widget, werden sie aus Akkustand, Akkugröße und Verbrauch errechnet.
- Verbrauch: Die Einstellung „Effizienz (km pro kWh)“ wurde als Meilen pro kWh gelesen. Der Verbrauch war dadurch um rund ein Drittel zu niedrig geschätzt. Ein Komma wird jetzt auch angenommen (5,5).
- Verbrauch: Der unterwegs mitgezählte Verbrauch berücksichtigt die gelernte Korrektur je Straßenart, wie die Planung.

### Verbessert
- Ladesäulen-Filter während der Navigation: auf dem Handy in der Zoom-Spalte und in Android Auto in der Navigationsleiste. Eine Änderung plant die Ladestopps der Route neu.
- Ladesäulen während der Fahrt: Sie werden alle 15 Sekunden für die Kartenansicht und die nächsten 30 km der Route neu geladen, auch in Android Auto (vorher nur, wenn die Karte stillstand).
- Blitzer ohne Route: Warnung nur für Kameras, die voraus und höchstens 35 m seitlich der Fahrlinie liegen. Kameras auf anderen Straßen lösen keinen Alarm mehr aus.
- Blitzer auf der Route: Der Suchstreifen ist 30 m breit (vorher 40 m).
- Tag und Nacht: Die Systemeinstellung wird alle 1,5 Sekunden gelesen (vorher alle 4), dadurch schaltet die Karte schneller um.
- Neigung der 3D-Karte: Einstellungen > Karte (Spuren und Kreuzungen) setzt die Neigung getrennt für breiten und hohen Bildschirm, auf Auto oder 30 bis 65 Grad. Im Auto stellst du sie unter Android Auto > Einstellungen ein.
- Android Auto: Neue Zeile „Kartenrichtung“ in den Einstellungen. Nord oben oder mit der Fahrtrichtung, die Wahl wird für das Auto gemerkt.
- Verbrauch mit Anhänger oder Wohnmobil (Einstellungen > Fahrzeug): mehr Gewicht und mehr Luftwiderstand werden eingerechnet.
- Verbrauch: Heizung und Klimaanlage zählen nach Fahrzeit statt nach Strecke. Ein Stau im Winter kostet mehr Akku, eine flotte Fahrt weniger.
- Verbrauch: Staus und stockender Verkehr auf der Route fließen ein (langsamer, dafür länger unterwegs).
- Verbrauch: Ohne eingetragene Effizienz dient die Reichweitenanzeige des Autos als ruhige Grundlage, die sich nur langsam anpasst. Die gelernte Korrektur wird anteilig auf Stadt, Land und Autobahn verteilt.
- Ladeplanung: Kein versteckter Zuschlag von 10 % mehr. Die Sicherheit kommt aus deinen Reserven (an der Ladesäule und am Ziel), dadurch weniger unnötige Ladestopps.


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
