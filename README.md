# CompareApp

An Android app that allows users to compare ride-sharing services (Uber and Bolt) side-by-side in split screen mode.

## Features

- Modern Jetpack Compose UI with Material3 design
- Simple and intuitive interface with a "Compare App" title
- Two text input fields for Pickup and Dropoff locations
- Compare button with loading indicator during geocoding
- Deep linking support for both Uber and Bolt apps with automatic coordinate conversion

## How to Use

1. Enter your pickup location in the "Pickup" field
2. Enter your dropoff location in the "Dropoff" field
3. Tap the "Compare" button
4. The app will open Uber and Bolt in split screen mode with your locations pre-filled

## Requirements

- Android API 24 (Android 7.0) or higher
- Uber app installed (for Uber deep link to work)
- Bolt app installed (for Bolt deep link to work)

## Building the App

To build this app, you need:
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 36
- Gradle 8.0+

### Build Steps

```bash
./gradlew assembleDebug
```

### Release and Deployment

For information on signing APKs and deploying to Google Play Store, see [DEPLOYMENT.md](DEPLOYMENT.md).

## Deep Link Format

The app uses the following deep link formats:

**Uber:**
```
uber://?action=setPickup&pickup[formatted_address]=PICKUP&dropoff[formatted_address]=DROPOFF
```

**Bolt:**
```
bolt://ride?pickup_lat=LAT&pickup_lng=LNG&destination_lat=LAT&destination_lng=LNG
```

The app automatically converts text addresses to coordinates using Android's Geocoder service for the Bolt deep link. If geocoding fails, it falls back to a text-based format.

## Technical Details

- **Language:** Kotlin
- **Minimum SDK:** 24
- **Target SDK:** 36
- **UI Framework:** Jetpack Compose with Material3
- **Architecture:** Single Activity with Compose UI 
