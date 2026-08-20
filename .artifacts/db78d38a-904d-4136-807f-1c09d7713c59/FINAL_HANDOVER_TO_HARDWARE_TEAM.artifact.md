# SoberWatch Hardware-to-Cloud Integration Package

This document provides the necessary details for the ESP32-S3 hardware to upload biometric telemetry to the SoberWatch Health platform.

## 1. API Connection Details
*   **Protocol**: HTTPS (POST Request)
*   **Endpoint**: `https://<YOUR_DEPLOYED_URL>/uploadTelemetry`
*   **Authentication Header**:
    *   Key: `x-api-key`
    *   Value: `SOBER_WATCH_DEVICE_KEY_2026`

## 2. JSON Data Structure (Payload)
The device must send a JSON body with the following schema:

```json
{
  "uid": "USER_ID_FROM_APP",
  "bac": 0.02,
  "heartRate": 72,
  "spo2": 98,
  "temp": 36.7,
  "ecgStatus": "Stable"
}
```

## 3. Field Definitions
- **uid**: The unique ID of the user (obtainable from the mobile app profile).
- **bac**: Current Blood Alcohol Concentration as a float.
- **heartRate**: Current BPM as an integer.
- **spo2**: Oxygen saturation percentage (optional).
- **temp**: Body temperature in Celsius (optional).

## 4. Backend Source Code
The backend logic is written in Node.js. If you need to deploy or modify the API, the source code is located in the project at:
`soberwatch-health/firebase-backend/functions/index.js`

---
**Developer Note**: Please ensure the ESP32-S3 is configured with a valid Wi-Fi connection before attempting to call this API.
