# Dashboard Enhancement Implementation Plan

This plan aims to centralize clinical detections, AI insights, and trend analysis on the main Dashboard (HomeScreen), reducing the reliance on the chatbot for primary information delivery.

## User Review Required

> [!IMPORTANT]
> The "Detection Report" will be presented as a structured, form-like clinical summary on the dashboard. This will replace or supplement the current biometric grid to provide a more "official" look to the detections.

## Proposed Changes

### UI Components

#### [NEW] [TrendChart.kt](file:///Users/user/Downloads/soberwatch-health/app/src/main/java/com/example/ui/components/TrendChart.kt)
* Extract `TrendChartCard` from `LiveMonitoringScreen.kt` to this new file to enable sharing across multiple screens.

#### [MODIFY] [LiveMonitoringScreen.kt](file:///Users/user/Downloads/soberwatch-health/app/src/main/java/com/example/ui/screens/monitor/LiveMonitoringScreen.kt)
* Remove `TrendChartCard` definition and use the one from `com.example.ui.components`.

### Dashboard Enhancement

#### [MODIFY] [HomeScreen.kt](file:///Users/user/Downloads/soberwatch-health/app/src/main/java/com/example/ui/screens/home/HomeScreen.kt)
* **Signature Update**: Add `recentReadings: List<SensorReading>` to the parameters.
* **Clinical Detection Form**: Add a new section `DetectionReportForm` that displays biometric data in a structured, tabular, or form-like layout.
* **AI Insights Expansion**: Update the `CLINICAL ADVISORY` section to show all available insights (using a scrollable row or a list) instead of just the first one.
* **Trend Analysis Section**: Add a new section `TrendAnalysis` that uses `TrendChartCard` to show BAC and Heart Rate history directly on the dashboard.

### Navigation & Integration

#### [MODIFY] [SoberWatchApp.kt](file:///Users/user/Downloads/soberwatch-health/app/src/main/java/com/example/ui/SoberWatchApp.kt)
* Fix the `HomeScreen` call to correctly pass `insights`, `alertsHistory`, and the new `recentReadings` parameter.

## Verification Plan

### Automated Tests
* Run `gradle_build` to ensure the new shared component and signature changes compile correctly.

### Manual Verification
* Deploy to the device and verify:
    * The Dashboard shows a structured "Detection Report".
    * Multiple AI insights are visible or accessible.
    * Real-time/historical charts for BAC and Heart Rate appear on the Dashboard.
