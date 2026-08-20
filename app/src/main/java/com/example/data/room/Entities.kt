package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val email: String,
    val name: String,
    val password: String = "",
    val age: Int,
    val gender: String,
    val weightKg: Double,
    val heightCm: Double,
    val bloodGroup: String,
    val photoResId: Int,
    val photoUri: String?,
    val isLoggedIn: Boolean,
    val isEmailVerified: Boolean = false
)

@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val relationship: String,
    val email: String,
    val alertEnabled: Boolean
)

@Entity(tableName = "alert_events")
data class AlertEventEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val bacLevel: Double,
    val title: String,
    val message: String,
    val recommendations: List<String>,
    val isAcknowledged: Boolean
)

@Entity(tableName = "health_reports")
data class HealthReportEntity(
    @PrimaryKey val reportId: String,
    val dateRangeLabel: String,
    val generatedAt: Long,
    val highestBac: Double,
    val averageHeartRate: Int,
    val averageSpO2: Int,
    val averageTemperature: Double,
    val totalReadingsCount: Int,
    val alertCount: Int,
    val wellnessScore: Int
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String,
    val content: String,
    val audioPath: String?,
    val timestamp: Long
)
