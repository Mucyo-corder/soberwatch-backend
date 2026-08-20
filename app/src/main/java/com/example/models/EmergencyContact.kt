package com.example.models

data class EmergencyContact(
  val id: String,
  val name: String,
  val phone: String,
  val relationship: String,
  val email: String,
  val alertEnabled: Boolean = true
)
