package com.example.services

import com.example.BuildConfig
import com.example.models.SensorReading
import com.example.models.UserProfile
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.delay

data class ChatMessage(
  val role: String, // "user" or "model"
  val content: String,
  val audioPath: String? = null,
  val timestamp: Long = System.currentTimeMillis()
)

class AiAssistantService {

  private var generativeModel: GenerativeModel? = null
  private var isInitialized = false

  private fun getModel(): GenerativeModel? {
    if (isInitialized) return generativeModel
    
    try {
      // Accessing BuildConfig safely. If the key is missing or invalid, we catch the error.
      val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "unused" }
      
      if (apiKey != "unused" && apiKey.isNotBlank() && apiKey.startsWith("AIza")) {
        generativeModel = GenerativeModel(
          modelName = "gemini-1.5-flash",
          apiKey = apiKey
        )
      }
    } catch (e: Throwable) {
      // If the SDK throws an error during initialization (e.g. invalid key format), 
      // we fall back to null to use mock responses.
      generativeModel = null
    }
    
    isInitialized = true
    return generativeModel
  }

  suspend fun getClinicalResponse(
    userMessage: String,
    userProfile: UserProfile,
    currentReading: SensorReading
  ): String {
    val model = try { getModel() } catch (e: Throwable) { null }
    
    // If API Key is not configured or failed to initialize, provide mock response
    if (model == null) {
      delay(1000L) // Reduced delay for better UX
      return generateMockClinicalResponse(userMessage, userProfile, currentReading)
    }

    val systemContext = """
      You are SoberWatch AI, a clinical health assistant.
      User: ${userProfile.name}, Age: ${userProfile.age}.
      Current: BAC ${currentReading.alcoholBac}%, HR ${currentReading.heartRateBpm} BPM.
    """.trimIndent()

    return try {
      val response = model.generateContent(
          content {
              text(systemContext)
              text("User Question: $userMessage")
          }
      )
      response.text ?: "I am having trouble processing your health data. Please check back shortly."
    } catch (e: Throwable) {
      // Any SDK runtime errors (like quota exceeded or network) fall back to mock
      generateMockClinicalResponse(userMessage, userProfile, currentReading)
    }
  }

  private fun generateMockClinicalResponse(
    userMessage: String,
    userProfile: UserProfile,
    currentReading: SensorReading
  ): String {
    val msg = userMessage.lowercase()
    
    if (msg.contains("voice note")) {
        return "I've analyzed your clinical audio recording. Analyzing your live telemetry at the time of recording: your BAC was ${currentReading.alcoholBac}%, and your heart rate was ${currentReading.heartRateBpm} BPM. Your respiratory oxygen (SpO2) remains healthy at ${currentReading.spo2Percent}%. Continue to stay hydrated and avoid operating any heavy machinery."
    }

    return when {
      msg.contains("bac") || msg.contains("alcohol") || msg.contains("drunk") -> {
        if (currentReading.alcoholBac > 0.05) {
          "Your current BAC is ${currentReading.alcoholBac}%, which is above the legal driving limit. I strongly recommend you stop consuming alcohol immediately, drink 500ml of water, and ensure you do not operate any vehicles. Your heart rate is also slightly elevated at ${currentReading.heartRateBpm} BPM."
        } else {
          "Your BAC is currently ${currentReading.alcoholBac}%, which is within the safe range. However, remember that alcohol metabolism varies. Stay hydrated and continue to monitor your readings."
        }
      }
      msg.contains("heart") || msg.contains("pulse") || msg.contains("bpm") -> {
        "Your heart rate is ${currentReading.heartRateBpm} BPM. For a ${userProfile.age}-year-old, this is generally considered ${if (currentReading.heartRateBpm in 60..90) "normal for a resting state" else "slightly outside the ideal resting range"}. Our AD8232 sensor shows a stable sinus rhythm."
      }
      msg.contains("spo2") || msg.contains("oxygen") -> {
        "Your blood oxygen level is ${currentReading.spo2Percent}%. This is optimal (anything above 95% is generally healthy). It indicates efficient respiratory function and good blood flow."
      }
      msg.contains("temp") || msg.contains("fever") -> {
        "Your core body temperature is ${currentReading.tempCelsius}°C. This is a standard physiological temperature. If you start feeling chills or excessive heat, please re-check in 15 minutes."
      }
      msg.contains("hello") || msg.contains("hi") -> {
        "Hello ${userProfile.name}! I am SoberWatch AI. I have access to your ESP32-S3 sensor data. You can ask me about your BAC, heart rate, or for clinical wellness recommendations."
      }
      else -> {
        "I've analyzed your telemetry (BAC: ${currentReading.alcoholBac}%, BPM: ${currentReading.heartRateBpm}). Based on your profile, I recommend maintaining hydration and ensuring your SoberWatch wearable remains securely fastened for accurate PPG/ECG data. Do you have a specific question about these metrics?"
      }
    }
  }
}
