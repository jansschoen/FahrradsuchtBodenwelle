# Dein erster Fahrtest in Berlin

Dieser Prototyp sammelt **Verdachtsstellen**. Er bestimmt weder eine Bodenwellenhöhe noch, ob der Straßenbelag rechtswidrig oder gefährlich ist. Die Erkennungsparameter sind Ausgangswerte; die bisherigen Rückmeldungen vom Fairphone 4 sind positiv, eine systematische Kalibrierung mit deiner Valkental-Halterung und deinem Trekking N8 steht noch aus.

## Installation

1. Installiere die erzeugte `app-debug.apk` auf dem Fairphone. Android fragt gegebenenfalls nach der Erlaubnis zur Installation aus dieser Quelle. Die Debug-App ist für persönliche Tests, noch nicht für eine Veröffentlichung im Play Store.
2. Öffne **Bodenwelle**. Befestige das Telefon fest und ohne Spiel in der Halterung. Halterung, Reifendruck und Beladung sollten zwischen Vergleichsfahrten gleich bleiben.
   Die integrierte OpenStreetMap-Karte zeigt zunächst Berlin. **Mein Standort** erlaubt die genaue Position und folgt dir. **Karte vergrößern** öffnet die Vollbildansicht. Das Laden neuer Kartenausschnitte benötigt Internet; der Kartenanbieter sieht die Anfragen und deine IP-Adresse.
3. Tippe vor der Fahrt oben auf **Fahrt starten**. Erlaube den **genauen** Standort während der Nutzung und nach Möglichkeit Benachrichtigungen. Die große Kartenansicht öffnet sich nach erfolgreichem Start automatisch. Darin stehen auch **Stelle markieren** und **Fahrt beenden** bereit. Es werden weder ein Konto noch Hintergrund-Standortberechtigung benötigt.
4. Warte draußen auf eine ausreichend genaue Position. Die Automatik wird erst ab etwa 5 km/h aktiv. Die Anzeige erklärt, wenn GPS oder Geschwindigkeit nicht ausreichen.
   Unter dem Startknopf steht sofort, ob die Fahrt startet, läuft oder nicht gestartet werden konnte. Die Karte zeigt **Genauigkeit (geschätzt): ± … m** und das Alter der Messung. Die Anzeige aktualisiert sich bei neuen Android-Positionsmessungen; deren Häufigkeit hängt vom Gerät und Empfang ab. Ein kleinerer Meterwert bedeutet eine präzisere Android-Schätzung. „Position veraltet“ bezeichnet eine alte Messung, keine weiterhin garantierte Genauigkeit.
5. Nach dem Start kann das Display ausgeschaltet werden. Die laufende Aufzeichnung ist über eine Android-Benachrichtigung erkennbar und lässt sich dort beenden. Eine aktive Aufzeichnung verbraucht durch GPS und Sensoren zusätzlichen Strom; sie endet spätestens nach sechs Stunden. Nach Prozessabbruch oder Handy-Neustart startet sie nicht selbstständig erneut.

## Vergleich statt gefährlicher Testmanöver

Wähle eine kurze, alltägliche Strecke mit einem glatten Abschnitt und wenigen bereits bekannten Unebenheiten. Fahre wie gewohnt und suche keine gefährlichen Stellen für den Versuch auf. Bediene das Telefon nur im sicheren Stand.

Notiere die bekannten Stellen vorab oder markiere sie nach dem Anhalten. Fahre die gleiche Strecke zwei- bis dreimal unter ähnlichen Bedingungen. Prüfe nach jeder Fahrt:

- Welche bekannten Unebenheiten hat die Automatik gefunden?
- Welche Treffer stammen vermutlich von Bremsen, Absteigen, Pflaster oder einer lockeren Halterung?
- Liegen die Koordinaten an der richtigen Stelle? GPS-Genauigkeit und Alter der Position stehen beim Eintrag.
- Gibt es mehrere Treffer für dieselbe Unebenheit oder fehlen dicht aufeinanderfolgende Stellen?
- Läuft die Aufzeichnung auch mit ausgeschaltetem Display? Werden nach erneutem Öffnen neue Fundstellen angezeigt?
- Folgt **Mein Standort** deiner Position? Erscheint die gefahrene Strecke als türkise Linie, auch nach dem Ausschalten und erneuten Öffnen des Displays? GPS-Lücken sollen ohne Verbindungslinie bleiben.

Die App erkennt die Sensorantwort von Fahrrad und Halterung. Sie kann Wurzeln, Bodenwellen und schiefe Platten noch nicht automatisch unterscheiden. Die Kategorie trägst du selbst nach der Fahrt ein. Langsam überfahrene, flache oder längere Unebenheiten können unerkannt bleiben. Die Abklingzeit von 1,5 Sekunden vermeidet Mehrfachtreffer, kann aber nahe beieinanderliegende Schäden zusammenfassen. Die Schwelle bleibt auf deinem eingestellten Wert; sie steigt nicht automatisch mit holpriger Fahrbahn.

