# Kann das Handy eine Bodenwellenhöhe messen?

Aus den derzeit gespeicherten Spitzenwerten und der GPS-Geschwindigkeit lässt sich keine verlässliche Höhe berechnen. Das Handy misst seine eigene Bewegung am Lenker. Reifen, Fahrrad, Halterung und Körperbewegung verändern diese Reaktion. Eine kurze Kante und eine lange Welle gleicher Höhe können sehr unterschiedliche Beschleunigungen erzeugen.

Auch die doppelte Integration der Beschleunigung würde zunächst die Bewegung des Handys liefern, nicht direkt das Straßenprofil. Kleine Fehler beim Abzug der Schwerkraft, Neigung und unbekannte Anfangsbedingungen erzeugen dabei große Höhenfehler. Ein Algorithmus, der daraus ohne Kalibrierung Zentimeter verspricht, wäre irreführend.

Eine experimentelle Höhenschätzung für deine konkrete Kombination wäre ein späteres Forschungsziel. Dafür brauchen wir:

- synchronisierte Rohmessreihen der drei Beschleunigungsachsen, Drehratensensoren und Orientierung vor, während und nach jeder Unebenheit sowie Geschwindigkeit und Zeitstempel;
- bekannte, ungefährliche Referenzstellen mit gemessener Höhe **und Form beziehungsweise Länge in Fahrtrichtung**;
- Reifendruck vorne/hinten, Reifenbreite, vorhandene Federung und Beladung; Fahrrad, Handy und Halterungsmodell sind bereits bekannt;
- wiederholte normale Fahrten über dieselben Stellen bei unterschiedlichen bekannten Geschwindigkeiten sowie glatte Vergleichsabschnitte, mit gleichbleibender Befestigung.

Die aktuelle Testversion speichert solche Rohmessreihen noch nicht. Ein Kalibrierungsmodus müsste sie gezielt lokal erfassen und zur Prüfung exportierbar machen. Anschließend wäre ein Modell auf einem Teil der Referenzfahrten zu entwickeln und auf anderen, unabhängigen Fahrten zu prüfen. Erst diese Prüfung zeigt, ob eine Höhenschätzung sinnvoll ist und wie groß ihr Fehler ist. Das Modell wäre zunächst nur für die getestete Kombination belastbar.

Bis dahin hilft der Sensitivitätsregler, Fehlalarme und verpasste Stellen an deinem Fahrrad einzustellen. Seine Einheit m/s² bezeichnet die gefilterte Sensorreaktion, keine Straßenhöhe oder rechtliche Bewertung.
