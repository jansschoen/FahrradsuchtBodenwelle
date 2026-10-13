# Bodenwelle 0.2.2 – Android-Testversion

Dies ist eine Testversion für persönliche Fahrtests.

## Herunterladen und installieren

[APK 0.2.2 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.2.2.apk). Öffne die Datei anschließend auf deinem Android-Handy. Falls Android nachfragt, erlaube die Installation aus dieser Quelle. Die APK muss nicht entpackt werden.

Installiere sie als Update über die vorhandene App. App-ID und Signatur sind mit 0.2.0 und 0.2.1 identisch; lokale Fundstellen und Fahrtspur bleiben beim Update erhalten. Eine Deinstallation würde deine lokalen Daten löschen.

Android 8.0 oder neuer und ein Beschleunigungssensor werden benötigt. Erlaube den genauen Standort während der Nutzung. Neue OpenStreetMap-Kartenkacheln benötigen Internet; Fundstellen und Fahrtspur bleiben lokal. Es gibt keinen automatischen Versand an Behörden. Nur bewusst geprüfte Fundstellen werden auf Wunsch als CSV gespeichert.

## Neu in 0.2.2

Die Auslöseschwelle startet bei **40 m/s²**. An der Karte öffnet **Empfindlichkeit einstellen** den Regler von 5 bis 100 m/s²: links weniger Treffer, rechts mehr. Die Änderung wird gespeichert und gilt sofort während einer laufenden Aufzeichnung. Bitte nur im sicheren Stand bedienen.

Neue automatische Fundstellen werden aus GPS-Fahrtrichtung, Geschwindigkeit und Zeitabstand zum stärksten Sensorstoß auf dessen Zeitpunkt korrigiert. Dies ist eine Schätzung bei gleichbleibender Bewegung. Originalkoordinaten und Korrektur bleiben nachvollziehbar; bestehende Einträge bleiben unverändert. Die App misst keine verlässliche Bodenwellenhöhe und trifft keine rechtliche Einstufung.

## Prüfstand

67 automatisierte Tests bestehen, einschließlich Live-Änderung der Empfindlichkeit auf simuliertem Android 9 und 15, vorwärts/rückwärts gerechneter Positionen, gespeicherter Einstellungen, Export und Datenbankmigration. Android-Build und APK-Signatur sind geprüft; Lint meldet keine Fehler und eine bereits bestehende Warnung zur Konstruktorform der programmgesteuerten Kartenansicht. Diese Prüfungen ersetzen keine praktische Vergleichsfahrt auf dem Fairphone.

Die APK ist 668690 Bytes groß. Die SHA-256-Prüfsumme steht in [SHA256SUMS](SHA256SUMS). Mehr zum Fahrtest: [FAHRTEST.md](../docs/FAHRTEST.md). Zur möglichen Höhenschätzung: [HOEHENMESSUNG.md](../docs/HOEHENMESSUNG.md).
