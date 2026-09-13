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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.example.routineforge.theme.NothingRed
import com.example.routineforge.theme.NothingRedLight
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialTimerScreen(
    viewModel: SpecialTimerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SPECIAL TIMER",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = "// STANDALONE CATEGORY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = NothingRed
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                        modifier = Modifier.size(290.dp)
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
                text = "// SET DURATION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NothingRed,
                    letterSpacing = 1.sp
                )
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
                    unitLabel = "HRS"
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
                    unitLabel = "MIN"
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
                    unitLabel = "SEC"
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Preset Buttons
            Text(
                text = "QUICK PRESETS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val sweepColor = if (uiState.status == SpecialTimerStatus.FINISHED) Color(0xFF4CAF50) else NothingRed
    val dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val categoryColor = try {
        Color(uiState.selectedCategory.colorHex)
    } catch (e: Exception) {
        NothingRed
    }

    // Smooth sweep progress
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "dial_progress"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - 18.dp.toPx()
            val strokeWidth = 5.dp.toPx()

            // 1. Ambient perimeter tick dots (60 tick marks)
            val tickCount = 60
            for (i in 0 until tickCount) {
                val angleRad = Math.toRadians((i * 6.0) - 90.0)
                val dotRadius = if (i % 5 == 0) 2.2.dp.toPx() else 1.2.dp.toPx()
                val dist = radius + 10.dp.toPx()
                val x = center.x + (dist * cos(angleRad)).toFloat()
                val y = center.y + (dist * sin(angleRad)).toFloat()
                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = Offset(x, y)
                )
            }

            // 2. Background Track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // 3. Progress Arc (Remaining time)
            if (animatedProgress > 0f) {
                drawArc(
                    color = sweepColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Glowing Leading Bead Indicator
                val beadAngleRad = Math.toRadians((-90.0 + 360.0 * animatedProgress))
                val beadX = center.x + (radius * cos(beadAngleRad)).toFloat()
                val beadY = center.y + (radius * sin(beadAngleRad)).toFloat()
                drawCircle(
                    color = Color.White,
                    radius = 4.5.dp.toPx(),
                    center = Offset(beadX, beadY)
                )
            }
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Category Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = categoryColor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(categoryColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.selectedCategory.name.uppercase(),
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

            Spacer(modifier = Modifier.height(10.dp))

            // Big Countdown Digits with Milliseconds
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.formattedRemaining,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 42.sp,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = ".${uiState.formattedMillis}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        color = NothingRed
                    ),
                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // State label
            Text(
                text = when (uiState.status) {
                    SpecialTimerStatus.RUNNING -> "// COUNTING DOWN"
                    SpecialTimerStatus.PAUSED -> "// PAUSED"
                    SpecialTimerStatus.FINISHED -> "// TIME COMPLETED"
                    SpecialTimerStatus.IDLE -> "// READY"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    color = if (uiState.status == SpecialTimerStatus.FINISHED) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
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
                            text = "START COUNTDOWN",
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
