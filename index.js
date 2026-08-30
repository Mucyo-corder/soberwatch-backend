
const express = require("express");
const admin = require("firebase-admin");
const cors = require("cors");

const app = express();

// ============================================================
// CONFIGURATION
// ============================================================

app.use(
  cors({
    origin: "*",
    methods: ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
    allowedHeaders: [
      "Content-Type",
      "Authorization",
      "X-API-Key",
    ],
  })
);

app.use(express.json({ limit: "2mb" }));

// ============================================================
// FIREBASE ADMIN
// ============================================================

let db = null;
let firebaseReady = false;

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
  firebaseReady = true;

  console.log("Firebase Admin initialized successfully.");
} catch (error) {
  console.error(
    "Firebase Initialization Error:",
    error.message
  );
}

// ============================================================
// FIREBASE WEB API KEY
// ============================================================

const FIREBASE_WEB_API_KEY =
  process.env.FIREBASE_WEB_API_KEY || "";

if (!FIREBASE_WEB_API_KEY) {
  console.warn(
    "WARNING: FIREBASE_WEB_API_KEY is not configured. Login will not work."
  );
}

// ============================================================
// DEVICE API KEY
// ============================================================

const DEVICE_API_KEY =
  process.env.SOBERWATCH_DEVICE_KEY ||
  "SOBER_WATCH_DEVICE_KEY_2026";

function validateDeviceKey(req) {
  const apiKey =
    req.headers["x-api-key"] ||
    req.headers["X-API-Key"];

  return Boolean(
    apiKey && apiKey === DEVICE_API_KEY
  );
}

// ============================================================
// HELPERS
// ============================================================

function calculateStatus(bac) {
  const value = Number(bac) || 0;

  if (value >= 0.08) return "DANGER";
  if (value >= 0.02) return "CAUTION";

  return "SAFE";
}

