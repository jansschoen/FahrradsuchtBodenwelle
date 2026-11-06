# Bodenwelle 0.14.0 – Android-Testversion

[APK 0.14.0 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.14.0.apk). Als Update über die vorhandene App installieren, **nicht vorher deinstallieren**. App-ID und Signatur bleiben gleich; Messungen, Bündelungen, gemeinsame Fotos, Fahrtspur und Freigaben bleiben erhalten.

## Kartenausschnitte direkt bei Vorschlägen und beim Löschen

Unter **Mehrfachtreffer bündeln** enthält jeder automatische Vorschlag eine Karte um genau die zugehörigen Messpunkte. Die Nummern passen zu den Einträgen darunter; versetzte Beschriftungen unterscheiden auch sehr nahe Positionen. Karten lassen sich verschieben und zoomen. Sie werden automatisch geladen, sobald sie sichtbar sind. Nicht sichtbare Vorschläge erzeugen keine zusätzlichen Kartenabrufe und geben ihre Kartenansicht wieder frei.

Auch die letzte Löschbestätigung enthält jetzt direkt den Kartenausschnitt und die Messungsnummer. Das gilt sowohl für normales Löschen aus Liste/Karte als auch für die neuen Park-/Privat-Vorschläge. Erst **Messung löschen** entfernt die ausgewählte Einzelmessung. Abbrechen verändert nichts; andere Bündelmitglieder, die Fahrtspur und weiterhin verwendete gemeinsame Fotos bleiben nach den bisherigen Regeln erhalten.

Die Karten verwenden den vorhandenen OSM-Kachelcache. Mit Internet werden benötigte Kartenteile geladen; offline sind vorhandene Kacheln nutzbar. Messpunkte und Nummern bleiben auch ohne neuen Hintergrund sichtbar. Die Cloud-Verbindung zum OSM-Kachelserver wurde am **6. November 2026** erneut beim HTTPS-Tunnel mit HTTP 403 blockiert; die Bilder zeigen deshalb ehrlich den nicht geladenen Hintergrund. Das ist kein Nachweis für einen Fehler auf deinem Handy.

## Park-/Privat-Hinweise nach Fahrtende

Die optionale OSM-Abfrage berücksichtigt zusätzlich Park-/Gartenflächen, privaten Zugang und ausdrücklich privat gekennzeichnete Flächen. Bekannte Hinweise zu neuen Messungen erscheinen nach Fahrtende vor dem Bündelungsvergleich, mit Karte und dem erkannten Grund. **Behalten und weiter** lässt die Messung unverändert. **Diese Messung löschen …** öffnet erst die separate Bestätigung mit Karte. **Prüfung jetzt beenden** und Zurück schließen sofort, ohne offene Messungen zu löschen.

**Parks können zum Bezirksamt gehören, häufig zum Grünflächenamt.** Privater Zugang beweist kein Privateigentum. OSM ist kein Kataster und entscheidet nicht über die zuständige Verwaltung. Daher werden diese Funde zur eigenen Prüfung angeboten und niemals allein aufgrund der Erkennung gelöscht. Ungenaues GPS und fehlende/veraltete OSM-Daten begrenzen die Erkennung.

Kommt ein passender OSM-Hinweis erst später an, kann einmalig eine ergänzende Nachprüfung erscheinen. Nach bewusstem sofortigem Beenden werden solche verspäteten automatischen Hinweise unterdrückt. Unter **Mehr → Daten verwalten → Park-/Privatbereich prüfen** sind vorhandene Hinweise auch später zugänglich. Dieser Einstieg ist während der Fahrt deaktiviert. Bei Stopp über die Benachrichtigung wartet die Prüfung auf eine sichtbare Haupt- oder Kartenansicht. [Anleitung und Grenzen](../docs/PARK-PRIVAT-PRUEFUNG.md).

## Prüfung und Grenzen

351 Projekttests bestehen, ergänzt um zwei native Bildprüfungen. Neue Tests prüfen Polygon-Innen-/Außenlagen, Innenlöcher und zusammengesetzte Parkgrenzen, unvollständige Geometrien, private Zugangs-/Eigentumshinweise ohne Gleichsetzung, keine Übernahme eines mehrdeutigen Nachbarwegs, Abfrage am ursprünglichen GPS-Mittelpunkt trotz verschobener Ereignisposition, Offline-Cache, nachträgliche Hinweise, einmaliges Öffnen, sofortiges Beenden, Behalten, Abbruch und bestätigtes Löschen einer einzelnen Bündelmessung. Die Kartenprüfungen decken automatische Vorschauen, begrenzte Erzeugung nur sichtbarer Karten, korrekte Zentrierung und Freigabe beim Verlassen ab. Die bisherigen Sensor-, OSM-, PDF-, Foto-, Mail- und Freigabetests bestehen weiter.

Android-Build und Lint sind geprüft. Lint meldet keine Fehler und die zwei bisherigen Hinweise zur Annotation-Abhängigkeit und zum Kartenkonstruktor. Frameworktests laufen auf simuliertem Android 9 und 15; native Grafikprüfungen und Flächenparser-Tests auf simuliertem Android 15. Die bisherige APK-Signatur bleibt erhalten; der öffentliche Download wird bytegenau geprüft.

Der Live-Overpass-Abruf war am 6. November 2026 ebenfalls durch den Cloud-Proxy blockiert. Die erweiterte Abfrage und praktische Erkennung auf dem Fairphone müssen deshalb mit **OSM-Abrufdiagnose** geprüft werden. Bei fehlenden Daten wird kein Eigentums- oder Zuständigkeitsstatus erfunden. Android 8.0 oder neuer wird benötigt.

[Bilder der neuen Oberfläche](../docs/screenshots/0.14.0/README.md) · [Bündelung und Fotos](../docs/BUENDELUNG-FOTOS.md) · [OSM-Dokumentation](../docs/KLASSIFIZIERUNG-OSM.md) · [SHA-256-Prüfsummen](SHA256SUMS).

Dateigröße: 4432375 Bytes. SHA-256: `3885460931103de7d2013a865073c2568d2a32ea16f0c00f478d2ca35c2f55a1`.
