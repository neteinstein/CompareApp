# CompareApp

An Android app that lets users compare 2 providers of a service at a time - ride-sharing (Uber and Bolt) or food delivery (pick any 2 of Uber Eats, Bolt Food, and Glovo) - side-by-side in split screen mode.

## Overview

CompareApp simplifies comparing prices between two apps at once by opening both side-by-side in split screen with your search already filled in - locations for rides, or a restaurant/dish for food delivery. A snackbar reminds you to swipe the middle divider up or down to pick the cheaper one. Which food delivery apps are compared is configurable from Settings.

## Features

- **Modern UI**: Built with Jetpack Compose and Material3 design system
- **Split Screen Mode**: Automatically opens two apps side-by-side, with a hint on how to swipe to the one you want
- **Ride Comparison**: Compares Uber and Bolt for a given pickup/dropoff
- **Food Delivery Comparison**: Compares any 2 of Uber Eats, Bolt Food, and Glovo for a restaurant or dish search, configurable from Settings
- **Smart Geocoding**: Converts text addresses to coordinates for accurate location matching
- **Deep Linking**: Seamlessly integrates with each provider's app using their deep link APIs
- **Incoming Location Links**: Share a location from Maps ("Open with" a `geo:` link) to prepopulate the dropoff field, using your current location as pickup
- **Location Services**: Supports current location detection with Google Play Services
- **Offline Fallback**: Gracefully handles geocoding failures with text-based fallbacks
- **MVVM Architecture**: Clean separation of concerns with Hilt dependency injection

## Quick Start

### Prerequisites

- Android device or emulator running Android 7.0 (API 24) or higher
- Uber and Bolt apps installed (for ride comparison)
- 2 of Uber Eats, Bolt Food, and Glovo installed (for food delivery comparison)
- Android Studio Hedgehog (2023.1.1) or later (for development)

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/neteinstein/CompareUberVsBoltPriceApp.git
   cd CompareUberVsBoltPriceApp
   ```

2. Open the project in Android Studio

3. Sync Gradle and build:
   ```bash
   ./gradlew assembleDebug
   ```

4. Run on your device or emulator

### How to Use

**Rides:**
1. Launch the CompareApp
2. Enter your **pickup location** (e.g., "Times Square, New York")
3. Enter your **dropoff location** (e.g., "Central Park, New York")
4. Tap the **Compare** button
5. Both Uber and Bolt apps will open in split screen mode with your locations pre-filled
6. Swipe the middle divider up or down to bring the cheaper one to full screen

**Food delivery:**
1. Enter a restaurant name or a dish to search for
2. Tap **Search Food**
3. Your 2 selected food delivery apps (configurable under Settings > Comparison configuration) open in split screen with the search pre-filled where supported
4. Swipe the middle divider up or down to bring the cheaper one to full screen

## High-Level Architecture

CompareApp follows the MVVM (Model-View-ViewModel) architecture pattern with Hilt dependency injection:

```
┌─────────────────────────────────────────────────────────┐
│                   Presentation Layer                     │
│  ┌──────────────┐         ┌────────────────────┐       │
│  │  MainActivity │ ◄─────► │  CompareScreen     │       │
│  │ (Entry Point) │         │  (Compose UI)      │       │
│  └──────────────┘         └─────────┬──────────┘       │
│                                      │                   │
│                                      ▼                   │
│                            ┌─────────────────┐          │
│                            │  MainViewModel  │          │
│                            │ (Business Logic)│          │
│                            └────────┬────────┘          │
└─────────────────────────────────────┼───────────────────┘
                                      │
┌─────────────────────────────────────┼───────────────────┐
│                    Domain Layer     │                   │
│                                     │                   │
│            ┌────────────────────────┴────────┐          │
│            │                                 │          │
│            ▼                                 ▼          │
│  ┌──────────────────┐           ┌─────────────────┐    │
│  │ LocationRepository│           │  AppRepository  │    │
│  │  (Geocoding &    │           │  (App Install   │    │
│  │   Location)      │           │   Checking)     │    │
│  └──────────────────┘           └─────────────────┘    │
└─────────────────────────────────────────────────────────┘
                         │
┌────────────────────────┴─────────────────────────────────┐
│                    Data Layer                            │
│  ┌──────────────────┐           ┌─────────────────┐     │
│  │    Geocoder      │           │  PackageManager │     │
│  │ (Android System) │           │ (Android System)│     │
│  └──────────────────┘           └─────────────────┘     │
└──────────────────────────────────────────────────────────┘
```

### Key Components

- **MainActivity**: Android entry point and deep link launcher
- **CompareScreen**: Jetpack Compose UI for user input
- **MainViewModel**: Manages UI state and business logic
- **LocationRepository**: Handles geocoding and location services
- **AppRepository**: Checks app installation status

## How to Run It

### Development Build

```bash
# Debug build
./gradlew assembleDebug

# Install to connected device
./gradlew installDebug

# Run tests
./gradlew test

# Run lint checks
./gradlew lint
```

### Release Build

```bash
# Build unsigned release APK
./gradlew assembleRelease
```

The APK will be at: `app/build/outputs/apk/release/app-release-unsigned.apk`

For signed releases and Play Store deployment, see [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)

## Documentation

- **[CHANGELOG](CHANGELOG.md)** - Release history and version changes
- **[Architecture Guide](docs/ARCHITECTURE.md)** - Detailed low-level architecture
- **[Deep Links Reference](docs/DEEP_LINKS.md)** - Uber, Uber Eats, Bolt, and Bolt Food deep-link formats: what's officially documented, what's verified from each app's manifest, and what's still an unverified guess
- **[CI/CD Pipeline](docs/CICD.md)** - Continuous integration and deployment
- **[Deployment Guide](docs/DEPLOYMENT.md)** - Play Store deployment process
- **[Future Roadmap](docs/BRAINSTORM.md)** - Ideas for future enhancements

## Technical Stack

- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 36 (Android 14+)
- **UI Framework**: Jetpack Compose with Material3
- **Architecture**: MVVM with Hilt dependency injection
- **Location Services**: Google Play Services Location API
- **Geocoding**: Android Geocoder API
- **Testing**: JUnit, Mockito, Robolectric, Espresso

## Requirements

- Android device with API 24+ (Android 7.0 or higher)
- Split screen support (available on Android 7.0+)
- Uber and Bolt apps installed from Play Store (for ride comparison)
- The 2 food delivery apps selected in Settings installed from Play Store (for food delivery comparison)
- Internet connection (for geocoding)
- Location permissions (for current location feature)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and questions, please open an issue on GitHub. 
