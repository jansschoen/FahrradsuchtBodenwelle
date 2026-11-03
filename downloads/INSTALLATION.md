# Bodenwelle 0.13.0 – Android-Testversion

[APK 0.13.0 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.13.0.apk). Als Update installieren, **nicht vorher deinstallieren**. App-ID und Signatur bleiben gleich; vorhandene Messungen, gemeinsame Fotos, Bündelungen, Fahrtspur und Freigaben bleiben erhalten.

## Neues Anschreiben

Die Mail erklärt das Anliegen, die Verwaltung beim Auffinden und gezielten Prüfen von Schlaglöchern und Unebenheiten zu unterstützen. Bei Sensordaten beschreibt sie vertikale Beschleunigung am Fahrradlenker und GPS-Zuordnung. Geschwindigkeit, Reifen, Halterung und Fahrweise beeinflussen die Reaktion; Fehlalarme und GPS-Abweichungen sind möglich. Eine automatische Schadenshöhe oder rechtliche Bewertung wird nicht behauptet. Rein manuelle Meldungen werden entsprechend beschrieben. Die Kalenderwochen-Anonymisierung bleibt erhalten.

Unter **Meldung vorbereiten → Kurze persönliche Vorstellung** kannst du ein bis zwei eigene Sätze ergänzen. Name und Vorstellung sind optional, werden lokal gespeichert und in neue Entwürfe übernommen. **Mailentwürfe erzeugen** übernimmt Änderungen und ersetzt bisherige Entwürfe. Die Vorstellung wird nicht zusätzlich ins PDF aufgenommen. Das Anschreiben bleibt bearbeitbar; erst das externe Mailprogramm versendet. [Mail-Anleitung](../docs/MAIL-MELDUNGEN.md).

## Freigaben auf der Karte

Ein **grüner Haken** am farbigen Messpunkt kennzeichnet vollständig freigegebene, konfliktfreie Fundstellen. Ein **grüner Strich** zeigt teilweise vorbereitete Gruppen; offene Messungen erhalten keinen Haken. Nummer und Intensitätsfarbe bleiben sichtbar. Die Symbole aktualisieren sich auf kleiner und großer Karte auch beim Widerrufen einer Freigabe. **Sie bestätigen keinen Versand.**

## Kartenprüfung nach Fahrtende

Bei nahen Treffern öffnet sich nach Fahrtende eine Prüfung für Paare mit mindestens einer neuen Messung und **höchstens fünf Metern Abstand**. Neue Messungen werden mit neuen und älteren Fundstellen verglichen. Die Karte zeigt beide Punkte mit getrennten A-/B-Beschriftungen und Messungsnummern. GPS-Genauigkeit, Art und Koordinaten stehen darüber; längere Angaben lassen sich scrollen.

- **Ja, zusammenfassen …** öffnet die gemeinsame Bestätigung, bei Bedarf mit Konfliktauswahl. Erst Bestätigen speichert.
- **Nein, getrennt lassen** zeigt das nächste Paar ohne Änderungen.
- **Prüfung jetzt beenden** oder die Zurück-Taste schließt sofort. Sämtliche Messungen bleiben erhalten.

Drehen erhält den aktuellen Vergleich. Bereits gebündelte oder gelöschte Paare werden übersprungen. Bei Fahrtende über die Benachrichtigung wartet die Prüfung auf eine sichtbare Haupt- oder Kartenansicht; es wird keine Oberfläche aus dem Hintergrund geöffnet. Ohne passende neue Treffer bleibt die Prüfung aus. Nach dem Schließen wird sie nicht bei jedem erneuten Öffnen wiederholt. Die allgemeinen Vorschläge unter vier Metern bleiben unverändert. [Ausführliche Anleitung und Grenzen](../docs/FAHRTENDE-PRUEFUNG.md).

OSM-Karten brauchen verfügbare oder bereits zwischengespeicherte Kacheln; Punkte und Koordinaten bleiben auch ohne Hintergrund sichtbar. Der Live-OSM-Abruf ist aus dieser Cloud durch den Proxy blockiert und muss auf dem Gerät geprüft werden. Die Screenshots zeigen ausdrücklich lokale Beispieldaten ohne geladenen Straßenhintergrund.

## Prüfung

333 Projekttests bestehen, ergänzt um eine native Bildvorschau. Die neuen Prüfungen umfassen Anschreibenziel und Messgrenzen, rein manuelle Meldungen ohne Sensorbehauptung, persönliche Vorstellung mit lokaler Speicherung und Drehung, Freigabezeichen auf beiden Karten, Widerruf und gemischte Gruppen, ungeklärte alte Bündel ohne vollständigen Haken, Abstand einschließlich fünf Metern, neue/alte Treffer ohne reine Altpaare, keine automatische Verkettung, sofortiges Beenden, Überspringen, Bestätigung/Abbruch, Drehung, bereits gebündelte Paare, ID-Grenzen und Fahrtende über den Service. Die bisherigen Sensor-, OSM-, PDF-, Foto-, Mail- und Navigationstests bestehen weiterhin. Frameworktests laufen auf simuliertem Android 9 und 15; Grafik-, Foto- und PDF-Prüfungen nutzen native Grafikverarbeitung.

Android-Build und Lint sind geprüft. Lint meldet keine Fehler und die zwei bisherigen Hinweise zur Annotation-Abhängigkeit und zum Kartenkonstruktor. Kleine Fahrtstatus-Übergänge werden bewusst synchron gespeichert, damit sie vor Serviceende beziehungsweise Start der einmaligen Prüfung dauerhaft vorliegen. Die bisherige APK-Signatur bleibt erhalten; der öffentliche Download wird bytegenau geprüft. Ein tatsächlicher Fairphone-Test von GPS, Kartenabruf, Display-Drehung und Mailprogramm bleibt nötig. Android 8.0 oder neuer wird benötigt.

[Bilder der neuen Oberfläche](../docs/screenshots/0.13.0/README.md).

PDF-Berichte mit Karte, Tabelle, Fotos und interner Vorschau sowie gemeinsame Beschreibungen aus 0.12.0 bleiben enthalten. [PDF-Anleitung](../docs/PDF-BERICHT.md) · [Vor-Ort-Testplan](../docs/FAHRTEST.md) · [SHA-256-Prüfsummen](SHA256SUMS).

Dateigröße: 4413467 Bytes. SHA-256: `a35f0a2d211e8ca3d61d37b818a9c0ca99bed213e32a9abd844018025372204b`.
