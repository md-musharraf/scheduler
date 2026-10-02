package com.example.routineforge

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.routineforge.core.database.UserProfile
import com.example.routineforge.core.theme.FocusFlowTheme
import com.example.routineforge.navigation.FocusFlowRoot
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** Deep link from a notification; cleared once navigation has handled it. */
    private val pendingNav = mutableStateOf<String?>(null)

    companion object {
        const val EXTRA_NAV = "focusflow_nav"
        const val NAV_TIMER = "timer"
        const val NAV_STOPWATCH = "stopwatch"
        const val NAV_REMINDERS = "reminders"
        const val NAV_CALENDAR = "calendar"

        fun launchIntent(context: Context, nav: String): Intent =
            Intent(context, MainActivity::class.java).apply {
                setPackage(context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_NAV, nav)
            }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNav.value = intent.getStringExtra(EXTRA_NAV)
    }

    override fun onResume() {
        super.onResume()
        // The app may have been in the background when a session ran out.
        lifecycleScope.launch { container.timer.completeIfDue() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingNav.value = intent.getStringExtra(EXTRA_NAV)
        enableEdgeToEdge()
        setContent {
            val profile by container.profiles.profile.collectAsStateWithLifecycle(initialValue = null)
            val dark = when (profile?.themeMode) {
                UserProfile.THEME_DARK -> true
                UserProfile.THEME_LIGHT -> false
                else -> isSystemInDarkTheme()
            }
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            val nav by pendingNav
            FocusFlowTheme(dark = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    FocusFlowRoot(launchNav = nav, onLaunchHandled = { pendingNav.value = null })
                }
            }
        }
    }
}
