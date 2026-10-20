# BeеWarrior club

Offline karate training companion for Android (Kotlin, Jetpack Compose, Material 3).

Discover dojo sessions, register without an account, practice kata section by section with
**Kata Coach**, track workouts, set personal goals and level up through **Dojo Progress**.
Everything is stored locally on the device.

## Features

- **Home** – hero, next dojo training, app progress level (belt bar), current goal, kata of the week, streak, recent training, Quick Start Kata / Workout.
- **Dojo** – catalog of 10 fictional sessions with filters (All, Kata, Kumite, Technique, Conditioning, Beginner, Intermediate, Advanced), details with *What to bring*, validated registration (no account, optional belt / type / comment, consent), confirmation screen and weekly schedule.
- **Kata** – library of 12 kata in Beginner / Intermediate / Advanced with a style disclaimer; kata detail with section breakdown and progress.
- **Kata Coach** – mark each section *Completed / Needs Practice / Difficult*, optional elapsed-time timer (Start, Pause, Resume, Restart, Finish), self-assessment (Technique, Balance, Timing, Confidence, difficulty), Personal Kata Score, accuracy, local analysis with strong areas, most difficult section and next-practice suggestion. Single-section practice mode for weak sections.
- **Workout tracker** – 7 workout types, 30/45/60/90 min targets, elapsed-time timer, effort rating, Save Workout.
- **Personal goals** – weekly sessions / kata / workouts / minutes, practice a kata N times, improve a kata score, practice the weakest section, training streak. Progress updates automatically.
- **Dojo Progress** – Training Points (XP): workout +20, kata +30, goal +50, weak section +15, 7-day streak +75. Levels White → Black, clearly labelled *App Progress Level*, not an official rank.
- **Progress** – 8 statistics and charts (minutes per week, kata score over time, workout frequency, 14-day streak), recent XP.
- **History, Profile, Notification settings, Privacy Policy, Terms, Reset Local Data.**

## Architecture

```
app/src/main/java/com/katadojo/app/
├── data/
│   ├── model/        KarateTraining, TrainingRegistration, Kata, KataSection, KataSession,
│   │                 SectionResult, WorkoutSession, PersonalGoal, UserProfile, ProgressLevel …
│   ├── database/     Room entities, DAOs, mappers (katadojo.db)
│   ├── repository/   Training, Kata, Workout, Goal, XP repositories + DataStore preferences
│   ├── ProgressEngine.kt   awards XP for activities, goals and streaks (unique event keys)
│   ├── DemoDataSeeder.kt   demo content generated relative to today
│   └── AppContainer.kt     manual dependency injection
├── navigation/       routes + NavHost with bottom navigation
├── viewmodel/        one ViewModel per screen (MVVM, StateFlow / Compose state)
├── ui/               home, dojo, kata, workout, progress, profile, components, theme
└── utils/            KataAnalyzer, KataScoring, XpCalculator, GoalProgressCalculator,
                      StatisticsCalculator, Stopwatch (timer logic), Formatters, Validation
```

Timers use `SystemClock.elapsedRealtime()` (accumulated time + time since start), never UI tick counting,
and are persisted in `SavedStateHandle`, so recomposition, rotation or process death do not break them.

## Build

Requirements: Android Studio (Ladybug or newer), JDK 17, Android SDK 36.

- AGP 8.13.0, Gradle 8.13, Kotlin 2.0.21, KSP, Compose BOM 2024.12.01, Room 2.6.1
- `minSdk 26`, `targetSdk 36`, `applicationId com.starfoodbv.katadojo` (code namespace `com.katadojo.app`)

```bash
gradle wrapper --gradle-version 8.13   # once, if you want ./gradlew
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew bundleRelease                 # app/build/outputs/bundle/release/app-release.aab
```

### Release signing

Create `keystore.properties` in the project root (it is git-ignored):

```
storeFile=katadojo-upload.jks
storePassword=...
keyAlias=katadojo
keyPassword=...
```

Without it, `bundleRelease` still produces an unsigned `.aab`.

### GitHub Actions

`.github/workflows/build.yml` runs unit tests and builds the debug APK and release AAB.
Optional signing secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## Privacy

No account, no network permission, no analytics, no ads. All data stays on the device and can be deleted
with **Profile → Reset Local Data**.
