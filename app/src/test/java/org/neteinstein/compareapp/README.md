# Unit Tests for CompareApp

This directory contains comprehensive unit tests for the CompareApp Android application.

## Test Files

### 1. MainActivityTest.kt
Tests for the Uber deep link creation functionality:
- URL encoding of addresses
- Handling special characters
- Handling empty strings
- Correct deep link format validation

### 2. BoltDeepLinkTest.kt
Tests for the Bolt deep link creation with geocoding:
- Successful geocoding to coordinates
- Fallback to address-based format when geocoding fails
- Partial geocoding scenarios
- Coordinate validation in deep links

### 3. GeocodingTest.kt
Tests for the address geocoding functionality:
- Valid address geocoding
- Invalid address handling
- Multiple results handling (returns first)
- Empty string handling
- Special characters in addresses
- International address support

### 4. InputValidationTest.kt
Tests for input validation and edge cases:
- URL encoding with various characters
- Empty and blank string validation
- Coordinate range validation (latitude/longitude)
- Deep link format validation

### 5. CoordinateDecimalPrecisionTest.kt
Tests to ensure all coordinates have at most 6 decimal places:
- Validates coordinates in Bolt deep links are rounded to 6 decimal places
- Tests correct rounding behavior (HALF_UP mode)
- Tests extreme coordinates (near poles, equator, zero)
- Tests negative coordinates (Southern/Western hemispheres)
- Ensures coordinates with less than 6 decimals are preserved
- Verifies consistency across multiple calls
- Tests zero coordinates (Null Island)

### 6. RoundDecimalTest.kt
Tests for the MathUtils.roundDecimal() function:
- Default scale (6 decimal places)
- Custom scale values
- Negative numbers
- Zero values
- HALF_UP rounding mode behavior

### 7. LocationTest.kt
Tests for location-related functionality:
- Reverse geocoding with valid/invalid coordinates
- Multiple geocoding results handling
- Location permission checks

### 8. DeepLinkIntegrationTest.kt
Integration tests for deep link creation:
- Uber deep links with null coordinates
- Bolt deep links with coordinate rounding
- Mixed scenarios with partial coordinates

### 9. AppInstallationTest.kt
Tests for app installation detection:
- Checks if Uber/Bolt apps are installed
- Handles package manager exceptions

### 10. ThemeTest.kt
Tests for app theming and UI:
- Theme color validation
- Material3 design system compliance

### 11. DeepLinkEncodingTest.kt
Tests for deep link URL encoding:
- Special character encoding
- Parameter formatting

## Running the Tests

### Using Android Studio
1. Open the project in Android Studio
2. Right-click on the `test` folder
3. Select "Run 'Tests in 'org.neteinstein.compareapp''"

### Using Command Line
```bash
./gradlew test
```

### Running Specific Test Classes
```bash
# Run only MainActivityTest
./gradlew test --tests "org.neteinstein.compareapp.MainActivityTest"

# Run only geocoding tests
./gradlew test --tests "org.neteinstein.compareapp.GeocodingTest"
```

### Generating Test Reports
```bash
./gradlew test
# Reports will be generated in: app/build/reports/tests/testDebugUnitTest/index.html
```

## Test Dependencies

The following testing libraries are used:
- **JUnit 4.13.2** - Test framework
- **Mockito 5.14.2** - Mocking framework
- **Mockito Kotlin 5.4.0** - Kotlin extensions for Mockito
- **Robolectric 4.14.1** - Android framework mocking
- **Kotlinx Coroutines Test 1.10.2** - Testing coroutines
- **AndroidX Test Core 1.6.1** - Android testing utilities
- **AndroidX Test JUnit 1.2.1** - JUnit extensions for Android

## Test Coverage

The tests cover the following areas:
1. **Deep Link Creation** - Both Uber and Bolt deep link formats
2. **Geocoding** - Address to coordinate conversion
3. **Input Validation** - URL encoding and parameter validation
4. **Edge Cases** - Empty strings, special characters, invalid inputs
5. **Async Operations** - Coroutine-based geocoding tests
6. **Coordinate Precision** - Ensures all coordinates have at most 6 decimal places

## Code Changes for Testability

To enable unit testing, the following minimal changes were made to `MainActivity.kt`:
- Changed `createUberDeepLink()` from `private` to `internal`
- Changed `createBoltDeepLink()` from `private` to `internal`
- Changed `geocodeAddress()` from `private` to `internal`

These changes allow the test classes to access the methods while keeping them package-private (not exposed to external users).

## Best Practices

1. **Isolation** - Each test is independent and can run in any order
2. **Clear Naming** - Test method names clearly describe what they test
3. **Arrange-Act-Assert** - Tests follow AAA pattern for clarity
4. **Mock Dependencies** - Uses Robolectric to mock Android components
5. **Async Testing** - Properly tests coroutine-based code using `runTest`

## Continuous Integration

These tests are designed to run in CI/CD pipelines. They do not require:
- An Android device or emulator
- Network connectivity
- External services

All tests use mocked Android components (via Robolectric) and run on the JVM.