function normalizeTelemetry(body) {
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

function cleanEmail(email) {
  return String(email || "")
    .trim()
    .toLowerCase();
}

function getBearerToken(req) {
  const authorization =
    req.headers.authorization ||
    req.headers.Authorization ||
    "";

  if (!authorization.startsWith("Bearer ")) {
    return null;
  }

  return authorization.substring(7).trim();
}

// ============================================================
// FIREBASE AUTH MIDDLEWARE
// ============================================================

async function requireFirebaseAuth(req, res, next) {
  if (!firebaseReady) {
    return res.status(503).json({
      status: "error",
      message: "Firebase is not connected",
    });
  }

  const token = getBearerToken(req);

  if (!token) {
    return res.status(401).json({
      status: "error",
      message: "Authentication required",
    });
  }

  try {
    const decodedToken =
      await admin.auth().verifyIdToken(token);

    req.firebaseUser = decodedToken;

    next();
  } catch (error) {
    console.error(
      "AUTH ERROR:",
      error.code || error.message
    );

    return res.status(401).json({
      status: "error",
      message: "Invalid or expired authentication token",
    });
  }
}

// ============================================================
// VERIFY UID BELONGS TO AUTHENTICATED USER
// ============================================================

function requireOwnUid(req, res) {
  const uid = String(req.query.uid || "").trim();

  if (!uid) {
    res.status(400).json({
      status: "error",
      message: "uid is required",
    });

    return null;
  }

  if (
    !req.firebaseUser ||
    req.firebaseUser.uid !== uid
  ) {
    res.status(403).json({
      status: "error",
      message: "Access denied for this user",
    });

    return null;
  }

  return uid;
}

// ============================================================
// ROOT
// ============================================================

app.get("/", (req, res) => {
  res.status(200).json({
    service: "SoberWatch Telemetry API",
    status: "online",
    firebase: firebaseReady
      ? "connected"
      : "not_connected",

    authentication: FIREBASE_WEB_API_KEY
      ? "email_password_enabled"
      : "login_not_configured",

    endpoints: {
      register: "POST /api/register",
      login: "POST /api/login",
      readings: "GET /api/readings?uid=UID",
      health: "GET /api/health?uid=UID",
      telemetry: "POST /uploadTelemetry",
      testTelemetry: "POST /testTelemetry",
      emergency: "POST /api/emergency",
      emergencies:
        "GET /api/emergencies?uid=UID",
    },

    version: "4.0.0",
  });
});

// ============================================================
// REGISTER
// POST /api/register
// ============================================================

app.post("/api/register", async (req, res) => {
  const email = cleanEmail(req.body.email);
  const password = req.body.password;

  if (!email || !password) {
    return res.status(400).json({
      status: "error",
      message: "Email and password are required",
    });
  }

  if (password.length < 6) {
    return res.status(400).json({
      status: "error",
      message: "Password must be at least 6 characters",
    });
  }

  if (!firebaseReady || !db) {
    return res.status(503).json({
      status: "error",
      message: "Firebase is not connected",
    });
  }

  try {
    let existingUser = null;

    try {
      existingUser =
        await admin.auth().getUserByEmail(email);
    } catch (error) {
      if (error.code !== "auth/user-not-found") {
        throw error;
      }
    }

    if (existingUser) {
      return res.status(409).json({
        status: "error",
        message: "User already exists",
        uid: existingUser.uid,
        email: existingUser.email,
      });
    }

    const userRecord =
      await admin.auth().createUser({
        email,
        password,
        emailVerified: false,
      });

    await db
      .collection("users")
      .doc(userRecord.uid)
      .set(
        {
          uid: userRecord.uid,
          email,
          createdAt: Date.now(),
          updatedAt: Date.now(),
        },
        {
          merge: true,
        }
      );

    return res.status(200).json({
      status: "success",
      message: "Registration successful",
      uid: userRecord.uid,
      email: userRecord.email,
    });
  } catch (error) {
    console.error(
      "REGISTER ERROR:",
      error
    );

    let message = "Registration failed";

    if (
      error.code ===
      "auth/email-already-exists"
    ) {
      message = "Email already exists";
    } else if (
      error.code === "auth/invalid-email"
    ) {
      message = "Invalid email address";
    } else if (
      error.code === "auth/weak-password"
    ) {
      message = "Password is too weak";
    }

    return res.status(400).json({
      status: "error",
      message,
      code: error.code || "REGISTER_ERROR",
    });
  }
});

// ============================================================
// LOGIN
// POST /api/login
// ============================================================

app.post("/api/login", async (req, res) => {
  const email = cleanEmail(req.body.email);
  const password = req.body.password;

  if (!email || !password) {
    return res.status(400).json({
      status: "error",
      message: "Email and password are required",
    });
  }

  if (!FIREBASE_WEB_API_KEY) {
    return res.status(503).json({
      status: "error",
      message:
        "Firebase login is not configured. Add FIREBASE_WEB_API_KEY to backend environment variables.",
    });
  }

  try {
    const firebaseResponse =
      await fetch(
        `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${encodeURIComponent(
          FIREBASE_WEB_API_KEY
        )}`,
        {
          method: "POST",
          headers: {
            "Content-Type":
              "application/json",
          },
          body: JSON.stringify({
            email,
            password,
            returnSecureToken: true,
          }),
        }
      );

    const data =
      await firebaseResponse.json();

    if (!firebaseResponse.ok) {
      console.error(
        "Firebase Login Error:",
        data
      );

      let message =
        "Invalid email or password";

      const firebaseError =
        data?.error?.message || "";

      if (
        firebaseError === "EMAIL_NOT_FOUND"
      ) {
        message = "User does not exist";
      }

      if (
        firebaseError === "INVALID_PASSWORD"
      ) {
        message = "Incorrect password";
      }

      if (
        firebaseError ===
        "INVALID_LOGIN_CREDENTIALS"
      ) {
        message =
          "Invalid email or password";
      }

      if (
        firebaseError === "USER_DISABLED"
      ) {
        message =
          "This account has been disabled";
      }

      return res.status(401).json({
        status: "error",
        message,
      });
    }

    const uid = data.localId;
    const returnedEmail =
      data.email || email;

    if (db && uid) {
      await db
        .collection("users")
        .doc(uid)
        .set(
          {
            uid,
            email: returnedEmail,
            lastLoginAt: Date.now(),
            updatedAt: Date.now(),
          },
          {
            merge: true,
          }
        );
    }

    return res.status(200).json({
      status: "success",
      message: "Login successful",
      uid,
      email: returnedEmail,
      idToken: data.idToken,
      refreshToken: data.refreshToken,
      expiresIn: data.expiresIn,
    });
  } catch (error) {
    console.error(
      "LOGIN ERROR:",
      error
    );

    return res.status(500).json({
      status: "error",
      message:
        "Login service temporarily unavailable",
    });
  }
});

// ============================================================
// GET READINGS
// GET /api/readings?uid=UID
//
// QUOTA OPTIMIZATION:
// - Default limit reduced from 100 to 20
// - Maximum limit is 50
// - Authentication required
// ============================================================

app.get(
  "/api/readings",
  requireFirebaseAuth,
  async (req, res) => {
    const uid = requireOwnUid(
      req,
      res
    );

    if (!uid) return;

    const requestedLimit =
      Number(req.query.limit);

    const limit =
      Number.isFinite(requestedLimit) &&
      requestedLimit > 0
        ? Math.min(
            Math.floor(requestedLimit),
            50
          )
        : 20;

    if (!firebaseReady || !db) {
      return res.status(503).json({
        status: "error",
        message:
          "Firebase is not connected",
      });
    }

    try {
      const snapshot =
        await db
          .collection("users")
          .doc(uid)
          .collection("readings")
          .orderBy(
            "timestamp",
            "desc"
          )
          .limit(limit)
          .get();

      const readings =
        snapshot.docs.map((doc) => ({
          id: doc.id,
          ...doc.data(),
        }));

      return res.status(200).json({
        status: "success",
        count: readings.length,
        limit,
        readings,
      });
    } catch (error) {
      console.error(
        "READINGS ERROR:",
        error
      );

      if (
        error.code ===
          8 ||
        error.code ===
          "RESOURCE_EXHAUSTED"
      ) {
        return res.status(429).json({
          status: "error",
          message:
            "Firestore quota temporarily exceeded. Please retry later.",
          code: "RESOURCE_EXHAUSTED",
        });
      }

      return res.status(500).json({
        status: "error",
        message:
          "Failed to fetch readings",
      });
    }
  }
);

// ============================================================
// GET LATEST HEALTH
// GET /api/health?uid=UID
//
// ONLY ONE FIRESTORE DOCUMENT READ
// ============================================================

app.get(
  "/api/health",
  requireFirebaseAuth,
  async (req, res) => {
    const uid = requireOwnUid(
      req,
      res
    );

    if (!uid) return;

    if (!firebaseReady || !db) {
      return res.status(503).json({
        status: "error",
        message:
          "Firebase is not connected",
      });
    }

    try {
      const userDoc =
        await db
          .collection("users")
          .doc(uid)
          .get();

      if (!userDoc.exists) {
        return res.status(404).json({
          status: "error",
          message: "User not found",
        });
      }

      const userData =
        userDoc.data();

      if (!userData.lastReading) {
        return res.status(404).json({
          status: "error",
          message:
            "No health reading available",
        });
      }

      return res.status(200).json({
        status: "success",
        data: userData.lastReading,
      });
    } catch (error) {
      console.error(
        "HEALTH ERROR:",
        error
      );

      if (
        error.code === 8 ||
        error.code ===
          "RESOURCE_EXHAUSTED"
      ) {
        return res.status(429).json({
          status: "error",
          message:
            "Firestore quota temporarily exceeded. Please retry later.",
          code: "RESOURCE_EXHAUSTED",
        });
      }

      return res.status(500).json({
        status: "error",
        message:
          "Failed to fetch health data",
      });
    }
  }
);

// ============================================================
// UPLOAD TELEMETRY
// POST /uploadTelemetry
//
// DEVICE AUTHENTICATION:
// x-api-key
//
// QUOTA OPTIMIZATION:
// Uses a Firestore batch so the reading + lastReading
// are committed together.
// ============================================================

app.post(
  "/uploadTelemetry",
  async (req, res) => {
    console.log(
      "--------------------------------------------------"
    );

    console.log(
      "Telemetry request received"
    );

    console.log(
      "Device ID:",
      req.body.deviceId
    );

    console.log(
      "UID:",
      req.body.uid
    );

    if (!validateDeviceKey(req)) {
      return res.status(401).json({
        status: "error",
        message:
          "Unauthorized: Invalid Device Key",
      });
    }

    if (!firebaseReady || !db) {
      return res.status(503).json({
        status: "error",
        message:
          "Firebase is not connected",
      });
    }

    const uid = String(
      req.body.uid || ""
    ).trim();

    if (!uid) {
      return res.status(400).json({
        status: "error",
        message: "uid is required",
      });
    }

    const telemetryData =
      normalizeTelemetry(req.body);

    try {
      const userRef = db
        .collection("users")
        .doc(uid);

      const readingRef =
        userRef
          .collection("readings")
          .doc();

      const batch = db.batch();

      batch.set(
        readingRef,
        telemetryData
      );

      batch.set(
        userRef,
        {
          lastReading:
            telemetryData,
          lastReadingId:
            readingRef.id,
          updatedAt: Date.now(),
        },
        {
          merge: true,
        }
      );

      await batch.commit();

      return res.status(200).json({
        status: "success",
        message:
          "Telemetry uploaded successfully",
        readingId:
          readingRef.id,
        data: telemetryData,
      });
    } catch (error) {
      console.error(
        "TELEMETRY ERROR:",
        error
      );

      if (
        error.code === 8 ||
        error.code ===
          "RESOURCE_EXHAUSTED"
      ) {
        return res.status(429).json({
          status: "error",
          message:
            "Firestore quota temporarily exceeded. Telemetry was not saved.",
          code: "RESOURCE_EXHAUSTED",
        });
      }

      return res.status(500).json({
        status: "error",
        message:
          "Failed to save telemetry",
      });
    }
  }
);

// ============================================================
// TEST TELEMETRY
// POST /testTelemetry
// ============================================================

app.post(
  "/testTelemetry",
  async (req, res) => {
    if (!firebaseReady || !db) {
      return res.status(503).json({
        status: "error",
        message:
          "Firebase is not connected",
      });
    }

    const uid = String(
      req.body.uid ||
        "test-user"
    ).trim();

    const testData = {
      alcoholBac: 0.04,
      heartRateBpm: 78,
      spo2Percent: 98,
      tempCelsius: 36.7,
      ecgStatus: "Stable",
      sensorRaw: 1200,
      sensorResponse: 20,
      status: "CAUTION",
      deviceId:
        "SOBERWATCH-TEST",
      timestamp: Date.now(),
      source: "test",
    };

    try {
      const userRef = db
        .collection("users")
        .doc(uid);

      const readingRef =
        userRef
          .collection("readings")
          .doc();

      const batch = db.batch();

      batch.set(
        readingRef,
        testData
      );

      batch.set(
        userRef,
        {
          uid,
          lastReading:
            testData,
          lastReadingId:
            readingRef.id,
          updatedAt: Date.now(),
        },
        {
          merge: true,
        }
      );

      await batch.commit();

      return res.status(200).json({
        status: "success",
        message:
          "Test telemetry saved",
        readingId:
          readingRef.id,
        data: testData,
      });
    } catch (error) {
      console.error(
        "TEST TELEMETRY ERROR:",
        error
      );

      if (
        error.code === 8 ||
        error.code ===
          "RESOURCE_EXHAUSTED"
      ) {
        return res.status(429).json({
          status: "error",
          message:
            "Firestore quota temporarily exceeded.",
          code: "RESOURCE_EXHAUSTED",
        });
      }

      return res.status(500).json({
        status: "error",
        message:
          "Failed to save test telemetry",
      });
    }
  }
);

// ============================================================
// EMERGENCY
// POST /api/emergency
// ============================================================

app.post(
  "/api/emergency",
  requireFirebaseAuth,
  async (req, res) => {
    if (!firebaseReady || !db) {
      return res.status(503).json({
        status: "error",
        message:
          "Firebase is not connected",
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

    if (
      req.firebaseUser.uid !==
      String(uid)
    ) {
      return res.status(403).json({
        status: "error",
        message:
          "Access denied for this user",
      });
    }

    try {
      const emergencyData = {
        eventId:
          eventId ||
          `emg-${Date.now()}`,

        type:
          type || "SOS",

        severity:
          severity || "high",

        latitude:
          Number(latitude) || 0,

        longitude:
          Number(longitude) || 0,

        accuracy:
          Number(accuracy) || 0,

        timestamp:
          timestamp ||
          new Date().toISOString(),

        contact:
          contact || "",

        notes:
          notes || "",

        mapsUrl:
          mapsUrl || "",

        recognizedText:
          recognizedText || "",

        createdAt: Date.now(),
      };

      const emergencyRef =
        await db
          .collection("users")
          .doc(uid)
          .collection("emergencies")
          .add(
            emergencyData
          );

      return res.status(200).json({
        status: "success",
        message:
          "Emergency event recorded",
        emergencyId:
          emergencyRef.id,
        data: emergencyData,
      });
    } catch (error) {
      console.error(
        "EMERGENCY ERROR:",
        error
      );

      if (
        error.code === 8 ||
        error.code ===
          "RESOURCE_EXHAUSTED"
      ) {
        return res.status(429).json({
          status: "error",
          message:
            "Firestore quota temporarily exceeded.",
          code: "RESOURCE_EXHAUSTED",
        });
      }

      return res.status(500).json({
        status: "error",
        message:
          "Failed to save emergency event",
      });
    }
  }
);

// ============================================================
// GET EMERGENCIES
// GET /api/emergencies?uid=UID
// ============================================================

app.get(
  "/api/emergencies",
  requireFirebaseAuth,
  async (req, res) => {
    const uid = requireOwnUid(
      req,
      res
    );

    if (!uid) return;

    if (!firebaseReady || !db) {
      return res.status(503).json({
        status: "error",
        message:
          "Firebase is not connected",
      });
    }

    try {
      const snapshot =
        await db
          .collection("users")
          .doc(uid)
          .collection("emergencies")
          .orderBy(
            "createdAt",
            "desc"
          )
          .limit(20)
          .get();

      const emergencies =
        snapshot.docs.map(
          (doc) => ({
            id: doc.id,
            ...doc.data(),
          })
        );

      return res.status(200).json({
        status: "success",
        emergencies,
      });
    } catch (error) {
      console.error(
        "EMERGENCIES ERROR:",
        error
      );

      if (
        error.code === 8 ||
        error.code ===
          "RESOURCE_EXHAUSTED"
      ) {
        return res.status(429).json({
          status: "error",
          message:
            "Firestore quota temporarily exceeded.",
          code: "RESOURCE_EXHAUSTED",
        });
      }

      return res.status(500).json({
        status: "error",
        message:
          "Failed to fetch emergency history",
      });
    }
  }
);

// ============================================================
// 404
// ============================================================

app.use(
  (req, res) => {
    res.status(404).json({
      status: "error",
      message:
        "Endpoint not found",
      path: req.path,
    });
  }
);

// ============================================================
// GLOBAL ERROR HANDLER
// ============================================================

app.use(
  (
    error,
    req,
    res,
    next
  ) => {
    console.error(
      "GLOBAL ERROR:",
      error
    );

    if (
      error.code === 8 ||
      error.code ===
        "RESOURCE_EXHAUSTED"
    ) {
      return res.status(429).json({
        status: "error",
        message:
          "Firestore quota temporarily exceeded.",
        code: "RESOURCE_EXHAUSTED",
      });
    }

    res.status(500).json({
      status: "error",
      message:
        "Internal server error",
    });
  }
);

// ============================================================
// SERVER
// ============================================================

const PORT =
  process.env.PORT || 3000;

app.listen(
  PORT,
  () => {
    console.log(
      `SoberWatch backend running on port ${PORT}`
    );
  }
);

