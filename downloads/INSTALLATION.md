# Bodenwelle 0.8.0 – Android-Testversion

[APK 0.8.0 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.8.0.apk). Installiere sie **als Update über die vorhandene App**. App-ID und Signatur bleiben gleich; Einträge, Fahrtspur und Einstellungen bleiben erhalten. Bitte nicht zuerst deinstallieren.

## Neue Navigation

- **Fahrt:** Karte, Standortgenauigkeit und Start/Ende der Aufzeichnung. Nach dem Start weiter zwei Sekunden im Stand kalibrieren. **Kartenoptionen** enthält Fahrtspuransicht, Empfindlichkeit, Neukalibrierung und Legende.
- **Fundstellen:** Kompakte Einträge mit **Prüfen & Fotos**, **Löschen** und **Details & Karte**. Über **Alle / Offen / Freigegeben** filtern. **Meldung vorbereiten** öffnet die Mailauswahl; **Weitere Aktionen** enthält manuelles Bündeln und CSV-Export. Vorschläge vergleichen jetzt **streng unter vier Meter** Abstand jedes Messpunktpaars; es wird weiterhin erst nach Bestätigung zusammengefasst.
- **Mehr:** Messung einstellen, Karte & Geodaten, Daten verwalten, Datenschutz und Hilfe. **Alle Fundstellen löschen** und **Fahrtspur löschen** liegen in der Datenverwaltung und brauchen weiterhin eine Bestätigung.

Der ausgewählte Bereich und der Fundstellenfilter bleiben beim Drehen des Handys erhalten. Wechsel zwischen den Bereichen unterbricht keine laufende Fahrt. Im Fahrtbereich bleibt die Karte auch bei neuen Messungen oder OSM-Ergänzungen erhalten.

Fotos, individuelle Freigaben, unebene Abschnitte und Mailentwürfe bleiben verfügbar. Der Mailversand erfolgt erst im externen Mailprogramm. Vier Meter Koordinatenabstand sind keine Zusicherung der GPS-Genauigkeit.

[Kalibrierung und Abschnitte](../docs/KALIBRIERUNG-ABSCHNITTE.md) · [Bündelung und Fotos](../docs/BUENDELUNG-FOTOS.md)

## Prüfstand

227 automatisierte Tests bestehen, einschließlich neuer Navigation, Filter und Drehung des Handys, fortgesetzter Aufnahme beim Bereichswechsel, sicherer Datenverwaltung, CSV-Untermenü, Vier-Meter-Vorschlägen, Kalibrierung im Stand, Unterdrückung horizontaler Beschleunigung, anhaltenden Schwingungen, gespeichertem Abschnitt beim Fahrtende, Abschnittsmetadaten, Kalenderwochen, Bezirksgrenzen, Kartenbearbeitung auf Android 9 und 15, direktem Löschen, Abbrechen, Auswahl einzelner Bündelbeobachtungen, Erhalt anderer Fotos und der Fahrtspur, Datenmigration, reversibler Bündelung, einzelner Freigaben, Kamera-/Bildauswahl und Mailanhängen auf simuliertem Android 9 und 15. Eine echte JPEG-Prüfung mit nativer Grafikverarbeitung auf simuliertem Android 15 bestätigt die Entfernung von GPS-, Zeit- und Kamerametadaten sowie korrekte Orientierung und ein unverändertes Original. Android-Build und APK-Signatur sind geprüft. Lint meldet keine Fehler, einen Hinweis zur älteren Annotation-Abhängigkeit und die bestehende Konstruktorwarnung der Kartenansicht.

Android 8.0 oder neuer und ein Beschleunigungssensor werden benötigt. Kamera und dein konkretes Mailprogramm müssen auf dem Fairphone praktisch geprüft werden. Die Bezirksgrenzen stammen weiterhin aus 2013 und liefern nur korrigierbare Vorschläge. Die App berechnet keine automatische Schadenshöhe und trifft keine rechtlich verbindliche Einstufung.

Dateigröße: 1032915 Bytes. SHA-256-Prüfsumme: [SHA256SUMS](SHA256SUMS). Fahrtest: [FAHRTEST.md](../docs/FAHRTEST.md).
