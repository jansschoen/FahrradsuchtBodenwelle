# FahrradsuchtBodenwelle

Android-Prototyp für Fahrradfahrten in Berlin: auf einer integrierten OpenStreetMap-Karte den eigenen Standort und die Fahrtspur sehen, auffällige Erschütterungen erfassen, nach der Fahrt prüfen und nur freigegebene Fundstellen exportieren.

Die App misst die Reaktion des montierten Smartphones. **Sie misst keine Bodenwellenhöhe und bewertet keine Rechtskonformität.** Fundstellen und die letzte Fahrtspur bleiben auf dem Handy. Die App lädt OSM-Kartenkacheln über HTTPS und übermittelt dafür angefragte Kartenausschnitte und die IP-Adresse an den Kartenanbieter. Sie versendet keine Schadensmeldungen oder Streckendateien.

## Karte und Fahrtspur (Version 0.2)

Die Startseite enthält eine zoombare native OSM-Karte. **Mein Standort** aktiviert die Standortnachführung; Verschieben der Karte unterbricht die Nachführung. **Fahrtspur ansehen** zeigt die gesamte gespeicherte Spur, **Karte vergrößern** öffnet die Vollbildansicht. Fundstellen sind als farbige Punkte sichtbar; im Stillstand lassen sie sich auf der Startseitenkarte antippen und prüfen. Die Karte zeigt höchstens die neuesten 500 Fundstellen, der Export enthält weiterhin alle freigegebenen Fundstellen.

Die Fahrtspur beginnt mit einer bewusst gestarteten Aufzeichnung. GPS-Lücken über 15 Sekunden werden als getrennte Abschnitte dargestellt; unplausible GPS-Sprünge werden verworfen. Nach Fahrtende bleibt die Spur lokal sichtbar. Die nächste bestätigte Fahrt ersetzt diese Spur, nicht die Fundstellen. Die Spur lässt sich separat löschen und wird nicht mit dem CSV-Fundstellenexport weitergegeben.

Kartenanbieter: `https://tile.openstreetmap.org/`, © OpenStreetMap contributors. Karten werden lokal zwischengespeichert, unter Berücksichtigung der Server-Cacheangaben. Es gibt keinen Gebietsdownload oder vorauseilendes Herunterladen von Kacheln. Ohne Internet können bereits gespeicherte Kacheln verfügbar sein; eine vollständige Offline-Karte wird nicht zugesichert. GPS und lokale Aufzeichnung funktionieren unabhängig vom Laden neuer Kartenkacheln.

Für eine spätere breite öffentliche Veröffentlichung ist die aktuelle [OSM-Kachelnutzungsrichtlinie](https://operations.osmfoundation.org/policies/tiles/) zu prüfen und gegebenenfalls ein passender Kartenanbieter einzusetzen. Der Live-Kachelabruf aus dieser Cloud wurde beim Test mit HTTP 403 blockiert; die Netzwerkfreigabe ist als Konfigurationsentwurf vorbereitet. Das ist kein bestätigter Test des Abrufs auf einem Android-Gerät.

## Auf dem Fairphone ausprobieren

**[Android-Testversion 0.2.0 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.2.0.apk)**

Öffne die heruntergeladene APK auf deinem Android-Handy. Falls Android danach fragt, erlaube die Installation aus dieser Quelle. Die [kurze Installationsanleitung](downloads/INSTALLATION.md) und die [SHA-256-Prüfsumme](downloads/SHA256SUMS) liegen neben der APK. Dies ist eine Debug-Testversion; eine reale Funktionsprüfung auf dem Fairphone steht noch aus.

Installierbare Testdatei nach dem Build: `app/build/outputs/apk/debug/app-debug.apk`.

Die Anleitung für Installation, Vergleichsfahrten und Freigabe steht in [FAHRTEST.md](docs/FAHRTEST.md). Für sachliche Meldungen gibt es eine [Berliner Meldungsvorlage mit offenem Recherchestand](docs/BERLIN-MELDUNGEN.md). Umfang und nächste Schritte beschreibt der [Entwicklungsplan](docs/ENTWICKLUNGSPLAN.md).

## Entwickeln

- Android Studio oder JDK 17–21;
- Android SDK Platform 35 und Build Tools 35.0.0;
- Gradle 8.11.1 über den beigefügten Wrapper;
- Android 8.0 / API 26 oder neuer auf dem Testgerät.

Öffne dieses Verzeichnis in Android Studio oder setze `ANDROID_HOME` auf dein SDK-Verzeichnis. Alternativ verwende eine lokale, ignorierte `local.properties` mit `sdk.dir=...`.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Installation bei aktiviertem USB-Debugging und angeschlossenem Gerät:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Tests mit synthetischen Sensordaten überprüfen unter anderem Mehrfachschwingungen, ruhige Fahrten, fehlende Messbedingungen und den Ausschluss ungeprüfter Daten beim Export. Die Erkennung ist experimentell; die Funktionsprüfung auf einem realen Android-Gerät und Fahrtests sind weiterhin erforderlich.

In dieser Linux-Cloud sind JDK, Gradle und Android SDK unter `/workspace/.bodenwelle-tools` installiert. Der folgende Einstieg aktiviert diese Werkzeuge sowie den vorhandenen Proxy und den System-Zertifikatsspeicher, ohne Zugangsdaten auszugeben:

```sh
python3 tools/setup-cloud.py
python3 tools/cloud-gradle.py :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Der Bootstrap prüft die heruntergeladenen Werkzeuge anhand festgelegter Hersteller-Prüfsummen und erhält eine vorhandene `local.properties`. In der Cloud werden keine Geräteprozesse gestartet; der APK-Test auf dem Fairphone erfolgt separat.

## Datenmodell

Lokale SQLite-Fundstellen enthalten Ereigniszeit, Koordinaten, GPS-Genauigkeit, GPS-Fix-Alter, gegebenenfalls Geschwindigkeit, Sensor-Spitzenwert und Erkennungsschwelle sowie Quelle, Kategorie, Notiz und Freigabestatus. Die zuletzt gestartete Fahrt wird zusätzlich als lokale GPS-Spur mit getrennten Abschnitten gespeichert. Die Datenbankmigration von Version 0.1 erhält vorhandene Fundstellen. Rohsensorzeitreihen werden nicht gespeichert. Manuelle Einträge enthalten keine erfundenen Sensorwerte. Der Fundstellenexport verwendet UTC-Zeitpunkte und dokumentierte SI-Einheiten beziehungsweise km/h und enthält keine Fahrtspur.

## Verwendete Kartenbibliothek

osmdroid 6.1.20 ist unter der Apache-Lizenz 2.0 lizenziert. Eine Kopie der Lizenz liegt unter `app/src/main/assets/licenses/osmdroid-apache-2.0.txt`. Die OSM-Datenattribution bleibt direkt unter jeder Kartenansicht sichtbar und verlinkt die [OSM-Copyrightseite](https://www.openstreetmap.org/copyright).
