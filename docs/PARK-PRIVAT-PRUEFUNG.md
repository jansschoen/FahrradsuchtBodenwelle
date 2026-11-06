# Park-/Privat-Hinweise und freiwilliges Löschen – 0.14.0

Nach einer Fahrt prüft die App neue Messungen auf vorhandene OSM-Hinweise zu Parks, Gärten, privatem Zugang oder privat gekennzeichnetem Eigentum. Solche Messungen werden **zum Prüfen und optionalen Löschen** vorgeschlagen, mit Karte, Messungsnummer und dem konkreten OSM-Grund. Es wird niemals allein wegen eines Kartenmerkmals gelöscht oder eine Freigabe zurückgenommen.

**Öffentliche Parks können zum Bezirksamt gehören, häufig zum Grünflächenamt.** Eine Parkfläche belegt deshalb keine fehlende Zuständigkeit des Bezirks. `access=private` beschreibt Zugangsbeschränkungen und beweist kein privates Eigentum. Selbst `ownership=private` ist eine ehrenamtliche OSM-Angabe und keine amtliche Katasterauskunft. Öffentlich gewidmete Wege oder besondere Zuständigkeitsregeln können abweichen. Der Prüfdialog erklärt diese Grenzen.

## Bedienung

1. Fahrt beenden. Bekannte Hinweise zu neuen Messungen erscheinen vor dem Bündelungsvergleich. Bei Ende über die Benachrichtigung wartet die Oberfläche auf das nächste Öffnen der Haupt- oder Kartenansicht.
2. Karte und Grund prüfen. **Behalten und weiter** lässt die Messung unverändert und zeigt den nächsten Hinweis.
3. **Diese Messung löschen …** öffnet eine zweite Bestätigung mit eigener Karte. **Abbrechen** lässt die Daten unverändert. Erst **Messung löschen** entfernt genau diese Einzelmessung; andere Mitglieder eines Bündels, deren gemeinsame Fotos und die Fahrtspur bleiben nach den bisherigen Regeln erhalten.
4. **Prüfung jetzt beenden** oder Zurück schließt sofort, ohne offene Messungen zu löschen. Bildschirmdrehung erhält die aktuelle Messung. Anschließend können reguläre Fünf-Meter-Bündelungsvorschläge folgen, sofern du die Prüfung nicht vorzeitig beendest.
5. Unter **Mehr → Daten verwalten → Park-/Privatbereich prüfen** lassen sich auch ältere Hinweise später ansehen. Der Einstieg ist während einer laufenden Fahrt deaktiviert.

Bei fehlendem Netz, Abfragepausen oder unvollständigen Daten können Hinweise erst später oder gar nicht vorliegen. Solange die beendete Fahrt die letzte ist, darf ein später eintreffender Hinweis einmalig eine zusätzliche Prüfung auslösen. Nach bewusstem sofortigem Beenden wird diese automatische Nachprüfung unterdrückt. Weitere später erkannte Hinweise bleiben über den manuellen Einstieg erreichbar. Die App öffnet keine Prüfansicht aus dem Hintergrund und bietet dieselben bereits gezeigten Hinweise nicht jede Sekunde erneut an.

## Was erkannt werden kann

Die optionale OSM-Abfrage ergänzt `leisure=park`/`garden`, `access=private` und `ownership=private`. Die App prüft die Form einer geschlossenen Fläche einschließlich Innenlöchern am ursprünglichen GPS-Mittelpunkt. Private Weg-Tags dürfen nur von einem nahen zugeordneten Weg stammen. Ein bloßer Name wie „Privatpark“, die Nähe zu einer Parkgrenze oder fehlende OSM-Angaben reichen nicht aus.

OSM kann unvollständig oder veraltet sein. Grundstücksgrenzen, Eigentum und Zuständigkeit werden nicht flächendeckend amtlich geprüft. Große oder unvollständig modellierte Flächen können fehlen; ein GPS-Mittelpunkt kann auf der falschen Seite einer Grenze liegen. Bereits gespeicherte Informationen sind auch offline nutzbar, dann unter Umständen älter. **Ein fehlender Hinweis bedeutet weder „öffentlich“ noch „Straßenamt zuständig“.** Prüfe bei Unsicherheit den Ort selbst oder behalte die Messung.

Die Daten bleiben lokal. Die Karten nutzen den vorhandenen OSM-Kachelcache. Der externe Overpass-Anbieter erhält wie bisher einen geografischen Ausschnitt und die IP-Adresse; keine Fotos, Notizen oder Messwerte. Abschalten der OSM-Ergänzung verhindert weitere solche Abfragen. Die App kontaktiert wegen dieser Hinweise keine Verwaltung oder Grundstückseigentümer automatisch.
