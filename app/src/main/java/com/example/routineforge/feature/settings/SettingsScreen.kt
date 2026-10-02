package com.example.routineforge.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routineforge.AppContainer
import com.example.routineforge.core.database.UserProfile
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.core.ui.FFCard
import com.example.routineforge.core.ui.FFViewModel
import com.example.routineforge.core.ui.ScreenHeader
import com.example.routineforge.core.ui.Segmented
import com.example.routineforge.core.ui.ShowMessages
import com.example.routineforge.core.ui.appViewModel
import com.example.routineforge.feature.reminders.PermissionBanners
import com.example.routineforge.core.ui.SettingSwitch
import com.example.routineforge.navigation.AppNav
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(private val c: AppContainer) : FFViewModel() {
    val profile: StateFlow<UserProfile?> = c.profiles.profile.stateIn(null)
    fun update(transform: (UserProfile) -> UserProfile) = launchSafe { c.profiles.update(transform) }
}

private val THEMES = listOf(UserProfile.THEME_SYSTEM to "System", UserProfile.THEME_LIGHT to "Light", UserProfile.THEME_DARK to "Dark")

@Composable
fun SettingsScreen(nav: AppNav) {
    val vm = appViewModel { SettingsViewModel(it) }
    ShowMessages(vm)
    val p by vm.profile.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader("Settings", onBack = nav::back)
        val profile = p ?: return@Column
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PermissionBanners()
            Section("Profile") {
                var name by rememberSaveable { mutableStateOf(profile.name) }
                LaunchedEffect(name) {
                    delay(500) // save shortly after typing stops
                    if (name != profile.name) vm.update { it.copy(name = name) }
                }
                OutlinedTextField(
                    name, { name = it.take(40) }, label = { Text("Your name") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth()
                )
            }
            Section("Appearance") {
                Segmented(THEMES.map { it.first }, profile.themeMode, { k -> THEMES.first { it.first == k }.second }, { m -> vm.update { it.copy(themeMode = m) } })
            }
            Section("Focus timer") {
                ValueSlider("Focus length", profile.focusMinutes, 5..180, 5, TimeFormat::minutes) { v -> vm.update { it.copy(focusMinutes = v) } }
                ValueSlider("Short break", profile.shortBreakMinutes, 1..30, 1, TimeFormat::minutes) { v -> vm.update { it.copy(shortBreakMinutes = v) } }
                ValueSlider("Long break", profile.longBreakMinutes, 5..60, 5, TimeFormat::minutes) { v -> vm.update { it.copy(longBreakMinutes = v) } }
                ValueSlider("Long break after", profile.longBreakEvery, 2..8, 1, { "$it sessions" }) { v -> vm.update { it.copy(longBreakEvery = v) } }
                SettingSwitch("Auto-start breaks", profile.autoStartBreak) { v -> vm.update { it.copy(autoStartBreak = v) } }
                SettingSwitch("Auto-start focus after breaks", profile.autoStartFocus) { v -> vm.update { it.copy(autoStartFocus = v) } }
                SettingSwitch("Sound when a session ends", profile.soundEnabled) { v -> vm.update { it.copy(soundEnabled = v) } }
                SettingSwitch("Vibrate when a session ends", profile.vibrationEnabled) { v -> vm.update { it.copy(vibrationEnabled = v) } }
            }
            Section("Streaks") {
                ValueSlider("Active day after", profile.streakMinMinutes, 5..120, 5, TimeFormat::minutes) { v -> vm.update { it.copy(streakMinMinutes = v) } }
                Text("Only completed focus sessions count toward streaks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Section("About") {
                val context = LocalContext.current
                val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "–"
                Text("FocusFlow $version", style = MaterialTheme.typography.bodyLarge)
                Text("Focus Today. Build Tomorrow.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Your data stays on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    FFCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
}

/** Commits when released, so dragging doesn't write to the database every frame. */
@Composable
private fun ValueSlider(label: String, value: Int, range: IntRange, step: Int, format: (Int) -> String, onChange: (Int) -> Unit) {
    var v by rememberSaveable(value) { mutableFloatStateOf(value.toFloat()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(format(v.toInt()), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
    Slider(
        value = v,
        onValueChange = { v = (Math.round(it / step) * step).toFloat().coerceIn(range.first.toFloat(), range.last.toFloat()) },
        onValueChangeFinished = { onChange(v.toInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        // Tick marks only when there are few enough to read.
        steps = ((range.last - range.first) / step - 1).coerceAtLeast(0).takeIf { it <= 12 } ?: 0
    )
}
