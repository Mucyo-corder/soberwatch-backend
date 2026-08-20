package com.example.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnboardingStep(
  val title: String,
  val subtitle: String,
  val description: String,
  val icon: ImageVector
)

val onboardingSteps = listOf(
  OnboardingStep(
    title = "Precision Biometrics",
    subtitle = "IOT SENSOR INTERFACE",
    description = "Connect your SoberWatch wearable to track Alcohol Concentration, Heart Rate, SpO2, and ECG in real time with medical-grade accuracy.",
    icon = Icons.Default.BluetoothConnected
  ),
  OnboardingStep(
    title = "Clinical AI Insights",
    subtitle = "ADVANCED TELEMETRY",
    description = "Receive personalized clinical guidance and trend analysis powered by our advanced biometric processing engine.",
    icon = Icons.Default.AutoGraph
  ),
  OnboardingStep(
    title = "Safety First",
    subtitle = "EMERGENCY PROTOCOLS",
    description = "Automated high-BAC alerts ensure you stay safe by notifying emergency contacts and providing recovery recommendations.",
    icon = Icons.Default.Warning
  )
)

@Composable
fun OnboardingScreen(
  onFinishOnboarding: () -> Unit,
  modifier: Modifier = Modifier
) {
  var currentStep by remember { mutableIntStateOf(0) }
  val step = onboardingSteps[currentStep]

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(24.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top row with Skip button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        TextButton(onClick = onFinishOnboarding) {
          Text(
            text = "SKIP",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp
          )
        }
      }

      // Center card
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        shadowElevation = 2.dp
      ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
          ) {
            Box(
              modifier =
                Modifier
                  .size(80.dp)
                  .clip(RoundedCornerShape(24.dp))
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = step.icon,
                contentDescription = step.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
              )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
              text = step.subtitle,
              color = MaterialTheme.colorScheme.primary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = step.title,
              color = MaterialTheme.colorScheme.onSurface,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = step.description,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 15.sp,
              lineHeight = 24.sp,
              textAlign = TextAlign.Center
            )
          }
      }

      // Bottom Row: step indicator + Next button
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 16.dp)
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          onboardingSteps.indices.forEach { idx ->
            Box(
              modifier =
                Modifier
                  .size(if (idx == currentStep) 24.dp else 8.dp, 8.dp)
                  .clip(CircleShape)
                  .background(if (idx == currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            )
          }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
          onClick = {
            if (currentStep < onboardingSteps.lastIndex) {
              currentStep++
            } else {
              onFinishOnboarding()
            }
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
          shape = RoundedCornerShape(16.dp),
          modifier =
            Modifier
              .fillMaxWidth()
              .height(56.dp)
        ) {
          Text(
            text = if (currentStep == onboardingSteps.lastIndex) "GET STARTED" else "CONTINUE",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
        }
      }
    }
  }
}
