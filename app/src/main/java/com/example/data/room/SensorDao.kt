package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReading(reading: SensorReadingEntity)

  @Query("SELECT * FROM sensor_readings ORDER BY timestamp DESC LIMIT 300")
  fun getRecentReadingsFlow(): Flow<List<SensorReadingEntity>>

  @Query("SELECT * FROM sensor_readings WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
  suspend fun getReadingsSince(sinceTimestamp: Long): List<SensorReadingEntity>

  @Query("SELECT MAX(alcoholBac) FROM sensor_readings WHERE timestamp >= :sinceTimestamp")
  suspend fun getHighestBacSince(sinceTimestamp: Long): Double?

  @Query("SELECT AVG(heartRateBpm) FROM sensor_readings WHERE timestamp >= :sinceTimestamp")
  suspend fun getAverageHeartRateSince(sinceTimestamp: Long): Double?

  @Query("SELECT AVG(spo2Percent) FROM sensor_readings WHERE timestamp >= :sinceTimestamp")
  suspend fun getAverageSpO2Since(sinceTimestamp: Long): Double?

  @Query("SELECT AVG(tempCelsius) FROM sensor_readings WHERE timestamp >= :sinceTimestamp")
  suspend fun getAverageTempSince(sinceTimestamp: Long): Double?

  @Query("SELECT COUNT(*) FROM sensor_readings")
  suspend fun getTotalCount(): Int
}
