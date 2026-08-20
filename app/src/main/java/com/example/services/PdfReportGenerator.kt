package com.example.services

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.models.HealthReport
import com.example.models.SensorReading
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

  fun generateHealthReportPdf(
    context: Context,
    report: HealthReport,
    recentReadings: List<SensorReading>,
    userName: String
  ): File {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (595x842 pt)
    val page = pdfDocument.startPage(pageInfo)
    val canvas: Canvas = page.canvas

    val paint = Paint()
    val titlePaint = Paint().apply {
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textSize = 24f
      color = Color.parseColor("#047857") // Emerald Dark
    }

    val subtitlePaint = Paint().apply {
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      textSize = 12f
      color = Color.parseColor("#475569")
    }

    val headerPaint = Paint().apply {
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textSize = 14f
      color = Color.parseColor("#0F172A")
    }

    val bodyPaint = Paint().apply {
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      textSize = 12f
      color = Color.parseColor("#1E293B")
    }

    val accentPaint = Paint().apply {
      color = Color.parseColor("#10B981") // Emerald Primary
    }

    // Top Emerald Banner bar
    canvas.drawRect(0f, 0f, 595f, 16f, accentPaint)

    // Title
    canvas.drawText("SoberWatch Health — Clinical Telemetry Report", 40f, 60f, titlePaint)
    canvas.drawText("Patient/User: $userName | Date Range: ${report.dateRangeLabel}", 40f, 85f, subtitlePaint)
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(report.generatedAt))
    canvas.drawText("Generated on: $dateStr | Device: SoberWatch_S3 (ESP32-S3)", 40f, 103f, subtitlePaint)

    // Divider Line
    paint.color = Color.parseColor("#CBD5E1")
    paint.strokeWidth = 1f
    canvas.drawLine(40f, 120f, 555f, 120f, paint)

    // Summary Statistics Section
    canvas.drawText("EXECUTIVE WELLNESS & SENSOR SUMMARY", 40f, 150f, headerPaint)

    var y = 180f
    canvas.drawText("• Overall Wellness Score: ${report.wellnessScore}/100 (Optimal State)", 50f, y, bodyPaint)
    y += 24f
    canvas.drawText("• Highest Blood Alcohol Concentration (BAC): ${String.format("%.3f", report.highestBac)} %BAC", 50f, y, bodyPaint)
    y += 24f
    canvas.drawText("• Average Resting Heart Rate: ${report.averageHeartRate} BPM", 50f, y, bodyPaint)
    y += 24f
    canvas.drawText("• Average Blood Oxygen Saturation (SpO2): ${report.averageSpO2} %", 50f, y, bodyPaint)
    y += 24f
    canvas.drawText("• Average Body Temperature: ${String.format("%.1f", report.averageTemperature)} °C", 50f, y, bodyPaint)
    y += 24f
    canvas.drawText("• Total Telemetry Samples Processed: ${report.totalReadingsCount} readings", 50f, y, bodyPaint)
    y += 24f
    canvas.drawText("• Threshold Exceedance Alerts: ${report.alertCount} alert(s)", 50f, y, bodyPaint)

    // Telemetry Sample Log Header
    y += 40f
    canvas.drawText("RECENT TELEMETRY SAMPLE LOG (LAST 10 SNAPSHOTS)", 40f, y, headerPaint)
    y += 25f

    // Table Header
    paint.color = Color.parseColor("#F1F5F9")
    canvas.drawRect(40f, y - 16f, 555f, y + 8f, paint)
    
    val tableHeaderPaint = Paint(bodyPaint).apply {
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = Color.parseColor("#0F172A")
    }
    canvas.drawText("Time", 50f, y, tableHeaderPaint)
    canvas.drawText("BAC (%)", 150f, y, tableHeaderPaint)
    canvas.drawText("Heart Rate", 250f, y, tableHeaderPaint)
    canvas.drawText("SpO2", 350f, y, tableHeaderPaint)
    canvas.drawText("Temp (°C)", 440f, y, tableHeaderPaint)
    y += 24f

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    val samples = recentReadings.take(10)
    for (sample in samples) {
      val timeStr = timeFormat.format(Date(sample.timestamp))
      canvas.drawText(timeStr, 50f, y, bodyPaint)
      canvas.drawText(String.format("%.2f %%", sample.alcoholBac), 150f, y, bodyPaint)
      canvas.drawText("${sample.heartRateBpm} BPM", 250f, y, bodyPaint)
      canvas.drawText("${sample.spo2Percent} %", 350f, y, bodyPaint)
      canvas.drawText(String.format("%.1f", sample.tempCelsius), 440f, y, bodyPaint)
      y += 22f
    }

    // Footer
    val footerPaint = Paint(subtitlePaint).apply {
      textSize = 10f
    }
    canvas.drawText("SoberWatch Health • Embedded ESP32-S3 IoT Telemetry Platform • Confidential Healthcare Data", 40f, 800f, footerPaint)

    pdfDocument.finishPage(page)

    // Save to cache directory
    val dir = File(context.cacheDir, "reports")
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, "SoberWatch_Report_${System.currentTimeMillis()}.pdf")
    FileOutputStream(file).use { out ->
      pdfDocument.writeTo(out)
    }
    pdfDocument.close()

    return file
  }

  fun sharePdfReport(context: Context, pdfFile: File) {
    try {
      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        pdfFile
      )
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "SoberWatch Health Telemetry Report")
        putExtra(Intent.EXTRA_TEXT, "Attached is the clinical telemetry report generated by SoberWatch Health (ESP32-S3).")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(shareIntent, "Share Health Report").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      })
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
