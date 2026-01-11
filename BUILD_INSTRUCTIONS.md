# Build Instructions for Compare App

## Prerequisites

1. **Android Studio**: Download and install Android Studio (Arctic Fox or newer)
2. **Java JDK**: JDK 8 or higher
3. **Android SDK**: SDK 33 (will be downloaded by Android Studio)

## Setup Steps

### 1. Clone the Repository

```bash
git clone https://github.com/neteinstein/CompareApp.git
cd CompareApp
```

### 2. Open in Android Studio

1. Launch Android Studio
2. Select "Open an Existing Project"
3. Navigate to the CompareApp directory
4. Click "OK"

### 3. Sync Gradle

Android Studio will automatically prompt you to sync Gradle files. If not:
1. Go to File > Sync Project with Gradle Files
2. Wait for the sync to complete

### 4. Build the Project

#### Using Android Studio:
1. Click "Build" in the menu
2. Select "Make Project" (or press Ctrl+F9 / Cmd+F9)

#### Using Command Line:
```bash
./gradlew assembleDebug
```

### 5. Run the App

#### Using Android Studio:
1. Connect an Android device or start an emulator
2. Click the "Run" button (green play icon)
3. Select your device/emulator

#### Using Command Line:
```bash
./gradlew installDebug
```

## Testing the App

### Requirements for Full Functionality:
- Android device or emulator running Android 7.0 (API 24) or higher
- Uber app installed on the device
- Bolt app installed on the device

### Testing Steps:
1. Launch the Compare App
2. Enter a pickup location (e.g., "Times Square, New York")
3. Enter a dropoff location (e.g., "Central Park, New York")
4. Tap the "Compare" button
5. Observe that Uber and Bolt apps open in split screen mode

## Troubleshooting

### Gradle Sync Failed
- Ensure you have an internet connection
- Check that Android SDK is properly installed
- Try "File > Invalidate Caches / Restart"

### App Crashes on Launch
- Verify minimum SDK version is met (API 24+)
- Check logcat for error messages

### Deep Links Not Working
- Ensure Uber and Bolt apps are installed
- Deep links only work with official apps from app stores
- Some emulators may not have these apps installed

## Building Release APK

```bash
./gradlew assembleRelease
```

The APK will be located at:
```
app/build/outputs/apk/release/app-release-unsigned.apk
```

## Notes

- The app requires both Uber and Bolt to be installed for full functionality
- Split screen feature requires Android 7.0 (API 24) or higher
- For production use, you'll need to sign the APK with your own keystore
