package com.example.routineforge.feature.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routineforge.core.database.SessionType
import com.example.routineforge.core.theme.FF
import com.example.routineforge.core.theme.TimerDigits
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.core.ui.ShowMessages
import com.example.routineforge.core.ui.SubjectBadge
import com.example.routineforge.data.TimerEvent
import com.example.routineforge.domain.TimerMath
import com.example.routineforge.navigation.AppNav
import com.example.routineforge.navigation.SettingsNav

/** Colours of a session kind: green for focus, warm amber for breaks. */
private class Look(val accent: Color, val sand: Color, val icon: ImageVector)

@Composable
private fun lookOf(type: SessionType): Look {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (type == SessionType.FOCUS) Look(if (dark) Color(0xFF3E9C74) else Color(0xFF1F5E45), Color(0xFF4E9470), Icons.Filled.HourglassTop)
    else Look(if (dark) Color(0xFFD08A45) else Color(0xFFA45F1E), Color(0xFFD9A05B), Icons.Filled.LocalCafe)
}

private fun presetsFor(type: SessionType) = when (type) {
    SessionType.FOCUS -> listOf(25, 50, 90)
    SessionType.SHORT_BREAK -> listOf(5, 10, 15)
    SessionType.LONG_BREAK -> listOf(15, 20, 30)
}

