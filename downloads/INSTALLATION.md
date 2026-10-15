# Bodenwelle 0.10.0 – Android-Testversion

[APK 0.10.0 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.10.0.apk). Installiere sie **als Update über die vorhandene App**. Bitte nicht zuerst deinstallieren. App-ID und Signatur bleiben gleich; vorhandene Messungen, Fotos, Fahrtspur und Freigaben werden übernommen.

## Gemeinsame Angaben für gebündelte Messungen

**Fotos, Verkehrsfläche, Oberfläche und zusätzliche Gefährdungsmerkmale** gelten gemeinsam für alle Messungen eines bestätigten Bündels. Das gilt beim Bündeln und für spätere Änderungen unter **Prüfen & Fotos**.

- Beim Zusammenführen werden Gefährdungsmerkmale vereinigt. Späteres An- oder Abwählen wird für alle Bündelmitglieder gespeichert.
- Manuelle Verkehrsflächen- und Oberflächenangaben haben Vorrang. Bei widersprüchlichen manuellen Angaben musst du vor der Bestätigung einen gemeinsamen Wert oder **Unbekannt** wählen. Abbrechen übernimmt nichts.
- Neue oder vorhandene Fotos sind bei allen Bündelmitgliedern sichtbar. Die App speichert jede bereinigte Bilddatei nur einmal. Der gemeinsame Speicher umfasst fünf Plätze je enthaltener Messung.
- Gemeinsame Fotos bleiben beim Löschen einer einzelnen Messung für die übrigen erhalten. Das Entfernen eines Fotos in der Galerie gilt für das ganze Bündel. Nach dem Auflösen betrifft es nur die ausgewählte getrennte Fundstelle.
- Auflösen trennt die Messungen wieder. Die übernommenen Angaben und Fotos bleiben bei allen erhalten; frühere abweichende Angaben werden dadurch nicht wiederhergestellt. Vorhandene Fotos bleiben auch oberhalb des danach kleineren Fotospeicherlimits erhalten.
- Messungsnummern, Sensorwerte, Zeitpunkte, Koordinaten, gemessene Höhen, Schadensarten, Notizen, Bezirkszuordnungen und Versandfreigaben bleiben individuell. Eine Bearbeitung gibt keine andere Messung automatisch frei.

Fotoänderungen werden wie bisher sofort gespeichert. **Abbrechen** im Prüfdialog nimmt sie nicht zurück; gemeinsame Text-/Auswahlfelder werden erst mit **Speichern** übernommen.

## Bereits vorhandene Bündel und Meldungen

Die Datenbankmigration übernimmt Fotos und eindeutige gemeinsame Angaben auch in bestehende Bündel. Widersprüchliche manuelle Angaben werden bewahrt und in der Fundstellenliste mit **Gemeinsame Angaben prüfen** markiert. Über **Prüfen & Fotos** kannst du sie abgleichen. Bis dahin werden für solche alten Bündel keine neuen Mailentwürfe erzeugt.

Eine Mail enthält weiterhin nur ausgewählte freigegebene Messungen. Ihre gemeinsamen Fundstellenfotos erscheinen je gemeldeter Fundstelle **einmal**, auch wenn das Foto ursprünglich bei einer anderen Bündelmessung ergänzt wurde. Fotos anderer Fundstellen werden nicht angeboten. Die Fotoauswahl lässt sich individuell ändern und bleibt beim Drehen erhalten. Auch nach dem Auflösen kann dasselbe gemeinsame Bild für zwei getrennte Fundstellen unabhängig gewählt werden. Höchstens zehn Fotos / 10 MB je Mail, Kalenderwochen statt genauer Messzeiten und bewusster Versand im externen Mailprogramm bleiben bestehen.

[Vorschau mit Beispieldaten](../docs/screenshots/0.10.0/README.md) · [Ausführliche Anleitung](../docs/BUENDELUNG-FOTOS.md) · [Fahrtest](../docs/FAHRTEST.md)

## Prüfstand

280 automatisierte Tests bestehen. Neue Prüfungen auf simuliertem Android 9 und 15 decken gemeinsame Metadaten, Konfliktauswahl und Abbruch, spätere Bearbeitung einschließlich Entfernen von Gefährdungsmerkmalen, individuelle Freigaben und Höhen, gemeinsame OSM-Vorschläge und manuelle Korrekturen, transiente OSM-Fehler nach Löschen der Ursprungsmessung, eine Bilddatei mit mehreren Zuordnungen, Ergänzen und Entfernen von Fotos, Auflösen und erneutes Bündeln, Löschung des ursprünglichen Fotoimports, Mailauswahl ohne Duplikate oder fremde private Fotos, getrennte Fotoauswahl nach Drehung sowie die Migration bestehender Bündel aus Schema 6 ab. Frühere Migrations-, Sensor-, Foto-, Datenschutz-, Karten- und Navigationstests bestehen weiterhin.

Android-Build und APK-Signatur sind geprüft. Lint meldet keine Fehler und die zwei bestehenden Hinweise zur Annotation-Abhängigkeit und zum Konstruktor der Kartenansicht. Die Vorschauen sind aus nativen Android-Ansichten mit Beispieldaten gerendert und keine Fairphone-Aufnahmen.

Android 8.0 oder neuer wird benötigt. Kamera und konkretes Mailprogramm bleiben auf dem Fairphone zu erproben. Der Live-Overpass-Abruf ist aus dieser Cloud weiterhin blockiert; die OSM-Logik wird mit kontrollierten Antworten geprüft. Die App berechnet keine automatische Schadenshöhe und trifft keine rechtlich verbindliche Einstufung. Bezirksgrenzen bleiben korrigierbare Vorschläge auf dem bisherigen Datenstand.

Dateigröße: 1071945 Bytes. SHA-256-Prüfsumme: [SHA256SUMS](SHA256SUMS).
