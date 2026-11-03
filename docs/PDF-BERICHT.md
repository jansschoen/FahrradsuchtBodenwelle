# PDF-Berichte an die Verwaltung

Ab Bodenwelle 0.11.0 enthält jede vorbereitete Mail einen übersichtlichen PDF-Bericht. Du erzeugst und prüfst ihn auf dem Handy; erst im Mailprogramm versendest du die Meldung.

## Bedienung

1. Unter **Fundstellen** die Messungen prüfen und einzeln freigeben. **Meldung vorbereiten** öffnet die Auswahl für Stellen und Bezirke.
2. **Mailentwürfe erzeugen** erstellt die Anschreiben. Die bisherige Bezirksaufteilung bleibt erhalten; höchstens 20 Fundstellen stehen in einem Bericht. Bestätigte Bündel zählen als eine Fundstelle.
3. Gewünschte Fotos auswählen. Sie erscheinen direkt in der Fotodokumentation des PDFs. **Fotos zusätzlich einzeln anhängen** ist optional und zunächst ausgeschaltet. Höchstens zehn Fotos mit zusammen 10 MB sind möglich.
4. **PDF ansehen** erstellt den Bericht und öffnet die Vorschau in der App. Seiten lassen sich einzeln durchblättern; mit zwei Fingern oder Doppeltippen vergrößerst du sie. **Zurück zur Meldung** führt zum Anschreiben zurück.
5. **Im Mailprogramm öffnen** übergibt das geprüfte PDF und gegebenenfalls zusätzliche Bilder. PDF-Vorschau und Mail verwenden bei gleicher Auswahl exakt dieselbe Berichtskopie. Ändern sich Auswahl, Fotos, Notizfreigabe oder Name, wird eine neue Kopie erstellt. **PDF neu erstellen** erneuert auch die Karte.

Beim Drehen bleiben Anschreiben und Fotoauswahl erhalten; der Bericht kann danach erneut erzeugt werden. Eine laufende Erstellung wird beim Schließen der Meldungsansicht abgebrochen. Danach wird keine Mailauswahl automatisch geöffnet. Ein bereits geöffnetes Mailprogramm bleibt für den Versand zuständig.

## Aufbau

- **Kartenübersicht:** Nummerierte GPS-Positionen, Verbindungslinien zu versetzten Nummern bei dicht liegenden Stellen, Nordpfeil und ungefähre Maßstabsleiste. Geschätzte unebene Abschnitte werden orange gezeichnet. Die Fahrtspur wird nicht übernommen.
- **Fundstellentabelle:** Nummer, Bezirk, Straße laut OSM soweit vorhanden, Koordinaten, GPS-Genauigkeit, Kalenderwochen, Schadensart, Verkehrsfläche, Oberfläche, ausgewählte Messungsnummern, Sensorreaktion, tatsächlich gemessene Höhe und zusätzliche Gefährdungsmerkmale. Die Lagefelder öffnen die passende OSM-Webkarte.
- **Ergänzende Angaben:** Freigegebene eigene Notizen, Abschnitte mit Endkoordinaten und Länge sowie lange Angaben, die nicht vollständig in die Übersicht passen. In der Tabelle wird auf den Anhang verwiesen; Inhalte werden nicht still abgeschnitten. Umfangreiche Notizen werden auf Folgeseiten verteilt.
- **Fotodokumentation:** Ausgewählte, bereinigte Bilder mit derselben Stellennummer wie Karte und Tabelle. Ein gemeinsames Foto wird je Fundstelle einmal verwendet. Fotos sind zusätzlich als einzelne Anhänge nur bei entsprechender Auswahl enthalten.

Die erbetenen Schritte beschreiben eine Zuordnung und örtliche Prüfung sowie gegebenenfalls geeignete Sicherungs- oder Instandsetzungsmaßnahmen. Der Bericht behauptet weder eine gesicherte Zuständigkeit noch einen Rechtsverstoß. Die Sensorwerte bleiben Erschütterungshinweise und werden nicht in Höhen oder Dringlichkeitsstufen umgerechnet.

Das Anschreiben ist kurz, höflich und bearbeitbar. Zusätzlich wird eine HTML-Darstellung mit klarer Typografie übergeben; ob sie angezeigt wird, hängt vom Mailprogramm ab. Der PDF-Bericht behält seine Gestaltung unabhängig davon.

