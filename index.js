const express = require('express');
const admin = require('firebase-admin');
const app = express();
app.use(express.json());

/**
 * FIREBASE INITIALIZATION
 * On Render, we store the service account JSON in an Environment Variable
 * named FIREBASE_SERVICE_ACCOUNT for security.
 */
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

/**
 * TELEMETRY API ENDPOINT
 * POST https://your-app.onrender.com/uploadTelemetry
 */
app.post('/uploadTelemetry', async (req, res) => {
  // 1. Security Check
  const apiKey = req.headers["x-api-key"];
  if (!apiKey || apiKey !== "SOBER_WATCH_DEVICE_KEY_2026") {
    return res.status(401).send("Unauthorized: Invalid Device Key");
  }

  // 2. Validate Data
  const { uid, bac, heartRate, spo2, temp, ecgStatus } = req.body;
  if (!uid || bac === undefined || !heartRate) {
    return res.status(400).send("Missing required telemetry fields (uid, bac, or heartRate)");
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
      source: "hardware"
    };

    // 3. Save to user's database collection
    await db.collection("users").document(uid).collection("readings").add(telemetryData);

    // 4. Update the latest status in the user's profile
    await db.collection("users").document(uid).update({
      lastReading: telemetryData
    });

    return res.status(200).json({
        status: "success",
        received: timestamp
    });
  } catch (error) {
    console.error("Firestore Write Error:", error);
    return res.status(500).send("Internal Server Error: " + error.message);
  }
});

// Health check endpoint
app.get('/', (req, res) => {
  res.send('SoberWatch Telemetry API is live and waiting for hardware data!');
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Server is running on port ${PORT}`));
