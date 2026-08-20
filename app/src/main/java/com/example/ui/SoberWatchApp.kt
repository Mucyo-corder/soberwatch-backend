package com.example.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.data.preferences.AppTheme
import com.example.ui.screens.alerts.AlertsScreen
import com.example.ui.screens.auth.ForgotPasswordScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.OnboardingScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.auth.SplashScreen
import com.example.ui.screens.contacts.EmergencyContactsScreen
import com.example.ui.screens.device.DeviceConnectionScreen
import com.example.ui.screens.history.HealthHistoryScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.insights.AiAssistantScreen
import com.example.ui.screens.insights.AiInsightsScreen
import com.example.ui.screens.monitor.LiveMonitoringScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SoberWatchViewModel

sealed class Screen(val route: String) {
  object Splash : Screen("splash")
  object Onboarding : Screen("onboarding")
  object Login : Screen("login")
  object Register : Screen("register")
  object ForgotPassword : Screen("forgot_password")
  object Home : Screen("home")
  object Trends : Screen("trends")
  object Device : Screen("device")
  object Profile : Screen("profile")
  object Assistant : Screen("assistant") // Clinical Oracle
  object Settings : Screen("settings")
  object Alerts : Screen("alerts")
  object History : Screen("history")
  object Contacts : Screen("contacts")
  object Reports : Screen("reports")
}

data class BottomNavItem(
  val name: String,
  val route: String,
  val icon: androidx.compose.ui.graphics.vector.ImageVector
)

val bottomNavItems = listOf(
  BottomNavItem("Home", Screen.Home.route, Icons.Default.Home),
  BottomNavItem("Monitor", Screen.Trends.route, Icons.Default.MonitorHeart),
  BottomNavItem("AI Oracle", Screen.Assistant.route, Icons.Default.AutoGraph),
  BottomNavItem("Devices", Screen.Device.route, Icons.Default.BluetoothConnected),
  BottomNavItem("Profile", Screen.Profile.route, Icons.Default.Person)
)

