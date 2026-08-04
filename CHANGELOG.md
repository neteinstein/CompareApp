# Changelog

All notable changes to CompareApp will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Support for receiving a location deep link (e.g. sharing a place from Google Maps via
  "Open with" -> Compare App, using standard `geo:` URIs, or the app's own
  `compareapp://dropoff?lat=..&lng=..&address=..` scheme)
  - The shared location prepopulates the dropoff field
  - The device's current location is automatically used as the pickup

### Changed
- Redesigned the app launcher icon with a bold diagonal split background and a car +
  lightning-bolt mark, giving the app its own distinct visual identity inspired by (but
  not copying) the look and feel of ride-hailing apps
- Redesigned the main screen with a custom Material3 color scheme, a card-based layout,
  and icons for a more polished look

### Fixed
- Fixed Bolt deep link not working when coordinates have more than 6 decimal places
  - Changed coordinate formatting to use `String.format(Locale.US, "%.6f", ...)` 
  - Ensures exactly 6 decimal places in all coordinates
  - Uses locale-independent formatting (period as decimal separator)
  - Matches documented Bolt deep link format from API documentation

### Changed
- Release notes are now automatically generated from commit history

## [1.0.0] - 2024-01-15

### Added
- Initial release of CompareApp
- Side-by-side comparison of Uber and Bolt ride-sharing services
- Modern Jetpack Compose UI with Material3 design
- Location input with geocoding support
- Deep linking to Uber and Bolt apps
- Split screen functionality for simultaneous comparison
- Support for Android 7.0 (API 24) and higher

[Unreleased]: https://github.com/neteinstein/CompareUberVsBoltPriceApp/compare/v1.0.0.26...HEAD
[1.0.0]: https://github.com/neteinstein/CompareUberVsBoltPriceApp/releases/tag/v1.0.0.26
