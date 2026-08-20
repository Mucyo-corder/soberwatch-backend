package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AppTheme
import com.example.ui.screens.profile.ProfileSection

@Composable
fun SettingsScreen(
  bacThreshold: Double,
  onThresholdChanged: (Double) -> Unit,
  appTheme: AppTheme,
  onThemeChanged: (AppTheme) -> Unit,
  soundAlerts: Boolean,
  onSoundAlertsChanged: (Boolean) -> Unit,
  vibrationAlerts: Boolean,
  onVibrationAlertsChanged: (Boolean) -> Unit,
  unitCelsius: Boolean,
  onUnitCelsiusChanged: (Boolean) -> Unit,
  autoSync: Boolean,
  onAutoSyncChanged: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(scrollState)
        .padding(top = 20.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
  ) {
    // Header
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
      Text(
        text = "SYSTEM PREFERENCES",
        color = MaterialTheme.colorScheme.primary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
      )
      Text(
        text = "Settings",
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
      )
    }

    // Hardware Section
    ProfileSection(title = "Hardware") {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsInfoRow(
          icon = Icons.Default.Bluetooth,
          title = "Model",
          value = "SoberWatch Pro"
        )
        SettingsInfoRow(
          icon = Icons.Default.SystemUpdate,
          title = "Firmware",
          value = "v2.4.1",
          onClick = {
            Toast.makeText(context, "Up to date", Toast.LENGTH_SHORT).show()
          }
        )
        SettingsSwitchRow(
          icon = Icons.Default.Wifi,
          title = "Auto-Sync",
          subtitle = "Cloud sync over Wi-Fi",
          checked = autoSync,
          onCheckedChange = onAutoSyncChanged
        )
      }
    }

    // Appearance Section
    ProfileSection(title = "Appearance") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "App Theme",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppTheme.entries.forEach { theme ->
                    val isSelected = appTheme == theme
                    FilterChip(
                        selected = isSelected,
                        onClick = { onThemeChanged(theme) },
                        label = { Text(theme.name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Alerts Section
    ProfileSection(title = "Notifications") {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsSwitchRow(
          icon = Icons.Default.NotificationsActive,
          title = "Sound Alerts",
          subtitle = "Audible threshold warning",
          checked = soundAlerts,
          onCheckedChange = onSoundAlertsChanged
        )
        SettingsSwitchRow(
          icon = Icons.Default.Vibration,
          title = "Haptic Feedback",
          subtitle = "Vibrate on alert",
          checked = vibrationAlerts,
          onCheckedChange = onVibrationAlertsChanged
        )
        
        Text(
            text = "BAC Threshold Warning",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Slider(
            value = bacThreshold.toFloat(),
            onValueChange = { onThresholdChanged(it.toDouble()) },
            valueRange = 0.01f..0.15f,
            steps = 14,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Alert at ${String.format("%.2f", bacThreshold)} %BAC",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End)
        )
      }
    }

    // Units Section
    ProfileSection(title = "Measurements") {
        SettingsSwitchRow(
          icon = Icons.Default.Thermostat,
          title = "Celsius",
          subtitle = "Display temperature in °C",
          checked = unitCelsius,
          onCheckedChange = onUnitCelsiusChanged
        )
    }

    Spacer(modifier = Modifier.height(40.dp))
  }
}

@Composable
fun SettingsInfoRow(
  icon: ImageVector,
  title: String,
  value: String,
  onClick: (() -> Unit)? = null
) {
  Row(
    modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
      Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }

    Text(
      text = value,
      color = MaterialTheme.colorScheme.primary,
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
fun SettingsSwitchRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.weight(1f)
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
      Column {
        Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
      }
    }

    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange
    )
  }
}
