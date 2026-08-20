package com.example.models

import com.example.R

data class UserProfile(
  val name: String = "",
  val email: String = "",
  val password: String = "",
  val age: Int = 0,
  val gender: String = "",
  val weightKg: Double = 0.0,
  val heightCm: Double = 0.0,
  val bloodGroup: String = "",
  val photoResId: Int = 0,
  val photoUri: String? = null,
  val isLoggedIn: Boolean = false,
  val isEmailVerified: Boolean = false,
  val healthScore: Int = 0
)
