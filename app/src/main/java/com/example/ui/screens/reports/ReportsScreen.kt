package com.example.ui.screens.reports

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.HealthReport
import com.example.models.SensorReading
import com.example.services.PdfReportGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
  reports: List<HealthReport>,
  recentReadings: List<SensorReading>,
  userName: String,
  onGenerateNewReport: suspend () -> HealthReport,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()
  val scope = androidx.compose.runtime.rememberCoroutineScope()

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
          text = "DATA EXPORT",
          color = MaterialTheme.colorScheme.primary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.5.sp
        )
        Text(
          text = "Clinical Reports",
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Button(
        onClick = {
          scope.launch {
              try {
                  val newRep = onGenerateNewReport()
                  val file = PdfReportGenerator.generateHealthReportPdf(context, newRep, recentReadings, userName)
                  Toast.makeText(context, "Report Generated and Saved", Toast.LENGTH_SHORT).show()
                  PdfReportGenerator.sharePdfReport(context, file)
              } catch (e: Exception) {
                  Toast.makeText(context, "Failed to generate report", Toast.LENGTH_SHORT).show()
              }
          }
        },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("CREATE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }

    // Latest Report Hero
    val latest = reports.firstOrNull()
    if (latest != null) {
        Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("LATEST SUMMARY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ReportStatItem("BAC PEAK", "${String.format("%.2f", latest.highestBac)}%", MaterialTheme.colorScheme.primary)
                        ReportStatItem("AVG HR", "${latest.averageHeartRate}", MaterialTheme.colorScheme.primary)
                        ReportStatItem("WELLNESS", "${latest.wellnessScore}", MaterialTheme.colorScheme.primary)
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(latest.dateRangeLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = {
                            val file = PdfReportGenerator.generateHealthReportPdf(context, latest, recentReadings, userName)
                            PdfReportGenerator.sharePdfReport(context, file)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share PDF", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }

    // History
    Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("REPORT ARCHIVE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        reports.forEach { report ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(report.dateRangeLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text("Score: ${report.wellnessScore} • ${report.totalReadingsCount} samples", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        val file = PdfReportGenerator.generateHealthReportPdf(context, report, recentReadings, userName)
                        PdfReportGenerator.sharePdfReport(context, file)
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    Spacer(modifier = Modifier.height(40.dp))
  }
}

@Composable
fun ReportStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Black)
    }
}
