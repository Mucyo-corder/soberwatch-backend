package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_readings")
data class SensorReadingEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  val timestamp: Long,
  val alcoholBac: Double,
  val heartRateBpm: Int,
  val spo2Percent: Int,
  val tempCelsius: Double,
  val ecgStatus: String,
  val overallHealthScore: Int
)
