const express = require('express');
const admin = require('firebase-admin');
const cors = require('cors'); // <-- Iki ni cyo cyongeweho kugira ngo App ibashe kuvugana na server!
const app = express();

app.use(express.json());
app.use(cors());

try {
  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  console.log("Firebase Admin initialized successfully.");
} catch (error) {
  console.error("Firebase Initialization Error:", error.message);
}

const db = admin.firestore();

// API KEY
const DEVICE_API_KEY = "SOBER_WATCH_DEVICE_KEY_2026";

app.get('/', (req, res) => {
  res.status(200).json({
    service: "SoberWatch Telemetry API",
    status: "online",
    firebase: db ? "connected" : "not_connected",
    endpoint: "/uploadTelemetry",
    version: "1.0.0"
  });
});

app.post('/uploadTelemetry', async (req, res) => {
  const apiKey = req.headers["x-api-key"];
  if (!apiKey || apiKey !== DEVICE_API_KEY) {
    return res.status(401).json({
      status: "error",
      message: "Unauthorized: Invalid Device Key"
    });
  }

  if (!db) {
    return res.status(503).json({
      status: "error",
      message: "Firebase is not connected"
    });
  }

  const { uid, bac, heartRate, spo2, temp, ecgStatus, sensorRaw, sensorResponse, status, deviceId } = req.body;

  if (!uid) {
    return res.status(400).json({
      status: "error",
      message: "uid is required"
    });
  }

  if (bac === undefined) {
    return res.status(400).json({
      status: "error",
      message: "bac is required"
    });
  }

  if (heartRate === undefined) {
    return res.status(400).json({
      status: "error",
      message: "heartRate is required"
    });
  }

  const telemetryData = {
    alcoholBac: Number(bac) || 0,
    heartRateBpm: Number(heartRate) || 0,
    spo2Percent: Number(spo2) || 0,
    tempCelsius: Number(temp) || 0,
    ecgStatus: ecgStatus || "Stable",
    sensorRaw: Number(sensorRaw) || 0,
    sensorResponse: Number(sensorResponse) || 0,
    status: status || "SAFE",
    deviceId: deviceId || "SOBERWATCH-V1",
    timestamp: Date.now(),
    source: "hardware"
  };

  try {
    // YAHINDUWE: .document() -> .doc()
    const userRef = db.collection("users").doc(uid);
    const readingRef = await userRef.collection("readings").add(telemetryData);
    await userRef.set({
      lastReading: telemetryData,
      lastReadingId: readingRef.id,
      updatedAt: Date.now()
    }, { merge: true });

    return res.status(200).json({
      status: "success",
      message: "Telemetry uploaded successfully",
      readingId: readingRef.id,
      received: telemetryData.timestamp
    });
  } catch (error) {
    console.error("Firestore Write Error:", error);
    return res.status(500).json({
      status: "error",
      message: "Failed to save telemetry",
      error: error.message
    });
  }
});

app.post('/testTelemetry', async (req, res) => {
  if (!db) {
    return res.status(503).json({
      status: "error",
      message: "Firebase is not connected"
    });
  }

  const uid = req.body.uid || "test-user";
  const testData = {
    alcoholBac: 0.04,
    heartRateBpm: 78,
    spo2Percent: 98,
    tempCelsius: 36.7,
    ecgStatus: "Stable",
    sensorRaw: 1200,
    sensorResponse: 20.0,
    status: "CAUTION",
    deviceId: "SOBERWATCH-TEST",
    timestamp: Date.now(),
    source: "test"
  };

  try {
    // YAHINDUWE: .document() -> .doc()
    const userRef = db.collection("users").doc(uid);
    const readingRef = await userRef.collection("readings").add(testData);
    await userRef.set({
      lastReading: testData,
      lastReadingId: readingRef.id,
      updatedAt: Date.now()
    }, { merge: true });

    return res.status(200).json({
      status: "success",
      message: "Test telemetry saved",
      readingId: readingRef.id,
      data: testData
    });
  } catch (error) {
    console.error("Test telemetry error:", error);
    return res.status(500).json({
      status: "error",
      message: error.message
    });
  }
});

app.use((req, res) => {
  res.status(404).json({
    status: "error",
    message: "Endpoint not found",
    path: req.path
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`SoberWatch server running on port ${PORT}`);
});
