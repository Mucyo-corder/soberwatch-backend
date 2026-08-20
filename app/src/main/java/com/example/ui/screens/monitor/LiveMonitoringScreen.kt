package com.example.ui.screens.monitor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.SensorReading
import com.example.ui.components.TrendChartCard
import com.example.ui.theme.*

@Composable
fun LiveMonitoringScreen(
  currentReading: SensorReading,
  recentReadings: List<SensorReading>,
  isMonitoring: Boolean,
  isConnected: Boolean,
  onStartMonitoring: () -> Unit,
  onPauseMonitoring: () -> Unit,
  onDisconnectDevice: () -> Unit,
  onSimulateHighBac: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  var selectedTab by remember { mutableStateOf(0) } // 0=Day, 1=Week, 2=Month

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
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "HEALTH ANALYTICS",
          color = MaterialTheme.colorScheme.primary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.5.sp
        )
        Text(
          text = "Trends",
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Surface(
          color = if (isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
          shape = RoundedCornerShape(100.dp)
      ) {
          Text(
              text = if (isConnected) "LIVE" else "OFFLINE",
              color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
              fontSize = 10.sp,
              fontWeight = FontWeight.Black,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
      }
    }

    // Time Range Selector
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 24.dp,
        divider = {},
        indicator = { tabPositions ->
            if (selectedTab < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        listOf("DAY", "WEEK", "MONTH", "YEAR").forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { selectedTab = index },
                text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }
    }

    // Charts List
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // BAC Chart
        val bacValues = recentReadings.map { it.alcoholBac.toFloat() }.ifEmpty { listOf(0.02f, 0.01f) }
        TrendChartCard(
            title = "Alcohol Concentration",
            valueText = "${String.format("%.2f", currentReading.alcoholBac)}%",
            color = MetricBac,
            icon = Icons.Default.LocalDrink,
            dataPoints = bacValues,
            min = 0f,
            max = 0.15f
        )

        // Heart Rate Chart
        val bpmValues = recentReadings.map { it.heartRateBpm.toFloat() }.ifEmpty { listOf(72f, 75f) }
        TrendChartCard(
            title = "Heart Rate",
            valueText = "${currentReading.heartRateBpm} bpm",
            color = MetricHeartRate,
            icon = Icons.Default.Favorite,
            dataPoints = bpmValues,
            min = 40f,
            max = 120f
        )

        // SpO2 Chart
        val spo2Values = recentReadings.map { it.spo2Percent.toFloat() }.ifEmpty { listOf(98f, 99f) }
        TrendChartCard(
            title = "Blood Oxygen",
            valueText = "${currentReading.spo2Percent}%",
            color = MetricSpO2,
            icon = Icons.Default.Air,
            dataPoints = spo2Values,
            min = 90f,
            max = 100f
        )
    }

    Spacer(modifier = Modifier.height(40.dp))
  }
}
