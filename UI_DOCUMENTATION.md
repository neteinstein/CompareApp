# Compare App - UI Documentation

## Screen Layout

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
│  │        [Button]               │  │
│  └───────────────────────────────┘  │
│                                     │
└─────────────────────────────────────┘
```

## User Flow

1. **App Launch**: User opens the Compare App
2. **Input**: User enters:
   - Pickup location in the first text field
   - Dropoff location in the second text field
3. **Compare**: User taps the "Compare" button
4. **Split Screen**: The app opens:
   - Uber app with pre-filled pickup and dropoff locations (via deep link)
   - Bolt app with pre-filled pickup and dropoff locations (via deep link)
   - Both apps appear in split screen mode

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

- Validates that both pickup and dropoff fields are filled before proceeding
- Shows Toast messages if:
  - Either field is empty
  - Uber app cannot be opened
  - Bolt app cannot be opened

## Requirements

- **Minimum Android Version**: Android 7.0 (API 24) - for split screen support
- **Required Apps**: 
  - Uber app installed
  - Bolt app installed
- **Permissions**: INTERNET permission (for potential future features)
