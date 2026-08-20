package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HealthScoreHero(
  score: Int,
  modifier: Modifier = Modifier
) {
  val statusLabel = when {
    score >= 90 -> "Optimal"
    score >= 75 -> "Good"
    score >= 60 -> "Fair"
    else -> "Action Required"
  }

  val primaryColor = MaterialTheme.colorScheme.primary
  val onSurface = MaterialTheme.colorScheme.onSurface

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(32.dp))
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(
              primaryColor.copy(alpha = 0.12f),
              primaryColor.copy(alpha = 0.04f)
            )
          )
        )
        .border(
          width = 1.dp,
          color = primaryColor.copy(alpha = 0.15f),
          shape = RoundedCornerShape(32.dp)
        )
        .padding(32.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = "WELLNESS SCORE",
        color = primaryColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
      ) {
        Text(
          text = "$score",
          color = onSurface,
          fontSize = 64.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "/100",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 20.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Surface(
        color = primaryColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(100.dp)
      ) {
        Text(
          text = statusLabel.uppercase(),
          color = primaryColor,
          fontSize = 12.sp,
          fontWeight = FontWeight.Black,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
      }
    }
  }
}