@Composable
fun SoberWatchApp(
  viewModel: SoberWatchViewModel = viewModel()
) {
  val appTheme by viewModel.appTheme.collectAsState()
  val context = LocalContext.current
  val darkTheme = when (appTheme) {
    AppTheme.LIGHT -> false
    AppTheme.DARK -> true
    AppTheme.SYSTEM -> isSystemInDarkTheme()
  }

  MyApplicationTheme(darkTheme = darkTheme) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
      Screen.Home.route,
      Screen.Trends.route,
      Screen.Assistant.route,
      Screen.Device.route,
      Screen.Profile.route
    )

    Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      bottomBar = {
        if (showBottomBar) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
          ) {
            bottomNavItems.forEach { item ->
              val isSelected = currentRoute == item.route
              NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.name) },
                label = {
                  Text(
                    text = item.name,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                  )
                },
                selected = isSelected,
                onClick = {
                  if (currentRoute != item.route) {
                    navController.navigate(item.route) {
                      // Pop up to the start destination of the graph to
                      // avoid building up a large stack of destinations
                      // on the back stack as users select items
                      popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                      }
                      // Avoid multiple copies of the same destination when
                      // reselecting the same item
                      launchSingleTop = true
                      // Restore state when reselecting a previously selected item
                      restoreState = true
                    }
                  }
                },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.primary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
              )
            }
          }
        }
      }
    ) { innerPadding ->
      val userProfile by viewModel.userProfile.collectAsState()
      val currentReading by viewModel.currentReading.collectAsState()
      val isConnected by viewModel.isConnected.collectAsState()
      val isMonitoring by viewModel.isMonitoring.collectAsState()
      val activeAlert by viewModel.activeAlert.collectAsState()
      val recentReadings by viewModel.recentReadingsList.collectAsState()
      val insights by viewModel.aiInsights.collectAsState()
      val chatMessages by viewModel.chatMessages.collectAsState()
      val availableDevices by viewModel.availableDevices.collectAsState()
      val connectedDevice by viewModel.connectedDevice.collectAsState()
      val isScanning by viewModel.isScanning.collectAsState()
      val alertsHistory by viewModel.alertsHistory.collectAsState()
      val bacThreshold by viewModel.bacThreshold.collectAsState()
      val contacts by viewModel.emergencyContacts.collectAsState()
      val reports by viewModel.reports.collectAsState()
      val authError by viewModel.authError.collectAsState()
      val isListening by viewModel.isVoiceListening.collectAsState()
      val isRecordingNote by viewModel.isRecordingNote.collectAsState()
      val spokenText by viewModel.spokenText.collectAsState()
      
      val soundAlerts by viewModel.soundAlerts.collectAsState()
      val vibrationAlerts by viewModel.vibrationAlerts.collectAsState()
      val unitCelsius by viewModel.unitCelsius.collectAsState()
      val autoSync by viewModel.autoSync.collectAsState()
      val passwordResetSent by viewModel.passwordResetSent.collectAsState()

      NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = Modifier.padding(innerPadding)
      ) {
        composable(Screen.Splash.route) {
          SplashScreen(onSplashFinished = {
            val nextRoute = if (viewModel.isUserAuthenticated()) Screen.Home.route else Screen.Onboarding.route
            navController.navigate(nextRoute) {
              popUpTo(Screen.Splash.route) { inclusive = true }
            }
          })
        }
        composable(Screen.Onboarding.route) {
          OnboardingScreen(onFinishOnboarding = {
            navController.navigate(Screen.Login.route) {
              popUpTo(Screen.Onboarding.route) { inclusive = true }
            }
          })
        }
        composable(Screen.Login.route) {
          LoginScreen(
            error = authError,
            onLogin = { email, password ->
              viewModel.login(email, password) {
                navController.navigate(Screen.Home.route) {
                  popUpTo(Screen.Login.route) { inclusive = true }
                }
              }
            },
            onNavigateToRegister = { 
                viewModel.clearAuthError()
                navController.navigate(Screen.Register.route) 
            },
            onNavigateToForgotPassword = { 
                viewModel.clearAuthError()
                navController.navigate(Screen.ForgotPassword.route) 
            },
            onGoogleLogin = {
              viewModel.googleLogin {
                navController.navigate(Screen.Home.route) {
                  popUpTo(Screen.Login.route) { inclusive = true }
                }
              }
            }
          )
        }
        composable(
            route = Screen.Register.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "soberwatch://register" },
                navDeepLink { uriPattern = "https://soberwatch-health.app/register" }
            )
        ) {
          RegisterScreen(
            error = authError,
            onRegister = { name, email, password ->
              viewModel.register(name, email, password) {
                navController.navigate(Screen.Home.route) {
                  popUpTo(Screen.Register.route) { inclusive = true }
                }
              }
            },
            onNavigateToLogin = { 
                viewModel.clearAuthError()
                navController.popBackStack() 
            },
            onQuickRegister = {
                viewModel.quickRegister {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            }
          )
        }
        composable(Screen.ForgotPassword.route) {
          ForgotPasswordScreen(
            error = authError,
            isSent = passwordResetSent,
            onSendResetLink = { email -> viewModel.sendPasswordResetEmail(email) },
            onNavigateBack = { navController.popBackStack() }
          )
        }
        composable(
            route = Screen.Home.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "soberwatch://open" },
                navDeepLink { uriPattern = "soberwatch://app" },
                navDeepLink { uriPattern = "https://soberwatch-health.app/open" }
            )
        ) {
          HomeScreen(
            userProfile = userProfile,
            currentReading = currentReading,
            isConnected = isConnected,
            activeAlert = activeAlert,
            insights = insights,
            alertsHistory = alertsHistory,
            recentReadings = recentReadings,
            onDismissAlert = { viewModel.dismissActiveAlert() },
            onNotifyEmergencyContact = { /* Trigger SOS */ },
            onNavigateToDevice = { navController.navigate(Screen.Device.route) },
            onNavigateToMonitor = { navController.navigate(Screen.Trends.route) },
            onNavigateToInsights = { navController.navigate(Screen.Assistant.route) },
            onNavigateToAlerts = { navController.navigate(Screen.Alerts.route) },
            onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
            onSimulateHighBac = { viewModel.simulateHighAlcoholAlert() },
            onSaveReport = { 
                viewModel.generateNewReport()
                android.widget.Toast.makeText(context, "Detection report saved to history", android.widget.Toast.LENGTH_SHORT).show()
            }
          )
        }
        composable(Screen.Trends.route) {
          LiveMonitoringScreen(
            currentReading = currentReading,
            recentReadings = recentReadings,
            isMonitoring = isMonitoring,
            isConnected = isConnected,
            onStartMonitoring = { viewModel.resumeMonitoring() },
            onPauseMonitoring = { viewModel.pauseMonitoring() },
            onDisconnectDevice = { viewModel.disconnectDevice() },
            onSimulateHighBac = { viewModel.simulateHighAlcoholAlert() }
          )
        }
        composable(Screen.Assistant.route) {
          AiAssistantScreen(
            chatMessages = chatMessages,
            currentReading = currentReading,
            isListening = isListening,
            isRecordingNote = isRecordingNote,
            spokenText = spokenText,
            onSendMessage = { viewModel.sendMessageToAi(it) },
            onStartListening = { viewModel.startVoiceAssistant() },
            onStopListening = { viewModel.stopVoiceAssistant() },
            onStartRecordingNote = { viewModel.startRecordingVoiceNote() },
            onStopRecordingNote = { viewModel.stopAndSendVoiceNote() },
            onPlayVoiceNote = { viewModel.playVoiceNote(it) }
          )
        }
        composable(Screen.Device.route) {
          DeviceConnectionScreen(
            connectedDevice = connectedDevice,
            availableDevices = availableDevices,
            isScanning = isScanning,
            onStartScan = { viewModel.startBleScan() },
            onConnectDevice = { viewModel.connectToDevice(it) },
            onDisconnectDevice = { viewModel.disconnectDevice() }
          )
        }
        composable(Screen.Profile.route) {
          ProfileScreen(
            userProfile = userProfile,
            appTheme = appTheme,
            onSetTheme = { viewModel.setTheme(it) },
            onSaveProfile = { viewModel.updateProfile(it) },
            onLogout = {
              viewModel.logout {
                navController.navigate(Screen.Login.route) {
                  popUpTo(Screen.Home.route) { inclusive = true }
                }
              }
            }
          )
        }
        composable(Screen.Alerts.route) {
          AlertsScreen(
            alertsHistory = alertsHistory,
            bacThreshold = bacThreshold,
            onThresholdChanged = { viewModel.setBacThreshold(it) },
            onSimulateAlert = { viewModel.simulateHighAlcoholAlert() }
          )
        }
        composable(Screen.Settings.route) {
          SettingsScreen(
            bacThreshold = bacThreshold,
            onThresholdChanged = { viewModel.setBacThreshold(it) },
            appTheme = appTheme,
            onThemeChanged = { viewModel.setTheme(it) },
            soundAlerts = soundAlerts,
            onSoundAlertsChanged = { viewModel.setSoundAlerts(it) },
            vibrationAlerts = vibrationAlerts,
            onVibrationAlertsChanged = { viewModel.setVibrationAlerts(it) },
            unitCelsius = unitCelsius,
            onUnitCelsiusChanged = { viewModel.setUnitCelsius(it) },
            autoSync = autoSync,
            onAutoSyncChanged = { viewModel.setAutoSync(it) }
          )
        }
        composable(Screen.History.route) {
          HealthHistoryScreen(recentReadings = recentReadings)
        }
        composable(Screen.Contacts.route) {
          EmergencyContactsScreen(
            contacts = contacts,
            onAddContact = { n, p, r, e -> viewModel.addEmergencyContact(n, p, r, e) },
            onRemoveContact = { viewModel.removeEmergencyContact(it) }
          )
        }
        composable(Screen.Reports.route) {
          ReportsScreen(
            reports = reports,
            recentReadings = recentReadings,
            userName = userProfile.name,
            onGenerateNewReport = { viewModel.generateNewReport() }
          )
        }
      }
    }
  }
}
