# Bodenwelle 0.15.0 – Android-Testversion

[APK 0.15.0 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.15.0.apk). Als Update über die vorhandene App installieren, **nicht vorher deinstallieren**. App-ID und Signatur bleiben gleich; Messungen, Bündelungen, gemeinsame Fotos, Fahrtspur und Freigaben bleiben erhalten. Android 8.0 oder neuer wird benötigt.

## Eine Datei für die Bearbeitung am Computer

1. Fahrt beenden. **Mehr → Daten verwalten → Am Computer bearbeiten / Rückimport → Projektdatei für den Browser speichern** öffnen.
2. Die gespeicherte HTML-Datei auf den Computer übertragen und dort im aktuellen Browser öffnen. Sie enthält bereits Editor, Messdaten, Fotos und letzte Fahrtspur. Es gibt keine Installation oder Anmeldung.
3. Über Liste und Karte eine Stelle auswählen. Art, Beschreibung, Verkehrsfläche, Oberfläche, Gefährdungsmerkmale, Fotos, Bündelung, individuell gemessene Höhe, Freigabe und Bezirk lassen sich bearbeiten. Originalmesswerte sind schreibgeschützt.
4. **Bearbeitete Datei speichern** wählen und den angebotenen Download im Downloadordner prüfen. Die neue HTML-Datei zurück auf das Handy kopieren und über **Bearbeitete Projektdatei öffnen** auswählen.
5. Vorschau und gegebenenfalls Konflikte prüfen. Erst **Sichern und importieren** übernimmt Änderungen. Neue Handymessungen und die aktuelle Spur bleiben erhalten. Eine vollständige lokale Sicherung ermöglicht **Letzten Import rückgängig machen**.

Für jede weitere Bearbeitungsrunde nach erfolgreichem Rückimport eine **neue Datei vom Handy exportieren**. Die Rückgabe gehört zur ursprünglichen App-Installation; Deinstallation oder das Löschen der App-Daten entfernt die Exportgrundlage. Die Datei enthält genaue Zeiten, Standorte und private Angaben. Für das Bezirksamt weiterhin die gesonderte PDF-/Mail-Funktion verwenden.

[Ausführliche Anleitung einschließlich Größenlimits](../docs/BROWSER-NACHBEARBEITUNG.md) · [Bilder](../docs/screenshots/0.15.0/README.md) · [Beispiel mit erfundenen Daten als ZIP herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-Browser-Beispiel.zip).

## Karte online und offline

Im Browser zeigt die Karte nummerierte Stellen, gemeinsame Bündel, Freigabehaken, GPS-Genauigkeitskreis und optional die letzte Fahrtspur. Klicks öffnen die Beschreibung, überlappende Punkte eine Auswahl. Suchen und Filtern wirken auch auf die Karte.

Nur der OSM-Straßenhintergrund benötigt Internet; Messpunkte, Fotos, Spur und Bearbeitung bleiben offline verfügbar. Der Hintergrund lässt sich abschalten. Kartenabrufe übertragen den angefragten Ausschnitt und die IP-Adresse an den Anbieter, keine Beschreibungen, Fotos oder Sensorwerte. Eine vollständige Offline-Straßenkarte ist nicht enthalten.

## Prüfung und Grenzen

382 Android-Prüfungen bestehen. Die neuen Prüfungen decken Export und Rückgabe aller Beschreibungsfelder, neue Handymessungen, Konfliktauswahl, Vorschauänderungen, Galerieübernahme, gemeinsame Fotos, Bündeländerungen, Löschen, Sicherung und Wiederherstellung einschließlich älterer ungeklärter Bündel ab. Rohdatenänderungen und fremde Exportdateien werden abgewiesen. Neue Fotos werden als JPEG ohne ursprüngliche EXIF-Orts-/Zeitangaben aufbereitet. Datenbankmigration auf Schema 9, Dateidialoge und der native Importablauf sind geprüft.

Zusätzlich wurde ein echter Androidexport in Chromium bearbeitet, mit einem neuen Foto gespeichert und erfolgreich zurückimportiert. Die Browserprüfung verwendet echte Bedienelemente, Dateiauswahl und Downloadereignisse. Die gespeicherte Datei wurde erneut geöffnet und in Android auf unveränderte Rohwerte, neue Handyaufzeichnungen und Fahrtspur geprüft.

**Die Cloud-Browserrichtlinie blockiert das direkte Öffnen von `file://`.** Der Test hat denselben HTML-Inhalt als Testdokument gerendert. Der Doppelklick auf deinem Computer, Firefox, Safari und das reale Fairphone sind damit noch nicht geprüft. Die Browserprüfung blockiert OSM-Kacheln absichtlich; die Bilder zeigen entsprechend den Offline-Rasterhintergrund. Der reale Straßenhintergrund muss am Computer mit erreichbarem Kartenserver geprüft werden.

Build und Lint bestehen; Lint meldet keine Fehler und zwei bisherige Hinweise zur Annotation-Abhängigkeit und zum Kartenkonstruktor. Die APK verwendet die bisherige Signatur. Die [Prüfsummen](SHA256SUMS) gehören zu den bereitgestellten Dateien.

Die bisherigen Funktionen einschließlich Kartenprüfung nach Fahrtende, Park-/Privat-Hinweisen, Bündelung, Fotos, PDF und Mail sind weiterhin enthalten. [Anleitung für den Fahrtest](../docs/FAHRTEST.md).

APK-Dateigröße: 4511462 Bytes. SHA-256: `06e6e605e3fe1a93fdcbc01daf1a47ff89a60248c323b21725780c9e407b7f2d`.
