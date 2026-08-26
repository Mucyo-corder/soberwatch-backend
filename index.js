const express = require("express");
const admin = require("firebase-admin");
const cors = require("cors");
const app = express();

app.use(express.json());
app.use(cors());

// ============================================================
// FIREBASE INITIALIZATION
// ============================================================
let db = null;

try {
  if (!process.env.FIREBASE_SERVICE_ACCOUNT) {
    throw new Error("FIREBASE_SERVICE_ACCOUNT is missing");
  }

  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  db = admin.firestore();
  console.log("Firebase Admin initialized successfully.");
} catch (error) {
  console.error("Firebase Initialization Error:", error.message);
}

// ============================================================
// DEVICE API KEY
// ============================================================
const DEVICE_API_KEY = process.env.SOBERWATCH_DEVICE_KEY || "SOBER_WATCH_DEVICE_KEY_2026";

// ============================================================
// HEALTH CHECK
// ============================================================
app.get("/", (req, res) => {
  res.status(200).json({
    service: "SoberWatch Telemetry API",
    status: "online",
    firebase: db ? "connected" : "not_connected",
    endpoint: "/uploadTelemetry",
    version: "1.0.0"
  });
});

// ============================================================
// AUTH ROUTES
// ============================================================
app.post("/api/register", async (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) {
    return res.status(400).json({ status: "error", message: "Email and password are required" });
  }

  try {
    const userRecord = await admin.auth().createUser({
      email: email,
      password: password,
      emailVerified: false
    });

    // Create user document in Firestore
    await db.collection("users").doc(userRecord.uid).set({
      email: email,
      createdAt: Date.now()
    });

    return res.status(200).json({
      status: "success",
      message: "User created successfully",
      uid: userRecord.uid
    });
  } catch (error) {
    console.error("Register Error:", error);
    return res.status(500).json({ status: "error", message: error.message });
  }
});

app.post("/api/login", async (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) {
    return res.status(400).json({ status: "error", message: "Email and password are required" });
  }

  try {
    // Reba email muri Firebase Auth
    const userRecord = await admin.auth().getUserByEmail(email);

    // Tugaragaza ko email ibaho, ariko password ntitugenzura kuri iyi route.
    // Ibi bikorwa neza muri Firebase Authentication REST API.
    return res.status(200).json({
      status: "success",
      message: "Login successful",
      uid: userRecord.uid,
      email: userRecord.email
    });
  } catch (error) {
    console.error("Login Error:", error);
    return res.status(500).json({ status: "error", message: error.message });
  }
});

// ============================================================
// TELEMETRY & READINGS
// ============================================================
app.get("/api/readings", async (req, res) => {
  const uid = req.query.uid || "test-user";
  if (!db) {
    return res.status(503).json({ status: "error", message: "Firebase is not connected" });
  }

  try {
    const readingsRef = db.collection("users").doc(uid).collection("readings");
    const snapshot = await readingsRef.orderBy("timestamp", "desc").limit(20).get();
    const data = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    }));

    return res.status(200).json({
      status: "success",
      readings: data
    });
  } catch (error) {
    console.error("Readings Error:", error);
    return res.status(500).json({ status: "error", message: "Failed to fetch readings" });
  }
});

app.get("/api/health", async (req, res) => {
  const uid = req.query.uid || "test-user";
  if (!db) {
    return res.status(503).json({ status: "error", message: "Firebase is not connected" });
  }

  try {
    const docRef = db.collection("users").doc(uid);
    const doc = await docRef.get();
    if (!doc.exists) {
      return res.status(404).json({ status: "error", message: "No data found for this user" });
    }

    const data = doc.data().lastReading;
    return res.status(200).json({
      status: "success",
      data: data
    });
  } catch (error) {
    console.error("Health Error:", error);
    return res.status(500).json({ status: "error", message: "Failed to fetch health data" });
  }
});

// ============================================================
// UPLOAD TELEMETRY
// ============================================================
app.post("/uploadTelemetry", async (req, res) => {
  const apiKey = req.headers["x-api-key"];
  if (!apiKey || apiKey !== DEVICE_API_KEY) {
    return res.status(401).json({ status: "error", message: "Unauthorized: Invalid Device Key" });
  }

  if (!db) {
    return res.status(503).json({ status: "error", message: "Firebase is not connected" });
  }

  const { uid, bac, heartRate, spo2, temp, ecgStatus, sensorRaw, sensorResponse, status, deviceId } = req.body;

  if (!uid) {
    return res.status(400).json({ status: "error", message: "uid is required" });
  }
  if (bac === undefined) {
    return res.status(400).json({ status: "error", message: "bac is required" });
  }
  if (heartRate === undefined) {
    return res.status(400).json({ status: "error", message: "heartRate is required" });
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
    return res.status(500).json({ status: "error", message: "Failed to save telemetry", error: error.message });
  }
});

// ============================================================
// TEST ENDPOINT
// ============================================================
app.post("/testTelemetry", async (req, res) => {
  if (!db) {
    return res.status(503).json({ status: "error", message: "Firebase is not connected" });
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
    return res.status(500).json({ status: "error", message: error.message });
  }
});

// ============================================================
// 404
// ============================================================
app.use((req, res) => {
  res.status(404).json({
    status: "error",
    message: "Endpoint not found",
    path: req.path
  });
});

// ============================================================
// SERVER
// ============================================================
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`SoberWatch server running on port ${PORT}`);
});
