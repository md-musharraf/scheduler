package com.example.routineforge.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.routineforge.AppContainer
import com.example.routineforge.core.database.GoalPeriod
import com.example.routineforge.core.theme.FF
import com.example.routineforge.core.ui.FFViewModel
import com.example.routineforge.core.ui.ShowMessages
import com.example.routineforge.core.ui.appViewModel
import com.example.routineforge.feature.timer.SandHourglass

class OnboardingViewModel(private val c: AppContainer) : FFViewModel() {
    fun finish(name: String, dailyGoalMinutes: Int?) = launchSafe {
        if (dailyGoalMinutes != null) c.goals.set(GoalPeriod.DAILY, dailyGoalMinutes)
        c.profiles.update { it.copy(name = name, onboardingDone = true) }
    }
}

@Composable
fun OnboardingScreen() {
    val vm = appViewModel { OnboardingViewModel(it) }
    ShowMessages(vm)
    var name by rememberSaveable { mutableStateOf("") }
    var goalHours by rememberSaveable { mutableIntStateOf(4) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Works either way; reminders just stay silent without it.
        vm.finish(name, goalHours * 60)
    }
    val finish: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else vm.finish(name, goalHours * 60)
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { vm.finish("", null) }) { Text("Skip") }
        }
        Text("Focus Today", style = MaterialTheme.typography.headlineLarge)
        Text("Build Tomorrow", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Plan your time. Track your progress. Reach your goals.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
            SandHourglass(remaining = { 0.67f }, running = true, sand = androidx.compose.ui.graphics.Color(0xFF4E9470), modifier = Modifier.size(width = 170.dp, height = 240.dp))
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            label = { Text("Your name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))
        Text("Daily focus goal", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 2, 4, 6, 8).forEach { h ->
                FilterChip(selected = goalHours == h, onClick = { goalHours = h }, label = { Text("${h}h") })
            }
        }
        Text(
            "You can change this any time in Goals.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = finish,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FF.colors.hero, contentColor = FF.colors.onHero)
        ) {
            Text("Get Started", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
        Spacer(Modifier.height(24.dp))
    }
}
