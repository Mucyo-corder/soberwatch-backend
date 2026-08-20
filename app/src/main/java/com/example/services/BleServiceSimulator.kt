package com.example.services

import com.example.models.BleDevice
import com.example.models.SensorReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

class BleServiceSimulator {

  private val _isConnected = MutableStateFlow(false)
  val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

  private val _isMonitoring = MutableStateFlow(false)
  val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

  private val _connectedDevice = MutableStateFlow<BleDevice?>(null)
  val connectedDevice: StateFlow<BleDevice?> = _connectedDevice.asStateFlow()

  private val _availableDevices = MutableStateFlow<List<BleDevice>>(emptyList())
  val availableDevices: StateFlow<List<BleDevice>> = _availableDevices.asStateFlow()

  private val _currentReading = MutableStateFlow(SensorReading())
  val currentReading: StateFlow<SensorReading> = _currentReading.asStateFlow()

  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private var monitoringJob: Job? = null
  private val scope = CoroutineScope(Dispatchers.Default)
  
  private var isHighBacSimulated = false

  init {
    startSensorStream()
  }

  fun startSensorStream() {
    monitoringJob?.cancel()
    _isMonitoring.value = true
    monitoringJob = scope.launch {
      var counter = 0
      while (true) {
        if (_isMonitoring.value && _isConnected.value) {
          counter++
          
          // Data Generation Logic
          val baseBac = if (isHighBacSimulated) 0.08 else 0.00
          val randomBac = Random.nextDouble(0.00, 0.02)
          val newBac = roundTo2Decimals(baseBac + randomBac)
          
          val baseBpm = if (newBac > 0.05) 92 else 72
          val newBpm = baseBpm + Random.nextInt(-4, 8)
          
          val newSpo2 = if (newBac > 0.10) Random.nextInt(92, 96) else Random.nextInt(96, 100)
          val newTemp = roundTo1Decimal(Random.nextDouble(36.4, 37.2))
          
          // Generate a lively ECG waveform slice
          val newWaveform = buildList {
            for (i in 0 until 30) {
              val t = (counter * 5 + i) * 0.3f
              val base = 20f + 5f * sin(t)
              // Add a larger QRS spike if heart rate is high
              val interval = if (newBpm > 90) 10 else 15
              val spike = if (i % interval == 7) 32f else if (i % interval == 8) -18f else 0f
              add(base + spike)
            }
          }

          val score = calculateHealthScore(newBac, newBpm, newSpo2, newTemp)

          _currentReading.value = SensorReading(
            alcoholBac = newBac,
            heartRateBpm = newBpm,
            spo2Percent = newSpo2,
            tempCelsius = newTemp,
            ecgStatus = if (newBac > 0.08) "Tachycardia Detected" else "Normal Sinus Rhythm",
            timestamp = System.currentTimeMillis(),
            ecgWaveform = newWaveform,
            batteryPercent = (_connectedDevice.value?.batteryLevel ?: 84),
            rssi = (_connectedDevice.value?.rssi ?: -58) + Random.nextInt(-2, 3),
            isBleConnected = true,
            isWifiConnected = true,
            overallHealthScore = score
          )
        }
        delay(1000L)
      }
    }
  }

  fun pauseMonitoring() {
    _isMonitoring.value = false
  }

  fun resumeMonitoring() {
    _isMonitoring.value = true
  }

  fun disconnectDevice() {
    _isConnected.value = false
    _isMonitoring.value = false
    _connectedDevice.value = null
  }

  fun connectToDevice(device: BleDevice) {
    val updatedDevice = device.copy(isConnected = true)
    _connectedDevice.value = updatedDevice
    _isConnected.value = true
    _isMonitoring.value = true
    isHighBacSimulated = false
  }

  fun startBleScan() {
    scope.launch {
      _isScanning.value = true
      delay(1500L)
      _availableDevices.value = listOf(
        BleDevice("ESP32_S3_01", "SoberWatch_S3", "E8:9F:6D:3A:4C:12", -58, 84, "v2.4.1-S3", isConnected = _connectedDevice.value?.id == "ESP32_S3_01"),
        BleDevice("ESP32_PRO_02", "SoberWatch_Pro_ESP32", "C4:4F:33:1B:90:88", -66, 92, "v2.5.0-S3", isConnected = false),
        BleDevice("SOBER_BAND_03", "SoberBand_v2", "A2:18:7E:5D:11:4A", -74, 67, "v1.8.2", isConnected = false)
      )
      _isScanning.value = false
    }
  }

  fun simulateHighAlcoholAlert() {
    isHighBacSimulated = true
  }

  private fun calculateHealthScore(bac: Double, bpm: Int, spo2: Int, temp: Double): Int {
    var score = 100
    if (bac > 0.08) score -= 40 else if (bac > 0.05) score -= 25 else if (bac > 0.02) score -= 10
    if (bpm > 100 || bpm < 50) score -= 15 else if (bpm > 90) score -= 5
    if (spo2 < 94) score -= 20 else if (spo2 < 97) score -= 5
    if (temp > 37.8 || temp < 35.8) score -= 15
    return score.coerceIn(0, 100)
  }

  private fun roundTo2Decimals(valNum: Double): Double {
    return Math.round(valNum * 100.0) / 100.0
  }

  private fun roundTo1Decimal(valNum: Double): Double {
    return Math.round(valNum * 10.0) / 10.0
  }
}
