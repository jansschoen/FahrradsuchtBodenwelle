# Bodenwelle 0.9.0 – Android-Testversion

[APK 0.9.0 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.9.0.apk). Installiere sie **als Update über die vorhandene App**. App-ID und Signatur bleiben gleich; Messungen, Fotos, Fahrtspur und Einstellungen bleiben erhalten. Bitte nicht zuerst deinstallieren.

## Neu in dieser Version

- **Nachvollziehbare Nummern:** Bestehende und neue Messungen haben eine dauerhafte Nummer **#N**, sichtbar in Liste, Details, Bündelung und Karte. Auch CSV und Mail nennen die Nummern. Löschen hinterlässt Lücken; Nummern werden nicht neu vergeben. Mailentwürfe enthalten weiterhin ausschließlich ausgewählte, freigegebene Beobachtungen und Kalenderwochen statt genauer Zeiten.
- **Mehrfachtreffer vergleichen:** Vorschläge erscheinen als übersichtliche Karten mit Nummern, Freigabestatus, Messwerten und GPS-Genauigkeit. Vorschläge bleiben auf unter vier Meter Abstand begrenzt. Originale und einzelne Freigaben bleiben erhalten.
- **Auf der Karte bündeln:** **Kartenoptionen → Punkte auf der Karte bündeln** wählen, passende Fundstellen antippen, bei überlappenden Punkten die gewünschten Einträge auswählen und **Auswahl übernehmen**. Dann **Auswahl bündeln** drücken und die Nummern sowie Abstände vor **Zusammenfassen** prüfen. Alternativ im Punktmenü **Mit anderem Punkt bündeln** wählen; dieser Punkt ist bereits ausgewählt. Abbruch ändert keine Messungen. Die Auswahl bleibt beim Drehen erhalten. Eine Bündelung lässt sich weiterhin auflösen.
- **Dauerhafte Kartenfunde:** Die frühere Grenze von 500 Fundstellen entfällt. **×N** zählt Originalmessungen einer zoomabhängigen Anzeigegruppe, **#N** bezeichnet einen einzelnen Messpunkt. Antippen erreicht auch überlappende Fundstellen. Eine Anzeigegruppe ändert keine gespeicherte Bündelung. Ältere Listeneinträge sind über **Weitere Fundstellen anzeigen** erreichbar. Neue Fahrten und das Löschen der Fahrtspur entfernen keine Messungen.
- **OSM-Erkennung:** Wegsuche bis 50 m Abstand; GPS-Genauigkeiten bis 50 m werden mit entsprechend geringer Sicherheit berücksichtigt. Mehrere plausible Wege liefern nur dann einen Gebietsvorschlag, wenn bekannte Angaben übereinstimmen; widersprüchliche oder fehlende Werte bleiben unbekannt. Benachbarte Suchgebiete verwenden denselben Cache. Nach dem Minutenlimit zurückgestellte Abfragen werden bei geöffneter Haupt-/Kartenansicht oder laufender Aufzeichnung nachgeholt. Alte Auswertungen werden beim Öffnen erneut geprüft; manuelle Angaben bleiben geschützt.

[Vorschau der Oberfläche mit Beispieldaten](../docs/screenshots/0.9.0/README.md) · [Bündelung und Fotos](../docs/BUENDELUNG-FOTOS.md) · [OSM-Verfahren](../docs/KLASSIFIZIERUNG-OSM.md)

## Prüfstand

252 automatisierte Tests bestehen. Neu geprüft sind unter anderem: direkte Bündelung auf beiden Kartenansichten unter Android 9 und 15, überlappende Koordinaten, Vorauswahl aus dem Punktmenü, Abbrechen, Auswahl nach Drehung, Erhalt individueller Freigaben, Nummern nach Bündelung/Auflösen/Löschen, Kartenfunde oberhalb der bisherigen Grenze von 500, 1.200 Messungen in Anzeigegruppen, Nachladen älterer Listeneinträge, erweiterter OSM-Radius, nur teilweise übereinstimmende OSM-Angaben, Cache über Rastergrenzen, Projektionsmarge und zeitgesteuerte Wiederholungen. Die bisherigen Prüfungen zu Sensorik, Fotos, Migration, Datenschutz, Mail und Navigation bestehen weiterhin.

Android-Build und APK-Signatur sind geprüft. Lint meldet keine Fehler und die zwei bestehenden Hinweise zur Annotation-Abhängigkeit und zum Konstruktor der programmgesteuerten Kartenansicht. Die Vorschauen sind aus echten Android-Ansichten mit nativer Grafikverarbeitung auf simuliertem Android 15 gerendert; sie enthalten Beispieldaten und keine Fairphone-Aufnahmen.

Der Live-Overpass-Abruf ist hier weiterhin durch den Cloud-Proxy blockiert (erneut HTTP 403 am 15.10.2026). Die Logik wurde mit kontrollierten Antworten geprüft; die Erreichbarkeit und Zuordnungsqualität müssen auf dem Fairphone erprobt werden. Ein größerer Radius ergänzt keine in OSM fehlenden Tags. Kamera und dein konkretes Mailprogramm bleiben ebenfalls auf dem Gerät zu prüfen. Bezirksgrenzen stammen weiterhin aus 2013 und liefern korrigierbare Vorschläge.

Android 8.0 oder neuer und ein Beschleunigungssensor werden benötigt. Die App berechnet keine automatische Schadenshöhe und trifft keine rechtlich verbindliche Einstufung.

Dateigröße: 1058312 Bytes. SHA-256-Prüfsumme: [SHA256SUMS](SHA256SUMS). Fahrtest: [FAHRTEST.md](../docs/FAHRTEST.md).
