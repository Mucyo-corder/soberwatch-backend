package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MetricEcg

@Composable
fun EcgWaveformWidget(
  ecgStatus: String,
  waveform: List<Float>,
  modifier: Modifier = Modifier
) {
  val primary = MaterialTheme.colorScheme.primary
  val onSurface = MaterialTheme.colorScheme.onSurface
  val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left info block
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier =
            Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(MetricEcg.copy(alpha = 0.1f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.MonitorHeart,
            contentDescription = "ECG",
            tint = MetricEcg,
            modifier = Modifier.size(22.dp)
          )
        }

        Column {
          Text(
            text = "Live ECG Feed",
            color = onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = if (ecgStatus == "Normal" || ecgStatus.contains("Sinus", ignoreCase = true)) "SINUS RHYTHM" else ecgStatus.uppercase(),
            color = if (ecgStatus.contains("Normal", ignoreCase = true)) onSurfaceVariant else MaterialTheme.colorScheme.error,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
          )
        }
      }

      // Right ECG wave canvas
      Box(
        modifier =
          Modifier
            .width(110.dp)
            .height(38.dp)
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          if (waveform.size < 2) return@Canvas
          val width = size.width
          val height = size.height
          val stepX = width / (waveform.size - 1).toFloat()
          val path = Path()

          waveform.forEachIndexed { idx, value ->
            val x = idx * stepX
            // Normalize value to height
            val y = (height / 2f) - (value / 50f * (height / 2f))
            val clampedY = y.coerceIn(4f, height - 4f)
            if (idx == 0) {
              path.moveTo(x, clampedY)
            } else {
              path.lineTo(x, clampedY)
            }
          }

          drawPath(
            path = path,
            color = MetricEcg,
            style = Stroke(
              width = 2.dp.toPx(),
              cap = StrokeCap.Round,
              join = StrokeJoin.Round
            )
          )
        }
      }
    }
  }
}
