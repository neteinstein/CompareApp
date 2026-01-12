# Compare App - UI Documentation

## Screen Layout

### When Both Apps Installed
```
┌─────────────────────────────────────┐
│                                     │
│         Compare App                 │
│         (Title - Bold, 24sp)        │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  ┌───────────────────────────────┐  │
│  │  Pickup                       │  │
│  │  [Text input field]           │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │  Dropoff                      │  │
│  │  [Text input field]           │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │        Compare                │  │
│  │        [Button - Enabled]     │  │
│  └───────────────────────────────┘  │
│                                     │
└─────────────────────────────────────┘
```

### When Apps Missing
```
┌─────────────────────────────────────┐
│                                     │
│         Compare App                 │
│         (Title - Bold, 24sp)        │
│                                     │
│   ⚠️  Warning: Uber and Bolt apps   │
│      are required for this to work  │
│         (Red text, 14sp)            │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  ┌───────────────────────────────┐  │
│  │  Pickup                       │  │
│  │  [Text input field]           │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │  Dropoff                      │  │
│  │  [Text input field]           │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │        Compare                │  │
│  │        [Button - Disabled]    │  │
│  └───────────────────────────────┘  │
│                                     │
└─────────────────────────────────────┘
```

## User Flow

### Normal Flow (Apps Installed)
1. **App Launch**: User opens the Compare App
   - App checks if Uber and Bolt are installed
   - No warning shown, Compare button is enabled
2. **Input**: User enters:
   - Pickup location in the first text field
   - Dropoff location in the second text field
3. **Compare**: User taps the "Compare" button
4. **Split Screen**: The app opens:
   - Uber app with pre-filled pickup and dropoff locations (via deep link)
   - Bolt app with pre-filled pickup and dropoff locations (via deep link)
   - Both apps appear in split screen mode

### Apps Not Installed Flow
1. **App Launch**: User opens the Compare App
   - App detects Uber and/or Bolt are not installed
   - Red warning message appears beneath the title
   - Compare button is disabled (grayed out)
2. **Install Apps**: User sees warning and installs missing app(s)
3. **Return to App**: User returns to Compare App
   - App automatically re-checks installation status (ON_RESUME)
   - Warning disappears if apps are now installed
   - Compare button becomes enabled
4. **Continue**: User can now proceed with normal flow

## Technical Implementation

### Deep Links

**Uber Deep Link Format:**
```
uber://?action=setPickup&pickup[formatted_address]=PICKUP&dropoff[formatted_address]=DROPOFF
```

**Bolt Deep Link Format:**
```
bolt://rideplanning?pickup=PICKUP&destination=DROPOFF
```

### Split Screen Implementation

The app uses Android's split screen functionality by:
1. Setting `FLAG_ACTIVITY_NEW_TASK` and `FLAG_ACTIVITY_LAUNCH_ADJACENT` on both intents
2. Starting Uber first
3. Waiting 500ms for the split screen to be ready
4. Starting Bolt second

### Error Handling

- **App Installation Validation**:
  - Checks if Uber (com.ubercab) and Bolt (ee.mtakso.client) apps are installed
  - Shows warning label when apps are missing
  - Disables Compare button when apps are not installed
  - Automatically refreshes validation when returning to the app
- **Input Validation**:
  - Validates that both pickup and dropoff fields are filled before proceeding
- **Toast Messages**:
  - Shows message if either field is empty
  - Shows message if Uber app cannot be opened
  - Shows message if Bolt app cannot be opened

### App Installation Detection

The app uses PackageManager to check if required apps are installed:
- **Check on Launch**: Validates app installation when screen is first displayed
- **Check on Resume**: Re-validates when user returns to the app (e.g., after installing missing apps)
- **Dynamic UI Updates**: Warning and button state update automatically based on installation status

## Requirements

- **Minimum Android Version**: Android 7.0 (API 24) - for split screen support
- **Required Apps**: 
  - Uber app installed
  - Bolt app installed
- **Permissions**: INTERNET permission (for potential future features)
