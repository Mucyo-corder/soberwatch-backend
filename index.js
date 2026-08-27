const express = require("express");
const admin = require("firebase-admin");
const cors = require("cors");

const app = express();

app.use(cors());
app.use(express.json({ limit: "1mb" }));

// ============================================================
// FIREBASE
// ============================================================

let db = null;

try {
  if (!process.env.FIREBASE_SERVICE_ACCOUNT) {
    throw new Error("FIREBASE_SERVICE_ACCOUNT is missing");
  }

  const serviceAccount = JSON.parse(
    process.env.FIREBASE_SERVICE_ACCOUNT
  );

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });

  db = admin.firestore();

  console.log("Firebase Admin initialized successfully.");
} catch (error) {
  console.error("Firebase Initialization Error:", error.message);
}

// ============================================================
// DEVICE AUTHENTICATION
// ============================================================

const DEVICE_API_KEY =
  process.env.SOBERWATCH_DEVICE_KEY ||
  "SOBER_WATCH_DEVICE_KEY_2026";

function validateDeviceKey(req) {
  const apiKey = req.headers["x-api-key"];

  if (!apiKey) {
    return false;
  }

  return apiKey === DEVICE_API_KEY;
}

// ============================================================
// HELPER FUNCTIONS
// ============================================================

function calculateStatus(bac) {
  const value = Number(bac) || 0;

  if (value >= 0.08) {
    return "DANGER";
  }

  if (value >= 0.02) {
    return "CAUTION";
  }

  return "SAFE";
}

function normalizeTelemetry(body) {
  // Frontend names
  const alcoholBac =
    body.alcoholBac !== undefined
      ? body.alcoholBac
      : body.bac;

  const heartRateBpm =
    body.heartRateBpm !== undefined
      ? body.heartRateBpm
      : body.heartRate;

  const spo2Percent =
    body.spo2Percent !== undefined
      ? body.spo2Percent
      : body.spo2;

  const tempCelsius =
    body.tempCelsius !== undefined
      ? body.tempCelsius
      : body.temp;

  const bacValue = Number(alcoholBac) || 0;

  return {
    alcoholBac: bacValue,

    heartRateBpm:
      heartRateBpm !== undefined
        ? Number(heartRateBpm) || 0
        : 0,

    spo2Percent:
      spo2Percent !== undefined
        ? Number(spo2Percent) || 0
        : 0,

    tempCelsius:
      tempCelsius !== undefined
        ? Number(tempCelsius) || 0
        : 0,

    ecgStatus:
      body.ecgStatus || "Not connected",

    sensorRaw:
      body.sensorRaw !== undefined
        ? Number(body.sensorRaw) || 0
        : 0,

    sensorResponse:
      body.sensorResponse !== undefined
        ? Number(body.sensorResponse) || 0
        : 0,

    status:
      body.status || calculateStatus(bacValue),

    deviceId:
      body.deviceId || "SW-001",

    timestamp:
      body.timestamp !== undefined
        ? Number(body.timestamp) || Date.now()
        : Date.now(),

    source:
      body.source || "hardware",
  };
}

// ============================================================
// ROOT / SERVER HEALTH
// ============================================================

app.get("/", (req, res) => {
  res.status(200).json({
    service: "SoberWatch Telemetry API",
    status: "online",
    firebase: db ? "connected" : "not_connected",
    endpoint: "/uploadTelemetry",
    version: "2.0.0",
    telemetry: "enabled",
    deviceAuthentication: "enabled",
  });
});

// ============================================================
// REGISTER
// ============================================================

app.post("/api/register", async (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({
      status: "error",
      message: "Email and password are required",
    });
  }

  if (!db) {
    return res.status(503).json({
      status: "error",
      message: "Firebase is not connected",
    });
  }

  try {
    const existingUser = await admin
      .auth()
      .getUserByEmail(email)
      .catch(() => null);

    if (existingUser) {
      return res.status(409).json({
        status: "error",
        message: "User already exists",
        uid: existingUser.uid,
      });
    }

    const userRecord = await admin.auth().createUser({
      email,
      password,
      emailVerified: false,
    });

    await db.collection("users").doc(userRecord.uid).set({
      email,
      createdAt: Date.now(),
    });

    return res.status(200).json({
      status: "success",
      message: "User created successfully",
      uid: userRecord.uid,
      email: userRecord.email,
    });
  } catch (error) {
    console.error("Register Error:", error);

    return res.status(500).json({
      status: "error",
      message: error.message,
    });
  }
});

