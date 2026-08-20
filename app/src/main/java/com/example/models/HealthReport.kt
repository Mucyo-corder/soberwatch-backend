package com.example.models

data class HealthReport(
  val reportId: String,
  val dateRangeLabel: String,
  val generatedAt: Long = System.currentTimeMillis(),
  val highestBac: Double,
  val averageHeartRate: Int,
  val averageSpO2: Int,
  val averageTemperature: Double,
  val totalReadingsCount: Int,
  val alertCount: Int,
  val wellnessScore: Int
)
