# Android Kotlin Compass

A simple Android compass app built with Kotlin that points toward predefined geographic coordinates.

### Features
- Calculates the direction to a target location.
- Uses device orientation sensors and GPS location updates.
- Smoothly rotates the compass arrow.

### Technologies
- Kotlin
- Android SDK
- SensorManager
- Fused Location Provider
- Kotlin Coroutines & StateFlow

### How it works
`NavigationBoundService` processes sensor and location data and calculates the target angle. `MainActivity` observes angle updates and animates the compass arrow.

**Note:** Target coordinates are currently predefined in the source code.
