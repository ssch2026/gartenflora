# GartenFlora

Pflanzenerkennung für Android — fotografieren, identifizieren, Gartentagebuch führen.

## Funktionen

- Kamera-Aufnahme mit bis zu 5 Fotos pro Identifikation
- Pflanzenbestimmung via [Pl@ntNet API](https://my.plantnet.org)
- Optionale KI-Pflegehinweise via Google Gemini 2.5 Flash
- Offline-Gartentagebuch (Room-Datenbank)
- Standorterfassung, Gartenstandort, eigene Notizen
- Vollständig auf Deutsch

## Voraussetzungen

- Android Studio Ladybug oder neuer
- JDK 21
- Android SDK 35
- Ein Pl@ntNet API-Schlüssel (kostenlos, 500 Anfragen/Tag)

## API-Schlüssel einrichten

### Pl@ntNet API-Schlüssel

1. Registrierung unter [my.plantnet.org](https://my.plantnet.org)
2. Im Profil unter „API access" einen Schlüssel erstellen
3. Kostenloses Kontingent: 500 Anfragen/Tag

### Schlüssel in `local.properties` eintragen

```properties
# Datei: local.properties (im Projektstamm, NICHT in Git)
PLANTNET_API_KEY=ihr_schluessel_hier

# Optional: Gemini-Pflegehinweise
# GEMINI_API_KEY=ihr_gemini_schluessel_hier
```

> Hinweis: `local.properties` ist in `.gitignore` eingetragen — Schlüssel werden nie ins Repository übertragen.

## Projekt bauen

```bash
# Debug-APK bauen
./gradlew assembleDebug

# Unit-Tests ausführen
./gradlew testDebugUnitTest

# Lint prüfen
./gradlew lintDebug
```

Die Debug-APK befindet sich nach dem Build unter:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Installation via ADB (Sideloading)

### Kabelgebunden

```bash
# Verbindung prüfen
adb devices

# APK installieren (-r = überschreiben falls vorhanden)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Kabellos (Wireless Debugging) — Pixel 10

1. **Entwickleroptionen aktivieren:**
   - Einstellungen → Über das Telefon → Build-Nummer 7× tippen

2. **Wireless Debugging aktivieren:**
   - Einstellungen → Entwickleroptionen → Kabellose Fehlerbehebung → Ein

3. **Pairing (einmalig):**
   - In „Kabellose Fehlerbehebung" → „Gerät mit QR-Code koppeln" tippen
   - Alternativ: „Gerät mit Kopplungscode koppeln"
   - Am PC ausführen:
     ```bash
     adb pair <IP>:<Port>
     # Kopplungscode aus dem Telefonbildschirm eingeben
     ```

4. **Verbinden:**
   ```bash
   adb connect <IP>:<Port>
   # Die IP:Port aus „Kabellose Fehlerbehebung" → „IP-Adresse & Port"
   ```

5. **APK installieren:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

## Projektstruktur

```
gartenflora/
├── app/src/main/kotlin/de/gartenflora/
│   ├── ui/               # Jetpack Compose Screens & ViewModels
│   │   ├── capture/      # Kamera & Aufnahme
│   │   ├── results/      # Identifikationsergebnisse
│   │   ├── garden/       # Mein Garten (Übersicht)
│   │   ├── detail/       # Pflanzendetails & Bearbeitung
│   │   └── settings/     # Einstellungen
│   ├── data/
│   │   ├── local/        # Room-Datenbank
│   │   ├── remote/       # Retrofit API-Services
│   │   └── repository/   # Repository-Implementierung
│   └── domain/
│       ├── model/        # Domain-Modelle
│       └── usecase/      # Use Cases
├── .github/workflows/    # CI/CD (GitHub Actions)
└── local.properties.example
```

## Technologie-Stack

| Komponente | Technologie |
|-----------|-------------|
| Sprache | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architektur | MVVM + Repository |
| Dependency Injection | Hilt |
| Datenbank | Room |
| Netzwerk | Retrofit + OkHttp |
| Serialisierung | kotlinx.serialization |
| Bilder | Coil |
| Kamera | CameraX |
| Tests | JUnit4 + MockK + Turbine |

## CI/CD

GitHub Actions Workflows:
- **ci.yml**: Unit-Tests + Debug-APK Build bei Push/PR
- **lint.yml**: Android Lint bei jedem Push

## Lizenz

Privat / nicht veröffentlicht.
