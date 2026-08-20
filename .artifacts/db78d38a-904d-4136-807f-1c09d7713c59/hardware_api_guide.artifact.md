# Hardware Integration Guide (ESP32-S3)

Use this guide to connect your SoberWatch hardware to the Firebase API I just created.

## 1. Deploy the Backend
You need to deploy the code in `firebase-backend/` to your Firebase project:
1. Install Firebase CLI: `npm install -g firebase-tools`
2. Login: `firebase login`
3. Initialize in the folder: `firebase init functions`
4. Deploy: `firebase deploy --only functions`

## 2. ESP32-S3 Arduino Code Snippet
Add this to your Arduino sketch to send data to the API.

```cpp
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";

// Replace with your URL after deployment
const char* apiUrl = "https://YOUR_REGION-YOUR_PROJECT.cloudfunctions.net/uploadTelemetry";
const char* userUid = "PASTE_YOUR_UID_FROM_APP_PROFILE"; // Get this from the app's profile screen

void sendTelemetry(float bac, int hr, int spo2) {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    http.begin(apiUrl);
    http.addHeader("Content-Type", "application/json");
    http.addHeader("x-api-key", "SOBER_WATCH_DEVICE_KEY_2026");

    StaticJsonDocument<200> doc;
    doc["uid"] = userUid;
    doc["bac"] = bac;
    doc["heartRate"] = hr;
    doc["spo2"] = spo2;
    doc["temp"] = 36.6;

    String requestBody;
    serializeJson(doc, requestBody);

    int httpResponseCode = http.POST(requestBody);
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    http.end();
  }
}
```

## 3. How it Works
1. **The Device** (ESP32) detects high alcohol or heart rate.
2. It calls the **Cloud Function** via HTTP POST.
3. The Cloud Function writes to **Firestore**.
4. The **Android App** (already configured) gets a real-time update and shows the "Critical Analysis Form" or "Trend Charts" immediately.
