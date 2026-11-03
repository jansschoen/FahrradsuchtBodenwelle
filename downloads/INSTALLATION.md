# Bodenwelle 0.12.0 – Android-Testversion

[APK 0.12.0 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.12.0.apk). Als Update über die vorhandene App installieren, **nicht vorher deinstallieren**. App-ID und Signatur bleiben gleich; Messungen, gemeinsame Fotos, Bündelungen, Fahrtspur und Freigaben bleiben erhalten.

## Neu: gemeinsame Beobachtung und verständlichere OSM-Vorschläge

**Prüfen & Fotos** öffnet bei einem Bündel direkt die gemeinsame Fundstelle. Unebenheitsart, Beschreibung, Gefährdungsmerkmale, Verkehrsfläche und Oberfläche gelten nach dem Speichern für alle enthaltenen Messungen; Fotos sind weiterhin gemeinsam. Messungsnummern, Zeitpunkte, Koordinaten, Sensorwerte, Schwellen und tatsächlich gemessene Höhen bleiben einzeln erhalten. **Einzelmessungen, Höhen und Freigaben ansehen** führt zu den Originalmessungen. Freigaben bleiben standardmäßig unverändert und können nur über eine ausdrückliche Auswahl für alle geändert werden.

Beim Zusammenfassen bleiben unterschiedliche vorhandene Notizen zunächst als gemeinsame Absätze erhalten. Bekannte widersprüchliche Unebenheitsarten erfordern eine Auswahl. Auch bestehende Bündel werden beim Update angepasst; abweichende bekannte Arten bleiben bis zur Auswahl erhalten. Auflösen trennt die Messungen, stellt frühere unterschiedliche Beschreibungen jedoch nicht wieder her.

Die OSM-Abfrage verwendet jetzt den **ursprünglichen GPS-Mittelpunkt**. Die zeitlich korrigierte Ereignisposition bleibt gespeichert. Der Kreismittelpunkt ist weiterhin eine Schätzung. Ein ungenauer GPS-Kreis kann Radweg, Gehweg und Fahrbahn zugleich überdecken. Der Suchradius bleibt 50 m, weil größere Radien eher fremde Nachbarwege einbeziehen.

Bei mehreren unterscheidbaren Wegen kann der bestbewertete Weg einen ausdrücklich **unsicheren Vorschlag** liefern. Allgemeinere Angaben wie **Pflaster (Material unbekannt)** bleiben erhalten. Gleichwertige Widersprüche oder fehlende Belagsangaben werden nicht erfunden. Ein Internet-fähiges Netz braucht keine zusätzliche Android-Validierung mehr. Manuelle Korrekturen bleiben geschützt.

1. **Prüfen & Fotos → Verkehrsfläche und Oberfläche prüfen** öffnen.
2. **OSM-Vorschlag neu abfragen** fordert neue Daten an; bei geöffneter Bearbeitung aktualisiert sich der automatische Vorschlag. Die Minutenlimits und Fehlerpausen bleiben bestehen.
3. **OSM-Abrufdiagnose** zeigt Netzfehler, HTTP-Status, Cache-Nutzung und Zuordnung. Sie enthält keine Koordinaten, Notizen oder Sensorwerte. Unter **Mehr → Karte & Geodaten** stehen Diagnose und eine erneute Abfrage fehlender Angaben ebenfalls bereit.
4. Eine manuell gesetzte Angabe **Unbekannt** bleibt erhalten. Häkchen bei **selbst festlegen** entfernen und Speichern übernimmt wieder die automatische Auswertung.

Der echte Overpass-Abruf wurde am 3. November 2026 erneut beim HTTPS-Tunnel durch den Cloud-Proxy mit HTTP 403 blockiert. Die tatsächliche Erreichbarkeit auf dem Fairphone und die Ursache bisheriger unbekannter Werte müssen dort geprüft werden. Eine sichtbare OSM-Karte bestätigt nur den unabhängigen Kartenkachelabruf. [OSM-Erklärung und Grenzen](../docs/KLASSIFIZIERUNG-OSM.md) · [Bündelungsanleitung](../docs/BUENDELUNG-FOTOS.md) · [Bild der gemeinsamen Bearbeitung](../docs/screenshots/0.12.0/README.md).

Die PDF-Berichte mit Karte, Tabelle, Fotos und interner Vorschau aus 0.11.0 bleiben enthalten. [PDF-Anleitung](../docs/PDF-BERICHT.md) · [Beispiel mit erfundenen Daten und gekennzeichneter Offline-Karte](../docs/examples/0.11.0/Fundstellenbericht-Beispiel.pdf).

## Prüfung

307 automatisierte Tests bestehen. Neue Prüfungen decken gemeinsame Art und Beschreibung, Erhalt unterschiedlicher Rohmesswerte und tatsächlicher Höhen, ausdrückliche Freigaben, Konfliktauswahl, Migration von Schema 7 auf 8, direkte gemeinsame Bearbeitung und Zugang zu Originalmessungen ab. OSM-Tests prüfen den ursprünglichen GPS-Mittelpunkt trotz abweichender korrigierter Position, unsichere Nachbarweg-Vorschläge, unvertauschte fehlende Beläge, gleichwertige Kreuzungskandidaten, frische manuelle Abfragen mit gemeinsamem Cache und Anfragebudget, HTTP-Diagnosen ohne private Daten, Netze ohne Android-Validierung und nachgeladene Vorschläge ohne Überschreiben ungespeicherter manueller Angaben. Die bisherigen Sensor-, Foto-, PDF-, Mail- und Navigationstests bestehen weiterhin. Frameworktests laufen auf simuliertem Android 9 und 15; PDF-/Fotoprüfungen mit nativer Grafikverarbeitung auf simuliertem Android 15.

Android-Build und Lint sind geprüft. Lint meldet keine Fehler und die zwei bisherigen Hinweise zur Annotation-Abhängigkeit und zum Kartenkonstruktor. Die bisherige APK-Signatur ist bestätigt; der öffentliche Download wird bytegenau geprüft. Das konkrete Gerät, Mailprogramm und der Live-OSM-Abruf bleiben praktisch zu erproben. Android 8.0 oder neuer wird benötigt; die App misst keine automatische Schadenshöhe und trifft keine rechtlich verbindliche Einstufung.

Dateigröße: 4395200 Bytes. [SHA-256-Prüfsummen](SHA256SUMS).
