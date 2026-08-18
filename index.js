const express = require('express');
const admin = require('firebase-admin');
const app = express();
app.use(express.json());

/**
 * Robust Firebase Initialization
 */
let db;
try {
  const serviceAccountValue = process.env.FIREBASE_SERVICE_ACCOUNT;
  
  if (!serviceAccountValue) {
    throw new Error("FIREBASE_SERVICE_ACCOUNT environment variable is missing!");
  }

  // Parse the JSON string from environment variable
  const serviceAccount = JSON.parse(serviceAccountValue);
  
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  
  db = admin.firestore();
  console.log("✅ Firebase Admin initialized successfully.");
} catch (error) {
  console.error("❌ Firebase Initialization Error:", error.message);
  // Important: On Render, we should let the app start but log the error 
  // so you can fix the environment variable without the loop crashing.
}

/**
 * TELEMETRY API ENDPOINT
 */
app.post('/uploadTelemetry', async (req, res) => {
  if (!db) {
    return res.status(500).send("Database not initialized. Check server logs.");
  }

  const apiKey = req.headers["x-api-key"];
  if (!apiKey || apiKey !== "SOBER_WATCH_DEVICE_KEY_2026") {
    return res.status(401).send("Unauthorized");
  }

  const { uid, bac, heartRate, spo2, temp, ecgStatus } = req.body;
  if (!uid || bac === undefined || !heartRate) {
    return res.status(400).send("Missing required fields");
  }

  try {
    const timestamp = Date.now();
    const telemetryData = {
      alcoholBac: parseFloat(bac),
      heartRateBpm: parseInt(heartRate),
      spo2Percent: parseInt(spo2) || 0,
      tempCelsius: parseFloat(temp) || 0,
      ecgStatus: ecgStatus || "Stable",
      timestamp: timestamp,
    };

    await db.collection("users").document(uid).collection("readings").add(telemetryData);
    await db.collection("users").document(uid).update({ lastReading: telemetryData });

    return res.status(200).json({ status: "success", received: timestamp });
  } catch (error) {
    console.error("Firestore Write Error:", error);
    return res.status(500).send(error.message);
  }
});

app.get('/', (req, res) => {
  res.send('SoberWatch API Status: ' + (db ? 'Connected' : 'Firebase Error (Check Logs)'));
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