## Empfindlichkeit ausprobieren

Tippe an der Karte auf **Empfindlichkeit einstellen**. Der Standard ist **40 m/s²**. Der Schieberegler reicht von 5 bis 100 m/s²; nach rechts steigt die Schwelle und es entstehen weniger Treffer. Eine Zahl von 5 bis 100 kannst du auch direkt eingeben und mit **Übernehmen** anwenden. **Standard wiederherstellen** setzt 40 m/s² zurück. Die Einstellung bleibt nach dem Schließen der App erhalten und gilt bei laufender Aufzeichnung sofort. Ein gerade noch nicht abgeschlossener Treffer wird bei einer Änderung verworfen, damit alte und neue Einstellungen nicht vermischt werden.

Halte zum Verstellen sicher an. Vergleiche dieselbe Strecke mit wenigen unterschiedlichen Einstellungen und notiere Fehlalarme, verpasste Stellen und die verwendete Schwelle. Die Messdetails jeder Fundstelle zeigen Spitzenwert und damalige Auslöseschwelle. Das sind Smartphone-Reaktionen, keine Zentimeterangaben oder gesetzlichen Grenzwerte.

Neue automatische Fundstellen berücksichtigen die GPS-Fahrtrichtung, Geschwindigkeit und den Zeitabstand zum stärksten Sensorstoß. Die Karte zeigt die daraus geschätzte Ereignisposition; die Messdetails enthalten auch den ursprünglichen GPS-Punkt und die Verschiebung. Bei 6 m/s und einer drei Sekunden alten Position wird beispielsweise 18 m in Fahrtrichtung korrigiert. Wenn eine GPS-Messung erst nach dem Ereignis kam, wird rückwärts gerechnet. In Kurven und bei Geschwindigkeitsänderungen kann die Schätzung danebenliegen; auch Reifen, Fahrrad und Halterung können die Sensorantwort verzögern. Die angegebene GPS-Genauigkeit beschreibt die ursprüngliche Messung. Alte Fundstellen bleiben unverändert.

## Prüfen und exportieren

Über **So funktioniert’s** kannst du die drei Schritte jederzeit in der App nachlesen.

Beende die Fahrt. **Auf der Karte ansehen** zeigt die Fundstelle direkt in der integrierten Vollbildkarte. **Fahrtspur ansehen** zeigt den gespeicherten Streckenverlauf. Im Stillstand kannst du die Fundstellen auf der Startseitenkarte antippen, um sie zu prüfen. Die Punktfarbe zeigt die Intensitätsklasse von Grün bis Rot; Grau bedeutet keine Messgrundlage. Im Text steht, ob ein Eintrag geprüft ist. Sensorfarben sind nur vorläufige Reaktionsklassen und keine Höhen- oder Gefahrenmessung. Technische Angaben wie Koordinaten und Sensorwerte findest du unter **Messdetails anzeigen**.

Öffne **Stelle prüfen** bei jeder relevanten Stelle. Ergänze Straße, Fahrtrichtung, Bezugspunkt und eine sachliche Beschreibung des sichtbaren Mangels. Entferne Fehlalarme. Aktiviere die Freigabe nur für nachvollziehbare Einträge.

**Geprüfte Stellen als Datei speichern** öffnet die Android-Dateiauswahl. Die Tabelle im CSV-Format enthält nur freigegebene Fundstellen einschließlich genauer Koordinaten, Zeitpunkt, GPS-Messqualität, Geschwindigkeit (sofern vorhanden), Erschütterungswert und Notiz. Prüfe die Datei vor der Weitergabe. Ein gewählter Cloud-Speicher kann die exportierte Datei seinerseits hochladen; die App versendet sie nicht.

Fundstellen und die zuletzt gestartete Fahrtspur bleiben im privaten App-Speicher und nehmen nicht am Android-App-Backup teil. Internet wird für Kartenkacheln benötigt. Die Streckendatei wird nicht an den Kartenanbieter oder die Verwaltung versendet und ist nicht Teil des Fundstellenexports. Kartenanfragen können allerdings erkennen lassen, in welchem Gebiet die Karte angezeigt wird.

Die Fahrtspur bleibt nach Fahrtende erhalten. Eine neue bestätigte Fahrt ersetzt sie; **Gespeicherte Fahrtspur löschen** entfernt sie separat. Fundstellen bleiben dabei erhalten. Durch Deinstallation gehen die lokalen Einträge verloren. **Alle Fundstellen löschen** entfernt die Fundstellen aus der App, nicht aus bereits exportierten Dateien oder die separat gespeicherte Fahrtspur.

## Neue Klassifizierung und OSM prüfen

Unter **Stelle prüfen** wählst du eine der sieben Unebenheitsarten. Zusätzliche Gefährdungsmerkmale lassen sich einzeln im aufklappbaren Bereich markieren. Eine tatsächlich vor Ort gemessene Höhe kannst du separat in Millimetern ergänzen und ausdrücklich bestätigen. Ohne diese Bestätigung bewertet die App ausschließlich die Sensorreaktion, vorläufig und ohne Höhenumrechnung.

