package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.AlertEvent

@Composable
fun AlertBannerModal(
  alert: AlertEvent?,
  onDismiss: () -> Unit,
  onNotifyEmergencyContact: () -> Unit,
  modifier: Modifier = Modifier
) {
  val errorColor = MaterialTheme.colorScheme.error

  AnimatedVisibility(
    visible = alert != null,
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically(),
    modifier = modifier.fillMaxWidth()
  ) {
    if (alert != null) {
      Surface(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, errorColor.copy(alpha = 0.3f)),
        shadowElevation = 4.dp
      ) {
        Column(
          modifier = Modifier.padding(20.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier =
                Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(errorColor.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alert",
                tint = errorColor,
                modifier = Modifier.size(22.dp)
              )
            }
            Column {
              Text(
                text = alert.title.uppercase(),
                color = errorColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
              )
              Text(
                text = alert.message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Recommendations
          Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            alert.recommendations.forEach { rec ->
              Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(
                  modifier =
                    Modifier
                      .padding(top = 6.dp)
                      .size(6.dp)
                      .clip(CircleShape)
                      .background(errorColor)
                )
                Text(
                  text = rec,
                  color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                  lineHeight = 18.sp
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = onDismiss) {
              Text(
                text = "DISMISS",
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = onNotifyEmergencyContact,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = errorColor,
                  contentColor = MaterialTheme.colorScheme.onError
                ),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text(
                text = "NOTIFY EMERGENCY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}