// ============================================================
// LOGIN
// ============================================================

app.post("/api/login", async (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({
      status: "error",
      message: "Email and password are required",
    });
  }

  try {
    /*
     * NOTE:
     * Firebase Admin SDK does not verify a password directly.
     * This endpoint confirms that the Firebase user exists.
     *
     * For production authentication, use Firebase Client SDK
     * or Firebase Identity Toolkit.
     */

    const userRecord = await admin
      .auth()
      .getUserByEmail(email);

    return res.status(200).json({
      status: "success",
      message: "Login successful",
      uid: userRecord.uid,
      email: userRecord.email,
    });
  } catch (error) {
    console.error("Login Error:", error);

    return res.status(401).json({
      status: "error",
      message: "Invalid email or user does not exist",
    });
  }
});

// ============================================================
// GET ALL READINGS
// ============================================================

app.get("/api/readings", async (req, res) => {
  const uid = req.query.uid;

  if (!uid) {
    return res.status(400).json({
      status: "error",
      message: "uid is required",
    });
  }

  if (!db) {
    return res.status(503).json({
      status: "error",
      message: "Firebase is not connected",
    });
  }

  try {
    const readingsRef = db
      .collection("users")
      .doc(uid)
      .collection("readings");

    const snapshot = await readingsRef
      .orderBy("timestamp", "desc")
      .limit(100)
      .get();

    const readings = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...doc.data(),
    }));

    return res.status(200).json({
      status: "success",
      count: readings.length,
      readings,
    });
  } catch (error) {
    console.error("Readings Error:", error);

    return res.status(500).json({
      status: "error",
      message: "Failed to fetch readings",
      error: error.message,
    });
  }
});

// ============================================================
// GET LATEST HEALTH
// ============================================================

app.get("/api/health", async (req, res) => {
  const uid = req.query.uid;

  if (!uid) {
    return res.status(400).json({
      status: "error",
      message: "uid is required",
    });
  }

  if (!db) {
    return res.status(503).json({
      status: "error",
      message: "Firebase is not connected",
    });
  }

  try {
    const userRef = db
      .collection("users")
      .doc(uid);

    const userDoc = await userRef.get();

    if (!userDoc.exists) {
      return res.status(404).json({
        status: "error",
        message: "No data found for this user",
      });
    }

    const userData = userDoc.data();

    if (!userData.lastReading) {
      return res.status(404).json({
        status: "error",
        message: "No health reading available",
      });
    }

    return res.status(200).json({
      status: "success",
      data: userData.lastReading,
    });
  } catch (error) {
    console.error("Health Error:", error);

    return res.status(500).json({
      status: "error",
      message: "Failed to fetch health data",
      error: error.message,
    });
  }
});

// ============================================================
// UPLOAD TELEMETRY FROM ESP32 / DEVICE
// ============================================================

app.post("/uploadTelemetry", async (req, res) => {
  console.log("--------------------------------------------------");
  console.log("Telemetry request received");
  console.log("Device ID:", req.body.deviceId);
  console.log("UID:", req.body.uid);

  // ----------------------------------------------------------
  // DEVICE KEY
  // ----------------------------------------------------------

  if (!validateDeviceKey(req)) {
    console.log("Invalid Device Key");

    return res.status(401).json({
      status: "error",
      message: "Unauthorized: Invalid Device Key",
    });
  }

  console.log("Device Key: VALID");

  // ----------------------------------------------------------
  // FIREBASE
  // ----------------------------------------------------------

  if (!db) {
    return res.status(503).json({
      status: "error",
      message: "Firebase is not connected",
    });
  }

  // ----------------------------------------------------------
  // UID
  // ----------------------------------------------------------

  const uid = req.body.uid;

  if (!uid) {
    return res.status(400).json({
      status: "error",
      message: "uid is required",
    });
  }

  // ----------------------------------------------------------
  // TELEMETRY
  // ----------------------------------------------------------

  const telemetryData = normalizeTelemetry(req.body);

  console.log("Telemetry:");
  console.log(telemetryData);

  // ----------------------------------------------------------
  // SAVE TO FIRESTORE
  // ----------------------------------------------------------

  try {
    const userRef = db
      .collection("users")
      .doc(uid);

    const readingRef = await userRef
      .collection("readings")
      .add(telemetryData);

    await userRef.set(
      {
        lastReading: telemetryData,
        lastReadingId: readingRef.id,
        updatedAt: Date.now(),
      },
      {
        merge: true,
      }
    );

    console.log(
      "Telemetry saved successfully:",
      readingRef.id
    );

    return res.status(200).json({
      status: "success",
      message: "Telemetry uploaded successfully",
      readingId: readingRef.id,
      data: telemetryData,
    });
  } catch (error) {
    console.error("Firestore Write Error:", error);

    return res.status(500).json({
      status: "error",
      message: "Failed to save telemetry",
      error: error.message,
    });
  }
});

