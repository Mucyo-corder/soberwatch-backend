package com.example.models

data class BleDevice(
  val id: String,
  val name: String,
  val macAddress: String,
  val rssi: Int, // e.g. -58
  val batteryLevel: Int, // e.g. 84
  val firmwareVersion: String, // e.g. v2.4.1-S3
  val isConnected: Boolean,
  val isSoberWatch: Boolean = true
)
