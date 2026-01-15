# Architecture Documentation

## Table of Contents

1. [Overview](#overview)
2. [Architecture Pattern](#architecture-pattern)
3. [Project Structure](#project-structure)
4. [Layer Breakdown](#layer-breakdown)
5. [Component Details](#component-details)
6. [Data Flow](#data-flow)
7. [Dependency Injection](#dependency-injection)
8. [Deep Link System](#deep-link-system)
9. [Testing Strategy](#testing-strategy)
10. [Design Decisions](#design-decisions)

## Overview

CompareApp follows the **MVVM (Model-View-ViewModel)** architecture pattern with **Clean Architecture** principles. This design ensures:

- **Separation of Concerns**: Each layer has a specific responsibility
- **Testability**: Business logic can be tested independently
- **Maintainability**: Code is organized and easy to understand
- **Scalability**: Easy to add new features following established patterns

## Architecture Pattern

### MVVM with Clean Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                        │
│                                                                  │
│  ┌────────────────┐                                             │
│  │  MainActivity  │  - Android Framework Entry Point            │
│  │ @AndroidEntry  │  - Launches Deep Links                      │
│  │     Point      │  - Sets up Compose UI                       │
│  └───────┬────────┘                                             │
│          │                                                       │
│          ▼                                                       │
│  ┌────────────────────────────────────────────┐                 │
│  │         CompareScreen (Composable)         │                 │
│  │  - User Interface (Material3)              │                 │
│  │  - Input Fields (Pickup/Dropoff)           │                 │
│  │  - Compare Button                          │                 │
│  │  - Loading States                          │                 │
│  └────────────────┬───────────────────────────┘                 │
│                   │                                              │
│                   ▼                                              │
│  ┌────────────────────────────────────────────┐                 │
│  │         MainViewModel                      │                 │
│  │  - UI State Management (StateFlow)         │                 │
│  │  - Business Logic                          │                 │
│  │  - Deep Link Creation                      │                 │
│  │  - Coordinates Repositories                │                 │
│  └────────────────┬───────────────────────────┘                 │
└───────────────────┼─────────────────────────────────────────────┘
                    │
┌───────────────────┼─────────────────────────────────────────────┐
│                   │            DOMAIN LAYER                      │
│                   │                                              │
│          ┌────────┴────────┐                                    │
│          │                 │                                    │
│          ▼                 ▼                                    │
│  ┌──────────────┐   ┌──────────────┐                          │
│  │  Location    │   │     App      │                          │
│  │  Repository  │   │  Repository  │                          │
│  │ (Interface)  │   │ (Interface)  │                          │
│  └──────┬───────┘   └──────┬───────┘                          │
│         │                  │                                    │
│         ▼                  ▼                                    │
│  ┌──────────────┐   ┌──────────────┐                          │
│  │  Location    │   │     App      │                          │
│  │RepositoryImpl│   │RepositoryImpl│                          │
│  └──────┬───────┘   └──────┬───────┘                          │
└─────────┼──────────────────┼─────────────────────────────────┘
          │                  │
┌─────────┼──────────────────┼─────────────────────────────────┐
│         │      DATA LAYER  │                                  │
│         │                  │                                  │
│         ▼                  ▼                                  │
│  ┌──────────────┐   ┌──────────────┐                         │
│  │   Geocoder   │   │   Package    │                         │
│  │  (Android)   │   │   Manager    │                         │
│  │              │   │  (Android)   │                         │
│  └──────────────┘   └──────────────┘                         │
│                                                                │
│  ┌──────────────────────────────────┐                         │
│  │  FusedLocationProviderClient     │                         │
│  │     (Google Play Services)       │                         │
│  └──────────────────────────────────┘                         │
└──────────────────────────────────────────────────────────────┘
```

## Project Structure

```
org.neteinstein.compareapp/
├── CompareApplication.kt              # Application class with Hilt setup
├── MainActivity.kt                    # Android entry point
│
├── ui/                                # Presentation Layer
│   ├── screens/
│   │   ├── CompareScreen.kt          # Main UI composable
│   │   └── MainViewModel.kt          # ViewModel for business logic
│   └── theme/
│       └── Theme.kt                  # Material3 theme configuration
│
├── data/                             # Data Layer
│   └── repository/
│       ├── LocationRepository.kt     # Location interface
│       ├── LocationRepositoryImpl.kt # Location implementation
│       ├── AppRepository.kt          # App checking interface
│       └── AppRepositoryImpl.kt      # App checking implementation
│
├── di/                               # Dependency Injection
│   ├── AppModule.kt                 # Provides app-level dependencies
│   └── RepositoryModule.kt          # Binds repository interfaces
│
└── utils/                            # Utilities
    └── MathUtils.kt                 # Math helper functions
```

## Layer Breakdown

### Presentation Layer

**Responsibility**: Handle user interactions and display data

#### MainActivity.kt
- **Lines**: ~80 (down from 545 before refactoring)
- **Responsibilities**:
  - Set up Hilt with `@AndroidEntryPoint`
  - Initialize Compose UI
  - Launch deep link Intents with split screen flags
  - Handle lifecycle events

#### CompareScreen.kt
- **Responsibilities**:
  - Render Material3 UI components
  - Collect ViewModel state with `collectAsState()`
  - Handle user input events
  - Display loading states and error messages

#### MainViewModel.kt
- **Responsibilities**:
  - Manage UI state with `StateFlow`
  - Create deep links for Uber and Bolt
  - Coordinate repository operations
  - Check app installation status
  - Handle geocoding requests
  - Emit UI events

**State Management**:
```kotlin
data class CompareUiState(
    val pickup: String = "",
    val dropoff: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isUberInstalled: Boolean = false,
    val isBoltInstalled: Boolean = false
)
```

### Domain Layer

**Responsibility**: Business logic and data operations

#### LocationRepository Interface
```kotlin
interface LocationRepository {
    suspend fun geocodeAddress(address: String): Pair<Double, Double>?
    suspend fun reverseGeocode(lat: Double, lng: Double): String?
    suspend fun getCurrentLocation(): Location?
    fun hasLocationPermission(): Boolean
}
```

#### LocationRepositoryImpl
- **Dependencies**: `Geocoder`, `FusedLocationProviderClient`, `Context`
- **Operations**:
  - `geocodeAddress()`: Convert text address to coordinates
  - `reverseGeocode()`: Convert coordinates to address
  - `getCurrentLocation()`: Get device's current location
  - `hasLocationPermission()`: Check location permissions

#### AppRepository Interface
```kotlin
interface AppRepository {
    fun isAppInstalled(packageName: String): Boolean
}
```

#### AppRepositoryImpl
- **Dependencies**: `PackageManager`
- **Operations**:
  - `isAppInstalled()`: Check if an app package is installed

### Data Layer

**Responsibility**: Interact with Android system APIs

- **Geocoder**: Android system service for address geocoding
- **FusedLocationProviderClient**: Google Play Services for location
- **PackageManager**: Android system service for app queries

## Component Details

### 1. CompareApplication

```kotlin
@HiltAndroidApp
class CompareApplication : Application()
```

- Annotated with `@HiltAndroidApp` to enable Hilt
- Initializes the dependency injection graph
- Entry point for the entire application

### 2. Dependency Injection Modules

#### AppModule
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideGeocoder(@ApplicationContext context: Context): Geocoder
    
    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient
}
```

Provides:
- **Geocoder**: For address-to-coordinate conversion
- **FusedLocationProviderClient**: For location services

#### RepositoryModule
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImpl
    ): LocationRepository
    
    @Binds
    @Singleton
    abstract fun bindAppRepository(
        impl: AppRepositoryImpl
    ): AppRepository
}
```

Binds repository interfaces to their implementations as singletons.

### 3. ViewModel Lifecycle

```
┌─────────────────────────────────────────────────┐
│  Configuration Change (Rotation, Language, etc) │
└──────────────────┬──────────────────────────────┘
                   │
        ┌──────────▼──────────┐
        │   Activity Dies     │
        │   Activity Recreated│
        └──────────┬──────────┘
                   │
        ┌──────────▼──────────┐
        │  ViewModel SURVIVES │  ← State preserved
        │  No data loss!      │
        └──────────┬──────────┘
                   │
        ┌──────────▼──────────┐
        │  UI Resubscribes    │
        │  State restored     │
        └─────────────────────┘
```

## Data Flow

### 1. User Clicks "Compare" Button

```
┌─────────────┐
│   User      │
│   Click     │
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│  CompareScreen      │  onCompareClick()
│  (UI Layer)         │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  MainViewModel      │  onCompareButtonClicked()
│                     │
│  1. Validate input  │
│  2. Set loading     │
│  3. Check apps      │
│  4. Geocode         │
│  5. Create links    │
│  6. Emit event      │
└──────┬──────────────┘
       │
       ├─────────────────┐
       │                 │
       ▼                 ▼
┌──────────────┐   ┌──────────────┐
│   Location   │   │     App      │
│  Repository  │   │  Repository  │
│              │   │              │
│  geocode()   │   │ isInstalled()│
└──────┬───────┘   └──────┬───────┘
       │                  │
       │ Coordinates      │ Boolean
       │                  │
       ▼                  ▼
┌─────────────────────────────┐
│     MainViewModel           │
│  Creates deep link URIs     │
└──────┬──────────────────────┘
       │
       │ Event: LaunchDeepLinks
       │
       ▼
┌─────────────────────────┐
│  MainActivity           │
│  Launches Intents with  │
│  FLAG_LAUNCH_ADJACENT   │
└─────────────────────────┘
```

### 2. Geocoding Flow

```
Text Address → LocationRepository.geocodeAddress()
                         ↓
                   Geocoder API
                         ↓
              List<Address> or null
                         ↓
                Extract coordinates
                         ↓
              Pair<Double, Double>?
                         ↓
              Return to ViewModel
```

## Deep Link System

### Uber Deep Link Format

```
uber://?action=setPickup
  &pickup[formatted_address]=<URL_ENCODED_PICKUP>
  &dropoff[formatted_address]=<URL_ENCODED_DROPOFF>
```

**Example**:
```
uber://?action=setPickup
  &pickup[formatted_address]=Times%20Square%2C%20New%20York
  &dropoff[formatted_address]=Central%20Park%2C%20New%20York
```

### Bolt Deep Link Format

**Primary (Coordinate-based)**:
```
bolt://ride
  ?pickup_lat=<LAT>
  &pickup_lng=<LNG>
  &destination_lat=<LAT>
  &destination_lng=<LNG>
```

**Fallback (Text-based)**:
```
bolt://ride
  ?pickup=<URL_ENCODED_ADDRESS>
  &destination=<URL_ENCODED_ADDRESS>
```

**Example**:
```
bolt://ride
  ?pickup_lat=40.758896
  &pickup_lng=-73.985130
  &destination_lat=40.785091
  &destination_lng=-73.968285
```

### Split Screen Launch

```kotlin
// MainActivity.kt
private fun launchDeepLinks(uberUri: Uri, boltUri: Uri) {
    val uberIntent = Intent(Intent.ACTION_VIEW, uberUri).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
    }
    
    val boltIntent = Intent(Intent.ACTION_VIEW, boltUri).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
    }
    
    startActivity(uberIntent)
    
    // Delay ensures split screen mode
    Handler(Looper.getMainLooper()).postDelayed({
        startActivity(boltIntent)
    }, 500)
}
```

**Flags Explained**:
- `FLAG_ACTIVITY_NEW_TASK`: Launch in a new task
- `FLAG_ACTIVITY_LAUNCH_ADJACENT`: Request split screen mode (API 24+)

## Testing Strategy

### Unit Tests

```
MainViewModel Tests
├── Deep link creation
├── State management
├── Geocoding coordination
└── App installation checks

Repository Tests
├── LocationRepository
│   ├── Geocoding
│   ├── Reverse geocoding
│   └── Location fetching
└── AppRepository
    └── App installation detection

Utility Tests
└── MathUtils
    └── Decimal rounding
```

### Test Doubles

```kotlin
// Example: Mock Repository in ViewModel Tests
@Test
fun `onCompareButtonClicked creates correct Uber deep link`() {
    // Arrange
    val mockLocationRepo = mock<LocationRepository>()
    val mockAppRepo = mock<AppRepository>()
    val viewModel = MainViewModel(mockLocationRepo, mockAppRepo)
    
    // Act
    viewModel.updatePickup("Times Square")
    viewModel.updateDropoff("Central Park")
    viewModel.onCompareButtonClicked()
    
    // Assert
    val event = viewModel.events.value
    assertTrue(event is LaunchDeepLinks)
    assertTrue(event.uberUri.toString().contains("Times%20Square"))
}
```

### Integration Tests

- Deep link encoding/decoding
- Geocoding with real Geocoder (using Robolectric)
- UI state updates

### Instrumented Tests

- Compose UI interactions
- Permission handling
- Intent launching

## Design Decisions

### 1. Why MVVM?

**Pros**:
- Clear separation between UI and business logic
- ViewModels survive configuration changes
- Easy to test business logic independently
- Reactive state management with StateFlow

**Alternatives Considered**:
- MVP: Too much boilerplate, tight coupling
- MVI: Overkill for this simple app

### 2. Why Hilt?

**Pros**:
- Compile-time dependency injection
- Integrates well with Android architecture components
- Automatic lifecycle management
- Less boilerplate than Dagger 2

**Alternatives Considered**:
- Koin: Runtime DI, less type-safe
- Manual DI: Too much boilerplate

### 3. Why Repository Pattern?

**Pros**:
- Abstracts data sources
- Easy to mock in tests
- Centralizes data access logic
- Can swap implementations (e.g., for testing)

**Example**:
```kotlin
// Easy to mock for testing
val mockRepo = mock<LocationRepository> {
    onBlocking { geocodeAddress(any()) } doReturn Pair(40.0, -73.0)
}
```

### 4. Why Jetpack Compose?

**Pros**:
- Declarative UI paradigm
- Less boilerplate than XML
- Better state management
- Modern and recommended by Google

### 5. Why StateFlow over LiveData?

**Pros**:
- Better Kotlin coroutines integration
- More flexible operators
- Type-safe
- Supports backpressure

### 6. Split Screen Implementation

**Challenge**: Reliably open two apps side-by-side

**Solution**: 
- Use `FLAG_ACTIVITY_LAUNCH_ADJACENT`
- Add 500ms delay between launches
- This gives the system time to set up split screen

**Trade-offs**:
- Delay might feel slow (but necessary for reliability)
- Not all devices support split screen
- Behavior varies by manufacturer

### 7. Geocoding Strategy

**Primary**: Use coordinates for Bolt (more accurate)

**Fallback**: Use text addresses if geocoding fails

**Reasoning**:
- Coordinates are more precise
- Works offline if geocoding fails
- Better user experience when network available

## Performance Considerations

### 1. Lazy Initialization

```kotlin
@Provides
@Singleton
fun provideGeocoder(@ApplicationContext context: Context): Geocoder {
    return Geocoder(context, Locale.getDefault())
}
```

Geocoder is created once and reused.

### 2. Coroutine Scoping

```kotlin
viewModelScope.launch {
    // Automatically cancelled when ViewModel is cleared
    locationRepository.geocodeAddress(address)
}
```

Prevents memory leaks.

### 3. State Management

```kotlin
private val _uiState = MutableStateFlow(CompareUiState())
val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()
```

Immutable public API prevents external state corruption.

## Error Handling

### 1. Geocoding Failures

```kotlin
suspend fun geocodeAddress(address: String): Pair<Double, Double>? {
    return try {
        val addresses = geocoder.getFromLocationName(address, 1)
        if (!addresses.isNullOrEmpty()) {
            val location = addresses[0]
            Pair(location.latitude, location.longitude)
        } else null
    } catch (e: Exception) {
        Log.e(TAG, "Geocoding failed", e)
        null
    }
}
```

Returns `null` on failure, allowing graceful fallback.

### 2. App Not Installed

```kotlin
try {
    startActivity(intent)
} catch (e: ActivityNotFoundException) {
    Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show()
}
```

User-friendly error message.

### 3. Invalid Input

```kotlin
if (pickup.isEmpty() || dropoff.isEmpty()) {
    _uiState.update { 
        it.copy(errorMessage = "Please enter both locations")
    }
    return
}
```

Validate before processing.

## Migration History

### Before Refactoring
- Single `MainActivity.kt` with 545 lines
- All logic in one file
- Hard to test
- Tight coupling

### After Refactoring
- `MainActivity.kt`: ~80 lines (activity lifecycle only)
- `MainViewModel.kt`: Business logic
- `LocationRepository`: Data operations
- Clean separation, easy to test

## Future Architecture Enhancements

### Potential Improvements

1. **Use Cases Layer**: Add dedicated use case classes for complex operations
2. **Multi-module**: Split into feature modules for better build times
3. **DataStore**: Replace SharedPreferences with DataStore for preferences
4. **Room Database**: Cache geocoding results for offline use
5. **WorkManager**: Background sync for app installation status

### Scalability

The current architecture supports:
- Adding new ride-sharing services (just add deep link logic)
- Adding new features (follow existing patterns)
- UI changes (Compose is flexible)
- Different data sources (swap repository implementations)

## References

- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Hilt Documentation](https://dagger.dev/hilt/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [MVVM Pattern](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel)
