package com.example.models

data class SensorReading(
  val alcoholBac: Double = 0.0,
  val heartRateBpm: Int = 0,
  val spo2Percent: Int = 0,
  val tempCelsius: Double = 0.0,
  val ecgStatus: String = "Waiting...",
  val timestamp: Long = System.currentTimeMillis(),
  val ecgWaveform: List<Float> = emptyList(),
  val batteryPercent: Int = 0,
  val rssi: Int = 0,
  val isBleConnected: Boolean = false,
  val isWifiConnected: Boolean = false,
  val overallHealthScore: Int = 0
) {
  companion object {
    fun defaultEcgWaveform(): List<Float> =
      listOf(
        20f, 20f, 22f, 20f, 18f, 20f, 15f, 5f, 35f, 10f, 20f, 20f, 21f, 23f, 20f, 20f, 20f, 20f,
        18f, 15f, 8f, 38f, 12f, 20f, 20f, 22f, 20f, 20f
      )
  }
}
