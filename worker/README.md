# Horizon Nav – Server für geteilte Meldungen

Ein kleiner Cloudflare Worker mit D1-Datenbank. Die App schickt hierhin Meldungen (Polizei, Unfall, Gefahr,
Panne, mobiler Blitzer, Stau) und holt die Meldungen anderer Fahrer ab. Andere Fahrer können „Noch da“ oder
„Weg“ antworten. Gespeichert werden nur Art, Position, Zeit und eine zufällige Geräte-ID – anonym.

- `src/index.js` – der Worker (die API)
- `schema.sql` – die Tabellen (wird bei jedem Deploy erneut ausgeführt, ändert vorhandene Daten nicht)
- `wrangler.toml` – Einstellungen; die `database_id` setzt der Workflow nur auf dem Runner ein

Bereitgestellt wird alles automatisch von `.github/workflows/deploy-reports.yml` bei jedem Push auf `main`,
der etwas in `worker/` ändert (oder von Hand unter Actions > „Deploy reports server“ > Run workflow).
Stündlich räumt der Worker abgelaufene Meldungen, alte Stimmen und alte Zähler auf.

## Einmalige Einrichtung

1. Bei [Cloudflare](https://dash.cloudflare.com) anmelden (kostenloses Konto reicht) und einmal
   **Workers & Pages** öffnen. Dabei eine workers.dev-Subdomain wählen, falls Cloudflare danach fragt.
2. Unter **My Profile > API Tokens > Create Token** ein Token mit der Vorlage „Edit Cloudflare Workers“
   erstellen und zusätzlich die Berechtigung **Account > D1 > Edit** hinzufügen.
3. Die **Account ID** kopieren (rechts auf der Übersichtsseite von Workers & Pages).
4. Im GitHub-Repo unter **Settings > Secrets and variables > Actions** zwei Secrets anlegen:
   - `CLOUDFLARE_API_TOKEN` – das Token aus Schritt 2
   - `CLOUDFLARE_ACCOUNT_ID` – die Account ID aus Schritt 3
5. Unter Actions den Workflow **Deploy reports server** einmal von Hand starten. Er legt die Datenbank an,
   richtet die Tabellen ein, veröffentlicht den Worker und zeigt am Ende die URL
   (`https://horizon-reports.<subdomain>.workers.dev`) und prüft `/v1/health`.

Danach bekommt jede neue APK die Server-URL automatisch eingebaut (der APK-Workflow fragt die Subdomain
bei Cloudflare ab). Soll eine andere URL verwendet werden, die Repo-Variable `REPORTS_URL`
(Settings > Secrets and variables > Actions > Variables) setzen. In der App lässt sich die URL außerdem
in den Einstellungen überschreiben.

## API (kurz)

Jede Anfrage der App schickt den Header `X-Device: <zufällige ID>`.

- `POST /v1/reports` mit `{"kind":"jam","lat":48.1,"lon":11.5}` → `{"id":123,"merged":false}`
- `GET /v1/reports?s=&w=&n=&e=` (höchstens 6° je Seite) → `{"now":…,"reports":[…]}`
- `POST /v1/reports/<id>/vote` mit `{"still":true}` oder `{"still":false}` → `{"ok":true,"exp":…}`
- `GET /v1/health` → `{"ok":true}`

Schutz vor Spam: höchstens 10 neue Meldungen pro Gerät und 30 pro IP-Adresse je Stunde, 60 Stimmen pro
Gerät je Stunde; eine gleiche Meldung im Umkreis von 300 m zählt als „Noch da“ für die vorhandene.
