package com.example.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.AlertEvent
import com.example.models.SensorReading
import com.example.models.UserProfile
import com.example.services.AiInsightCard
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun HomeScreen(
  userProfile: UserProfile,
  currentReading: SensorReading,
  isConnected: Boolean,
  activeAlert: AlertEvent?,
  insights: List<AiInsightCard>,
  alertsHistory: List<AlertEvent>,
  recentReadings: List<SensorReading>,
  onDismissAlert: () -> Unit,
  onNotifyEmergencyContact: () -> Unit,
  onNavigateToDevice: () -> Unit,
  onNavigateToMonitor: () -> Unit,
  onNavigateToInsights: () -> Unit,
  onNavigateToAlerts: () -> Unit,
  onNavigateToProfile: () -> Unit,
  onSimulateHighBac: () -> Unit,
  onSaveReport: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

  val bacLevelText = String.format("%.2f", currentReading.alcoholBac)
  val isHighBac = currentReading.alcoholBac > 0.05
  
  val bacStatus = when {
    isHighBac -> "High"
    currentReading.alcoholBac > 0.02 -> "Elevated"
    else -> "Sober"
  }
  
  val bpmStatus = when {
    currentReading.heartRateBpm > 95 -> "High"
    currentReading.heartRateBpm < 58 -> "Low"
    else -> "Normal"
  }

  val spo2Status = if (currentReading.spo2Percent >= 97) "Excellent" else "Normal"
  val tempStatus = if (currentReading.tempCelsius in 36.1..37.4) "Normal" else "Elevated"

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(scrollState)
  ) {
    // Header
    TopBarWithProfile(
      userName = userProfile.name,
      isBleConnected = isConnected,
      avatarResId = userProfile.photoResId,
      avatarUri = userProfile.photoUri,
      onAvatarClick = onNavigateToProfile
    )

    Column(
      modifier = Modifier.padding(horizontal = 24.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      // Alert Banner if threshold exceeded
      if (activeAlert != null) {
          AlertBannerModal(
            alert = activeAlert,
            onDismiss = onDismissAlert,
            onNotifyEmergencyContact = onNotifyEmergencyContact
          )
      }

      // BLE Connectivity Bar
      BleConnectivityBar(
        batteryLevel = currentReading.batteryPercent,
        deviceName = "SoberWatch Pro",
        isConnected = isConnected
      )

      // Main Health Score
      HealthScoreHero(score = currentReading.overallHealthScore)

      // CRITICAL AI ANALYSIS FORM (Appears when High BAC is detected)
      if (isHighBac) {
          HighAlcoholAiForm(
              bacLevel = currentReading.alcoholBac,
              insights = insights.filter { it.category.contains("ALCOHOL") }
          )
      }

      // AI Clinical Advice Section (The "Information after detection")
      if (insights.isNotEmpty() || alertsHistory.isNotEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(
                  text = "CLINICAL ADVISORY",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.2.sp
              )
              
              LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(16.dp),
                  contentPadding = PaddingValues(end = 24.dp)
              ) {
                  items(insights) { insight ->
                      Surface(
                          modifier = Modifier.width(300.dp),
                          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                          shape = RoundedCornerShape(24.dp),
                          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                      ) {
                          Row(
                              modifier = Modifier.padding(20.dp),
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.spacedBy(16.dp)
                          ) {
                              Box(
                                  modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                  contentAlignment = Alignment.Center
                              ) {
                                  Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                              }
                              Column(modifier = Modifier.weight(1f)) {
                                  Text(insight.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                  Spacer(modifier = Modifier.height(2.dp))
                                  Text(insight.recommendation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                              }
                          }
                      }
                  }
              }
              
              if (currentReading.alcoholBac > 0.05) {
                  Surface(
                      color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                      shape = RoundedCornerShape(24.dp),
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                  ) {
                      Row(
                          modifier = Modifier.padding(20.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(16.dp)
                      ) {
                          Box(
                              modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error),
                              contentAlignment = Alignment.Center
                          ) {
                              Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(20.dp))
                          }
                          Column {
                              Text("CRITICAL DETECTION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                              Text("High Alcohol Level Detected", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                              Text("Your current BAC is ${bacLevelText}%. Avoid driving.", style = MaterialTheme.typography.bodySmall)
                          }
                      }
                  }
              }
          }
      }

      Text(
        text = "DETECTION REPORT",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp
      )

      DetectionReportForm(currentReading, bacLevelText, bacStatus, bpmStatus, spo2Status, tempStatus, onSaveReport)

      Text(
        text = "TREND ANALYSIS",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp
      )

      TrendAnalysisSection(recentReadings, currentReading)

      Text(
        text = "LATEST BIOMETRICS",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp
      )

      // Metrics Grid
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        BentoCard(
          title = "BAC",
          value = bacLevelText,
          unit = "%",
          badgeText = bacStatus,
          badgeColor = MetricBac,
          icon = Icons.Default.LocalDrink,
          onClick = onNavigateToMonitor,
          modifier = Modifier.weight(1f)
        )

        BentoCard(
          title = "Heart Rate",
          value = "${currentReading.heartRateBpm}",
          unit = "bpm",
          badgeText = bpmStatus,
          badgeColor = MetricHeartRate,
          icon = Icons.Default.Favorite,
          onClick = onNavigateToMonitor,
          modifier = Modifier.weight(1f)
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        BentoCard(
          title = "SpO2",
          value = "${currentReading.spo2Percent}",
          unit = "%",
          badgeText = spo2Status,
          badgeColor = MetricSpO2,
          icon = Icons.Default.Air,
          onClick = onNavigateToMonitor,
          modifier = Modifier.weight(1f)
        )

        BentoCard(
          title = "Temp",
          value = "${currentReading.tempCelsius}",
          unit = "°C",
          badgeText = tempStatus,
          badgeColor = MetricTemp,
          icon = Icons.Default.Thermostat,
          onClick = onNavigateToMonitor,
          modifier = Modifier.weight(1f)
        )
      }

      // Quick Actions
      Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(
              1.dp, 
              MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
          )
      ) {
          Column(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(20.dp)
          ) {
              Text(
                  text = "QUICK ACTIONS",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold
              )
              
              Spacer(modifier = Modifier.height(12.dp))
              
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                  Button(
                      onClick = onNavigateToInsights,
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(16.dp),
                      colors = ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.primaryContainer,
                          contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                      )
                  ) {
                      Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("ORACLE AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                  
                  OutlinedButton(
                      onClick = onNavigateToDevice,
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(16.dp)
                  ) {
                      Icon(Icons.Default.SettingsInputAntenna, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("DEVICES", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
              }
          }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(onClick = onSimulateHighBac) {
          Text(
            text = "Simulate High BAC",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
          )
        }

        TextButton(onClick = onNavigateToAlerts) {
          Text(
            text = "View Alerts",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}

@Composable
fun DetectionReportForm(
    reading: SensorReading,
    bacText: String,
    bacStatus: String,
    bpmStatus: String,
    spo2Status: String,
    tempStatus: String,
    onSaveReport: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DetectionRow(label = "BAC Level", value = "$bacText%", status = bacStatus, color = MetricBac)
            HorizontalDivider(modifier = Modifier.alpha(0.5f))
            DetectionRow(label = "Heart Rate", value = "${reading.heartRateBpm} BPM", status = bpmStatus, color = MetricHeartRate)
            HorizontalDivider(modifier = Modifier.alpha(0.5f))
            DetectionRow(label = "SpO2", value = "${reading.spo2Percent}%", status = spo2Status, color = MetricSpO2)
            HorizontalDivider(modifier = Modifier.alpha(0.5f))
            DetectionRow(label = "Temperature", value = "${reading.tempCelsius}°C", status = tempStatus, color = MetricTemp)
            HorizontalDivider(modifier = Modifier.alpha(0.5f))
            DetectionRow(label = "ECG Status", value = reading.ecgStatus, status = "Stable", color = Color(0xFF8B5CF6))
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onSaveReport,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SAVE DETECTION REPORT")
        }
    }
}

@Composable
fun DetectionRow(label: String, value: String, status: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
        ) {
            Text(
                text = status.uppercase(),
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun TrendAnalysisSection(recentReadings: List<SensorReading>, currentReading: SensorReading) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val bacValues = recentReadings.map { it.alcoholBac.toFloat() }.ifEmpty { listOf(0.02f, 0.01f) }
        
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = MetricBac, modifier = Modifier.size(18.dp))
                        Text("BAC Distribution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text("${String.format("%.2f", currentReading.alcoholBac)}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MetricBac)
                }
                Spacer(modifier = Modifier.height(16.dp))
                SimpleBarChart(dataPoints = bacValues, color = MetricBac)
            }
        }

        TrendChartCard(
            title = "Heart Rate Trend",
            valueText = "${currentReading.heartRateBpm} bpm",
            color = MetricHeartRate,
            icon = Icons.Default.Favorite,
            dataPoints = recentReadings.map { it.heartRateBpm.toFloat() }.ifEmpty { listOf(72f, 75f) },
            min = 40f,
            max = 120f
        )
    }
}

@Composable
fun HighAlcoholAiForm(bacLevel: Double, insights: List<AiInsightCard>) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
                Text(
                    text = "AI ANALYSIS: CRITICAL BAC",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Black
                )
            }

            Column {
                Text(
                    text = "Detected BAC Number:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "${String.format("%.2f", bacLevel)}%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.error
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AI CLINICAL ADVICE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                if (insights.isNotEmpty()) {
                    insights.forEach { insight ->
                        Text(
                            text = "• ${insight.recommendation}",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Text(
                        text = "• High alcohol toxicity detected. Do not operate machinery or drive. Hydrate immediately and rest in a lateral recovery position.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Button(
                onClick = { /* Emergency SOS logic */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Emergency, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("NOTIFY EMERGENCY CONTACTS", fontWeight = FontWeight.Bold)
            }
        }
    }
}
