# CompareApp

An Android app that allows users to compare ride-sharing services (Uber and Bolt) side-by-side in split screen mode.

## Overview

CompareApp simplifies the process of comparing ride prices between Uber and Bolt by automatically opening both apps side-by-side with your pickup and dropoff locations pre-filled. This allows you to make quick, informed decisions about which service offers the best value for your journey.

## Features

- **Modern UI**: Built with Jetpack Compose and Material3 design system
- **Split Screen Mode**: Automatically opens Uber and Bolt apps side-by-side
- **Smart Geocoding**: Converts text addresses to coordinates for accurate location matching
- **Deep Linking**: Seamlessly integrates with Uber and Bolt apps using their deep link APIs
- **Location Services**: Supports current location detection with Google Play Services
- **Offline Fallback**: Gracefully handles geocoding failures with text-based fallbacks
- **MVVM Architecture**: Clean separation of concerns with Hilt dependency injection

## Quick Start

### Prerequisites

- Android device or emulator running Android 7.0 (API 24) or higher
- Uber app installed (for Uber comparison)
- Bolt app installed (for Bolt comparison)
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

1. Launch the CompareApp
2. Enter your **pickup location** (e.g., "Times Square, New York")
3. Enter your **dropoff location** (e.g., "Central Park, New York")
4. Tap the **Compare** button
5. Both Uber and Bolt apps will open in split screen mode with your locations pre-filled
6. Compare prices and features to choose the best option

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

- **[Architecture Guide](docs/ARCHITECTURE.md)** - Detailed low-level architecture
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
- Uber app installed from Play Store
- Bolt app installed from Play Store
- Internet connection (for geocoding)
- Location permissions (for current location feature)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and questions, please open an issue on GitHub. 
