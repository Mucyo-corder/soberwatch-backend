package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.AppTheme
import com.example.data.preferences.PreferenceManager
import com.example.data.repository.SoberWatchRepository
import com.example.data.room.ChatMessageEntity
import com.example.data.room.SoberWatchDatabase
import com.example.models.*
import com.example.services.AiAssistantService
import com.example.services.AiInsightCard
import com.example.services.ChatMessage
import com.example.services.VoiceService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SoberWatchViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = SoberWatchRepository(application.applicationContext)
  private val aiAssistantService = AiAssistantService()
  private val voiceService = VoiceService(application.applicationContext)
  private val preferenceManager = PreferenceManager(application.applicationContext)
  
  private val auth = FirebaseAuth.getInstance()
  private val chatDao = SoberWatchDatabase.getDatabase(application).chatMessageDao()

  val appTheme: StateFlow<AppTheme> = preferenceManager.themeFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.LIGHT)

  val soundAlerts: StateFlow<Boolean> = preferenceManager.soundAlertsFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  val vibrationAlerts: StateFlow<Boolean> = preferenceManager.vibrationAlertsFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  val unitCelsius: StateFlow<Boolean> = preferenceManager.unitCelsiusFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  val autoSync: StateFlow<Boolean> = preferenceManager.autoSyncFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  val currentReading: StateFlow<SensorReading> = repository.currentReading
  val isConnected: StateFlow<Boolean> = repository.isConnected
  val isMonitoring: StateFlow<Boolean> = repository.isMonitoring
  val connectedDevice: StateFlow<BleDevice?> = repository.connectedDevice
  val availableDevices: StateFlow<List<BleDevice>> = repository.availableDevices
  val isScanning: StateFlow<Boolean> = repository.isScanning
  val userProfile: StateFlow<UserProfile> = repository.userProfile
  val emergencyContacts: StateFlow<List<EmergencyContact>> = repository.emergencyContacts
  val bacThreshold: StateFlow<Double> = repository.bacThreshold
  val activeAlert: StateFlow<AlertEvent?> = repository.activeAlert
  val alertsHistory: StateFlow<List<AlertEvent>> = repository.alertsHistory
  val aiInsights: StateFlow<List<AiInsightCard>> = repository.aiInsights
  val reports: StateFlow<List<HealthReport>> = repository.reports
  val recentReadingsList: StateFlow<List<SensorReading>> = repository.recentReadingsList

  val isVoiceListening: StateFlow<Boolean> = voiceService.isListening
  val isRecordingNote: StateFlow<Boolean> = voiceService.isRecordingNote
  val spokenText: StateFlow<String> = voiceService.spokenText

  private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
  val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

  init {
      // Load chat history from DB
      viewModelScope.launch {
          chatDao.getAllMessages().collect { entities ->
              if (entities.isEmpty()) {
                  // Seed initial message if empty
                  val initial = ChatMessageEntity(role = "model", content = "Hello! I am SoberWatch AI. How can I help you today?", audioPath = null, timestamp = System.currentTimeMillis())
                  chatDao.insertMessage(initial)
              } else {
                  _chatMessages.value = entities.map { 
                      ChatMessage(it.role, it.content, it.audioPath, it.timestamp)
                  }
              }
          }
      }
  }

  // Simulated Auth status
  private val _authError = MutableStateFlow<String?>(null)
  val authError: StateFlow<String?> = _authError.asStateFlow()

  private val _emailVerificationSent = MutableStateFlow(false)
  val emailVerificationSent: StateFlow<Boolean> = _emailVerificationSent.asStateFlow()

  private val _passwordResetSent = MutableStateFlow(false)
  val passwordResetSent: StateFlow<Boolean> = _passwordResetSent.asStateFlow()

  fun startBleScan() {
    repository.startBleScan()
  }

  fun connectToDevice(device: BleDevice) {
    repository.connectToDevice(device)
  }

  fun disconnectDevice() {
    repository.disconnectDevice()
  }

  fun pauseMonitoring() {
    repository.pauseMonitoring()
  }

  fun resumeMonitoring() {
    repository.resumeMonitoring()
  }

  fun simulateHighAlcoholAlert() {
    repository.simulateHighAlcoholAlert()
  }

  fun dismissActiveAlert() {
    repository.dismissActiveAlert()
  }

  fun setTheme(theme: AppTheme) {
    viewModelScope.launch {
      preferenceManager.setTheme(theme)
    }
  }

  fun setSoundAlerts(enabled: Boolean) {
    viewModelScope.launch { preferenceManager.setSoundAlerts(enabled) }
  }

  fun setVibrationAlerts(enabled: Boolean) {
    viewModelScope.launch { preferenceManager.setVibrationAlerts(enabled) }
  }

  fun setUnitCelsius(enabled: Boolean) {
    viewModelScope.launch { preferenceManager.setUnitCelsius(enabled) }
  }

  fun setAutoSync(enabled: Boolean) {
    viewModelScope.launch { preferenceManager.setAutoSync(enabled) }
  }

  fun setBacThreshold(newThreshold: Double) {
    repository.setBacThreshold(newThreshold)
  }

  fun updateProfile(newProfile: UserProfile) {
    repository.updateProfile(newProfile)
  }

  fun addEmergencyContact(name: String, phone: String, relationship: String, email: String) {
    repository.addEmergencyContact(name, phone, relationship, email)
  }

  fun removeEmergencyContact(id: String) {
    repository.removeEmergencyContact(id)
  }

  fun refreshAiInsights() {
    viewModelScope.launch {
      repository.refreshAiInsights()
    }
  }

  fun sendMessageToAi(message: String) {
    if (message.isBlank()) return
    
    viewModelScope.launch {
      try {
          val userMsg = ChatMessageEntity(role = "user", content = message, audioPath = null, timestamp = System.currentTimeMillis())
          chatDao.insertMessage(userMsg)

          val response = aiAssistantService.getClinicalResponse(
            message,
            userProfile.value,
            currentReading.value
          )
          
          val modelMsg = ChatMessageEntity(role = "model", content = response, audioPath = null, timestamp = System.currentTimeMillis())
          chatDao.insertMessage(modelMsg)
          voiceService.speak(response)
      } catch (e: Throwable) {
          // Prevent crash if AI service fails (catch all Throwables including Errors)
          try {
              val errorMsg = ChatMessageEntity(role = "model", content = "I'm having trouble connecting to my clinical database. Please check your internet.", audioPath = null, timestamp = System.currentTimeMillis())
              chatDao.insertMessage(errorMsg)
          } catch (_: Throwable) {}
      }
    }
  }

  fun startVoiceAssistant() {
    voiceService.startListening()
  }

  fun stopVoiceAssistant() {
    voiceService.stopListening()
  }

  fun startRecordingVoiceNote() {
    voiceService.startRecordingNote()
  }

  fun stopAndSendVoiceNote() {
    val path = voiceService.stopRecordingNote()
    if (path != null) {
      viewModelScope.launch {
        val userMsg = ChatMessageEntity(role = "user", content = "Voice Note Sent", audioPath = path, timestamp = System.currentTimeMillis())
        chatDao.insertMessage(userMsg)

        val simulatedTranscription = "How is my heart rate and BAC?" 
        val response = aiAssistantService.getClinicalResponse(
          "I've analyzed your voice note: '$simulatedTranscription'. " + 
          "Based on your live telemetry context (BAC: ${currentReading.value.alcoholBac}%, HR: ${currentReading.value.heartRateBpm} BPM), your condition appears stable. Continue monitoring.",
          userProfile.value,
          currentReading.value
        )
        
        val modelMsg = ChatMessageEntity(role = "model", content = response, audioPath = null, timestamp = System.currentTimeMillis())
        chatDao.insertMessage(modelMsg)
        voiceService.speak(response)
      }
    }
  }

  fun playVoiceNote(path: String) {
    voiceService.playAudio(path)
  }

  override fun onCleared() {
    super.onCleared()
    voiceService.shutdown()
  }

  fun generateNewReport(): HealthReport {
    return repository.generateNewReport()
  }

  fun isUserAuthenticated(): Boolean {
    return auth.currentUser != null
  }

  // Firebase Authentication
  fun login(email: String, password: String, onSuccess: () -> Unit) {
    if (email.isBlank() || password.isBlank()) {
      _authError.value = "Please enter valid email and password."
      return
    }

    viewModelScope.launch {
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                _authError.value = null
                onSuccess()
            }
        } catch (e: Exception) {
            _authError.value = e.localizedMessage ?: "Login failed."
        }
    }
  }

  fun register(name: String, email: String, password: String, onSuccess: () -> Unit) {
    if (name.isBlank() || email.isBlank() || password.length < 6) {
      _authError.value = "Name required. Password must be at least 6 characters."
      return
    }
    
    viewModelScope.launch {
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                // Create user profile in Firestore
                repository.updateProfile(UserProfile(name = name, email = email))
                
                _authError.value = null
                onSuccess()
            }
        } catch (e: Exception) {
            _authError.value = e.localizedMessage ?: "Registration failed."
        }
    }
  }

  fun quickRegister(onSuccess: () -> Unit) {
      // For Quick Scan, we'll use an anonymous account or a fixed guest account
      viewModelScope.launch {
          try {
              auth.signInAnonymously().await()
              onSuccess()
          } catch (e: Exception) {
              _authError.value = "Quick access failed: ${e.localizedMessage}"
          }
      }
  }

  fun googleLogin(onSuccess: () -> Unit) {
    // Requires standard Google Sign-In setup, for now we can simulate or provide a note
    _authError.value = "Google Login is being configured. Please use Email for now."
  }

  fun sendPasswordResetEmail(email: String) {
    if (email.isBlank()) {
      _authError.value = "Please enter your registered email address."
      return
    }
    viewModelScope.launch {
        try {
            auth.sendPasswordResetEmail(email).await()
            _authError.value = null
            _passwordResetSent.value = true
        } catch (e: Exception) {
            _authError.value = e.localizedMessage
        }
    }
  }

  fun clearAuthError() {
      _authError.value = null
      _passwordResetSent.value = false
  }

  fun verifyEmail() {
    // Handled by Firebase background process
  }

  fun logout(onLoggedOut: () -> Unit) {
    auth.signOut()
    repository.disconnectDevice()
    onLoggedOut()
  }
}
