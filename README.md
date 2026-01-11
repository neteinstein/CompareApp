# CompareApp

An Android app that allows users to compare ride-sharing services (Uber and Bolt) side-by-side in split screen mode.

## Features

- Simple and intuitive UI with a "Compare App" title
- Two text input fields for Pickup and Dropoff locations
- Compare button to launch both Uber and Bolt apps in split screen
- Deep linking support for both Uber and Bolt apps

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
- Android Studio
- Android SDK 33
- Gradle 7.5

### Build Steps

```bash
./gradlew assembleDebug
```

## Deep Link Format

The app uses the following deep link formats:

**Uber:**
```
uber://?action=setPickup&pickup[formatted_address]=PICKUP&dropoff[formatted_address]=DROPOFF
```

**Bolt:**
```
bolt://rideplanning?pickup=PICKUP&destination=DROPOFF
```

## Technical Details

- **Language:** Kotlin
- **Minimum SDK:** 24
- **Target SDK:** 33
- **Architecture:** Single Activity with simple UI layout 
