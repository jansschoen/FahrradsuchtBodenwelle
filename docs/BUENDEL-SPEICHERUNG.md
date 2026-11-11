# Bündel dauerhaft als eine Fundstelle · 0.17.0

## Speichern auf dem Handy

**Speichern** im Bearbeitungsdialog gilt für die gemeinsamen Angaben der gesamten Fundstelle. Die Taste liegt außerhalb des begrenzten, scrollbaren Formulars. Auch **Bündel speichern** in der Bestätigung bleibt getrennt vom Formular erreichbar. In **Mehrfachtreffer bündeln** steht die Auswahlaktion fest am unteren Rand und zeigt die Zahl ausgewählter Fundstellen.

Ein bestätigtes Bündel erscheint in Liste und Karte als **eine Fundstelle #N**, bezeichnet mit der kleinsten enthaltenen Messungsnummer. Die Originalmessungen bleiben innerhalb der Fundstelle einsehbar. GPS, Erschütterung, Zeit, Höhe, Bezirk und Freigabe bleiben je Messung erhalten. Gemeinsame Eigenschaften und Fotos gelten weiter für alle Mitglieder. Die Übersicht unterscheidet die Zahl der Fundstellen von der Zahl der Messungen. Zoomabhängige Kartencluster zählen benachbarte **Fundstellen**, nicht die Messungen innerhalb einer Fundstelle. Auflösen ist weiterhin bewusst möglich; Löschen der kleinsten Messungsnummer ändert die angezeigte Referenz.

Der ausführliche Eigenschaftenvergleich erscheint weiterhin **nur bei unterschiedlichen gemeinsamen Angaben oder Fotos**. Gleiche Angaben brauchen lediglich die kurze Bestätigung.

## Im Browser über die Karte bündeln

1. **Auf Karte bündeln** wählen.
2. Passende Punkte anklicken. Ein erneuter Klick nimmt die Fundstelle aus der Auswahl. Ein vorhandenes Bündel wird immer vollständig ausgewählt.
3. Liegen Punkte übereinander, im Dialog die passenden Fundstellen anhaken und **Auswahl übernehmen** wählen.
4. **Auswahl bündeln (N)** bestätigen. Nur bei unterschiedlichen gemeinsamen Angaben erscheint der ausführliche Vergleich.
5. Angaben gegebenenfalls mit **Änderungen übernehmen** bearbeiten. Mit **Bearbeitete Datei speichern** den vollständigen Arbeitsstand als neue HTML-Datei herunterladen. Erst diese neue Datei zurück aufs Handy übertragen.

**Leeren** und **Beenden** verändern keine gespeicherten Bündel. Die bisherigen Kästchen links bleiben als alternative Bedienung verfügbar. **Rückgängig** kann eine gerade vorgenommene Bündelung zurücknehmen.

## Rückimport ohne unbeabsichtigtes Auflösen

Ein nachgestellter Fehler betraf nachträglich am Handy zu einem Bündel hinzugefügte Messungen: Der alte Rückimport konnte die exportierten Mitglieder herauslösen und als neue Gruppe speichern. Jetzt bleiben bei reinen Beschreibungsänderungen die **aktuelle Handygruppierung, neue Mitglieder, hinzugefügte Fotos und unveränderte Einzelangaben** erhalten. Geänderte gemeinsame Browserfelder gelten für sämtliche aktuellen Mitglieder. Bestehende Bündelnummern werden nach Möglichkeit wiederverwendet. Lokale Gruppennamen werden anhand ihrer Mitgliedschaft verglichen; eine andere Nummer allein ist kein Konflikt.

Widersprüche verlangen weiterhin die Auswahl zwischen Handy und Browser. Ein ausdrücklich im Browser vorgenommenes Auflösen bleibt möglich. Würde eine neue Browserbündelung stillschweigend nur einen Teil eines inzwischen erweiterten Handybündels herauslösen, bricht der Import mit einem Hinweis auf einen frischen Export ab. Die Daten bleiben dabei erhalten. Auch widersprüchliche gemeinsame Browserangaben für eine inzwischen zusammengelegte Handyfundstelle verlangen einen aktuellen Export.

Vor jedem Import wird eine Sicherung erstellt. Nach erfolgreichem Rückimport für die nächste Bearbeitungsrunde neu exportieren. Projektformat 1 und Datenbankschema 9 bleiben unverändert. Auch ältere HTML-Projekte können über **Projektdatei öffnen** im neuen Editor geöffnet werden. **Vor dem Rückimport zuerst die Android-App auf 0.17.0 aktualisieren**, ohne sie vorher zu deinstallieren.

## Messbasis statt scheinbarer Sicherheit

App und Browser zeigen denselben abgeleiteten Wert **Messbasis 1–5/5**. Er beschreibt den Umfang wiederholter Erfassungen:

| Wert | Grundlage |
| --- | --- |
| 1 | Eine Messung |
| 2 | Mindestens zwei Messungen |
| 3 | Mindestens zwei Messphasen |
| 4 | Mindestens drei Messphasen an zwei Tagen |
| 5 | Mindestens fünf Messphasen an drei Tagen |

Zeitlich sortierte Treffer mit weniger als fünf Minuten Abstand zum vorherigen Treffer gehören zur selben Messphase; ab fünf Minuten beginnt eine neue. Messtage beziehen sich auf **Europe/Berlin**. So erhöhen viele unmittelbar aufeinanderfolgende Erschütterungen den Wert höchstens auf 2/5. In der App lässt sich die Erklärung über die Anzeige öffnen, im Browser über den aufklappbaren Messbasis-Hinweis.

Der Wert ist **keine Wahrscheinlichkeit**, kein Nachweis eines Schadens und keine Bestätigung seiner Gefährlichkeit. Wiederholte Fehler desselben Geräts oder derselben Positionierung bleiben möglich. Er erfordert keine neuen Datenbankfelder und wird aus den erhaltenen Originalmessungen berechnet.

## Prüfungen

Die Regressionen prüfen den nachgestellten Importfehler mit neuen Handymitgliedern und Fotos, zwei Bearbeitungsrunden, Erhalt der Bündelnummer, bewusste Auflösung, Teillöschung, Schutz vor Teilabtrennung und lokale Gruppennamen. UI-Prüfungen prüfen Speicheraktionen außerhalb des Scrollbereichs bei begrenzter Höhe und die feste Auswahlaktion. Der Score wird einschließlich Fünf-Minuten-Grenze, Berliner Tageswechsel und Winterzeitumstellung geprüft.

Chromium prüft tatsächliche Kartenklicks, überlappende Punkte, Auswahl bestehender Bündel, Abbruch, Download und erneutes Öffnen. Eine mit Kartenklicks gespeicherte Datei wird zusätzlich in Android importiert, die Anzahl der Fundstellen in der Liste nach erneuter Erstellung der Activity und im Folgeexport geprüft. Daneben laufen die bisherigen Browser- und Kartentests.

Die Cloud blockiert direkte `file://`-Navigation und die Live-Verbindung zu OpenFreeMap. Browserprüfungen rendern denselben HTML-Inhalt als Testdokument; die gesonderte Kartenprüfung verwendet den echten WebGL-Renderer mit künstlichen Antworten. Der praktische Fairphone-Test, lokales Öffnen unter Windows/macOS/Linux und Firefox/Safari bleiben außerhalb dieser Prüfung. [Prüfkommandos](../tools/browser/README.md).