## Karte und Offline-Verhalten

Die Berichtskarte verwendet den vorhandenen privaten OSM-Kachelcache der App. Frische Kacheln werden wiederverwendet; offline können auch bereits gespeicherte ältere Kacheln angezeigt werden. Benötigt werden nur Kartenteile des aktuellen Berichtsausschnitts auf einer Zoomstufe, höchstens 20. Bei Internetzugang werden fehlende Teile über HTTPS bei `tile.openstreetmap.org` nachgeladen. Cache-Fristen werden berücksichtigt; es gibt keinen Gebiets- oder Mehrzoom-Download. Der Anbieter sieht Kartenausschnitt und IP-Adresse, keine Notizen, Fotos oder Messwerte. Nach einem Abruffehler werden nicht fortlaufend weitere Netzabfragen versucht.

Fehlende Kartenteile werden durch einen ausdrücklich als unvollständig bezeichneten Positionsplan ersetzt. Alle ausgewählten Punkte, Nummern und Koordinaten bleiben erhalten. Über **PDF neu erstellen** lässt sich bei verfügbarer Verbindung oder besser gefülltem Cache ein neuer Bericht erzeugen. In dieser Cloud blockiert der Proxy den Live-Kachelabruf am 2. November 2026 mit HTTP 403; der tatsächliche Abruf muss auf dem Fairphone geprüft werden. Cache, Rasterzusammensetzung und Offline-Kennzeichnung werden automatisiert geprüft.

## Datenkontrolle und Dateien

PDF und Mail enthalten ausschließlich ausgewählte freigegebene Einzelmessungen. Die ursprünglichen Messzeiten, das Alter der GPS-Messung, OSM-Abfragezeitpunkte und Rohdatenexporte werden nicht übernommen. Automatische Zeitangaben sind ausschließlich Kalenderwochen mit ISO-Wochenjahr in `Europe/Berlin`. Eigene Notizen sind zunächst ausgeschaltet, da sie selbst genaue Zeiten oder private Angaben enthalten können. Gewählte Fotos stammen aus den bereits verkleinerten, von ursprünglichen EXIF-Orts-/Zeitangaben bereinigten App-Kopien; sichtbare private Bildinhalte musst du selbst prüfen.

Gemeinsame Angaben und Fotos eines bestätigten Bündels stehen jeder enthaltenen Messung zur Verfügung. Seit 0.12.0 gehört auch die Beschreibung zur gemeinsamen Fundstelle und kann ursprünglich bei anderen Bündelmitgliedern verfasste Absätze enthalten; sie wird nur bei aktivierter Notizübernahme verwendet. Unausgewählte Einzelmessungen werden nicht mitgezählt und ihre Messwerte und Zeiten werden nicht ergänzt. Vor Erstellung und Übergabe werden aktuelle Freigaben, Bündelmitgliedschaften, Fotozuordnungen und Dateien erneut geprüft. Ein älterer Bericht kann keine widerrufene Freigabe umgehen.

Jeder Bericht erhält eine zufällige Referenz ohne Messzeit und wird als unveränderliche PDF-Kopie im privaten, vom Backup ausgeschlossenen Speicher abgelegt. Nur die konkret übergebene Datei erhält zeitweilige Leserechte. Schreiben, Verzeichnisauflistung und beliebige Dateipfade sind über den Provider nicht möglich. Alte Berichtskopien werden beim App-Start nach sieben Tagen bereinigt; bereits gespeicherte Empfängerkopien bleiben davon unabhängig.

PDFs enthalten eingebettete Noto-Sans-Schriften und durchsuchbaren Text. Nicht in der Schrift darstellbare Zeichen werden nachvollziehbar als Unicode-Code `[U+…]` angegeben und im Dokument kenntlich gemacht. PDFBox-Android 2.0.27.0 steht unter Apache 2.0, die mitgelieferten Noto-Schriften unter SIL Open Font License 1.1. Die Lizenztexte sind in den App-Assets enthalten. Verschlüsselungs-/Signaturmodule werden nicht eingebunden; die Vorschau öffnet nur die eigenen privaten, unverschlüsselten Berichte.
