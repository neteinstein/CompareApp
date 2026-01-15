# CompareApp - GitHub Copilot Instructions

## Project Overview

CompareApp is an Android application that allows users to compare ride-sharing services (Uber and Bolt) side-by-side in split screen mode. The app provides a simple interface where users enter pickup and dropoff locations, and the app opens both Uber and Bolt apps simultaneously for easy price comparison.

**Target Users:** Android users who want to compare ride prices between Uber and Bolt before booking.

**Key Features:**
- Modern Jetpack Compose UI with Material3 design
- Location input with geocoding support
- Deep linking to Uber and Bolt apps with coordinate conversion
- Split screen functionality for simultaneous comparison

## Tech Stack

- **Language:** Kotlin
- **Build System:** Gradle 8.14.3
- **Minimum SDK:** 24 (Android 7.0)
- **Target SDK:** 36
- **UI Framework:** Jetpack Compose with Material3
- **Architecture:** MVVM (Model-View-ViewModel)
- **Dependency Injection:** Hilt (Dagger)
- **Async Programming:** Kotlin Coroutines
- **Location Services:** Google Play Services Location API
- **Testing:**
  - JUnit 4 for unit tests
  - Robolectric for Android component testing
  - Roborazzi for screenshot testing
  - Espresso for instrumented tests
  - Mockito/MockK for mocking

## Coding Guidelines

### General Conventions
- Use Kotlin idioms and best practices (data classes, sealed classes, extension functions)
- Follow Android Architecture Components patterns
- Prefer immutability - use `val` over `var` when possible
- Use meaningful variable and function names

### Architecture Patterns
- Follow MVVM architecture as documented in `docs/ARCHITECTURE.md`
- Business logic belongs in ViewModels, not Activities
- Data operations go in Repository implementations
- UI code should be in Composable functions in `ui/screens/`
- Use Hilt for dependency injection - add modules to `di/` package

### Compose UI
- Use Material3 components and theming
- Prefer stateless composables with hoisted state
- Use `remember` and `rememberSaveable` appropriately
- Follow Compose best practices for recomposition optimization

### Testing
- Write unit tests for ViewModels and Repositories
- Use `TestViewModelFactory` helper for consistent ViewModel testing
- Mock dependencies using Mockito/MockK
- Test files should mirror the structure of source files
- All new features should include tests

### Code Style
- Use 4 spaces for indentation
- Follow Kotlin naming conventions (camelCase for functions/variables, PascalCase for classes)
- Keep functions small and focused on a single responsibility
- Add KDoc comments for public APIs

## Project Structure & Workflow

### Directory Organization

```
app/src/main/java/org/neteinstein/compareapp/
├── data/
│   └── repository/          # Repository implementations and interfaces
├── di/                      # Hilt dependency injection modules
├── ui/
│   ├── screens/            # Composable screens and ViewModels
│   └── theme/              # Material3 theme configuration
├── utils/                  # Utility classes and functions
├── CompareApplication.kt   # Application class with Hilt
└── MainActivity.kt         # Main activity entry point
```

### Build and Test Commands

**Build the app:**
```bash
./gradlew assembleDebug
```

**Run unit tests:**
```bash
./gradlew test
```

**Run lint checks:**
```bash
./gradlew lint
```

**Build release APK:**
```bash
./gradlew assembleRelease
```

### CI/CD Workflow
- Pull requests trigger automatic lint and test checks via GitHub Actions
- Pushing to `main` creates a signed release APK
- Manual workflow available for Play Store deployment
- Version code auto-increments using GitHub Actions run number

### Version Management
- Use semantic versioning (MAJOR.MINOR.PATCH) in `versionName`
- `versionCode` auto-increments via `BUILD_NUMBER` environment variable
- Update `versionName` in `app/build.gradle` for minor/major releases
- Release tags use format: `vMAJOR.MINOR.PATCH.BUILD`

## Resources

### Key Documentation
- **Project Overview:** `README.md` - Features, usage instructions, and quick start guide
- **Architecture:** `docs/ARCHITECTURE.md` - Detailed MVVM architecture and design patterns
- **CI/CD Pipeline:** `docs/CICD.md` - Continuous integration and deployment workflows
- **Deployment:** `docs/DEPLOYMENT.md` - APK signing and Play Store deployment guide
- **Future Roadmap:** `docs/BRAINSTORM.md` - Ideas for future enhancements

### Example Files
- **ViewModel Example:** `app/src/main/java/org/neteinstein/compareapp/ui/screens/MainViewModel.kt`
- **Repository Example:** `app/src/main/java/org/neteinstein/compareapp/data/repository/LocationRepositoryImpl.kt`
- **Compose UI Example:** `app/src/main/java/org/neteinstein/compareapp/ui/screens/CompareScreen.kt`
- **DI Module Example:** `app/src/main/java/org/neteinstein/compareapp/di/AppModule.kt`
- **Test Example:** `app/src/test/java/org/neteinstein/compareapp/MainActivityTest.kt`

### Important Configuration Files
- **App Gradle:** `app/build.gradle` - Dependencies, SDK versions, signing config
- **Root Gradle:** `build.gradle` - Build tool versions and repositories
- **Android Manifest:** `app/src/main/AndroidManifest.xml` - App configuration
