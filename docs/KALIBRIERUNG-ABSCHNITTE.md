# Vertikale Messung, nahe Treffer und Straßenabschnitte

## Fahrtbeginn

Das Handy vor dem Start fest am Lenker befestigen. Nach **Fahrt starten** zwei Sekunden im Stand ruhig halten, bis **Kalibriert** erscheint. Der Kalibrierfortschritt steht unter der Karte. GPS und Fahrtspur laufen währenddessen bereits; automatische Fundstellen warten auf Kalibrierung, brauchbaren Standort und Fahrgeschwindigkeit.

Die App mittelt stabile Beschleunigungswerte einschließlich Schwerkraft über mindestens zwei Sekunden und 100 Messungen. Bewegte, unplausible oder instabile Werte beginnen die Kalibrierung erneut. Bei einem aktuellen GPS-Fix mit mindestens 0,8 m/s wird keine Kalibrierung abgeschlossen. Ein fehlender GPS-Fix ersetzt diesen Bewegungsschutz nicht: Im Stand kalibrieren.

Der kalibrierte Schwerkraftvektor bestimmt die vertikale Achse; die Ruheschwerkraft wird abgezogen. Ausschließlich die Projektion auf diese Achse gelangt in die Erkennung. Aufwärts- und Abwärtsreaktionen werden über ihren Betrag gleich behandelt. Horizontalbeschleunigung verändert die Achse während der Fahrt nicht. Die Einzelstoßschwelle bleibt standardmäßig **40 m/s²**, einstellbar von 5 bis 100.

Wenn verfügbar, führt Androids Game-Rotation-Vector beziehungsweise Rotation-Vector die Orientierung bei Lageänderungen mit. Diese Sensorfusion ersetzt die bisherige laufende Neuschätzung der Achse aus dem Fahrsignal. Alte Lagedaten über 0,5 Sekunden werden nicht weiter zur Erkennung benutzt. Ohne verfügbaren Lagesensor bleibt die kalibrierte Achse fest; die Anzeige benennt diesen Modus. Bei veränderter Halterung im sicheren Stand **Im Stand neu kalibrieren** wählen. Eine Beschleunigungssensorlücke über eine Sekunde verlangt erneut stabile Kalibrierung.

Auch vertikale Smartphonebewegung beweist keinen Straßenschaden: Fahrweise, Halterung, Reifen und Fahrradbewegungen können beitragen. Die Messung bleibt ein Hinweis, der vor Ort geprüft werden muss. Eine Schadenshöhe wird nicht automatisch berechnet.

## Unter zwei Metern: zusammenfassbar

Die Startseite zeigt Vorschlagsgruppen **Zusammenfassbare Treffer** und markiert deren Originaleinträge. Auch ungeprüfte und noch nicht zugeordnete Messungen können teilnehmen. Über **Vorschläge prüfen** beziehungsweise **Mehrfachtreffer bündeln** lässt sich die Gruppe nach Ortskenntnis bestätigen.

Jedes Paar einer vorgeschlagenen Gruppe muss **streng unter 2 m** Abstand seiner angezeigten Koordinaten haben. So erzeugen verkettete nahe Punkte keine lange Gruppe. Bekannte widersprüchliche Schadensarten, verschiedene bekannte OSM-Wege und Fahrtrichtungsunterschiede über 45° verhindern einen Vorschlag. Ursprüngliche GPS-Genauigkeiten müssen endlich und höchstens 25 m sein. Straßenabschnitte werden nicht wie Einzelpunkte automatisch vorgeschlagen.

Zwei Meter sind ein Koordinatenvergleich, keine Zusicherung der GPS-Genauigkeit. GPS-Ungenauigkeit kann wesentlich größer sein. Vorschlagsgruppen sind noch keine bestätigten Fundstellen: Originale, Fotos und einzelne Freigaben bleiben erhalten, bis die Person die Bündelung bestätigt. Ungeprüfte Messungen werden dadurch nicht für Export oder Mail freigegeben.

## Unebener Straßenabschnitt

