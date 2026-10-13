# Bodenwelle 0.6.0 – Android-Testversion

[APK 0.6.0 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.6.0.apk). Installiere sie **als Update über die vorhandene App**. App-ID und Signatur bleiben gleich; Einträge, Fahrtspur und Einstellungen bleiben erhalten. Bitte nicht zuerst deinstallieren.

## Neu ausprobieren

**Löschen** steht direkt an jeder Fundstelle in der Liste. Tippe darauf und bestätige: Die einzelne Messung und ihre lokalen Fotos werden entfernt. Bei gebündelten Treffern wählst du zuerst die betroffene Messung. Andere Beobachtungen, ihre Fotos und die Fahrtspur bleiben erhalten. **Abbrechen** behält alles. Bereits exportierte Dateien und übergebene Mailkopien bleiben eigenständige Kopien.

Der Code enthält weniger doppelte Datenabfragen und ungenutzte Imports; der Fotobereich wird ohne feste Einfügepositionen aufgebaut. Die Downloadablage enthält nur die aktuelle Version und 0.5.0 als Reserve; ältere Test-APKs sind aus dem aktuellen Stand entfernt. Datenbankmigrationen für bestehende Installationen bleiben erhalten.

**Mehrfachtreffer bündeln** schlägt nahe Beobachtungen gleicher Art vor. Bestätige nur dieselbe tatsächliche Unebenheit. Messungen und einzelne Freigaben bleiben erhalten; die Bündelung lässt sich auflösen. Liste und Karte zeigen die gemeinsame Fundstelle mit der Beobachtungsanzahl.

Unter **Stelle prüfen und Fotos ergänzen** öffnet **Fotos ansehen und ergänzen** Kamera und Bildauswahl. Bis zu fünf Fotokopien je Beobachtung werden lokal verkleinert und ohne ursprüngliche EXIF-Orts-/Zeitangaben gespeichert. Albumoriginale bleiben unverändert. Bitte Bildinhalte prüfen und nur im sicheren Stand fotografieren.

Mailentwürfe fassen ausgewählte freigegebene Wiederholungen mit Anzahl, Kalenderwochen und Messwertbereichen zusammen. Du kannst Fotos ansehen und abwählen. Pro Mail sind 20 Fundstellen und zehn Fotos / 10 MB möglich; Dateinamen passen zur Nummer im Text. Erst im externen Mailprogramm wird versendet. Prüfe dort Empfänger, Text und tatsächliche Anhänge.

Weitere Anleitung: [BUENDELUNG-FOTOS.md](../docs/BUENDELUNG-FOTOS.md).

## Prüfstand

177 automatisierte Tests bestehen, einschließlich direktem Löschen, Abbrechen, Auswahl einzelner Bündelbeobachtungen, Erhalt anderer Fotos und der Fahrtspur, Datenmigration, reversibler Bündelung, einzelner Freigaben, Kamera-/Bildauswahl und Mailanhängen auf simuliertem Android 9 und 15. Eine echte JPEG-Prüfung mit nativer Grafikverarbeitung auf simuliertem Android 15 bestätigt die Entfernung von GPS-, Zeit- und Kamerametadaten sowie korrekte Orientierung und ein unverändertes Original. Android-Build und APK-Signatur sind geprüft. Lint meldet keine Fehler, einen Hinweis zur älteren Annotation-Abhängigkeit und die bestehende Konstruktorwarnung der Kartenansicht.

Android 8.0 oder neuer und ein Beschleunigungssensor werden benötigt. Kamera und dein konkretes Mailprogramm müssen auf dem Fairphone praktisch geprüft werden. Die Bezirksgrenzen stammen weiterhin aus 2013 und liefern nur korrigierbare Vorschläge. Die App berechnet keine automatische Schadenshöhe und trifft keine rechtlich verbindliche Einstufung.

Dateigröße: 995612 Bytes. SHA-256-Prüfsumme: [SHA256SUMS](SHA256SUMS). Fahrtest: [FAHRTEST.md](../docs/FAHRTEST.md).
