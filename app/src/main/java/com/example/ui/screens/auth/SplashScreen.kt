package com.example.ui.screens.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
  onSplashFinished: () -> Unit,
  modifier: Modifier = Modifier
) {
  var startAnim by remember { mutableStateOf(false) }
  val alpha by animateFloatAsState(
    targetValue = if (startAnim) 1f else 0f,
    animationSpec = tween(1200, easing = FastOutSlowInEasing),
    label = "splash_alpha"
  )
  val scale by animateFloatAsState(
    targetValue = if (startAnim) 1f else 0.8f,
    animationSpec = tween(1200, easing = FastOutSlowInEasing),
    label = "splash_scale"
  )

  LaunchedEffect(Unit) {
    startAnim = true
    delay(2500L)
    onSplashFinished()
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .alpha(alpha)
        .scale(scale)
        .padding(24.dp)
    ) {
      Surface(
        modifier = Modifier.size(100.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
            Image(
              painter = painterResource(id = R.drawable.img_app_icon_1785670221484),
              contentDescription = "SoberWatch Logo",
              modifier = Modifier.fillMaxSize()
            )
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      Text(
        text = "SoberWatch Health",
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "CLINICAL TELEMETRY SUITE",
        color = MaterialTheme.colorScheme.primary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp
      )

      Spacer(modifier = Modifier.height(48.dp))

      CircularProgressIndicator(
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 3.dp,
        modifier = Modifier.size(24.dp)
      )
      
      Spacer(modifier = Modifier.height(16.dp))
      
      Text(
          text = "Initializing Oracle AI...",
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
      )
    }
  }
}
