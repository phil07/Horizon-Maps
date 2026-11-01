# Horizon Nav über Google Play (interner Test) einrichten

Seit Juni 2026 zeigt Android Auto selbst installierte Navigations-Apps nicht mehr an, auch nicht mit „Unbekannte Quellen“. Über den internen Testkanal von Google Play erscheint Horizon Nav wieder in Android Auto. Die Play-Version heißt intern `com.headunit.nav` und lässt sich neben der GitHub-Version installieren.

Einmalig nötig, danach läuft alles automatisch.

## 1. Upload-Schlüssel in GitHub eintragen

Du hast von Claude zwei Dinge bekommen: den Schlüssel als Text (Base64) und ein Passwort.

1. GitHub > dein Repo > **Settings** > **Secrets and variables** > **Actions** > **New repository secret**.
2. Name `PLAY_UPLOAD_KEYSTORE_BASE64`, Wert: der komplette Text aus der Schlüssel-Datei.
3. Noch ein Secret: Name `PLAY_UPLOAD_KEYSTORE_PASSWORD`, Wert: das Passwort.

Ab dem nächsten Build liegt im Release jeder Version zusätzlich die Datei `HorizonNav-play.aab`.

Bewahre Schlüssel-Text und Passwort zusätzlich an einem sicheren Ort auf (z. B. Passwort-Manager) und lege sie niemals ins Repo.

## 2. Entwicklerkonto anlegen

1. [play.google.com/console](https://play.google.com/console) öffnen, mit deinem Google-Konto anmelden.
2. Kontotyp **Privat** wählen, die Gebühr (einmalig 25 $) bezahlen und die Identitätsprüfung mit Ausweis abschließen. Die Prüfung kann einige Tage dauern.

## 3. App anlegen

1. **App erstellen**: Name `Horizon Nav`, Standardsprache Deutsch, **App**, **Kostenlos**, Erklärungen bestätigen.
2. Unter **App-Inhalte** (links unten, „Richtlinie und Programme“) alles ausfüllen:
   - **Datenschutzerklärung**: `https://github.com/phil07/Horizon-Maps/blob/main/docs/datenschutz.md` (vorher dort Name und Kontakt eintragen).
   - **Werbung**: Nein.
   - **App-Zugriff**: Alle Funktionen ohne Anmeldung verfügbar.
   - **Inhaltsbewertung**: Fragebogen ausfüllen (keine problematischen Inhalte).
   - **Zielgruppe**: ab 18.
   - **Datensicherheit**: Standort (genau und ungefähr) wird erhoben und weitergegeben, für App-Funktionen, nicht verpflichtend gespeichert; Übertragung verschlüsselt; keine Kontodaten. Geräte-Kennung (zufällig, für geteilte Meldungen).
   - **Vordergrunddienste**: Standort, Zweck „Navigation“.

## 4. Erster Upload (von Hand, verlangt Google)

1. Im neuesten Release auf GitHub die Datei `HorizonNav-play.aab` herunterladen.
2. Play Console > **Testen** > **Interner Test** > **Tester**: E-Mail-Liste anlegen, deine eigene Adresse (und die von Mittestern) eintragen, speichern.
3. **Neuen Release erstellen**. Bei der Frage nach der App-Signatur **„Von Google verwalteter Schlüssel“** (Play App Signing) bestätigen.
4. Die `.aab`-Datei hochladen, Release speichern, **Prüfen** und **Einführung starten**.
5. Unter **Tester** den **Link kopieren** und auf dem Handy öffnen, Teilnahme bestätigen und Horizon Nav aus dem Play Store installieren.

Danach erscheint Horizon Nav in Android Auto (Unbekannte Quellen ist dafür nicht mehr nötig).

## 5. Automatischer Upload (optional, empfohlen)

Damit jeder Build automatisch im internen Test landet:

1. [console.cloud.google.com](https://console.cloud.google.com): Projekt anlegen, unter **APIs & Dienste** die **Google Play Android Developer API** aktivieren.
2. **IAM & Verwaltung** > **Dienstkonten** > **Dienstkonto erstellen** (Name z. B. `github-upload`), danach beim Dienstkonto **Schlüssel** > **Schlüssel hinzufügen** > **JSON**. Die Datei wird heruntergeladen.
3. Play Console > **Nutzer und Berechtigungen** > **Neue Nutzer einladen**: die E-Mail-Adresse des Dienstkontos (endet auf `iam.gserviceaccount.com`) eintragen, für die App Horizon Nav die Berechtigung **„Releases in Test-Tracks veröffentlichen“** geben.
4. GitHub-Secret `PLAY_SERVICE_ACCOUNT_JSON` anlegen, Wert: der komplette Inhalt der JSON-Datei.

Meldet der Upload „Only releases with status draft may be created on draft app“, lege unter **Settings** > **Secrets and variables** > **Actions** > **Variables** die Variable `PLAY_RELEASE_STATUS` mit dem Wert `draft` an, bis die App einmal von Hand eingeführt wurde; danach die Variable wieder löschen.

## Unterschiede der Play-Version

- Updates kommen über den Play Store, nicht über den eingebauten Updater.
- Die Liste der Widgets für den Akkustand zeigt nur Apps, die Widgets anbieten (Google erlaubt keinen Zugriff auf die Liste aller Apps).
- Einstellungen und Favoriten musst du einmal neu einrichten oder über die Sicherung in den Einstellungen übertragen.
