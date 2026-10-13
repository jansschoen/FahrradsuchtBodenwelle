# Bodenwelle 0.3.0 – Android-Testversion

[APK 0.3.0 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.3.0.apk). Öffne die Datei auf dem Fairphone und installiere sie **als Update über die vorhandene App**. Falls Android nachfragt, erlaube die Installation aus dieser Quelle. Die APK muss nicht entpackt werden.

App-ID und Signatur bleiben gleich. Vorhandene Fundstellen, Notizen, Freigaben, Fahrtspur und die eingestellte Empfindlichkeit bleiben erhalten; allgemeine alte Kategorien werden unter Erhalt der ursprünglichen Angabe migriert. Eine Deinstallation würde deine lokalen Daten löschen.

## Neu ausprobieren

- **Empfindlichkeit einstellen** an der Karte: standardmäßig 40 m/s², einstellbar 5–100. Links weniger Treffer, rechts mehr. Die gespeicherte Einstellung gilt sofort. Bitte nur im sicheren Stand bedienen.
- Neue automatische Positionen werden aus GPS-Fahrtrichtung, Geschwindigkeit und Zeitabstand zum stärksten Sensorstoß geschätzt. Ursprüngliche Koordinaten bleiben erhalten.
- Unter **Stelle prüfen** stehen sieben Unebenheitsarten und aufklappbare Zusatzmerkmale bereit. Eine tatsächlich gemessene Höhe kannst du ausdrücklich in Millimetern bestätigen. Ohne Höhenmessung bleibt die Intensität eine vorläufige Sensorbewertung, ohne Umrechnung in Millimeter.
- OSM ergänzt nach Möglichkeit Verkehrsfläche und Oberfläche. Unklare parallele Wege bleiben unbekannt. Manuelle Korrekturen bleiben geschützt. **Fehlende OSM-Angaben ergänzen** ermöglicht einen späteren Versuch; automatische Ergänzung ist abschaltbar.

Android 8.0 oder neuer und ein Beschleunigungssensor werden benötigt. Erlaube den genauen Standort während der Nutzung. Neue Kartenkacheln und Geodaten benötigen Internet. Die Anbieter erhalten angefragte Ausschnitte und die IP-Adresse; Notizen und Sensorwerte bleiben lokal. Offline-Aufzeichnung bleibt möglich. Nur geprüfte Fundstellen werden auf Wunsch als CSV gespeichert; es gibt keinen automatischen Behördenversand.

## Prüfstand und Grenzen

100 automatisierte Tests, Android-Build und APK-Signatur sind erfolgreich geprüft. Lint meldet keine Fehler und eine bestehende Warnung zur Konstruktorform der programmgesteuerten Kartenansicht. Tests decken unter anderem Klassengrenzen, Trennung von Höhen- und Sensorwerten, OSM-Tags, mehrdeutige Wege, Cache, Offline-Ergänzung, Serverpausen, manuelle Korrekturen und Datenmigration auf simuliertem Android 9 und 15 ab.

Der Cloud-Proxy blockiert den Live-Overpass-Endpunkt. Die Anbindung ist mit kontrollierten Antworten getestet, aber der tatsächliche Abruf und die Zuordnungsqualität müssen auf dem Fairphone geprüft werden. Eine automatische Höhenmessung oder rechtlich verbindliche Einstufung findet nicht statt.

Die APK ist 706879 Bytes groß. SHA-256-Prüfsumme: siehe [SHA256SUMS](SHA256SUMS). Weitere Anleitung: [FAHRTEST.md](../docs/FAHRTEST.md), [Klassifizierung und OSM](../docs/KLASSIFIZIERUNG-OSM.md), [Mögliche Höhenschätzung](../docs/HOEHENMESSUNG.md).