@Composable
fun TimerScreen(vm: TimerViewModel, nav: AppNav) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    ShowMessages(vm)
    var showStop by remember { mutableStateOf(false) }
    var editDuration by remember { mutableStateOf(false) }
    val look = lookOf(ui.type)
    val accent by animateColorAsState(look.accent, label = "accent")
    val view = LocalView.current
    DisposableEffect(ui.isRunning) {
        view.keepScreenOn = ui.isRunning
        onDispose { view.keepScreenOn = false }
    }

    val done = vm.completion
    if (done != null && !vm.showStopwatch) {
        CompletionScreen(done, lookOf(SessionType.FOCUS), onContinue = vm::continueAfterCompletion)
        return
    }

    // Everything fits on one screen: the hourglass takes whatever height the rest leaves.
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.08f), Color.Transparent))),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val isFocus = ui.type == SessionType.FOCUS
        Header(
            title = if (vm.showStopwatch) "Stopwatch" else if (isFocus) "Focus Session" else "Break Session",
            subtitle = when {
                vm.showStopwatch -> "Lap it, track it"
                ui.isActive && !ui.isRunning -> "Paused · resume when you're ready"
                isFocus && ui.isRunning -> "Stay focused"
                isFocus -> "Deep work, bigger results"
                ui.isRunning -> "Rest and recharge"
                else -> "Rest, refresh, come back stronger"
            },
            leading = {
                IconButton(onClick = { vm.showStopwatch = !vm.showStopwatch }) {
                    Icon(if (vm.showStopwatch) Icons.Filled.HourglassTop else Icons.Filled.Timer, contentDescription = if (vm.showStopwatch) "Timer" else "Stopwatch")
                }
            },
            trailing = { IconButton(onClick = { nav.push(SettingsNav) }) { Icon(Icons.Filled.Settings, contentDescription = "Timer settings") } }
        )
        if (vm.showStopwatch) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp)) { StopwatchPane(vm.stopwatch) }
            return@Column
        }

        ModeToggle(isFocus, enabled = !ui.isActive, accent = accent) { focus ->
            vm.setMode(if (focus) SessionType.FOCUS else if (ui.type == SessionType.LONG_BREAK) SessionType.LONG_BREAK else SessionType.SHORT_BREAK)
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            SandHourglass(
                remaining = { ui.remainingFraction(System.currentTimeMillis()) },
                running = ui.isRunning,
                sand = look.sand,
                modifier = Modifier.align(Alignment.Center).fillMaxHeight().aspectRatio(0.66f)
            )
            if (!ui.isActive) Box(Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)) {
                Presets(presetsFor(ui.type), ui.profile.minutesFor(ui.type), accent) { vm.setDuration(ui.type, it) }
            }
        }

        val clock = TimeFormat.clock(ui.remainingMs)
        Text(
            clock,
            style = TimerDigits.copy(fontSize = 52.sp, lineHeight = 56.sp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = !ui.isActive, onClickLabel = "Change duration") { editDuration = true }
                .padding(horizontal = 12.dp)
                .semantics { contentDescription = "$clock remaining" }
        )
        Text("/ ${TimeFormat.clock(ui.plannedMs)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        SessionCard(ui, look, accent, onPick = vm::selectSubject)
        Spacer(Modifier.height(14.dp))
        if (!ui.isActive) {
            Button(
                onClick = vm::toggle,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text(if (isFocus) "Start Focus" else "Start Break", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                RoundAction(Icons.Filled.FastForward, "Skip", onClick = vm::skip)
                RoundAction(
                    if (ui.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (ui.isRunning) "Pause" else "Resume",
                    size = 76, container = accent, content = Color.White, onClick = vm::toggle
                )
                RoundAction(Icons.Filled.Stop, "Stop", onClick = { showStop = true })
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    if (showStop) {
        val elapsed = ui.elapsedMs
        val canSave = ui.type == SessionType.FOCUS && elapsed >= TimerMath.MIN_SAVE_MS
        AlertDialog(
            onDismissRequest = { showStop = false },
            title = { Text(if (canSave) "Save this session?" else "End this session?") },
            text = {
                Text(
                    if (canSave) "You focused for ${TimeFormat.duration(elapsed)}. Save it to your progress, or discard it."
                    else if (ui.type == SessionType.FOCUS) "Sessions shorter than a minute aren't saved."
                    else "The break will end now."
                )
            },
            confirmButton = {
                TextButton(onClick = { showStop = false; vm.stop(save = canSave) }) { Text(if (canSave) "Save" else "End") }
            },
            dismissButton = {
                Row {
                    if (canSave) TextButton(onClick = { showStop = false; vm.stop(save = false) }) { Text("Discard", color = FF.colors.danger) }
                    TextButton(onClick = { showStop = false }) { Text("Keep going") }
                }
            }
        )
    }

    if (editDuration) {
        DurationDialog(
            type = ui.type,
            current = ui.profile.minutesFor(ui.type),
            onDismiss = { editDuration = false },
            onSave = { vm.setDuration(ui.type, it); editDuration = false }
        )
    }
}

@Composable
private fun Header(title: String, subtitle: String, leading: @Composable () -> Unit, trailing: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 2.dp)) {
        Box(Modifier.align(Alignment.CenterStart)) { leading() }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
    }
}

/** Focus | Break pill switch. */
@Composable
private fun ModeToggle(focus: Boolean, enabled: Boolean, accent: Color, onSelect: (Boolean) -> Unit) {
    Row(
        Modifier.padding(horizontal = 32.dp, vertical = 4.dp).fillMaxWidth().clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp)
    ) {
        listOf(true to "Focus", false to "Break").forEach { (isFocus, label) ->
            val selected = focus == isFocus
            Surface(
                onClick = { onSelect(isFocus) },
                enabled = enabled || selected,
                shape = CircleShape,
                color = if (selected) accent else Color.Transparent,
                contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isFocus) Icons.Filled.HourglassTop else Icons.Filled.LocalCafe, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

/** Quick lengths beside the hourglass while idle. */
@Composable
private fun Presets(options: List<Int>, selected: Int, accent: Color, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { m ->
            val on = m == selected
            Surface(
                onClick = { onPick(m) },
                shape = RoundedCornerShape(20.dp),
                color = if (on) accent else MaterialTheme.colorScheme.surface,
                contentColor = if (on) Color.White else MaterialTheme.colorScheme.onSurface,
                shadowElevation = if (on) 4.dp else 1.dp,
                modifier = Modifier.size(width = 58.dp, height = 64.dp).semantics { contentDescription = "$m minutes" }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("$m", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("min", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/** What this session is about: the subject for focus (tap to change while idle), or the break. */
@Composable
private fun SessionCard(ui: TimerUi, look: Look, accent: Color, onPick: (Long?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val focus = ui.type == SessionType.FOCUS
    val canPick = focus && !ui.isActive
    Box(Modifier.padding(horizontal = 24.dp)) {
        Surface(
            onClick = { open = true },
            enabled = canPick,
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                val s = ui.subject
                if (focus && s != null) SubjectBadge(s.icon, s.color, size = 42.dp)
                else Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(if (focus) Icons.AutoMirrored.Filled.MenuBook else look.icon, contentDescription = null, tint = accent)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (focus) s?.name ?: "No subject" else ui.type.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    val (ms, target) = ui.todayProgress
                    Text(
                        when {
                            !focus -> "Take a ${ui.plannedMs / 60_000} minute break"
                            target > 0 -> "${TimeFormat.duration(ms)} of ${TimeFormat.minutes(target)} today"
                            s == null && canPick -> "Tap to choose what you're working on"
                            else -> "${TimeFormat.duration(ms)} focused today"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canPick) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Choose subject")
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("No subject") }, onClick = { onPick(null); open = false })
            ui.activeSubjects.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.name) },
                    leadingIcon = { SubjectBadge(s.icon, s.color, size = 28.dp) },
                    onClick = { onPick(s.id); open = false }
                )
            }
        }
    }
}

@Composable
private fun RoundAction(
    icon: ImageVector, label: String, onClick: () -> Unit, size: Int = 56,
    container: Color = MaterialTheme.colorScheme.surface, content: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(onClick = onClick, shape = CircleShape, color = container, contentColor = content, shadowElevation = 6.dp, modifier = Modifier.size(size.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, modifier = Modifier.size((size * 0.42f).dp)) }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** Shown after a focus session runs to the end: today's totals, then Continue into the break. */
@Composable
private fun CompletionScreen(done: TimerEvent.Finished, look: Look, onContinue: () -> Unit) {
    val summary = done.summary
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(look.accent.copy(alpha = 0.10f), Color.Transparent)))
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Box(Modifier.weight(1f).aspectRatio(0.66f)) {
            SandHourglass(remaining = { 0f }, running = false, sand = look.sand, modifier = Modifier.fillMaxSize())
            Box(
                Modifier.align(Alignment.Center).offset(y = 24.dp).size(60.dp).clip(CircleShape).background(look.accent),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        Text("Focus Session Complete!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            buildAnnotatedString {
                append("Great job! You stayed focused for\n")
                withStyle(SpanStyle(color = look.accent, fontWeight = FontWeight.SemiBold)) { append(TimeFormat.duration(done.savedMs)) }
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Icons.Filled.LocalFireDepartment, Color(0xFFF08A24), "${summary?.sessions ?: 1}", if (summary?.sessions == 1) "Session" else "Sessions", Modifier.weight(1f))
            StatTile(Icons.Filled.Timer, look.accent, TimeFormat.duration(done.savedMs), "Focused", Modifier.weight(1f))
            StatTile(Icons.Filled.BarChart, Color(0xFF5B5FEF), summary?.goalPercent?.let { "$it%" } ?: "—", "Today Goal", Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = look.accent, contentColor = Color.White)
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
        Text(
            if (done.nextStarted) "Your ${done.next.label.lowercase()} has started" else "Continue starts your ${done.next.label.lowercase()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
    }
}

@Composable
private fun StatTile(icon: ImageVector, tint: Color, value: String, label: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DurationDialog(type: SessionType, current: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    val max = if (type == SessionType.FOCUS) 180 else 60
    var minutes by remember { mutableFloatStateOf(current.coerceIn(1, max).toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${type.label} length") },
        text = {
            Column {
                Text("${minutes.toInt()} min", style = MaterialTheme.typography.headlineMedium)
                Slider(value = minutes, onValueChange = { minutes = Math.round(it).toFloat() }, valueRange = 1f..max.toFloat())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presetsFor(type).forEach { p ->
                        FilterChip(selected = minutes.toInt() == p, onClick = { minutes = p.toFloat() }, label = { Text("$p") })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(minutes.toInt()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Compact bar shown above the bottom navigation while a session is active. */
@Composable
fun MiniTimerBar(vm: TimerViewModel, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    AnimatedVisibility(ui.isActive, modifier = modifier) {
        Surface(onClick = onOpen, shape = RoundedCornerShape(16.dp), color = lookOf(ui.type).accent, contentColor = Color.White) {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(lookOf(ui.type).icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(listOfNotNull(ui.type.label, ui.subject?.name).joinToString(" · "), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    Text(if (ui.isRunning) "Running" else "Paused", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                }
                Text(TimeFormat.clock(ui.remainingMs), style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"))
                IconButton(onClick = vm::toggle) {
                    Icon(if (ui.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (ui.isRunning) "Pause" else "Resume", tint = Color.White)
                }
            }
        }
    }
}
