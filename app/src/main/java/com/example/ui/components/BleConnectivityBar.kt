package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BleConnectivityBar(
  batteryLevel: Int,
  deviceName: String,
  isConnected: Boolean,
  modifier: Modifier = Modifier
) {
  val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
  val primary = MaterialTheme.colorScheme.primary
  val error = MaterialTheme.colorScheme.error

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
          shape = RoundedCornerShape(16.dp)
        )
        .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Battery
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = Icons.Default.BatteryChargingFull,
          contentDescription = "Battery",
          tint = if (batteryLevel > 20) primary else error,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = "$batteryLevel%",
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }

      // Vertical Divider
      Box(
        modifier =
          Modifier
            .width(1.dp)
            .height(14.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
      )

      // Device Name
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = Icons.Default.BluetoothConnected,
          contentDescription = "Bluetooth",
          tint = if (isConnected) primary else error,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = if (isConnected) deviceName else "OFFLINE",
          color = if (isConnected) MaterialTheme.colorScheme.onSurface else error,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }

      // Vertical Divider
      Box(
        modifier =
          Modifier
            .width(1.dp)
            .height(14.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
      )

      // Current Date
      Text(
        text = dateStr,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}