Verkehrsfläche und Oberfläche erscheinen als **OSM-Vorschlag** mit Zuordnungssicherheit. Prüfe gerade bei parallelen Wegen, ob die Fläche plausibel ist. Unter **Verkehrsfläche und Oberfläche prüfen** kannst du beide Angaben unabhängig manuell festlegen. Solche Änderungen bleiben bei späteren OSM-Antworten erhalten. Angaben zur Benutzungspflicht sind Vorschläge aus Kartentags; entscheidend ist die Beschilderung vor Ort, nicht allein OSM.

Auf der Startseite kannst du die automatische Ergänzung abschalten oder **Fehlende OSM-Angaben ergänzen** wählen. Ohne Internet bleiben alle Aufzeichnungen erhalten. Der Geodatenanbieter erhält den Ausschnitt um Fundstellen und deine IP-Adresse. OSM-Abfragen sind begrenzt; bei vielen neuen Gebieten kann die Ergänzung weitere Durchläufe brauchen. Bei „mehrdeutig“ ist eine manuelle Prüfung sinnvoll. Der Live-Abruf wurde aus der Cloud blockiert und muss auf dem Fairphone praktisch überprüft werden.

Mehr zu Skalen, gespeicherten Originaltags und Grenzen steht in [KLASSIFIZIERUNG-OSM.md](KLASSIFIZIERUNG-OSM.md).

## Nach diesem ersten Versuch

### Update auf 0.5.0

Installiere die neue APK über die vorhandene Version, damit deine gespeicherten Einträge erhalten bleiben. Prüfe danach draußen **Mein Standort** und die Meterangabe. Tippe auf **Fahrt starten**: Ein Startstatus soll sofort erscheinen, dann die große Karte. Beim Stehen kann die Fahrt bereits laufen, während die automatische Stoßerkennung noch auf Fahrgeschwindigkeit wartet. Manuelle Markierungen sind bei ausreichender aktueller Position möglich.

Falls der Start weiter scheitert, notiere den genauen Text direkt unter dem Startknopf oder im Fehlerdialog. Diese Rückmeldung hilft, die Ursache auf dem Fairphone zu bestimmen. Bitte keine Deinstallation als ersten Diagnoseversuch durchführen, da sie lokale Fundstellen und die Fahrtspur löschen würde.

Für die nächste Entwicklungsrunde brauchen wir vor allem konkrete Fehlalarme und verpasste Stellen aus normalen Fahrten. Die Erschütterungswerte dienen dem Vergleich unter ähnlichen Bedingungen. Eine Zentimeterangabe würde zusätzliche Referenzmessungen und ein validiertes Modell für Reifen, Fahrrad und Befestigung voraussetzen; allein aus Beschleunigung und GPS-Geschwindigkeit ist sie nicht zuverlässig ableitbar.

Weitere Informationen zur möglichen Höhenmessung stehen in [HOEHENMESSUNG.md](HOEHENMESSUNG.md).

## Meldung vorbereiten

Wähle nach dem Freigeben **Meldung an die Verwaltung vorbereiten**. Prüfe die ausgewählten Stellen und Bezirksvorschläge. **Nach Bezirk aufteilen** erstellt getrennte Entwürfe. Kontrolliere Empfängeradresse und Vorschau; der Entwurf nennt automatisch nur Kalenderwochen. **Im Mailprogramm öffnen** öffnet ihn auf dem Handy. Erst dort wird versendet. Eigene Notizen werden zunächst nicht übernommen. Näheres einschließlich Quellen und älterem Grenzdatenstand: [MAIL-MELDUNGEN.md](MAIL-MELDUNGEN.md).

## Mehrfachtreffer und Fotos ausprobieren

Öffne **Mehrfachtreffer bündeln** und bestätige nur dieselbe tatsächliche Unebenheit. Prüfe insbesondere benachbarte Schäden und die Straßenseite. Die bestätigte Fundstelle erscheint einmal mit ihrer Beobachtungsanzahl; Auflösen erhält die einzelnen Messungen.

Unter **Stelle prüfen und Fotos ergänzen** öffnest du **Fotos ansehen und ergänzen**. Wähle ein Bild oder nimm im sicheren Stand ein Foto auf. Tippe die Vorschau an und prüfe Bildinhalte. Die gespeicherte Kopie enthält keine ursprünglichen EXIF-Orts-/Zeitangaben; dein Albumoriginal bleibt erhalten.

Erzeuge eine Mail mit den gewünschten freigegebenen Beobachtungen. Wiederholungen erscheinen zusammen mit Anzahl und Kalenderwochen. Prüfe Fotoauswahl und Nummerierung und danach im Mailprogramm die tatsächlichen Anhänge. Versende erst nach Prüfung. Weitere Anleitung und Grenzen: [BUENDELUNG-FOTOS.md](BUENDELUNG-FOTOS.md).
