package com.example.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.SensorReading
import com.example.ui.components.TrendChartCard
import com.example.ui.theme.*

@Composable
fun HealthHistoryScreen(
  recentReadings: List<SensorReading>,
  modifier: Modifier = Modifier
) {
  var selectedTabIndex by remember { mutableIntStateOf(1) } // 0=Day, 1=Week, 2=Month
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
        text = "HISTORICAL DATA",
        color = MaterialTheme.colorScheme.primary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
      )
      Text(
        text = "Analytics",
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
      )
    }

    // Tab Row
    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 24.dp,
        divider = {}
    ) {
        listOf("DAY", "WEEK", "MONTH", "YEAR").forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { selectedTabIndex = index },
                text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }
    }

    // Historical Charts
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val bacHistory = when (selectedTabIndex) {
          0 -> listOf(0.01f, 0.02f, 0.01f, 0.00f, 0.03f, 0.02f, 0.01f, 0.02f)
          1 -> listOf(0.03f, 0.04f, 0.01f, 0.00f, 0.05f, 0.02f, 0.01f)
          else -> listOf(0.04f, 0.05f, 0.03f, 0.02f, 0.02f, 0.01f, 0.01f)
        }
        TrendChartCard(
            title = "Average BAC",
            valueText = "Avg 0.02%",
            color = MetricBac,
            icon = Icons.Default.LocalDrink,
            dataPoints = bacHistory,
            min = 0f,
            max = 0.15f
        )

        val bpmHistory = when (selectedTabIndex) {
          0 -> listOf(68f, 70f, 72f, 74f, 71f, 69f, 72f)
          1 -> listOf(71f, 73f, 70f, 75f, 72f, 71f, 72f)
          else -> listOf(72f, 74f, 73f, 71f, 72f, 70f, 71f)
        }
        TrendChartCard(
            title = "Average Heart Rate",
            valueText = "Avg 72 bpm",
            color = MetricHeartRate,
            icon = Icons.Default.Favorite,
            dataPoints = bpmHistory,
            min = 55f,
            max = 100f
        )

        TrendChartCard(
            title = "Average SpO2",
            valueText = "Avg 98%",
            color = MetricSpO2,
            icon = Icons.Default.Air,
            dataPoints = listOf(98f, 98f, 99f, 97f, 98f, 99f, 98f),
            min = 92f,
            max = 100f
        )
    }

    Spacer(modifier = Modifier.height(40.dp))
  }
}
