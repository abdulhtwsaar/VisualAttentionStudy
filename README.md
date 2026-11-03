# VisualAttentionStudy

**Android-Anwendung zur kamerabasierten Erfassung und Analyse des Blickverhaltens im Rahmen einer Bachelorarbeit**

## Projektübersicht

Dieses Projekt entstand im Rahmen meiner Bachelorarbeit im Studiengang Biomedizinische Technik an der htw saar.

**Titel der Bachelorarbeit:**

*Kamerabasiertes Eye-Tracking zur Blickdatenanalyse auf Android-Smartphones am Beispiel eines mobilen Spiels*

Ziel der Arbeit ist die Entwicklung und Evaluation eines prototypischen Systems zur kamerabasierten Erfassung und Analyse des Blickverhaltens auf Android-Smartphones. Dabei wird untersucht, inwieweit sich die integrierte Frontkamera eines Smartphones zur Erfassung von Blickdaten während einer visuellen Aufgabe einsetzen lässt.

## Funktionsumfang der Android-Anwendung

Die Android-Anwendung umfasst mehrere aufeinanderfolgende Schritte:

1. **Teilnehmerinformationen:** Erfassung grundlegender Angaben zur Versuchsperson.
2. **Kalibrierung:** Durchführung einer Kalibrierung mit 25 Zielpositionen in einem 5 × 5-Raster.
3. **Testinstruktion:** Darstellung der Anweisungen für die Versuchsdurchführung.
4. **Mobiles Spiel:** Durchführung eines visuellen Aufmerksamkeitstests mit eingeblendeten Bildern.
5. **Wiedererkennungstest:** Beantwortung von 20 Fragen zur Wiedererkennung zuvor präsentierter Bilder.

Während der Kalibrierung und des mobilen Spiels werden mithilfe der Frontkamera Videoaufnahmen erstellt.

## Technische Umsetzung

Die Android-Anwendung wurde mit **Kotlin** in **Android Studio** entwickelt.

Verwendete Technologien:

- Kotlin
- Android SDK
- CameraX
- Gradle
- Git und GitHub

Die Anwendung übernimmt die Durchführung der Versuche, die Videoaufzeichnung sowie die strukturierte Speicherung der Versuchsdaten.

## Blickdatenanalyse

Die eigentliche Blickschätzung erfolgt nicht direkt auf dem Android-Smartphone, sondern nachträglich in einer separaten Python-Auswertungspipeline.

Hierbei werden unter anderem folgende Verfahren eingesetzt:

- **MediaPipe:** Extraktion von Gesichts- und Augenmerkmalen.
- **L2CS-Net:** Schätzung der Blickrichtung.
- **Teilnehmerspezifische Kalibrierung:** Umrechnung ausgewählter Blickmerkmale in Bildschirmkoordinaten.
- **Region of Interest (ROI):** Untersuchung, ob geschätzte Blickpositionen innerhalb definierter Bildbereiche liegen.
- **Dwell-Time-Analyse:** Berechnung blickbasierter Aufenthaltszeiten innerhalb der Bildbereiche.

Die Python-Auswertung ist nicht Bestandteil dieses Android-Repositories.

## Projektarchitektur

Android-Anwendung → Videoaufzeichnung und Versuchsdaten → Python-Auswertung → Blickkoordinaten und ROI-Analyse → Ergebnisdateien

## Wissenschaftlicher Kontext

Das Projekt dient der Untersuchung der technischen Möglichkeiten und Grenzen eines kamerabasierten Eye-Tracking-Ansatzes auf handelsüblichen Android-Smartphones.

Die Ergebnisse sind als Evaluation eines Forschungsprototyps zu verstehen. Das System stellt kein medizinisch validiertes Diagnoseverfahren dar.

## Autor

**Abdul Majid Alabrash**

Bachelorstudiengang Biomedizinische Technik  
htw saar – Hochschule für Technik und Wirtschaft des Saarlandes

## Hinweis

Dieses Repository enthält den Quellcode der im Rahmen der Bachelorarbeit entwickelten Android-Anwendung. Die separate Python-Auswertungspipeline und die erhobenen personenbezogenen Versuchsdaten sind nicht enthalten.
