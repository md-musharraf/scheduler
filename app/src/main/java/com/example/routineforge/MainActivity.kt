package com.example.routineforge

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.routineforge.data.RoutineRepository
import com.example.routineforge.theme.RoutineForgeTheme

class MainActivity : ComponentActivity() {
  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
      }
    }

    enableEdgeToEdge()
    setContent {
      val repository = remember { RoutineRepository.getInstance(applicationContext) }
      val themeMode by repository.themeMode.collectAsState()
      val isSystemDark = isSystemInDarkTheme()
      val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemDark
      }

      val initialRoutineId = remember {
        intent?.getStringExtra(com.example.routineforge.service.RoutineTimerService.EXTRA_ROUTINE_ID)
          ?: intent?.getStringExtra("ROUTINE_ID")
      }

      RoutineForgeTheme(darkTheme = isDark) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainNavigation(initialRoutineId = initialRoutineId)
        }
      }
    }
  }
}
