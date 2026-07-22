# Color Flow

Puzzle game where you connect matching-colored dots by drawing paths on a grid. Fill the entire board without crossing paths.

## Screenshots

<div align="center">
  <img src="screenshots/1.png" width="300" alt="Start Screen">
  <img src="screenshots/2.png" width="300" alt="Gameplay">
  <img src="screenshots/3.png" width="300" alt="Victory">
  <img src="screenshots/4.png" width="300" alt="Levels">
</div>

## Features

- **6 color pairs** to connect on a grid
- **3 difficulty levels**: Easy (6x6), Medium (8x8), Hard (10x10)
- **Undo system** with full move history
- **Hints** to help when stuck
- **Level progression** with 10 levels per difficulty
- **Statistics tracking**: games played, win rate, best times
- **Dark/Light theme** support
- **Procedural board generation** — every game is unique

## Tech Stack

| | |
|---|---|
| Language | Kotlin 2.1.0 |
| UI | Compose Multiplatform 1.7.3 + Material 3 |
| Serialization | kotlinx-serialization 1.7.3 |
| Build | Gradle 9.6.1, JDK 21 |

## Build & Run

```bash
# Run desktop app
./gradlew :desktop:run

# Run tests
./gradlew :common:jvmTest

# Package as .dmg (macOS)
./gradlew :desktop:packageDmg
```

## Project Structure

```
common/          — Platform-agnostic logic, models, storage
  └─ src/
     ├─ commonMain/   — GameLogic, GameState, GameStorage
     ├─ commonTest/   — Unit tests
     └─ jvmMain/      — JVM storage implementation
desktop/         — Compose Desktop app (UI + entry point)
```

## License

MIT-0
