```javascript
const express = require("express");
const admin = require("firebase-admin");
const cors = require("cors");

const app = express();

// ============================================================
// CONFIGURATION
// ============================================================

app.set("trust proxy", 1);

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
    throw new Error(
      "FIREBASE_SERVICE_ACCOUNT is missing"
    );
  }

  const serviceAccount = JSON.parse(
    process.env.FIREBASE_SERVICE_ACCOUNT
  );

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });

  db = admin.firestore();

  firebaseReady = true;

  console.log(
    "Firebase Admin initialized successfully."
  );
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
    "WARNING: FIREBASE_WEB_API_KEY is not configured."
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
// QUOTA PROTECTION
// ============================================================
//
// IMPORTANT:
//
// Firestore free quota can be exhausted when:
// - frontend polls health every second
// - frontend polls readings repeatedly
// - device uploads telemetry every second
//
// These values protect Firestore.
//
// You can change them through Render environment variables.
// ============================================================

const HEALTH_CACHE_MS =
  Number(process.env.HEALTH_CACHE_MS) || 15000;

const READINGS_CACHE_MS =
  Number(process.env.READINGS_CACHE_MS) || 15000;

const TELEMETRY_MIN_INTERVAL_MS =
  Number(process.env.TELEMETRY_MIN_INTERVAL_MS) || 30000;

const HISTORY_WRITE_INTERVAL_MS =
  Number(process.env.HISTORY_WRITE_INTERVAL_MS) || 60000;

const USER_LAST_READING_INTERVAL_MS =
  Number(process.env.USER_LAST_READING_INTERVAL_MS) || 30000;

const MAX_READINGS_LIMIT =
  Number(process.env.MAX_READINGS_LIMIT) || 30;

// ============================================================
// MEMORY CACHE
// ============================================================

const healthCache = new Map();
const readingsCache = new Map();
const latestTelemetryCache = new Map();

const telemetryLastAcceptedAt = new Map();
const historyLastWrittenAt = new Map();
const lastReadingLastWrittenAt = new Map();

// ============================================================
// CACHE HELPERS
// ============================================================

function getCache(map, key) {
  const item = map.get(key);

  if (!item) {
    return null;
  }

  if (Date.now() - item.createdAt > item.ttl) {
    map.delete(key);
    return null;
  }

  return item.value;
}

function setCache(map, key, value, ttl) {
  map.set(key, {
    value,
    createdAt: Date.now(),
    ttl,
  });
}

function invalidateUserCache(uid) {
  if (!uid) return;

  healthCache.delete(uid);

  for (const key of readingsCache.keys()) {
    if (key.startsWith(`${uid}:`)) {
      readingsCache.delete(key);
    }
  }
}

// ============================================================
// HELPERS
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

  const bacValue =
    Number(alcoholBac) || 0;

  const heartRateValue =
    Number(heartRateBpm) || 0;

  const spo2Value =
    Number(spo2Percent) || 0;

  const temperatureValue =
    Number(tempCelsius) || 0;

  return {
    alcoholBac: bacValue,

    heartRateBpm:
      heartRateValue,

    spo2Percent:
      spo2Value,

    tempCelsius:
      temperatureValue,

    ecgStatus:
      body.ecgStatus ||
      "Not connected",

    sensorRaw:
      Number(body.sensorRaw) || 0,

    sensorResponse:
      Number(body.sensorResponse) || 0,

    status:
      body.status ||
      calculateStatus(bacValue),

    deviceId:
      body.deviceId ||
      "SW-001",

    timestamp:
      Number(body.timestamp) ||
      Date.now(),

    source:
      body.source ||
      "hardware",
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

  return authorization
    .substring(7)
    .trim();
}

function isResourceExhausted(error) {
  return (
    error &&
    (
      error.code === 8 ||
      error.code ===
        "RESOURCE_EXHAUSTED"
    )
  );
}

// ============================================================
// FIREBASE AUTH MIDDLEWARE
// ============================================================

async function requireFirebaseAuth(
  req,
  res,
  next
) {
  if (!firebaseReady) {
    return res.status(503).json({
      status: "error",
      message:
        "Firebase is not connected",
    });
  }

  const token =
    getBearerToken(req);

  if (!token) {
    return res.status(401).json({
      status: "error",
      message:
        "Authentication required",
    });
  }

  try {
    const decodedToken =
      await admin
        .auth()
        .verifyIdToken(token);

    req.firebaseUser =
      decodedToken;

    next();
  } catch (error) {
    console.error(
      "AUTH ERROR:",
      error.code ||
        error.message
    );

    return res.status(401).json({
      status: "error",
      message:
        "Invalid or expired authentication token",
    });
  }
}

// ============================================================
// UID SECURITY
// ============================================================

function requireOwnUid(req, res) {
  const uid = String(
    req.query.uid ||
      req.body?.uid ||
      ""
  ).trim();

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
      message:
        "Access denied for this user",
    });

    return null;
  }

  return uid;
}

// ============================================================
// SIMPLE REQUEST RATE LIMITER
// ============================================================

const requestCounters = new Map();

function simpleRateLimit(
  req,
  res,
  next
) {
  const ip =
    req.ip ||
    req.socket.remoteAddress ||
    "unknown";

  const now = Date.now();

  const existing =
    requestCounters.get(ip);

  if (
    !existing ||
    now - existing.startedAt >
      60000
  ) {
    requestCounters.set(ip, {
      startedAt: now,
      count: 1,
    });

    return next();
  }

  existing.count += 1;

  // Maximum 120 requests/minute/IP
  if (existing.count > 120) {
    return res.status(429).json({
      status: "error",
      message:
        "Too many requests. Please slow down.",
    });
  }

  next();
}

app.use(simpleRateLimit);

// ============================================================
// ROOT
// ============================================================

app.get("/", (req, res) => {
  res.status(200).json({
    service:
      "SoberWatch Telemetry API",

    status: "online",

    firebase:
      firebaseReady
        ? "connected"
        : "not_connected",

    authentication:
      FIREBASE_WEB_API_KEY
        ? "email_password_enabled"
        : "login_not_configured",

    quotaProtection: {
      healthCacheMs:
        HEALTH_CACHE_MS,

      readingsCacheMs:
        READINGS_CACHE_MS,

      telemetryMinIntervalMs:
        TELEMETRY_MIN_INTERVAL_MS,

      historyWriteIntervalMs:
        HISTORY_WRITE_INTERVAL_MS,

      userLastReadingIntervalMs:
        USER_LAST_READING_INTERVAL_MS,
    },

    endpoints: {
      register:
        "POST /api/register",

      login:
        "POST /api/login",

      readings:
        "GET /api/readings?uid=UID",

      health:
        "GET /api/health?uid=UID",

      telemetry:
        "POST /uploadTelemetry",

      testTelemetry:
        "POST /testTelemetry",

      emergency:
        "POST /api/emergency",

      emergencies:
        "GET /api/emergencies?uid=UID",
    },

    version: "5.0.0",
  });
});

// ============================================================
// REGISTER
// POST /api/register
// ============================================================

app.post(
  "/api/register",
  async (req, res) => {
    const email =
      cleanEmail(
        req.body.email
      );

    const password =
      req.body.password;

    if (!email || !password) {
      return res.status(400).json({
        status: "error",
        message:
          "Email and password are required",
      });
    }

    if (password.length < 6) {
      return res.status(400).json({
        status: "error",
        message:
          "Password must be at least 6 characters",
      });
    }

    if (
      !firebaseReady ||
      !db
    ) {
      return res.status(503).json({
        status: "error",
        message:
          "Firebase is not connected",
      });
    }

    try {
      let existingUser = null;

      try {
        existingUser =
          await admin
            .auth()
            .getUserByEmail(email);
      } catch (error) {
        if (
          error.code !==
          "auth/user-not-found"
        ) {
          throw error;
        }
      }

      if (existingUser) {
        return res.status(409).json({
          status: "error",
          message:
            "User already exists",
          uid:
            existingUser.uid,
          email:
            existingUser.email,
        });
      }

      const userRecord =
        await admin
          .auth()
          .createUser({
            email,
            password,
            emailVerified:
              false,
          });

      await db
        .collection("users")
        .doc(userRecord.uid)
        .set(
          {
            uid:
              userRecord.uid,

            email,

            createdAt:
              Date.now(),

            updatedAt:
              Date.now(),
          },
          {
            merge: true,
          }
        );

      return res.status(200).json({
        status: "success",
        message:
          "Registration successful",

        uid:
          userRecord.uid,

        email:
          userRecord.email,
      });
    } catch (error) {
      console.error(
        "REGISTER ERROR:",
        error
      );

      let message =
        "Registration failed";

      if (
        error.code ===
        "auth/email-already-exists"
      ) {
        message =
          "Email already exists";
      } else if (
        error.code ===
        "auth/invalid-email"
      ) {
        message =
          "Invalid email address";
      } else if (
        error.code ===
        "auth/weak-password"
      ) {
        message =
          "Password is too weak";
      }

      return res.status(400).json({
        status: "error",
        message,
        code:
          error.code ||
          "REGISTER_ERROR",
      });
    }
  }
);

// ============================================================
// LOGIN
// POST /api/login
//
// IMPORTANT:
// We DO NOT write to Firestore on every login.
// This saves Firestore write quota.
// ============================================================

app.post(
  "/api/login",
  async (req, res) => {
    const email =
      cleanEmail(
        req.body.email
      );

    const password =
      req.body.password;

    if (!email || !password) {
      return res.status(400).json({
        status: "error",
        message:
          "Email and password are required",
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

            body:
              JSON.stringify({
                email,
                password,
                returnSecureToken:
                  true,
              }),
          }
        );

      const data =
        await firebaseResponse.json();

      if (
        !firebaseResponse.ok
      ) {
        console.error(
          "Firebase Login Error:",
          data
        );

        let message =
          "Invalid email or password";

        const firebaseError =
          data?.error?.message ||
          "";

        if (
          firebaseError ===
          "EMAIL_NOT_FOUND"
        ) {
          message =
            "User does not exist";
        }

        if (
          firebaseError ===
          "INVALID_PASSWORD"
        ) {
          message =
            "Incorrect password";
        }

        if (
          firebaseError ===
          "INVALID_LOGIN_CREDENTIALS"
        ) {
          message =
            "Invalid email or password";
        }

        if (
          firebaseError ===
          "USER_DISABLED"
        ) {
          message =
            "This account has been disabled";
        }

        return res.status(401).json({
          status: "error",
          message,
        });
      }

      const uid =
        data.localId;

      const returnedEmail =
        data.email ||
        email;

      return res.status(200).json({
        status: "success",
        message:
          "Login successful",

        uid,

        email:
          returnedEmail,

        idToken:
          data.idToken,

        refreshToken:
          data.refreshToken,

        expiresIn:
          data.expiresIn,
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
  }
);

// ============================================================
// GET HEALTH
// GET /api/health?uid=UID
//
// FIRST:
// memory cache
//
// SECOND:
// Firestore
//
// This prevents the frontend from reading Firestore
// on every dashboard refresh.
// ============================================================

app.get(
  "/api/health",
  requireFirebaseAuth,
  async (req, res) => {
    const uid =
      requireOwnUid(
        req,
        res
      );

    if (!uid) return;

    // --------------------------------------------------------
    // 1. LATEST MEMORY CACHE
    // --------------------------------------------------------

    const memoryHealth =
      latestTelemetryCache.get(
        uid
      );

    if (memoryHealth) {
      return res.status(200).json({
        status: "success",
        source: "memory_cache",
        data: memoryHealth,
      });
    }

    // --------------------------------------------------------
    // 2. HEALTH CACHE
    // --------------------------------------------------------

    const cachedHealth =
      getCache(
        healthCache,
        uid
      );

    if (cachedHealth) {
      return res.status(200).json({
        status: "success",
        source: "cache",
        data: cachedHealth,
      });
    }

    if (
      !firebaseReady ||
      !db
    ) {
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
          message:
            "User not found",
        });
      }

      const userData =
        userDoc.data();

      if (
        !userData.lastReading
      ) {
        return res.status(404).json({
          status: "error",
          message:
            "No health reading available",
        });
      }

      setCache(
        healthCache,
        uid,
        userData.lastReading,
        HEALTH_CACHE_MS
      );

      latestTelemetryCache.set(
        uid,
        userData.lastReading
      );

      return res.status(200).json({
        status: "success",
        source: "firestore",
        data:
          userData.lastReading,
      });
    } catch (error) {
      console.error(
        "HEALTH ERROR:",
        error
      );

      if (
        isResourceExhausted(
          error
        )
      ) {
        const fallback =
          latestTelemetryCache.get(
            uid
          );

        if (fallback) {
          return res.status(200).json({
            status: "success",
            source:
              "memory_fallback",
            data: fallback,
          });
        }

        return res.status(429).json({
          status: "error",
          message:
            "Firestore quota temporarily exceeded.",
          code:
            "RESOURCE_EXHAUSTED",
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
// GET READINGS
// GET /api/readings?uid=UID&limit=20
//
// Cache protects Firestore from repeated dashboard requests.
// ============================================================

app.get(
  "/api/readings",
  requireFirebaseAuth,
  async (req, res) => {
    const uid =
      requireOwnUid(
        req,
        res
      );

    if (!uid) return;

    let requestedLimit =
      Number(req.query.limit);

    if (
      !Number.isFinite(
        requestedLimit
      ) ||
      requestedLimit <= 0
    ) {
      requestedLimit = 20;
    }

    const limit =
      Math.min(
        Math.floor(
          requestedLimit
        ),
        MAX_READINGS_LIMIT
      );

    const cacheKey =
      `${uid}:${limit}`;

    // --------------------------------------------------------
    // CACHE
    // --------------------------------------------------------

    const cached =
      getCache(
        readingsCache,
        cacheKey
      );

    if (cached) {
      return res.status(200).json({
        status: "success",
        source: "cache",
        count:
          cached.length,
        limit,
        readings:
          cached,
      });
    }

    if (
      !firebaseReady ||
      !db
    ) {
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
        snapshot.docs.map(
          (doc) => ({
            id: doc.id,
            ...doc.data(),
          })
        );

      setCache(
        readingsCache,
        cacheKey,
        readings,
        READINGS_CACHE_MS
      );

      return res.status(200).json({
        status: "success",
        source: "firestore",
        count:
          readings.length,
        limit,
        readings,
      });
    } catch (error) {
      console.error(
        "READINGS ERROR:",
        error
      );

      if (
        isResourceExhausted(
          error
        )
      ) {
        return res.status(429).json({
          status: "error",
          message:
            "Firestore quota temporarily exceeded. Cached data may become available shortly.",
          code:
            "RESOURCE_EXHAUSTED",
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
// UPLOAD TELEMETRY
// POST /uploadTelemetry
//
// DEVICE AUTH:
// x-api-key
//
// QUOTA STRATEGY:
//
// 1. Every request updates memory.
// 2. Requests arriving too quickly are NOT written.
// 3. History is persisted only every HISTORY_WRITE_INTERVAL_MS.
// 4. lastReading is persisted only periodically.
//
// This is the most important protection against Firestore
// write quota exhaustion.
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

    // --------------------------------------------------------
    // DEVICE AUTH
    // --------------------------------------------------------

    if (
      !validateDeviceKey(req)
    ) {
      return res.status(401).json({
        status: "error",
        message:
          "Unauthorized: Invalid Device Key",
      });
    }

    if (
      !firebaseReady ||
      !db
    ) {
      return res.status(503).json({
        status: "error",
        message:
          "Firebase is not connected",
      });
    }

    const uid =
      String(
        req.body.uid || ""
      ).trim();

    if (!uid) {
      return res.status(400).json({
        status: "error",
        message:
          "uid is required",
      });
    }

    const telemetryData =
      normalizeTelemetry(
        req.body
      );

    const now =
      Date.now();

    // --------------------------------------------------------
    // ALWAYS KEEP LATEST DATA IN MEMORY
    // --------------------------------------------------------

    latestTelemetryCache.set(
      uid,
      telemetryData
    );

    setCache(
      healthCache,
      uid,
      telemetryData,
      HEALTH_CACHE_MS
    );

    // --------------------------------------------------------
    // DEVICE TELEMETRY THROTTLING
    // --------------------------------------------------------

    const lastAccepted =
      telemetryLastAcceptedAt.get(
        uid
      ) || 0;

    const timeSinceLast =
      now - lastAccepted;

    // If telemetry arrives too quickly,
    // return success WITHOUT Firestore write.
    if (
      timeSinceLast <
      TELEMETRY_MIN_INTERVAL_MS
    ) {
      return res.status(200).json({
        status: "success",
        message:
          "Telemetry received and cached",
        persisted: false,
        reason:
          "quota_protection",
        data:
          telemetryData,
      });
    }

    telemetryLastAcceptedAt.set(
      uid,
      now
    );

    // --------------------------------------------------------
    // SHOULD WE WRITE HISTORY?
    // --------------------------------------------------------

    const lastHistoryWrite =
      historyLastWrittenAt.get(
        uid
      ) || 0;

    const shouldWriteHistory =
      now -
        lastHistoryWrite >=
      HISTORY_WRITE_INTERVAL_MS;

    // --------------------------------------------------------
    // SHOULD WE UPDATE USER LAST READING?
    // --------------------------------------------------------

    const lastUserWrite =
      lastReadingLastWrittenAt.get(
        uid
      ) || 0;

    const shouldUpdateLastReading =
      now -
        lastUserWrite >=
      USER_LAST_READING_INTERVAL_MS;

    try {
      const userRef =
        db
          .collection("users")
          .doc(uid);

      // ------------------------------------------------------
      // CASE 1:
      // ONLY UPDATE lastReading
      // ------------------------------------------------------

      if (
        !shouldWriteHistory &&
        shouldUpdateLastReading
      ) {
        await userRef.set(
          {
            lastReading:
              telemetryData,

            updatedAt:
              now,
          },
          {
            merge: true,
          }
        );

        lastReadingLastWrittenAt.set(
          uid,
          now
        );

        invalidateUserCache(
          uid
        );

        setCache(
          healthCache,
          uid,
          telemetryData,
          HEALTH_CACHE_MS
        );

        return res.status(200).json({
          status: "success",
          message:
            "Latest telemetry updated",
          persisted:
            true,
          historySaved:
            false,
          data:
            telemetryData,
        });
      }

      // ------------------------------------------------------
      // CASE 2:
      // SAVE HISTORY + LAST READING
      // ------------------------------------------------------

      if (
        shouldWriteHistory
      ) {
        const readingRef =
          userRef
            .collection(
              "readings"
            )
            .doc();

        const batch =
          db.batch();

        batch.set(
          readingRef,
          telemetryData
        );

        if (
          shouldUpdateLastReading
        ) {
          batch.set(
            userRef,
            {
              lastReading:
                telemetryData,

              lastReadingId:
                readingRef.id,

              updatedAt:
                now,
            },
            {
              merge: true,
            }
          );
        }

        await batch.commit();

        historyLastWrittenAt.set(
          uid,
          now
        );

        if (
          shouldUpdateLastReading
        ) {
          lastReadingLastWrittenAt.set(
            uid,
            now
          );
        }

        invalidateUserCache(
          uid
        );

        setCache(
          healthCache,
          uid,
          telemetryData,
          HEALTH_CACHE_MS
        );

        return res.status(200).json({
          status: "success",
          message:
            "Telemetry persisted",
          persisted:
            true,
          historySaved:
            true,
          readingId:
            readingRef.id,
          data:
            telemetryData,
        });
      }

      // ------------------------------------------------------
      // CASE 3:
      // MEMORY ONLY
      // ------------------------------------------------------

      return res.status(200).json({
        status: "success",
        message:
          "Telemetry received and cached",
        persisted: false,
        historySaved:
          false,
        data:
          telemetryData,
      });
    } catch (error) {
      console.error(
        "TELEMETRY ERROR:",
        error
      );

      if (
        isResourceExhausted(
          error
        )
      ) {
        // IMPORTANT:
        // Even if Firestore is exhausted,
        // latest telemetry remains available
        // in memory for dashboard use.

        return res.status(200).json({
          status: "success",
          message:
            "Telemetry received but Firestore quota is temporarily exhausted. Latest data is cached.",
          persisted: false,
          code:
            "RESOURCE_EXHAUSTED",
          data:
            telemetryData,
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
//
// AUTHENTICATED USER ONLY
// ============================================================

app.post(
  "/testTelemetry",
  requireFirebaseAuth,
  async (req, res) => {
    const uid =
      requireOwnUid(
        req,
        res
      );

    if (!uid) return;

    if (
      !firebaseReady ||
      !db
    ) {
      return res.status(503).json({
        status: "error",
        message:
          "Firebase is not connected",
      });
    }

    const now =
      Date.now();

    const testData = {
      alcoholBac: 0.04,

      heartRateBpm: 78,

      spo2Percent: 98,

      tempCelsius: 36.7,

      ecgStatus:
        "Stable",

      sensorRaw: 1200,

      sensorResponse: 20,

      status:
        "CAUTION",

      deviceId:
        "SOBERWATCH-TEST",

      timestamp:
        now,

      source:
        "test",
    };

    // Memory update
    latestTelemetryCache.set(
      uid,
      testData
    );

    setCache(
      healthCache,
      uid,
      testData,
      HEALTH_CACHE_MS
    );

    try {
      const userRef =
        db
          .collection("users")
          .doc(uid);

      const readingRef =
        userRef
          .collection(
            "readings"
          )
          .doc();

      const batch =
        db.batch();

      batch.set(
        readingRef,
        testData
      );

      batch.set(
        userRef,
        {
          lastReading:
            testData,

          lastReadingId:
            readingRef.id,

          updatedAt:
            now,
        },
        {
          merge: true,
        }
      );

      await batch.commit();

      historyLastWrittenAt.set(
        uid,
        now
      );

      lastReadingLastWrittenAt.set(
        uid,
        now
      );

      invalidateUserCache(
        uid
      );

      setCache(
        healthCache,
        uid,
        testData,
        HEALTH_CACHE_MS
      );

      latestTelemetryCache.set(
        uid,
        testData
      );

      return res.status(200).json({
        status: "success",
        message:
          "Test telemetry saved",

        readingId:
          readingRef.id,

        data:
          testData,
      });
    } catch (error) {
      console.error(
        "TEST TELEMETRY ERROR:",
        error
      );

      if (
        isResourceExhausted(
          error
        )
      ) {
        return res.status(200).json({
          status: "success",
          message:
            "Test telemetry cached, but Firestore quota is temporarily exhausted.",
          persisted: false,
          code:
            "RESOURCE_EXHAUSTED",
          data:
            testData,
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
    const uid =
      requireOwnUid(
        req,
        res
      );

    if (!uid) return;

    if (
      !firebaseReady ||
      !db
    ) {
      return res.status(503).json({
        status: "error",
        message:
          "Firebase is not connected",
      });
    }

    const {
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

    try {
      const emergencyData = {
        eventId:
          eventId ||
          `emg-${Date.now()}`,

        type:
          type ||
          "SOS",

        severity:
          severity ||
          "high",

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

        createdAt:
          Date.now(),
      };

      const emergencyRef =
        await db
          .collection("users")
          .doc(uid)
          .collection(
            "emergencies"
          )
          .add(
            emergencyData
          );

      return res.status(200).json({
        status: "success",
        message:
          "Emergency event recorded",

        emergencyId:
          emergencyRef.id,

        data:
          emergencyData,
      });
    } catch (error) {
      console.error(
        "EMERGENCY ERROR:",
        error
      );

      if (
        isResourceExhausted(
          error
        )
      ) {
        return res.status(429).json({
          status: "error",
          message:
            "Firestore quota temporarily exceeded.",
          code:
            "RESOURCE_EXHAUSTED",
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
    const uid =
      requireOwnUid(
        req,
        res
      );

    if (!uid) return;

    if (
      !firebaseReady ||
      !db
    ) {
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
          .collection(
            "emergencies"
          )
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
        count:
          emergencies.length,
        emergencies,
      });
    } catch (error) {
      console.error(
        "EMERGENCIES ERROR:",
        error
      );

      if (
        isResourceExhausted(
          error
        )
      ) {
        return res.status(429).json({
          status: "error",
          message:
            "Firestore quota temporarily exceeded.",
          code:
            "RESOURCE_EXHAUSTED",
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
// HEALTH CHECK
// ============================================================

app.get(
  "/api/status",
  (req, res) => {
    res.status(200).json({
      status: "online",

      firebase:
        firebaseReady
          ? "connected"
          : "not_connected",

      cache: {
        health:
          healthCache.size,

        readings:
          readingsCache.size,

        latestTelemetry:
          latestTelemetryCache.size,
      },

      quotaProtection: {
        telemetryMinIntervalMs:
          TELEMETRY_MIN_INTERVAL_MS,

        historyWriteIntervalMs:
          HISTORY_WRITE_INTERVAL_MS,

        healthCacheMs:
          HEALTH_CACHE_MS,

        readingsCacheMs:
          READINGS_CACHE_MS,
      },

      timestamp:
        new Date().toISOString(),
    });
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
      isResourceExhausted(
        error
      )
    ) {
      return res.status(429).json({
        status: "error",
        message:
          "Firestore quota temporarily exceeded.",
        code:
          "RESOURCE_EXHAUSTED",
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

    console.log(
      `Health cache: ${HEALTH_CACHE_MS}ms`
    );

    console.log(
      `Readings cache: ${READINGS_CACHE_MS}ms`
    );

    console.log(
      `Telemetry minimum interval: ${TELEMETRY_MIN_INTERVAL_MS}ms`
    );

    console.log(
      `History write interval: ${HISTORY_WRITE_INTERVAL_MS}ms`
    );
  }
);
```