Die App betrachtet zusätzlich 250-ms-Fenster der vertikalen Beschleunigung. Als aktiv gilt ein Fenster mit mindestens acht Werten, dessen Streuung um den Fenstermittelwert mindestens `max(1,5 m/s², 15 % der Einzelstoßschwelle)` beträgt und bei dem mindestens 20 % der Werte um diesen Betrag vom Mittel abweichen. Eine einzelne Spitze oder ein konstanter Offset reicht nicht.

Nach mindestens drei Sekunden, mindestens 80 % aktiver Zeit und mindestens zehn aus Fahrgeschwindigkeit und Dauer geschätzten Metern wird **Unebener Straßenabschnitt** vorgeschlagen. Kurze ruhige Lücken werden toleriert; eine ruhige Strecke von 0,5 Sekunden beendet den Abschnitt. Standortverlust, unplausible Geschwindigkeit, Sensorlücken, Schwellenänderungen, Neukalibrierung und Fahrtende trennen Abschnitte. Sehr lange Störungen werden in höchstens zweiminütige Teile begrenzt.

Einzelstoßkandidaten innerhalb einer möglichen Störung werden kurz zurückgehalten. Wird ein Abschnitt bestätigt, werden diese Kandidaten nicht zusätzlich als Einzelstellen gespeichert. Andernfalls bleiben bestätigte Einzelstöße erhalten. Die Einträge sind ungeprüft und brauchen weiter die persönliche Freigabe. Die Abschnittsgrenzen und Parameter sind experimentelle Heuristiken, mit synthetischen Signalen getestet; sie brauchen Fahrtests auf dem Fairphone.

Gespeichert werden Anfang, Ende, Dauer, stärkste vertikale Reaktion und geschätzte Länge. Die Endpunkt-GPS-Messung und deren zeitliche Positionskorrektur bleiben nachvollziehbar. Die orange Linie verbindet Anfang und Ende ungefähr; sie ist keine vollständige Geometrie einer kurvigen Straße. Die Fahrtspur bleibt davon getrennt. Messdetails, Prüfung und CSV enthalten Abschnittsmetadaten; die zusätzliche letzte CSV-Spalte heißt `rough_section`. Das bestehende SQLite-Schema 6 kann die optionalen JSON-Metadaten speichern; alte Einträge benötigen keine nachträgliche Umdeutung.

Mailentwürfe nennen Anfang, Ende und geschätzte Länge; exakte Messzeiten bleiben ausgeschlossen. Am Wochenwechsel können beide Kalenderwochen erscheinen. Wenn Anfang und Ende verschiedene Bezirksvorschläge haben oder an einer Grenze liegen, bleibt die Zuständigkeit offen und muss selbst geprüft werden. Der Datensatz stammt weiterhin aus 2013 und ist kein verbindliches Zuständigkeitsregister.

## Aktionen auf der Karte

Auf der Startseitenkarte und der großen Karte einen Fundstellenpunkt antippen. Das Menü bietet **Stelle prüfen und Fotos ergänzen** sowie **Löschen**. Bei bestätigten Bündeln folgt die Auswahl der einzelnen Beobachtung. Auch eine orange Abschnittslinie öffnet diese Aktionen. Änderungen werden in Liste und Karte aktualisiert. Fotos verwenden dieselbe lokale Verarbeitung und begrenzte Freigabe wie bisher. Bitte ausschließlich im sicheren Stand bedienen.

## Prüfstand

211 automatisierte Tests bestehen. Synthetische Signale prüfen beliebige Halterungslagen, reine horizontale Beschleunigung, aufwärts/abwärts gerichtete Reaktionen, unruhige oder bewegte Kalibrierung, veraltete Lagedaten, einzelne Stöße und kontinuierliche Abschnitte. Android-Frameworktests auf simuliertem Android 9 und 15 prüfen den Sensorkallback bis zur Datenbank, Abschluss beim Fahrtende, Kartenaktionen, Fotoauswahl, einzelne Freigaben, JSON-Persistenz, CSV, Kalenderwochen und unklare Bezirkszuständigkeit. Android-Build und Lint sind geprüft; Lint meldet keine Fehler und zwei bestehende Warnungen. Der praktische Fahrtest und die tatsächliche Sensorfusion auf dem Fairphone stehen noch aus.
