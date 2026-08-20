package com.example.ui.screens.insights

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.SensorReading
import com.example.services.ChatMessage
import com.example.ui.theme.*

@Composable
fun AiAssistantScreen(
  chatMessages: List<ChatMessage>,
  currentReading: SensorReading,
  isListening: Boolean,
  isRecordingNote: Boolean,
  spokenText: String,
  onSendMessage: (String) -> Unit,
  onStartListening: () -> Unit,
  onStopListening: () -> Unit,
  onStartRecordingNote: () -> Unit,
  onStopRecordingNote: () -> Unit,
  onPlayVoiceNote: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var userInput by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val context = LocalContext.current

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      // Permission granted
    }
  }

  LaunchedEffect(chatMessages.size) {
    if (chatMessages.isNotEmpty()) {
      listState.animateScrollToItem(chatMessages.size - 1)
    }
  }

  LaunchedEffect(spokenText) {
    if (spokenText.isNotBlank()) {
      userInput = spokenText
    }
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(top = 20.dp)
  ) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "CLINICAL ORACLE",
          color = MaterialTheme.colorScheme.primary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.5.sp
        )
        Text(
          text = "Health Assistant",
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold
        )
      }

      OraclePulseIndicator(isListening || isRecordingNote)
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Telemetry Context Bar
    TelemetryContextBar(currentReading)

    Spacer(modifier = Modifier.height(12.dp))

    // Message History
    Box(modifier = Modifier.weight(1f)) {
      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        items(chatMessages) { message ->
          ClinicalAdvisorBubble(message, onPlayVoiceNote)
        }
      }
    }

    // Suggested Questions Chips
    AnimatedVisibility(visible = !isRecordingNote) {
        SuggestedQuestionsRow { onSendMessage(it) }
    }

    // Recording Note Indicator
    AnimatedVisibility(visible = isRecordingNote) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recording Voice Note...", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Input Area
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 4.dp,
      shadowElevation = 8.dp
    ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
          // STT Button
          IconButton(
            onClick = {
              if (isListening) {
                onStopListening()
              } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                onStartListening()
              }
            },
            modifier =
              Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer)
          ) {
            Icon(
              imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
              contentDescription = "Voice Input",
              tint = if (isListening) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(20.dp)
            )
          }

          // Voice Note Button
          IconButton(
              onClick = {
                  if (isRecordingNote) {
                      onStopRecordingNote()
                  } else {
                      permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                      onStartRecordingNote()
                  }
              },
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isRecordingNote) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondaryContainer)
          ) {
              Icon(
                  imageVector = if (isRecordingNote) Icons.Default.Stop else Icons.Default.GraphicEq,
                  contentDescription = "Record Voice Note",
                  tint = if (isRecordingNote) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.size(20.dp)
              )
          }

          OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            placeholder = { Text(if (isListening) "Listening..." else "Ask your advisor...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 14.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.weight(1f)
          )

          IconButton(
            onClick = {
              if (userInput.isNotBlank()) {
                onSendMessage(userInput)
                userInput = ""
              }
            },
            modifier =
              Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = "Send",
              tint = MaterialTheme.colorScheme.onPrimary
            )
          }
        }
    }
  }
}

@Composable
fun TelemetryContextBar(reading: SensorReading) {
  LazyRow(
    contentPadding = PaddingValues(horizontal = 24.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    item { TelemetryPill(label = "BAC", value = "${try { String.format(java.util.Locale.US, "%.2f", reading.alcoholBac) } catch(e: Exception) { reading.alcoholBac }}%", color = MetricBac) }
    item { TelemetryPill(label = "HR", value = "${reading.heartRateBpm}", color = MetricHeartRate) }
    item { TelemetryPill(label = "SpO2", value = "${reading.spo2Percent}%", color = MetricSpO2) }
    item { TelemetryPill(label = "TEMP", value = "${reading.tempCelsius}°C", color = MetricTemp) }
  }
}

@Composable
fun TelemetryPill(label: String, value: String, color: Color) {
  Surface(
    color = color.copy(alpha = 0.1f),
    shape = RoundedCornerShape(100.dp),
    border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
  ) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
      Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.width(4.dp))
      Text(text = value, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
  }
}

@Composable
fun OraclePulseIndicator(isListening: Boolean) {
  val infiniteTransition = rememberInfiniteTransition(label = "oracle_pulse")
  val scale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = if (isListening) 1.4f else 1.1f,
    animationSpec = infiniteRepeatable(
      animation = tween(if (isListening) 600 else 3000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  val color = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

  Box(contentAlignment = Alignment.Center) {
    Box(
      modifier = Modifier
        .size(32.dp)
        .scale(scale)
        .clip(CircleShape)
        .background(color.copy(alpha = 0.2f))
    )
    Box(
      modifier = Modifier
        .size(16.dp)
        .clip(CircleShape)
        .background(color)
        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
    )
  }
}

@Composable
fun ClinicalAdvisorBubble(message: ChatMessage, onPlayAudio: (String) -> Unit) {
  val isModel = message.role == "model"
  val isVoiceNote = message.audioPath != null
  
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isModel) Alignment.Start else Alignment.End
  ) {
    Surface(
      color = if (isModel) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
      shape = RoundedCornerShape(
          topStart = if (isModel) 4.dp else 20.dp,
          topEnd = if (isModel) 20.dp else 4.dp,
          bottomStart = 20.dp,
          bottomEnd = 20.dp
      ),
      modifier = Modifier.widthIn(max = 300.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isVoiceNote) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(
                        onClick = { message.audioPath?.let { onPlayAudio(it) } },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isModel) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = if (isModel) MaterialTheme.colorScheme.primary else Color.White)
                    }
                    Column {
                        Text(
                            text = "Voice Note", 
                            color = if (isModel) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "0:02", // Mock duration
                            color = if (isModel) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
              Text(
                text = message.content,
                color = if (isModel) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
              )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(4.dp))
    
    Text(
      text = if (isModel) "Oracle Advisor" else "You",
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 4.dp)
    )
  }
}

@Composable
fun SuggestedQuestionsRow(onSelect: (String) -> Unit) {
  val suggestions = listOf(
    "How is my HR?",
    "BAC safety check",
    "Wellness report",
    "Recovery tips"
  )
  
  LazyRow(
    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    items(suggestions) { text ->
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier.clickable { onSelect(text) }
      ) {
        Text(
            text = text, 
            color = MaterialTheme.colorScheme.primary, 
            fontSize = 12.sp, 
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
      }
    }
  }
}
