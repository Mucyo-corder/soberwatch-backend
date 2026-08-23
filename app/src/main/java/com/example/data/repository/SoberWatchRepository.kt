package com.example.data.repository

import android.content.Context
import com.example.data.room.*
import com.example.models.AlertEvent
import com.example.models.BleDevice
import com.example.models.EmergencyContact
import com.example.models.HealthReport
import com.example.models.SensorReading
import com.example.models.UserProfile
import com.example.services.AiInsightCard
import com.example.services.AiInsightsService
import com.example.services.BleServiceSimulator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SoberWatchRepository(private val context: Context) {

  private val database = SoberWatchDatabase.getDatabase(context)
  private val sensorDao = database.sensorDao()
  private val profileDao = database.userProfileDao()
  private val contactDao = database.emergencyContactDao()
  private val alertDao = database.alertEventDao()
  private val reportDao = database.healthReportDao()
  
  private val bleSimulator = BleServiceSimulator()
  private val aiInsightsService = AiInsightsService()

  private val auth = FirebaseAuth.getInstance()
  private val firestore = FirebaseFirestore.getInstance()

  private val scope = CoroutineScope(Dispatchers.IO)

  // Real-time sensor stream from BLE simulator
  val currentReading: StateFlow<SensorReading> = bleSimulator.currentReading
  val isConnected: StateFlow<Boolean> = bleSimulator.isConnected
  val isMonitoring: StateFlow<Boolean> = bleSimulator.isMonitoring
  val connectedDevice: StateFlow<BleDevice?> = bleSimulator.connectedDevice
  val availableDevices: StateFlow<List<BleDevice>> = bleSimulator.availableDevices
  val isScanning: StateFlow<Boolean> = bleSimulator.isScanning

  // User Profile
  private val _userProfile = MutableStateFlow(UserProfile())
  val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

  // Emergency Contacts
  private val _emergencyContacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
  val emergencyContacts: StateFlow<List<EmergencyContact>> = _emergencyContacts.asStateFlow()

  // Alert Threshold (%BAC)
  private val _bacThreshold = MutableStateFlow(0.05)
  val bacThreshold: StateFlow<Double> = _bacThreshold.asStateFlow()

  // Active Alert Event (if threshold exceeded)
  private val _activeAlert = MutableStateFlow<AlertEvent?>(null)
  val activeAlert: StateFlow<AlertEvent?> = _activeAlert.asStateFlow()

  // All alerts history
  private val _alertsHistory = MutableStateFlow<List<AlertEvent>>(emptyList())
  val alertsHistory: StateFlow<List<AlertEvent>> = _alertsHistory.asStateFlow()

  // AI Insights
  private val _aiInsights = MutableStateFlow<List<AiInsightCard>>(emptyList())
  val aiInsights: StateFlow<List<AiInsightCard>> = _aiInsights.asStateFlow()

  // Reports
  private val _reports = MutableStateFlow<List<HealthReport>>(emptyList())
  val reports: StateFlow<List<HealthReport>> = _reports.asStateFlow()

  // Historical readings from Room DB
  private val _recentReadingsList = MutableStateFlow<List<SensorReading>>(emptyList())
  val recentReadingsList: StateFlow<List<SensorReading>> = _recentReadingsList.asStateFlow()

  init {
    // 1. Sync User Profile from Firebase
    auth.addAuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            firestore.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        _userProfile.value = UserProfile(
                            name = snapshot.getString("name") ?: "",
                            email = snapshot.getString("email") ?: user.email ?: "",
                            age = snapshot.getLong("age")?.toInt() ?: 0,
                            gender = snapshot.getString("gender") ?: "",
                            weightKg = snapshot.getDouble("weightKg") ?: 0.0,
                            heightCm = snapshot.getDouble("heightCm") ?: 0.0,
                            bloodGroup = snapshot.getString("bloodGroup") ?: "",
                            photoUri = snapshot.getString("photoUri"),
                            isLoggedIn = true
                        )
                    } else {
                        // User exists in Auth but not in Firestore yet
                        _userProfile.value = UserProfile(email = user.email ?: "", isLoggedIn = true)
                    }
                }
        } else {
            _userProfile.value = UserProfile(isLoggedIn = false)
        }
    }

    // 2. Sync Emergency Contacts from Firestore
    auth.addAuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            firestore.collection("users").document(user.uid).collection("contacts")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        _emergencyContacts.value = snapshot.documents.map { doc ->
                            EmergencyContact(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                phone = doc.getString("phone") ?: "",
                                relationship = doc.getString("relationship") ?: "",
                                email = doc.getString("email") ?: "",
                                alertEnabled = doc.getBoolean("alertEnabled") ?: true
                            )
                        }
                    }
                }
        } else {
            _emergencyContacts.value = emptyList()
        }
    }

    // 3. Sync Alerts History from Firestore
    auth.addAuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            firestore.collection("users").document(user.uid).collection("alerts")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        _alertsHistory.value = snapshot.documents.map { doc ->
                            AlertEvent(
                                id = doc.id,
                                timestamp = doc.getLong("timestamp") ?: 0L,
                                bacLevel = doc.getDouble("bacLevel") ?: 0.0,
                                title = doc.getString("title") ?: "",
                                message = doc.getString("message") ?: "",
                                recommendations = (doc.get("recommendations") as? List<String>) ?: emptyList(),
                                isAcknowledged = doc.getBoolean("isAcknowledged") ?: false
                            )
                        }
                    }
                }
        } else {
            _alertsHistory.value = emptyList()
        }
    }

    // 4. Sync Sensor Readings from Firestore
    auth.addAuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            firestore.collection("users").document(user.uid).collection("readings")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        _recentReadingsList.value = snapshot.documents.map { doc ->
                            SensorReading(
                                alcoholBac = doc.getDouble("alcoholBac") ?: 0.0,
                                heartRateBpm = doc.getLong("heartRateBpm")?.toInt() ?: 0,
                                spo2Percent = doc.getLong("spo2Percent")?.toInt() ?: 0,
                                tempCelsius = doc.getDouble("tempCelsius") ?: 0.0,
                                ecgStatus = doc.getString("ecgStatus") ?: "Stable",
                                timestamp = doc.getLong("timestamp") ?: 0L,
                                overallHealthScore = doc.getLong("overallHealthScore")?.toInt() ?: 0,
                                isBleConnected = true
                            )
                        }
                    }
                }
        } else {
            _recentReadingsList.value = emptyList()
        }
    }

    // 6. Monitor live data and save to Firestore
    scope.launch {
      currentReading.collect { reading ->
        val user = auth.currentUser
        if (isMonitoring.value && isConnected.value) {
            try {
              // Save to Local DB (Offline History)
              sensorDao.insertReading(SensorReadingEntity(
                  timestamp = reading.timestamp,
                  alcoholBac = reading.alcoholBac,
                  heartRateBpm = reading.heartRateBpm,
                  spo2Percent = reading.spo2Percent,
                  tempCelsius = reading.tempCelsius,
                  ecgStatus = reading.ecgStatus,
                  overallHealthScore = reading.overallHealthScore
              ))

              // Save to Firestore (Cloud Sync)
              if (user != null) {
                  val data = hashMapOf(
                      "timestamp" to reading.timestamp,
                      "alcoholBac" to reading.alcoholBac,
                      "heartRateBpm" to reading.heartRateBpm,
                      "spo2Percent" to reading.spo2Percent,
                      "tempCelsius" to reading.tempCelsius,
                      "ecgStatus" to reading.ecgStatus,
                      "overallHealthScore" to reading.overallHealthScore
                  )
                  firestore.collection("users").document(user.uid).collection("readings").add(data)
              }
            } catch (_: Exception) {}
        }

        if (reading.alcoholBac >= _bacThreshold.value) {
          triggerBacWarning(reading.alcoholBac)
        }
      }
    }

    scope.launch {
      refreshAiInsights()
    }

    // 7. Observe local Health Reports
    scope.launch {
        reportDao.getAllReports().collect { entities ->
            _reports.value = entities.map { 
                HealthReport(
                    it.reportId, it.dateRangeLabel, it.generatedAt,
                    it.highestBac, it.averageHeartRate, it.averageSpO2,
                    it.averageTemperature, it.totalReadingsCount,
                    it.alertCount, it.wellnessScore
                )
            }
        }
    }

    // 8. Observe local Sensor Readings (Offline History)
    scope.launch {
        sensorDao.getRecentReadingsFlow().collect { entities ->
            if (entities.isNotEmpty()) {
                val localReadings = entities.map { 
                    SensorReading(
                        alcoholBac = it.alcoholBac,
                        heartRateBpm = it.heartRateBpm,
                        spo2Percent = it.spo2Percent,
                        tempCelsius = it.tempCelsius,
                        ecgStatus = it.ecgStatus,
                        timestamp = it.timestamp,
                        overallHealthScore = it.overallHealthScore,
                        isBleConnected = true
                    )
                }
                // Update list if local data is more recent or if Firestore list is empty
                _recentReadingsList.value = localReadings
            }
        }
    }
  }

  fun startBleScan() = bleSimulator.startBleScan()
  fun connectToDevice(device: BleDevice) = bleSimulator.connectToDevice(device)
  fun disconnectDevice() = bleSimulator.disconnectDevice()
  fun pauseMonitoring() = bleSimulator.pauseMonitoring()
  fun resumeMonitoring() = bleSimulator.resumeMonitoring()
  fun simulateHighAlcoholAlert() = bleSimulator.simulateHighAlcoholAlert()

  fun triggerBacWarning(bacLevel: Double) {
    val alert = AlertEvent(
      id = "alert_${System.currentTimeMillis()}",
      timestamp = System.currentTimeMillis(),
      bacLevel = bacLevel,
      title = "Alcohol Threshold Exceeded (${String.format("%.2f", bacLevel)} %BAC)",
      message = "Alcohol level is high. Reaction time is impaired.",
      recommendations = listOf(
        "Drink at least 500ml of water immediately.",
        "Avoid driving or operating machinery.",
        "Rest before consuming any more alcohol.",
        "Notify emergency contact if feeling dizzy or unwell."
      ),
      isAcknowledged = false
    )
    _activeAlert.value = alert
    val user = auth.currentUser
    if (user != null) {
        firestore.collection("users").document(user.uid).collection("alerts").add(alert)
    }
  }

  fun dismissActiveAlert() {
    _activeAlert.value = null
  }

  fun setBacThreshold(newThreshold: Double) {
    _bacThreshold.value = newThreshold
  }

  fun updateProfile(newProfile: UserProfile) {
    _userProfile.value = newProfile
    val user = auth.currentUser
    if (user != null) {
        val data = hashMapOf(
            "name" to newProfile.name,
            "email" to newProfile.email,
            "age" to newProfile.age,
            "gender" to newProfile.gender,
            "weightKg" to newProfile.weightKg,
            "heightCm" to newProfile.heightCm,
            "bloodGroup" to newProfile.bloodGroup,
            "photoUri" to newProfile.photoUri
        )
        firestore.collection("users").document(user.uid).set(data, com.google.firebase.firestore.SetOptions.merge())
    }
  }

  fun addEmergencyContact(name: String, phone: String, relationship: String, email: String) {
    val contact = EmergencyContact(
      id = "contact_${System.currentTimeMillis()}",
      name = name,
      phone = phone,
      relationship = relationship,
      email = email,
      alertEnabled = true
    )
    val user = auth.currentUser
    if (user != null) {
        firestore.collection("users").document(user.uid).collection("contacts").add(contact)
    }
  }

  fun removeEmergencyContact(contactId: String) {
    val user = auth.currentUser
    if (user != null) {
        firestore.collection("users").document(user.uid).collection("contacts").document(contactId).delete()
    }
  }

  suspend fun refreshAiInsights() {
    try {
      val insights = aiInsightsService.generateInsights(
        userProfile = _userProfile.value,
        currentReading = currentReading.value,
        recentReadings = _recentReadingsList.value
      )
      _aiInsights.value = insights
    } catch (_: Exception) {}
  }

  suspend fun generateNewReport(): HealthReport = kotlinx.coroutines.withContext(Dispatchers.IO) {
    val recent = _recentReadingsList.value
    val current = currentReading.value
    
    val highestBac = if (recent.isNotEmpty()) recent.maxOf { it.alcoholBac } else current.alcoholBac
    val avgBpm = if (recent.isNotEmpty()) recent.map { it.heartRateBpm }.average().toInt() else current.heartRateBpm
    val avgSpo2 = if (recent.isNotEmpty()) recent.map { it.spo2Percent }.average().toInt() else current.spo2Percent
    val avgTemp = if (recent.isNotEmpty()) recent.map { it.tempCelsius }.average() else current.tempCelsius

    val reportId = "rep_${System.currentTimeMillis()}"
    val generatedAt = System.currentTimeMillis()
    val dateLabel = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.US).format(java.util.Date(generatedAt))
    
    val newReport = HealthReport(
      reportId = reportId,
      dateRangeLabel = "Detection Report: $dateLabel",
      generatedAt = generatedAt,
      highestBac = highestBac,
      averageHeartRate = avgBpm,
      averageSpO2 = avgSpo2,
      averageTemperature = avgTemp,
      totalReadingsCount = recent.size.coerceAtLeast(1),
      alertCount = _alertsHistory.value.size,
      wellnessScore = current.overallHealthScore
    )
    
    try {
        reportDao.insertReport(HealthReportEntity(
            newReport.reportId, newReport.dateRangeLabel, newReport.generatedAt,
            newReport.highestBac, newReport.averageHeartRate, newReport.averageSpO2,
            newReport.averageTemperature, newReport.totalReadingsCount,
            newReport.alertCount, newReport.wellnessScore
        ))
        android.util.Log.d("SoberWatch", "Report saved successfully: ${newReport.reportId}")
    } catch (e: Exception) {
        android.util.Log.e("SoberWatch", "Failed to save report", e)
    }
    
    newReport
  }

  private fun seedInitialContacts() {
      // scope.launch {
      //     contactDao.insertContact(EmergencyContactEntity("c1", "Dr. Elena Vance", "+1 (555) 349-2810", "Primary Physician", "elena.vance@clinic.org", true))
      //     contactDao.insertContact(EmergencyContactEntity("c2", "Marcus Rivers", "+1 (555) 912-7483", "Brother / Family", "marcus.r@email.com", true))
      // }
  }
}
