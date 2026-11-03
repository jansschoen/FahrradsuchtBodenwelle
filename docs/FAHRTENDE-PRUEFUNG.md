# Fundstellen nach Fahrtende vergleichen – 0.13.0

Nach einer erfolgreich gestarteten Fahrt merkt sich die App lokal den Bereich neu hinzugekommener Messungsnummern. Beim Beenden wird auch der letzte noch offene Sensorabschnitt gespeichert. Eine sichtbare Haupt- oder Kartenansicht öffnet danach einmalig eine Kartenprüfung, sofern passende Paare existieren. Ein Stopp über die Benachrichtigung öffnet aus dem Hintergrund keine Oberfläche; die Prüfung wartet auf die nächste sichtbare Ansicht. Nach einem erzwungenen Prozessabbruch ist kein reguläres Fahrtende garantiert.

Die Prüfung berücksichtigt Fundstellen mit einem Abstand von **höchstens fünf Metern**, wenn mindestens eine ihrer Messungen aus der beendeten Fahrt stammt. Neue Punkte werden sowohl mit neuen als auch mit älteren verglichen. Reine Altpaare, bereits gemeinsame Bündel und weiter entfernte Punkte werden nicht vorgeschlagen. Bei einem Bündel müssen alle Abstände zu den Messungen der anderen Stelle innerhalb der Grenze liegen. So wächst eine kurze Nachbarkette nicht unbemerkt über fünf Meter hinaus. Die bisherigen allgemeinen Vier-Meter-Vorschläge bleiben unverändert.

## Bedienung

Die Karte zeigt die beiden Messpunkte als **A** und **B** mit ihren Messungsnummern. Versetzte Beschriftungen und Verbindungslinien lassen auch nahezu identische Positionen unterscheiden. Die Übersicht nennt Messungsnummern, Art, Koordinaten, GPS-Genauigkeit und ob neue Messungen enthalten sind. Die Textübersicht lässt sich bei langen Angaben scrollen, die Karte verschieben und zoomen. Sie verwendet denselben OSM-Kachelcache und dieselben Abrufregeln wie die Fahrtkarte. Ohne Hintergrund bleiben Punkte und Koordinaten sichtbar.

- **Ja, zusammenfassen …** öffnet die vorhandene Bestätigung. Unterschiedliche bekannte Arten oder manuelle Beläge müssen abgeglichen werden. Erst die Bestätigung speichert die Bündelung.
- **Nein, getrennt lassen** überspringt das Paar und zeigt das nächste. Es wird keine Messung geändert.
- **Prüfung jetzt beenden** oder die Zurück-Taste schließt sofort. Auch bisher übersprungene oder noch nicht geprüfte Messungen bleiben erhalten.

Bildschirmdrehung erhält die aktuelle Paarliste und Fortschrittsposition. Bereits gebündelte oder gelöschte Paare werden beim nächsten Vergleich übersprungen. Nach einer bestätigten Bündelung werden neue Vorschläge anhand der tatsächlichen gemeinsamen Mitglieder erneut auf fünf Meter geprüft. Die Prüfung verändert keine individuellen Messwerte und erteilt keine Versandfreigaben. Sie wird nach dem Schließen nicht fortlaufend erneut aufgedrängt; später kannst du weiterhin über Fundstellen oder Karte bündeln.

Fünf Meter sind ein Abstand zwischen GPS-Schätzungen, kein Nachweis derselben Unebenheit. Die Genauigkeit kann deutlich schlechter sein. Deshalb werden auch unterschiedliche Beschreibungen nur zur menschlichen Entscheidung gezeigt und nicht automatisch vereinigt. Straßenseite, Fahrtrichtung und verschiedene benachbarte Schäden müssen selbst geprüft werden.

## Freigaben auf der Karte

Ein grüner Haken bedeutet: Alle dargestellten Messungen sind freigegeben und die gemeinsamen Angaben sind konfliktfrei. Ein grüner Strich kennzeichnet teilweise vorbereitete Gruppen. Offene Messungen behalten ihre normalen farbigen Markierungen. Bei einem Farbring beziehungsweise einer Intensitätsmarkierung bleibt deren bisherige Bedeutung erhalten; Nummer und Freigabezeichen sind getrennt. Der Status wird nach Bearbeitung und Widerruf auf beiden Karten aktualisiert. **Keines der Zeichen bestätigt einen Versand oder eine Bearbeitung durch das Bezirksamt.**
