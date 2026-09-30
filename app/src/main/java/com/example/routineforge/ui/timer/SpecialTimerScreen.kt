package com.example.routineforge.ui.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routineforge.data.RoutineCategory
import com.example.routineforge.data.TimerSettings
import com.example.routineforge.theme.NothingRed
import com.example.routineforge.theme.NothingRedLight
import com.example.routineforge.theme.TabularTimerDigits
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialTimerScreen(
    viewModel: SpecialTimerViewModel,
    onOpenStopwatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var showCustomize by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Timer", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = settings.label.ifBlank { uiState.selectedCategory.name },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCustomize = true }) {
                        Icon(Icons.Filled.Tune, contentDescription = "Customize timer")
                    }
                    IconButton(onClick = onOpenStopwatch) {
                        Icon(Icons.Outlined.Timer, contentDescription = "Stopwatch")
                    }
                    val context = LocalContext.current
                    IconButton(
                        onClick = {
                            val activity = context as? android.app.Activity
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                val params = android.app.PictureInPictureParams.Builder()
                                    .setAspectRatio(android.util.Rational(1, 1))
                                    .build()
                                activity?.enterPictureInPictureMode(params)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureInPictureAlt,
                            contentDescription = "Floating Mini Timer",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Standalone Category Selector
            CategorySelectorRow(
                categories = uiState.availableCategories,
                selectedCategory = uiState.selectedCategory,
                onSelectCategory = { viewModel.selectCategory(it) },
                enabled = uiState.status == SpecialTimerStatus.IDLE
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Center Dial & Timer Display
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.status == SpecialTimerStatus.IDLE) {
                    // Time Configuration View with Smooth Wheel Picker
                    WheelTimePickerSection(
                        selectedHours = uiState.selectedHours,
                        selectedMinutes = uiState.selectedMinutes,
                        selectedSeconds = uiState.selectedSeconds,
                        onTimeChanged = { h, m, s -> viewModel.setTime(h, m, s) },
                        onAddPreset = { viewModel.addPresetTime(it) }
                    )
                } else {
                    // Active Running / Paused / Finished Dial
                    SpecialTimerDial(
                        uiState = uiState,
                        label = settings.label.ifBlank { uiState.selectedCategory.name },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 3. Quick presets when running or paused
            if (uiState.status == SpecialTimerStatus.RUNNING || uiState.status == SpecialTimerStatus.PAUSED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    QuickAddChip(label = "+1m", onClick = { viewModel.addPresetTime(1) })
                    Spacer(modifier = Modifier.width(8.dp))
                    QuickAddChip(label = "+5m", onClick = { viewModel.addPresetTime(5) })
                    Spacer(modifier = Modifier.width(8.dp))
                    QuickAddChip(label = "+10m", onClick = { viewModel.addPresetTime(10) })
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4. Action Controls Bar
            SpecialTimerControls(
                status = uiState.status,
                onStart = { viewModel.start() },
                onPause = { viewModel.pause() },
                onResume = { viewModel.resume() },
                onReset = { viewModel.reset() },
                onRestart = { viewModel.restart() }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showCustomize) {
        TimerCustomizeSheet(
            settings = settings,
            onChange = { viewModel.updateSettings(it) },
            onDismiss = { showCustomize = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerCustomizeSheet(
    settings: TimerSettings,
    onChange: ((TimerSettings) -> TimerSettings) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Customize timer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = settings.label,
                onValueChange = { v -> onChange { it.copy(label = v.take(40)) } },
                label = { Text("Label") },
                placeholder = { Text("e.g. Deep work, Tea, Plank") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            SettingSwitch("Sound", "Play a tone when time is up", settings.soundEnabled) { v ->
                onChange { it.copy(soundEnabled = v) }
            }
            SettingSwitch("Vibration", "Vibrate when time is up", settings.vibrationEnabled) { v ->
                onChange { it.copy(vibrationEnabled = v) }
            }
            SettingSwitch("Auto-repeat", "Start again automatically (intervals)", settings.autoRepeat) { v ->
                onChange { it.copy(autoRepeat = v) }
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
fun CategorySelectorRow(
    categories: List<RoutineCategory>,
    selectedCategory: RoutineCategory,
    onSelectCategory: (RoutineCategory) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { cat ->
            val isSelected = cat.id == selectedCategory.id
            val categoryColor = try {
                Color(cat.colorHex)
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) categoryColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) categoryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(enabled = enabled) { onSelectCategory(cat) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) categoryColor else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = cat.name.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun WheelTimePickerSection(
    selectedHours: Int,
    selectedMinutes: Int,
    selectedSeconds: Int,
    onTimeChanged: (Int, Int, Int) -> Unit,
    onAddPreset: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Set duration",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Smooth Scroll Columns (Hours, Minutes, Seconds)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmoothWheelColumn(
                    range = 0..23,
                    selectedValue = selectedHours,
                    onValueSelected = { onTimeChanged(it, selectedMinutes, selectedSeconds) },
                    unitLabel = "hours"
                )

                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                SmoothWheelColumn(
                    range = 0..59,
                    selectedValue = selectedMinutes,
                    onValueSelected = { onTimeChanged(selectedHours, it, selectedSeconds) },
                    unitLabel = "min"
                )

                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                SmoothWheelColumn(
                    range = 0..59,
                    selectedValue = selectedSeconds,
                    onValueSelected = { onTimeChanged(selectedHours, selectedMinutes, it) },
                    unitLabel = "sec"
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Preset Buttons
            Text(
                text = "Quick add",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickPresetButton(label = "+1m", onClick = { onAddPreset(1) })
                QuickPresetButton(label = "+5m", onClick = { onAddPreset(5) })
                QuickPresetButton(label = "+10m", onClick = { onAddPreset(10) })
                QuickPresetButton(label = "+15m", onClick = { onAddPreset(15) })
                QuickPresetButton(label = "+25m", onClick = { onAddPreset(25) })
                QuickPresetButton(label = "+30m", onClick = { onAddPreset(30) })
                QuickPresetButton(label = "+60m", onClick = { onAddPreset(60) })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SmoothWheelColumn(
    range: IntRange,
    selectedValue: Int,
    onValueSelected: (Int) -> Unit,
    unitLabel: String,
    modifier: Modifier = Modifier
) {
    val items = remember(range) { range.toList() }
    val itemHeight = 44.dp
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = items.indexOf(selectedValue).coerceAtLeast(0)
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()

    // Sync scroll when value changes externally
    LaunchedEffect(selectedValue) {
        val targetIdx = items.indexOf(selectedValue)
        if (targetIdx >= 0 && listState.firstVisibleItemIndex != targetIdx) {
            listState.animateScrollToItem(targetIdx)
        }
    }

    // Detect user scroll settlement
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (!listState.isScrollInProgress && index in items.indices) {
                    onValueSelected(items[index])
                }
            }
    }

    Column(
        modifier = modifier.width(66.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = unitLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NothingRed
            )
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .height(itemHeight * 3)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Highlight frame for selected center row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NothingRed.copy(alpha = 0.08f))
                    .border(
                        1.dp,
                        NothingRed.copy(alpha = 0.35f),
                        RoundedCornerShape(8.dp)
                    )
            )

            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = itemHeight),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items) { index, value ->
                    val isCenter = remember {
                        derivedStateOf { listState.firstVisibleItemIndex == index }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .clickable {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                    onValueSelected(value)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%02d", value),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isCenter.value) FontWeight.Bold else FontWeight.Normal,
                                fontSize = if (isCenter.value) 24.sp else 16.sp,
                                color = if (isCenter.value) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPresetButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
fun QuickAddChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = NothingRed
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
fun SpecialTimerDial(
    uiState: SpecialTimerUiState,
    label: String,
    modifier: Modifier = Modifier
) {
    val finished = uiState.status == SpecialTimerStatus.FINISHED
    val sandColor = if (finished) Color(0xFF4CAF50) else NothingRed
    val categoryColor = Color(uiState.selectedCategory.colorHex)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = categoryColor.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(categoryColor))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp,
                        color = categoryColor
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Hourglass(
            remaining = uiState.progress,
            flowing = uiState.status == SpecialTimerStatus.RUNNING,
            sandColor = sandColor,
            modifier = Modifier
                .weight(1f, fill = false)
                .heightIn(max = 300.dp)
                .aspectRatio(0.66f)
        )

        Spacer(Modifier.height(20.dp))

        // Fixed-width digit slots so the numbers never jitter while the millis tick.
        val totalSec = uiState.remainingMillis / 1000
        TabularTimerDigits(
            hours = (totalSec / 3600).toInt(),
            minutes = ((totalSec % 3600) / 60).toInt(),
            seconds = (totalSec % 60).toInt(),
            millis = ((uiState.remainingMillis % 1000) / 10).toInt()
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = when (uiState.status) {
                SpecialTimerStatus.RUNNING -> "Running"
                SpecialTimerStatus.PAUSED -> "Paused"
                SpecialTimerStatus.FINISHED -> "Time's up"
                SpecialTimerStatus.IDLE -> "Ready"
            },
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = if (finished) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

/**
 * Framed hourglass: [remaining] (1 → 0) of the sand sits in the top bulb, the rest piles up below.
 * Both bulbs narrow like cones, so sand height follows sqrt of the amount to look physically right.
 */
@Composable
private fun Hourglass(remaining: Float, flowing: Boolean, sandColor: Color, modifier: Modifier = Modifier) {
    val frameColor = MaterialTheme.colorScheme.onSurface
    val glassColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val flow = rememberInfiniteTransition(label = "sand")
    val phase by flow.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing)),
        label = "stream"
    )

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val plate = 10.dp.toPx()

        // Frame: two plates and two posts.
        drawRoundRect(frameColor, Offset(0f, 0f), Size(w, plate), CornerRadius(plate / 2))
        drawRoundRect(frameColor, Offset(0f, h - plate), Size(w, plate), CornerRadius(plate / 2))
        listOf(w * 0.07f, w * 0.93f).forEach { x ->
            drawLine(frameColor.copy(alpha = 0.55f), Offset(x, plate), Offset(x, h - plate), 3.dp.toPx(), StrokeCap.Round)
        }

        val top = plate + 3.dp.toPx()
        val bottom = h - plate - 3.dp.toPx()
        val mid = (top + bottom) / 2f
        val half = mid - top
        val a = w * 0.33f
        val neck = 2.5.dp.toPx()
        fun bulb(edge: Float, dir: Float) = Path().apply {
            moveTo(cx - a, edge)
            lineTo(cx + a, edge)
            cubicTo(cx + a, edge + dir * half * 0.62f, cx + neck, mid - dir * half * 0.28f, cx + neck, mid)
            lineTo(cx - neck, mid)
            cubicTo(cx - neck, mid - dir * half * 0.28f, cx - a, edge + dir * half * 0.62f, cx - a, edge)
            close()
        }
        val upper = bulb(top, 1f)
        val lower = bulb(bottom, -1f)

        val f = remaining.coerceIn(0f, 1f)
        val cap = 0.86f // a full top bulb is filled to 86% of its height
        if (f > 0f) {
            clipPath(upper) {
                val level = mid - half * cap * sqrt(f)
                drawRect(sandColor, Offset(0f, level), Size(w, mid - level))
            }
        }
        // Same volume as what left the top: cone volume ~ height^2, measured from the wide end here.
        val pile = half * (1f - sqrt(1f - (1f - f) * cap * cap))
        val mound = 10.dp.toPx() * minOf(1f, (1f - f) * 8f)
        if (f < 1f) {
            clipPath(lower) {
                val level = bottom - pile
                drawPath(
                    Path().apply {
                        moveTo(0f, bottom)
                        lineTo(0f, level)
                        lineTo(cx - a, level)
                        quadraticTo(cx, level - mound * 2f, cx + a, level)
                        lineTo(w, level)
                        lineTo(w, bottom)
                        close()
                    },
                    sandColor
                )
            }
        }
        if (flowing && f > 0f) {
            val dash = 5.dp.toPx()
            drawLine(
                sandColor,
                Offset(cx, mid),
                Offset(cx, bottom - pile - mound),
                2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash * 0.6f), -phase * dash * 1.6f)
            )
        }

        // Glass outline and a soft highlight on each bulb.
        drawPath(upper, glassColor, style = Stroke(2.dp.toPx()))
        drawPath(lower, glassColor, style = Stroke(2.dp.toPx()))
        val shine = Color.White.copy(alpha = 0.10f)
        drawLine(shine, Offset(cx - a * 0.72f, top + half * 0.12f), Offset(cx - a * 0.42f, top + half * 0.55f), 4.dp.toPx(), StrokeCap.Round)
        drawLine(shine, Offset(cx - a * 0.72f, bottom - half * 0.12f), Offset(cx - a * 0.42f, bottom - half * 0.55f), 4.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
fun SpecialTimerControls(
    status: SpecialTimerStatus,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onRestart: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (status) {
            SpecialTimerStatus.IDLE -> {
                // Large Start Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NothingRed,
                    contentColor = Color.White,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onStart)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }
            SpecialTimerStatus.RUNNING -> {
                // Reset Button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onReset)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reset", modifier = Modifier.size(22.dp))
                    }
                }

                Spacer(modifier = Modifier.width(28.dp))

                // Pause Button
                Surface(
                    shape = CircleShape,
                    color = NothingRed,
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPause)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Pause, contentDescription = "Pause", modifier = Modifier.size(32.dp))
                    }
                }
            }
            SpecialTimerStatus.PAUSED -> {
                // Reset Button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onReset)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reset", modifier = Modifier.size(22.dp))
                    }
                }

                Spacer(modifier = Modifier.width(28.dp))

                // Resume Button
                Surface(
                    shape = CircleShape,
                    color = NothingRed,
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onResume)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(32.dp))
                    }
                }
            }
            SpecialTimerStatus.FINISHED -> {
                // Reset Button
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .height(52.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onReset)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RESET",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Restart Button
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = NothingRed,
                    contentColor = Color.White,
                    modifier = Modifier
                        .height(52.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onRestart)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RESTART",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
