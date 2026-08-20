package com.example.models

data class AlertEvent(
  val id: String,
  val timestamp: Long = System.currentTimeMillis(),
  val bacLevel: Double,
  val title: String,
  val message: String,
  val recommendations: List<String>,
  val isAcknowledged: Boolean = false
)
