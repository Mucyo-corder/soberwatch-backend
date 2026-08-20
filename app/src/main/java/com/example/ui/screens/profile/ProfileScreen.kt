package com.example.ui.screens.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.preferences.AppTheme
import com.example.models.UserProfile
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
  userProfile: UserProfile,
  appTheme: AppTheme,
  onSetTheme: (AppTheme) -> Unit,
  onSaveProfile: (UserProfile) -> Unit,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  var isEditing by remember { mutableStateOf(false) }

  var name by remember(userProfile) { mutableStateOf(userProfile.name) }
  var email by remember(userProfile) { mutableStateOf(userProfile.email) }
  var ageStr by remember(userProfile) { mutableStateOf(userProfile.age.toString()) }
  var gender by remember(userProfile) { mutableStateOf(userProfile.gender) }
  var weightStr by remember(userProfile) { mutableStateOf(userProfile.weightKg.toString()) }
  var heightStr by remember(userProfile) { mutableStateOf(userProfile.heightCm.toString()) }
  var bloodGroup by remember(userProfile) { mutableStateOf(userProfile.bloodGroup) }
  var photoUri by remember(userProfile) { mutableStateOf(userProfile.photoUri) }

  val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri ->
    uri?.let {
      photoUri = it.toString()
    }
  }

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
          text = "USER PROFILE",
          color = MaterialTheme.colorScheme.primary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.5.sp
        )
        Text(
          text = "Personal Health Identity",
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold
        )
      }

      IconButton(
        onClick = {
          if (isEditing) {
            val updated = userProfile.copy(
              name = name,
              email = email,
              age = ageStr.toIntOrNull() ?: userProfile.age,
              gender = gender,
              weightKg = weightStr.toDoubleOrNull() ?: userProfile.weightKg,
              heightCm = heightStr.toDoubleOrNull() ?: userProfile.heightCm,
              bloodGroup = bloodGroup,
              photoUri = photoUri
            )
            onSaveProfile(updated)
            isEditing = false
            Toast.makeText(context, "Profile Saved", Toast.LENGTH_SHORT).show()
          } else {
            isEditing = true
          }
        },
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
      ) {
        Icon(
            imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit, 
            contentDescription = null, 
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(20.dp)
        )
      }
    }

    // Avatar Hero
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp),
      contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier =
                Modifier
                  .size(100.dp)
                  .clip(CircleShape)
                  .border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                  .clickable(enabled = isEditing) {
                    launcher.launch("image/*")
                  },
              contentAlignment = Alignment.Center
            ) {
              if (photoUri != null) {
                AsyncImage(
                  model = photoUri,
                  contentDescription = "User Avatar",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
              } else if (userProfile.photoResId != 0) {
                Image(
                  painter = painterResource(id = userProfile.photoResId),
                  contentDescription = "User Avatar",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
              } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User Avatar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
              }
              
              if (isEditing) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change Photo",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                  )
                }
              }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = userProfile.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = userProfile.email,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }

    // Appearance / Theme Section
    ProfileSection(title = "Appearance") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeOption(
                label = "System Default",
                selected = appTheme == AppTheme.SYSTEM,
                onClick = { onSetTheme(AppTheme.SYSTEM) }
            )
            ThemeOption(
                label = "Light Mode",
                selected = appTheme == AppTheme.LIGHT,
                onClick = { onSetTheme(AppTheme.LIGHT) }
            )
            ThemeOption(
                label = "Dark Mode",
                selected = appTheme == AppTheme.DARK,
                onClick = { onSetTheme(AppTheme.DARK) }
            )
        }
    }

    // Biometric Data Section
    ProfileSection(title = "Biometric Data") {
        if (isEditing) {
          Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
              OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
              )

              Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                  value = ageStr,
                  onValueChange = { ageStr = it },
                  label = { Text("Age") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                  value = bloodGroup,
                  onValueChange = { bloodGroup = it },
                  label = { Text("Blood Group") },
                  modifier = Modifier.weight(1f)
                )
              }

              Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                  value = weightStr,
                  onValueChange = { weightStr = it },
                  label = { Text("Weight (kg)") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                  value = heightStr,
                  onValueChange = { heightStr = it },
                  label = { Text("Height (cm)") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  modifier = Modifier.weight(1f)
                )
              }
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                ProfileStatCell(label = "AGE", value = "${userProfile.age} Yrs")
                ProfileStatCell(label = "GENDER", value = userProfile.gender)
                ProfileStatCell(label = "BLOOD", value = userProfile.bloodGroup)
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                ProfileStatCell(label = "WEIGHT", value = "${userProfile.weightKg} kg")
                ProfileStatCell(label = "HEIGHT", value = "${userProfile.heightCm} cm")
                ProfileStatCell(label = "BMI", value = "22.7")
              }
          }
        }
    }

    // Account Actions
    Column(
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
          onClick = onLogout,
          colors = ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.error
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
          Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Sign Out", fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(40.dp))
  }
}

@Composable
fun ProfileSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Box(modifier = Modifier.padding(20.dp)) {
                content()
            }
        }
    }
}

@Composable
fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun ProfileStatCell(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.Start
  ) {
    Text(
        text = label, 
        color = MaterialTheme.colorScheme.onSurfaceVariant, 
        fontSize = 10.sp, 
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = value, 
        color = MaterialTheme.colorScheme.onSurface, 
        fontSize = 16.sp, 
        fontWeight = FontWeight.Bold
    )
  }
}