// ============================================================
// TEST TELEMETRY
// ============================================================

app.post("/testTelemetry", async (req, res) => {
  if (!db) {
    return res.status(503).json({
      status: "error",
      message: "Firebase is not connected",
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
    sensorResponse: 20,
    status: "CAUTION",
    deviceId: "SOBERWATCH-TEST",
    timestamp: Date.now(),
    source: "test",
  };

  try {
    const userRef = db
      .collection("users")
      .doc(uid);

    const readingRef = await userRef
      .collection("readings")
      .add(testData);

    await userRef.set(
      {
        lastReading: testData,
        lastReadingId: readingRef.id,
        updatedAt: Date.now(),
      },
      {
        merge: true,
      }
    );

    return res.status(200).json({
      status: "success",
      message: "Test telemetry saved",
      readingId: readingRef.id,
      data: testData,
    });
  } catch (error) {
    console.error("Test telemetry error:", error);

    return res.status(500).json({
      status: "error",
      message: error.message,
    });
  }
});

// ============================================================
// EMERGENCY EVENT
// ============================================================

app.post("/api/emergency", async (req, res) => {
  if (!db) {
    return res.status(503).json({
      status: "error",
      message: "Firebase is not connected",
    });
  }

  const {
    uid,
    eventId,
    type,
    severity,
    latitude,
    longitude,
    accuracy,
    timestamp,
    contact,
    notes,
    mapsUrl,
    recognizedText,
  } = req.body;

  if (!uid) {
    return res.status(400).json({
      status: "error",
      message: "uid is required",
    });
  }

  try {
    const emergencyData = {
      eventId: eventId || `emg-${Date.now()}`,
      type: type || "SOS",
      severity: severity || "high",
      latitude: Number(latitude) || 0,
      longitude: Number(longitude) || 0,
      accuracy: Number(accuracy) || 0,
      timestamp: timestamp || new Date().toISOString(),
      contact: contact || "",
      notes: notes || "",
      mapsUrl: mapsUrl || "",
      recognizedText: recognizedText || "",
      createdAt: Date.now(),
    };

    const emergencyRef = await db
      .collection("users")
      .doc(uid)
      .collection("emergencies")
      .add(emergencyData);

    return res.status(200).json({
      status: "success",
      message: "Emergency event recorded",
      emergencyId: emergencyRef.id,
      data: emergencyData,
    });
  } catch (error) {
    console.error("Emergency Error:", error);

    return res.status(500).json({
      status: "error",
      message: "Failed to save emergency event",
      error: error.message,
    });
  }
});

// ============================================================
// GET EMERGENCY HISTORY
// ============================================================

app.get("/api/emergencies", async (req, res) => {
  const uid = req.query.uid;

  if (!uid) {
    return res.status(400).json({
      status: "error",
      message: "uid is required",
    });
  }

  if (!db) {
    return res.status(503).json({
      status: "error",
      message: "Firebase is not connected",
    });
  }

  try {
    const snapshot = await db
      .collection("users")
      .doc(uid)
      .collection("emergencies")
      .orderBy("createdAt", "desc")
      .limit(50)
      .get();

    const emergencies = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...doc.data(),
    }));

    return res.status(200).json({
      status: "success",
      emergencies,
    });
  } catch (error) {
    console.error("Emergency history error:", error);

    return res.status(500).json({
      status: "error",
      message: "Failed to fetch emergency history",
      error: error.message,
    });
  }
});

// ============================================================
// 404
// ============================================================

app.use((req, res) => {
  res.status(404).json({
    status: "error",
    message: "Endpoint not found",
    path: req.path,
  });
});

// ============================================================
// SERVER
// ============================================================

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(
    `SoberWatch server running on port ${PORT}`
  );
});
