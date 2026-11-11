# Bodenwelle 0.17.0 – dauerhafte Bündel und Kartenklicks

[APK 0.17.0 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.17.0.apk). Als Update installieren, **nicht vorher deinstallieren**. App-ID, Signatur, Datenbankschema 9 und Projektformat 1 bleiben gleich. Android 8.0 oder neuer wird benötigt. Die App vor dem Rückimport auf diese Version aktualisieren.

## Speichern und eine gemeinsame Fundstelle

**Speichern** in der App übernimmt gemeinsame Eigenschaften für die gesamte Fundstelle. Die Taste liegt außerhalb des begrenzten Scrollbereichs. Auch **Bündel speichern** bleibt getrennt vom Formular erreichbar. Die manuelle Bündelauswahl hat eine feste Aktion am unteren Rand mit der Auswahlanzahl.

Ein Bündel wird in Liste und Karte als eine Fundstelle mit der kleinsten enthaltenen Messungsnummer angezeigt. Einzelmessungen bleiben darin einsehbar. Die Übersicht unterscheidet Fundstellen und Messungen. Rückimport und erneuter Export erhalten die Gruppierung und nach Möglichkeit die bisherige Bündelnummer. Reine Beschreibungsänderungen lösen später am Handy hinzugefügte Mitglieder nicht mehr ab; gemeinsame Browseränderungen gelten auch für diese. Neue Handyfotos und unveränderte Einzelangaben bleiben erhalten. Widersprüchliches gleichzeitiges Umgruppieren kann einen frischen Export erfordern, statt eine Gruppe unbeabsichtigt zu teilen.

## Im Browser mit Kartenklicks bündeln

Den [Browsereditor 0.17.0 als ZIP herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-Browser-0.17.0.zip), entpacken und **Bodenwelle-Browser.html** öffnen. Über **Projektdatei öffnen** die bisherige zuletzt gespeicherte HTML-Datei auswählen. Ein neuer Handyexport ist dafür nicht erforderlich, sofern dieser Export noch nicht zurückimportiert wurde.

1. **Auf Karte bündeln** aktivieren und passende Punkte anklicken. Ein vorhandenes Bündel zählt als eine Fundstelle. Überlappende Punkte öffnen eine Auswahl mit Kästchen.
2. **Auswahl bündeln (N)** bestätigen. Ein erneuter Punktklick nimmt die Fundstelle aus der Auswahl; **Leeren** und **Beenden** ändern keine gespeicherten Bündel.
3. **Änderungen übernehmen** übernimmt gemeinsame Bearbeitungen in den Arbeitsstand. **Bearbeitete Datei speichern** sichert den gesamten Stand als neue HTML-Datei; gültige offene Formulareingaben werden dabei übernommen. Den angebotenen Download prüfen und diese neue Datei zurück aufs Handy kopieren.
4. In der aktualisierten App **Mehr → Daten verwalten → Am Computer bearbeiten / Rückimport → Bearbeitete Projektdatei öffnen** wählen und die Vorschau bestätigen. Danach für eine neue Bearbeitungsrunde frisch exportieren.

Der ausführliche Vergleich bleibt auf **abweichende gemeinsame Angaben oder Fotos** beschränkt. Unterschiede bei Höhen, Rohwerten oder Freigaben lösen ihn nicht aus. Fotos werden beim Bündeln vereinigt; Höhen, Bezirke und individuelle Freigaben bleiben getrennt. [Anleitung und Messbasis](../docs/BUENDEL-SPEICHERUNG.md) · [Bilder](../docs/screenshots/0.17.0/README.md).

## Messbasis 1–5/5

Mehrfach erfasste Fundstellen erhalten eine höhere Messbasis. Viele Treffer kurz hintereinander erhöhen den Wert höchstens auf 2/5. Höhere Stufen erfordern getrennte Messphasen und bei 4/5 beziehungsweise 5/5 mehrere Tage. Die Erklärung lässt sich in App und Browser öffnen. Der Wert beschreibt Wiederholungen, **keine Wahrscheinlichkeit und keine bestätigte Gefährlichkeit**.

## Daten und Karte

Eindeutige Exportnamen mit Datum, Berliner Uhrzeit und UUID bleiben erhalten. Die Projektdatei enthält genaue Zeiten, Orte und private Angaben und gehört zur ursprünglichen App-Installation. Vor dem Import entsteht eine lokale Sicherung; neue Handymessungen und die aktuelle Spur bleiben erhalten. Verwaltungsberichte gesondert als PDF/Mail mit Kalenderwochen erstellen.

Der Browser lädt OpenFreeMap auf OSM-Basis. Bibliothek und Gestaltung sind eingebettet; nur Kartendaten werden angefordert. Der Anbieter sieht Ausschnitt und IP-Adresse, keine Beschreibungen, Fotos oder Sensorwerte. Internet und WebGL sind für den Straßenhintergrund nötig; Punkte, Spur und Bearbeitung bleiben auf einem Raster verfügbar.

## Prüfung und Grenzen

405 automatisierte Android-Prüfungen bestehen; drei gesonderte Austauschphasen sind im normalen Lauf absichtlich ausgenommen. Die Android-Regressionen prüfen den nachgestellten Abtrennungsfehler mit neuen Mitgliedern und Fotos, zwei Bearbeitungsrunden, Erhalt von Gruppenidentitäten, bewusste Auflösung, Teillöschung und Schutz vor versehentlicher Teilabtrennung. Hinzu kommen Speicheraktionen bei begrenzter Höhe und Messbasis einschließlich Berliner Tageswechsel und Zeitumstellung.

Die Browserprüfungen testen echte Kartenklicks, überlappende Punkte, Auswahl ganzer Bündel, Abbruch, Eigenschaftenvergleich, Übernehmen, Download, erneutes Öffnen und Messbasis. Zwei tatsächlich gespeicherte Dateien wurden erfolgreich in Android zurückimportiert. Die Kartenbündelung bleibt als eine Fundstelle erhalten, auch nach erneuter Erstellung der Activity und im Folgeexport. Rohwerte, Fotos, neue Handymessungen und Spur werden gesondert geprüft. [Prüfkommandos](../tools/browser/README.md).

Direkte `file://`-Navigation und die Live-Verbindung zu OpenFreeMap sind in der Cloud durch Browser-/Netzwerkrichtlinien blockiert. Browserprüfungen rendern denselben HTML-Inhalt als Testdokument; die Kartenprüfung nutzt den echten WebGL-Renderer mit künstlichen Antworten. Das reale Fairphone einschließlich Bildschirmtastatur, lokale Dateinavigation, Firefox und Safari bleiben praktische Prüfungen. Lint meldet keine Fehler und zwei bisherige Hinweise zu Annotation-Abhängigkeit und Kartenkonstruktor. Die APK-Signatur entspricht der bisherigen Testversion. [SHA-256-Prüfsummen](SHA256SUMS).

APK-Dateigröße: 4901664 Bytes. SHA-256: `f415eddd598519ecb291d1afdfa28ebd553ff9f44c2a03db11ed23ba4c488bc9`.
