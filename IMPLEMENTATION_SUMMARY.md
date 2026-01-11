# Compare App - Implementation Summary

## Overview
This Android application allows users to compare ride-sharing services (Uber and Bolt) by opening both apps side-by-side in split screen mode with pre-filled pickup and dropoff locations.

## Requirements Met ✓

### 1. Screen with Title "Compare App"
✓ Implemented in `activity_main.xml` with a TextView displaying "Compare App" in bold, 24sp font

### 2. Two Text Inputs
✓ **Pickup**: EditText with hint "Pickup"
✓ **Dropoff**: EditText with hint "Dropoff"

### 3. Compare Button
✓ Button labeled "Compare" that triggers the deep link functionality

### 4. Deep Link Integration
✓ **Uber Deep Link**: `uber://?action=setPickup&pickup[formatted_address]=...&dropoff[formatted_address]=...`
✓ **Bolt Deep Link**: `bolt://rideplanning?pickup=...&destination=...`

### 5. Split Screen Opening
✓ Uses `FLAG_ACTIVITY_LAUNCH_ADJACENT` to open both apps in split screen mode
✓ Implements proper timing with 500ms delay between launches

## File Structure

```
CompareApp/
├── app/
│   ├── build.gradle                         # App-level Gradle configuration
│   ├── proguard-rules.pro                   # ProGuard rules
│   └── src/main/
│       ├── AndroidManifest.xml              # App manifest with permissions
│       ├── java/com/example/compareapp/
│       │   └── MainActivity.kt              # Main activity with business logic
│       └── res/layout/
│           └── activity_main.xml            # UI layout file
├── build.gradle                             # Project-level Gradle configuration
├── settings.gradle                          # Gradle settings
├── gradle/wrapper/
│   └── gradle-wrapper.properties            # Gradle wrapper configuration
├── README.md                                # Project overview and documentation
├── BUILD_INSTRUCTIONS.md                    # Detailed build instructions
└── UI_DOCUMENTATION.md                      # UI design and flow documentation
```

## Key Features Implemented

### 1. Input Validation
```kotlin
if (pickup.isEmpty() || dropoff.isEmpty()) {
    Toast.makeText(this, "Please enter both pickup and dropoff locations", Toast.LENGTH_SHORT).show()
    return@setOnClickListener
}
```

### 2. URL Encoding
```kotlin
val pickupEncoded = URLEncoder.encode(pickup, "UTF-8")
val dropoffEncoded = URLEncoder.encode(dropoff, "UTF-8")
```

### 3. Split Screen Intent Flags
```kotlin
uberIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
boltIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
```

### 4. Error Handling
```kotlin
try {
    startActivity(uberIntent)
} catch (e: Exception) {
    Toast.makeText(this, "Could not open Uber app", Toast.LENGTH_SHORT).show()
}
```

### 5. Modern Handler Usage
```kotlin
Handler(Looper.getMainLooper()).postDelayed({
    // Launch Bolt after delay
}, 500)
```

## Technical Specifications

- **Language**: Kotlin
- **Minimum SDK**: 24 (Android 7.0 Nougat) - Required for split screen support
- **Target SDK**: 33 (Android 13)
- **Architecture**: Single Activity Application
- **UI Framework**: Android Views with LinearLayout
- **Dependencies**:
  - AndroidX Core KTX 1.9.0
  - AndroidX AppCompat 1.6.1
  - Material Components 1.8.0

## Testing Requirements

To fully test this application:
1. Android device or emulator with Android 7.0+
2. Uber app installed from Play Store
3. Bolt app installed from Play Store
4. Device must support split screen mode

## Usage Example

1. Launch the Compare App
2. Enter "Times Square, New York" in the Pickup field
3. Enter "Central Park, New York" in the Dropoff field
4. Tap the Compare button
5. Uber app opens on the left side with locations pre-filled
6. Bolt app opens on the right side with locations pre-filled
7. User can now compare prices and features side-by-side

## Security Considerations

✓ No sensitive data stored
✓ No custom permissions required beyond INTERNET
✓ URL encoding prevents injection attacks
✓ Proper exception handling for missing apps
✓ No deprecated APIs used (Handler updated to use Looper.getMainLooper())

## Build Status

✓ Project structure created
✓ Gradle configuration complete
✓ Code review passed with issues addressed
✓ No security vulnerabilities detected
✓ Documentation complete

## Notes

- The app requires both Uber and Bolt apps to be installed for full functionality
- If either app is not installed, a user-friendly error message is displayed
- Deep link formats are based on official Uber and Bolt documentation
- Split screen behavior may vary depending on device manufacturer and Android version
