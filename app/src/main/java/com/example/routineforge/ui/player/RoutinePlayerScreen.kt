package com.example.routineforge.ui.player

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routineforge.data.RoutineStep
import com.example.routineforge.data.StepType
import com.example.routineforge.theme.BrandPrimary
import com.example.routineforge.theme.BrandSecondary
import com.example.routineforge.theme.BrandTertiary
import com.example.routineforge.theme.PrepareColor
import com.example.routineforge.theme.RestColor
import com.example.routineforge.theme.WorkColor
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinePlayerScreen(
    viewModel: RoutinePlayerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Keep screen awake while routine is actively running
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val currentStep = uiState.currentStep
    val stepType = currentStep?.stepType ?: StepType.WORK
    val themeColor by animateColorAsState(
        targetValue = when (stepType) {
            StepType.WORK -> WorkColor
            StepType.REST -> RestColor
            StepType.PREPARE -> PrepareColor
        },
        label = "ThemeColor"
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.routine?.title ?: "Routine Countdown",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = "Elapsed: ${uiState.formattedElapsedTime()} • Step ${(uiState.currentStepIndex + 1).coerceAtMost(uiState.routine?.steps?.size ?: 1)}/${uiState.routine?.steps?.size ?: 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Sound toggle
                    IconButton(onClick = { viewModel.toggleSound() }) {
                        Icon(
                            imageVector = if (uiState.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                            contentDescription = "Sound",
                            tint = if (uiState.soundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Vibration toggle
                    IconButton(onClick = { viewModel.toggleVibration() }) {
                        Icon(
                            imageVector = Icons.Filled.Vibration,
                            contentDescription = "Vibration",
                            tint = if (uiState.vibeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Reset Routine
                    IconButton(onClick = { viewModel.resetRoutine() }) {
                        Icon(
                            imageVector = Icons.Filled.Replay,
                            contentDescription = "Reset",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated Transition Prompt Banner ("Aapka Video Record khatam ho gya hai!...")
            AnimatedVisibility(
                visible = uiState.showTransitionBanner && uiState.lastTransitionMessage != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = BrandPrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, BrandPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BrandPrimary,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = uiState.lastTransitionMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.dismissTransitionBanner() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Dual Gauge Chronograph Circular Timer Display
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Subtask countdown animated sweep
                val animatedStepProgress by animateFloatAsState(
                    targetValue = (1f - uiState.stepProgress).coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                    label = "SubtaskSweep"
                )

                // Category countdown animated sweep
                val animatedCategoryProgress by animateFloatAsState(
                    targetValue = (1f - uiState.categoryProgress).coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                    label = "CategorySweep"
                )

                // Breathing pulse when active
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (uiState.isPlaying) 1.03f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1100),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                val categoryGradientBrush = remember {
                    Brush.sweepGradient(
                        listOf(BrandPrimary, BrandSecondary, BrandTertiary, BrandPrimary)
                    )
                }

                // Dual concentric rings Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                ) {
                    val center = Offset(size.width / 2, size.height / 2)

                    // 1. OUTER RING: Overall Category Countdown
                    val outerStroke = 9.dp.toPx()
                    val outerDiameter = size.minDimension - outerStroke - 4.dp.toPx()
                    val outerTopLeft = Offset((size.width - outerDiameter) / 2, (size.height - outerDiameter) / 2)
                    val outerArcSize = Size(outerDiameter, outerDiameter)

                    // Outer Track
                    drawArc(
                        color = Color(0xFF334155).copy(alpha = 0.35f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = outerTopLeft,
                        size = outerArcSize,
                        style = Stroke(width = outerStroke, cap = StrokeCap.Round)
                    )

                    // Outer Active Arc (Category remaining)
                    drawArc(
                        brush = categoryGradientBrush,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedCategoryProgress,
                        useCenter = false,
                        topLeft = outerTopLeft,
                        size = outerArcSize,
                        style = Stroke(width = outerStroke, cap = StrokeCap.Round)
                    )

                    // Chronograph subtle tick notches between rings
                    val notchCount = 12
                    val notchRadius = (outerDiameter / 2) - 10.dp.toPx()
                    for (i in 0 until notchCount) {
                        val angle = (i * (360f / notchCount)) * (Math.PI / 180f)
                        val startX = center.x + (notchRadius - 4.dp.toPx()) * cos(angle).toFloat()
                        val startY = center.y + (notchRadius - 4.dp.toPx()) * sin(angle).toFloat()
                        val endX = center.x + notchRadius * cos(angle).toFloat()
                        val endY = center.y + notchRadius * sin(angle).toFloat()
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.35f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // 2. INNER RING: Active Sub-task Countdown
                    val innerStroke = 14.dp.toPx()
                    val innerDiameter = outerDiameter - 36.dp.toPx()
                    val innerTopLeft = Offset((size.width - innerDiameter) / 2, (size.height - innerDiameter) / 2)
                    val innerArcSize = Size(innerDiameter, innerDiameter)

                    // Inner Track
                    drawArc(
                        color = Color(0xFF1E293B).copy(alpha = 0.5f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = innerTopLeft,
                        size = innerArcSize,
                        style = Stroke(width = innerStroke, cap = StrokeCap.Round)
                    )

                    // Inner Active Arc (Subtask remaining)
                    drawArc(
                        color = themeColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedStepProgress,
                        useCenter = false,
                        topLeft = innerTopLeft,
                        size = innerArcSize,
                        style = Stroke(width = innerStroke, cap = StrokeCap.Round)
                    )
                }

                // Center Digital Chronograph Readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 28.dp)
                ) {
                    // Subtask phase badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = themeColor.copy(alpha = 0.16f),
                        modifier = Modifier.border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "${stepType.displayName()} • Step ${(uiState.currentStepIndex + 1)}/${uiState.routine?.steps?.size ?: 1}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 11.sp),
                            color = themeColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Primary Sub-task Countdown Clock
                    Text(
                        text = uiState.formattedRemainingTime(),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 52.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Active Step Title
                    Text(
                        text = currentStep?.title ?: "Ready",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Category Parallel Countdown Pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.border(1.dp, BrandPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ Total Category: ",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${uiState.formattedCategoryRemainingTime()} left",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = BrandPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Quick Adjust Countdown Buttons (-15s, +30s, +1m, Restart)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { viewModel.addSecondsToCurrentStep(-15) }
                ) {
                    Text(
                        text = "-15s",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { viewModel.addSecondsToCurrentStep(30) }
                ) {
                    Text(
                        text = "+30s",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { viewModel.addSecondsToCurrentStep(60) }
                ) {
                    Text(
                        text = "+1m",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { viewModel.restartCurrentStep() }
                ) {
                    Text(
                        text = "Restart",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Sub-tasks Timeline Horizontal Stepper Queue
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CATEGORY SUB-TASKS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Total ${uiState.routine?.steps?.size ?: 0} sub-tasks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val queueListState = rememberLazyListState()
                LaunchedEffect(uiState.currentStepIndex) {
                    val stepsCount = uiState.routine?.steps?.size ?: 0
                    if (uiState.currentStepIndex in 0 until stepsCount) {
                        queueListState.animateScrollToItem(uiState.currentStepIndex)
                    }
                }

                LazyRow(
                    state = queueListState,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    itemsIndexed(
                        uiState.routine?.steps ?: emptyList(),
                        key = { _, step -> step.id }
                    ) { index, step ->
                        SubtaskQueueCard(
                            step = step,
                            index = index,
                            isActive = index == uiState.currentStepIndex,
                            isPassed = index < uiState.currentStepIndex
                        )
                    }
                }
            }

            // Current Step Instruction / Cue Card
            if (!currentStep?.instruction.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentStep.instruction,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Luxury Floating Control Deck (Previous, Big Play/Pause, Next)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Subtask Step
                IconButton(
                    onClick = { viewModel.skipToPreviousStep() },
                    modifier = Modifier
                        .size(54.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous Sub-task",
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Big Glowing Play / Pause Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(BrandPrimary, BrandSecondary))
                        )
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }

                // Next Subtask Step (Skip)
                IconButton(
                    onClick = { viewModel.skipToNextStep() },
                    modifier = Modifier
                        .size(54.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next Sub-task",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }

    // Grand Celebration Dialog on Completion
    if (uiState.isCompleted) {
        AlertDialog(
            onDismissRequest = { /* Force explicit user action */ },
            icon = {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(WorkColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🏆", fontSize = 38.sp)
                }
            },
            title = {
                Text(
                    text = "Category Schedule Finished!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pura Category Session khatam ho gaya! Great execution on \"${uiState.routine?.title}\"!",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "⏱ Total Duration: ${uiState.formattedElapsedTime()}", fontWeight = FontWeight.SemiBold)
                            Text(text = "✅ Sub-tasks Completed: ${uiState.routine?.steps?.size ?: 0}")
                            Text(text = "📅 Automatically logged in your History & Calendar Analytics!")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onNavigateBack,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done & Return Home")
                }
            }
        )
    }
}

@Composable
fun SubtaskQueueCard(
    step: RoutineStep,
    index: Int,
    isActive: Boolean,
    isPassed: Boolean
) {
    val stepColor = when (step.stepType) {
        StepType.WORK -> WorkColor
        StepType.REST -> RestColor
        StepType.PREPARE -> PrepareColor
    }

    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when {
                isActive -> MaterialTheme.colorScheme.surface
                isPassed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            }
        ),
        modifier = Modifier
            .width(150.dp)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = when {
                    isActive -> stepColor
                    isPassed -> WorkColor.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = stepColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "#${index + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = stepColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (isPassed) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Done",
                        tint = WorkColor,
                        modifier = Modifier.size(16.dp)
                    )
                } else if (isActive) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = stepColor
                    ) {
                        Text(
                            text = "NOW",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = step.title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                ),
                maxLines = 1
            )

            Text(
                text = step.formattedDuration(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) stepColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

