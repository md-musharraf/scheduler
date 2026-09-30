package com.example.routineforge

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.routineforge.data.RoutineRepository
import com.example.routineforge.service.CalendarAlarmScheduler
import com.example.routineforge.service.TimerSessionManager
import com.example.routineforge.widget.NothingTimerWidgetProvider
import com.example.routineforge.theme.RoutineForgeTheme
import com.example.routineforge.ui.components.PipMiniTimerScreen

class MainActivity : ComponentActivity() {
  private val isInPipMode = mutableStateOf(false)
  /** Latest deep link (notification, widget); cleared once navigation has handled it. */
  private val pendingLaunch = mutableStateOf<Intent?>(null)

  companion object {
    const val NAV_SCHEDULE = "SCHEDULE"
    const val NAV_DASHBOARD = "DASHBOARD"
    const val EXTRA_TIMER_SECONDS = "extra_timer_seconds"
    const val EXTRA_TIMER_LABEL = "extra_timer_label"

    fun launchIntent(context: Context, targetNav: String): Intent =
      Intent(context, MainActivity::class.java).apply {
        setPackage(context.packageName)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(NothingTimerWidgetProvider.EXTRA_TARGET_NAV, targetNav)
      }

    fun timerIntent(context: Context, label: String, seconds: Int): Intent =
      launchIntent(context, NothingTimerWidgetProvider.NAV_SPECIAL_TIMER)
        .putExtra(EXTRA_TIMER_LABEL, label)
        .putExtra(EXTRA_TIMER_SECONDS, seconds)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    pendingLaunch.value = intent
  }

  override fun onPictureInPictureModeChanged(
    isInPictureInPictureMode: Boolean,
    newConfig: Configuration
  ) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    isInPipMode.value = isInPictureInPictureMode
  }

  override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    val session = TimerSessionManager.session.value
    if (session.isRunning && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      try {
        val params = android.app.PictureInPictureParams.Builder()
          .setAspectRatio(android.util.Rational(1, 1))
          .build()
        enterPictureInPictureMode(params)
      } catch (_: Exception) {
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    TimerSessionManager.appContext = applicationContext
    // Force-stop / "clear recents" on some phones wipes alarms; re-arm them on every launch.
    CalendarAlarmScheduler.rescheduleAllUpcoming(applicationContext)
    if (savedInstanceState == null) pendingLaunch.value = intent

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
      }
    }

    enableEdgeToEdge()
    setContent {
      val pipMode by isInPipMode
      val activeSession by TimerSessionManager.session.collectAsState()

      if (pipMode) {
        RoutineForgeTheme(darkTheme = true) {
          PipMiniTimerScreen(session = activeSession)
        }
      } else {
        val repository = remember { RoutineRepository.getInstance(applicationContext) }
        val themeMode by repository.themeMode.collectAsState()
        val isSystemDark = isSystemInDarkTheme()
        val isDark = when (themeMode) {
          "DARK" -> true
          "LIGHT" -> false
          else -> isSystemDark
        }

        val launch by pendingLaunch

        RoutineForgeTheme(darkTheme = isDark) {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
          ) {
            MainNavigation(launch = launch, onLaunchHandled = { pendingLaunch.value = null })
          }
        }
      }
    }
  }
}
