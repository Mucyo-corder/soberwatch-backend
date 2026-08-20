# Implementation Plan - Set up Main Navigation

The app currently displays a "Hello World" placeholder in `MainActivity.kt`. This plan will wire up the existing screens (Home, Live Monitoring, AI Insights, Device Connection, Profile, etc.) using Jetpack Navigation Compose.

## Proposed Changes

### Navigation Setup

#### [NEW] [SoberWatchApp.kt](file:///Users/user/Downloads/soberwatch-health/app/src/main/java/com/example/ui/SoberWatchApp.kt)
Create a main entry point for the UI that manages the `NavHost` and bottom navigation (if applicable, though based on `HomeScreen` it seems to use a more custom bento-grid and top-bar based navigation). We will implement a `NavHost` to handle transitions between:
- `Splash`
- `Login` / `Register`
- `Home`
- `LiveMonitor`
- `DeviceConnection`
- `AiInsights`
- `Profile`
- `Alerts`
- `History`
- `EmergencyContacts`
- `Reports`
- `Settings`

#### [MODIFY] [MainActivity.kt](file:///Users/user/Downloads/soberwatch-health/app/src/main/java/com/example/MainActivity.kt)
Replace the `Greeting` placeholder with `SoberWatchApp`, passing the `SoberWatchViewModel`.

### Navigation Routes
We will define a `Screen` sealed class or similar to manage routes.

## Verification Plan

### Manual Verification
- Deploy the app to the device/emulator.
- Verify the Splash screen appears.
- Navigate to the Home screen.
- Test navigation to all linked screens (Device, Insights, Alerts, etc.) from the Home screen cards and buttons.
