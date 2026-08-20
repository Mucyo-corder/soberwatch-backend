package com.example.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
  entities = [
    SensorReadingEntity::class,
    UserProfileEntity::class,
    EmergencyContactEntity::class,
    AlertEventEntity::class,
    HealthReportEntity::class,
    ChatMessageEntity::class
  ],
  version = 2,
  exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SoberWatchDatabase : RoomDatabase() {
  abstract fun sensorDao(): SensorDao
  abstract fun userProfileDao(): UserProfileDao
  abstract fun emergencyContactDao(): EmergencyContactDao
  abstract fun alertEventDao(): AlertEventDao
  abstract fun healthReportDao(): HealthReportDao
  abstract fun chatMessageDao(): ChatMessageDao

  companion object {
    @Volatile
    private var INSTANCE: SoberWatchDatabase? = null

    fun getDatabase(context: Context): SoberWatchDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance =
          Room.databaseBuilder(
            context.applicationContext,
            SoberWatchDatabase::class.java,
            "soberwatch_health_database"
          )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
