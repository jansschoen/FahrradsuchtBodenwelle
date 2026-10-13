# Bodenwelle 0.4.0 – Android-Testversion

[APK 0.4.0 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.4.0.apk). Öffne sie auf dem Fairphone und installiere sie **als Update über die vorhandene App**. App-ID und Signatur bleiben gleich; lokale Einträge, Fahrtspur und Empfindlichkeit bleiben erhalten. Bitte nicht zuerst deinstallieren.

## Neu ausprobieren

**Empfindlichkeit einstellen**: Nach rechts steigt der Zahlenwert und es entstehen weniger Treffer. Nach links sinkt er und es entstehen mehr Treffer. Du kannst eine ganze Zahl von 5 bis 100 m/s² eingeben und mit **Übernehmen** anwenden. Standard: 40 m/s². Bitte nur im sicheren Stand bedienen.

Nach der Prüfung und Freigabe von Fundstellen wählst du **Meldung an die Verwaltung vorbereiten**. Prüfe die Auswahl und Bezirksvorschläge; **Nach Bezirk aufteilen** erstellt getrennte Entwürfe. Alle zwölf Bezirkskontakte wurden auf Berlin.de geprüft. Der offline gebündelte Grenzdatenstand ist jedoch von 2013; Grenznähe und tatsächliche Zuständigkeit müssen geprüft werden. Manuelle Korrektur ist möglich.

Die Mail enthält automatisch nur Kalenderwochen statt genauer Messzeitpunkte. Eigene Notizen sind zunächst ausgeschaltet. Persönliche Formulierung und ein kurzer Hinweis auf Bodenwelle sind wählbar; Betreff, Empfänger und Text bleiben bearbeitbar. **Im Mailprogramm öffnen** öffnet den Entwurf; erst dort versendest du die Mail. Es gibt keine automatisch versendeten Nachrichten oder Rohdatenanhänge. Weitere Hinweise: [MAIL-MELDUNGEN.md](../docs/MAIL-MELDUNGEN.md).

## Prüfstand

134 automatisierte Tests bestehen; Android-Build und APK-Signatur sind geprüft. Lint meldet keine Fehler und eine bestehende Warnung zur Konstruktorform der programmgesteuerten Kartenansicht. Neue Tests auf simuliertem Android 9 und 15 prüfen unter anderem Reglerumkehr und Zahleneingabe, Kalenderwochen am Jahreswechsel, Bezirksaufteilung, Grenzfälle, Migration, Mail-Intent, manuelle Empfänger, Widerruf der Freigabe und erhaltene Entwürfe beim Drehen des Handys.

Android 8.0 oder neuer und ein Beschleunigungssensor werden benötigt. Kartenkacheln und OSM-Straßentags brauchen Internet; die Aufzeichnung und gebündelte Bezirkszuordnung funktionieren offline. Die Live-Overpass-Anbindung sowie das konkrete Mailprogramm müssen auf dem Fairphone praktisch geprüft werden. Die App berechnet keine automatische Schadenshöhe und trifft keine rechtlich verbindliche Einstufung.

Dateigröße: 878730 Bytes. SHA-256-Prüfsumme: [SHA256SUMS](SHA256SUMS). Fahrtest: [FAHRTEST.md](../docs/FAHRTEST.md).
