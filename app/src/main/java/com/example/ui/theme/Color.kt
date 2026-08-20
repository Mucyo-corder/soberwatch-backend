package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Palette
val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightPrimary = Color(0xFF0D9488) // Emerald Teal
val LightOnPrimary = Color(0xFFFFFFFF)
val LightSecondary = Color(0xFF3B82F6) // Clinical Blue
val LightTertiary = Color(0xFF8B5CF6) // Royal Purple
val LightTextPrimary = Color(0xFF1F2937) // Charcoal
val LightTextSecondary = Color(0xFF6B7280) // Slate Gray
val LightBorder = Color(0xFFE5E7EB)

// Dark Theme Palette
val DarkBackground = Color(0xFF0F172A) // Deep Navy/Slate
val DarkSurface = Color(0xFF1E293B) // Slate 800
val DarkPrimary = Color(0xFF10B981) // Fresh Emerald
val DarkOnPrimary = Color(0xFF064E3B)
val DarkSecondary = Color(0xFF60A5FA) // Sky Blue
val DarkTertiary = Color(0xFFA78BFA) // Soft Purple
val DarkTextPrimary = Color(0xFFF9FAFB) // Ghost White
val DarkTextSecondary = Color(0xFF9CA3AF) // Cool Gray
val DarkBorder = Color(0xFF334155) // Slate 700

// Health Metric Semantic Colors (Readability optimized for both modes)
val MetricHeartRate = Color(0xFFEF4444)
val MetricSpO2 = Color(0xFF0EA5E9)
val MetricBac = Color(0xFFF59E0B)
val MetricTemp = Color(0xFFF97316)
val MetricEcg = Color(0xFF8B5CF6)

// Functional Colors
val SuccessGreen = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)

// Keeping old names for compatibility during migration if needed, but we should move to MaterialTheme
val ObsidianBg = DarkBackground
val BentoSurface = DarkSurface
val EmeraldPrimary = DarkPrimary
val TextPrimary = DarkTextPrimary
val TextMuted = DarkTextSecondary
val BentoCardBorder = DarkBorder
val BacBlue = DarkSecondary
val HeartRed = MetricHeartRate
val SpO2Green = DarkPrimary
val TempOrange = MetricTemp
val AlertRed = ErrorRed
