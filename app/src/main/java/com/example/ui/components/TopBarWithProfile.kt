package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun TopBarWithProfile(
  userName: String,
  isBleConnected: Boolean,
  avatarResId: Int?,
  avatarUri: String?,
  onAvatarClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_alpha"
  )

  val primary = MaterialTheme.colorScheme.primary
  val onSurface = MaterialTheme.colorScheme.onSurface
  val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 20.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    // Left side: Avatar + Greeting
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.clickable { onAvatarClick() }
    ) {
      Box(
        modifier =
          Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(primary.copy(alpha = 0.1f))
            .border(
              width = 1.dp,
              color = primary.copy(alpha = 0.15f),
              shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
      ) {
        if (avatarUri != null) {
          AsyncImage(
            model = avatarUri,
            contentDescription = "User Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else if (avatarResId != null && avatarResId != 0) {
          Image(
            painter = painterResource(id = avatarResId),
            contentDescription = "User Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "User Avatar",
                tint = primary,
                modifier = Modifier.size(24.dp)
            )
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column {
        Text(
          text = "Good Morning,",
          color = onSurfaceVariant,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )
        Text(
          text = userName,
          color = onSurface,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    // Right side: Connection Status
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
              modifier =
                Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .alpha(if (isBleConnected) pulseAlpha else 1f)
                  .background(if (isBleConnected) primary else MaterialTheme.colorScheme.error)
            )
            Text(
              text = if (isBleConnected) "CONNECTED" else "OFFLINE",
              color = onSurface,
              fontSize = 10.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 0.5.sp
            )
        }
    }
  }
}
