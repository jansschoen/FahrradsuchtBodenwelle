# Bodenwelle 0.11.0 – Android-Testversion

[APK 0.11.0 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.11.0.apk). Als Update über die vorhandene App installieren, **nicht vorher deinstallieren**. App-ID und Signatur bleiben gleich; Messungen, gemeinsame Fotos, Bündelungen, Fahrtspur und Freigaben bleiben erhalten.

## Neu: PDF-Berichte für das Bezirksamt

Die Mail enthält ein kurzes, höfliches Anschreiben und einen gestalteten PDF-Bericht mit nummerierter Karte, Fundstellentabelle, gegebenenfalls ergänzenden Angaben und ausgewählten Fotos. Koordinatenfelder in der Tabelle öffnen die passende OSM-Webkarte. Die Behörde erhält GPS-Genauigkeit, Schadensart, Verkehrsfläche, Oberfläche, Kalenderwochen und nachvollziehbare Messungsnummern; Sensorreaktionen werden nicht als Schadenshöhen oder Rechtsverstöße ausgegeben.

1. **Fundstellen → Meldung vorbereiten → Mailentwürfe erzeugen** öffnen. Stellen und Bezirke weiterhin selbst prüfen.
2. Gewünschte Fotos auswählen; sie werden im PDF bei der passenden Fundstelle eingebettet. **Fotos zusätzlich einzeln anhängen** ist optional und zunächst aus.
3. **PDF ansehen** erstellt den Bericht und öffnet die interne Vorschau. Seiten einzeln wechseln, mit zwei Fingern oder Doppeltippen vergrößern und zum Lesen verschieben.
4. Zurück zur Meldung und **Im Mailprogramm öffnen** wählen. Bei gleicher Auswahl wird exakt das zuvor geprüfte PDF übergeben. Erst im Mailprogramm wird versendet.

**PDF neu erstellen** aktualisiert Bericht und Kartenhintergrund. Die App verwendet vorhandene OSM-Kacheln und ergänzt bei verfügbarem Internet nur den benötigten Ausschnitt. Fehlende Teile werden ausdrücklich als unvollständige Straßenkarte gekennzeichnet; alle Nummern und Koordinaten bleiben erhalten. Der Live-Abruf wird von diesem Cloud-Proxy mit HTTP 403 blockiert und muss auf dem Fairphone geprüft werden. Das öffentliche Beispiel zeigt deshalb bewusst die markierte Offline-Ansicht.

Es bleiben höchstens 20 Fundstellen je Bericht und zehn gewählte Fotos / 10 MB möglich. Die Bezirksaufteilung bleibt erhalten. Nur ausgewählte freigegebene Messungen werden gemeldet; automatische Zeitangaben sind ausschließlich Kalenderwochen. Eigene Notizen bleiben zunächst aus. Gemeinsame Angaben und Fotos eines Bündels stehen wie bisher allen enthaltenen Messungen zur Verfügung. HTML-Gestaltung des kurzen Anschreibens wird zusätzlich angeboten; die konkrete Darstellung hängt vom Mailprogramm ab, während das PDF sein Layout behält.

[PDF-Beispiel mit erfundenen Daten](../docs/examples/0.11.0/Fundstellenbericht-Beispiel.pdf) · [Vorschaubilder](../docs/examples/0.11.0/README.md) · [PDF-Anleitung und Datenschutz](../docs/PDF-BERICHT.md)

## Prüfung

290 automatisierte Tests bestehen. Neu geprüft sind echte mehrseitige PDFs, eingebettete Schrift und durchsuchbarer Text, anklickbare Kartenfelder, lange Tabellen und Notizen, identische Nummerierung mit Bündeln und Fotos, zeitlich anonymisierte Exportdaten, der Offline-Kachelcache, lesbare native Vorschau mit Seitenwechsel und Drehung, eingebettete Bilder, optionale einzelne Anhänge, unveränderte PDF-Wiederverwendung, widerrufene Freigaben, schreibgeschützte Berichtsdateien, Pfadschutz, HTML-Escaping und Abbruch ohne nachträgliche Mailöffnung. Die bisherigen Tests zu Sensorik, Migration, gemeinsamen Angaben, Datenschutz und Navigation bestehen weiterhin. Mail-Frameworktests laufen auf simuliertem Android 9 und 15, PDF-/Vorschauprüfungen mit nativer Grafikverarbeitung auf simuliertem Android 15.

Android-Build, bisherige APK-Signatur und öffentlicher Download sind geprüft. Lint meldet keine Fehler und die zwei bisherigen Hinweise zur Annotation-Abhängigkeit und zum Kartenkonstruktor. Das konkrete Mailprogramm und der Live-Kartenabruf bleiben auf dem Fairphone zu erproben. Android 8.0 oder neuer wird benötigt; die App misst keine automatische Schadenshöhe und trifft keine rechtlich verbindliche Einstufung.

PDFBox-Android (Apache 2.0) und eingebettete Noto-Sans-Schriften (SIL OFL 1.1) sorgen für eine gleichbleibende, durchsuchbare Darstellung. Lizenztexte liegen in den App-Assets.

Dateigröße: 4379397 Bytes. [SHA-256-Prüfsummen](SHA256SUMS).
