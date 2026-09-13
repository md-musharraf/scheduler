package com.example.routineforge.ui.player

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routineforge.data.RoutineStep
import com.example.routineforge.data.StepType
import com.example.routineforge.theme.BrandPrimary
import com.example.routineforge.theme.BrandSecondary
import com.example.routineforge.theme.BrandTertiary
import com.example.routineforge.theme.DarkBg
import com.example.routineforge.theme.NothingDottedDivider
import com.example.routineforge.theme.NothingPillTag
import com.example.routineforge.theme.NothingRed
import com.example.routineforge.theme.NothingRedLight
import com.example.routineforge.theme.NothingRedSubtle
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
            StepType.WORK -> NothingRed
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
                // Subtask countdown animated continuous liquid sweep
                val animatedStepProgress by animateFloatAsState(
                    targetValue = (1f - uiState.stepProgress).coerceIn(0f, 1f),
                    animationSpec = if (uiState.isPlaying) {
                        tween(durationMillis = 1000, easing = LinearEasing)
                    } else {
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                    },
                    label = "SubtaskSweep"
                )

                // Category countdown animated continuous liquid sweep
                val animatedCategoryProgress by animateFloatAsState(
                    targetValue = (1f - uiState.categoryProgress).coerceIn(0f, 1f),
                    animationSpec = if (uiState.isPlaying) {
                        tween(durationMillis = 1000, easing = LinearEasing)
                    } else {
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                    },
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

                val isDark = MaterialTheme.colorScheme.background == DarkBg
                val outlineColor = MaterialTheme.colorScheme.outline
                val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                val arcActiveColor = if (stepType == StepType.WORK) {
                    if (isDark) Color.White else Color(0xFF09090B)
                } else themeColor

                // Dual concentric rings Canvas (Nothing OS Ndot Chronometer)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                ) {
                    val center = Offset(size.width / 2, size.height / 2)

                    // 1. OUTER RING: 60-Dot Chronometer Track + Category Arc
                    val outerStroke = 4.dp.toPx()
                    val outerDiameter = size.minDimension - outerStroke - 12.dp.toPx()
                    val outerTopLeft = Offset((size.width - outerDiameter) / 2, (size.height - outerDiameter) / 2)
                    val outerArcSize = Size(outerDiameter, outerDiameter)

                    // Outer subtle track line
                    drawArc(
                        color = outlineColor.copy(alpha = 0.35f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = outerTopLeft,
                        size = outerArcSize,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Outer Active Category Arc
                    drawArc(
                        color = if (isDark) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.9f),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedCategoryProgress,
                        useCenter = false,
                        topLeft = outerTopLeft,
                        size = outerArcSize,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 60-Dot Chronometer Perimeter (Nothing Ndot Style)
                    val dotCount = 60
                    val dotRadius = (outerDiameter / 2) + 7.dp.toPx()
                    val activeCategoryDots = (animatedCategoryProgress * dotCount).toInt()
                    for (i in 0 until dotCount) {
                        val angle = ((-90f + i * (360f / dotCount)) * (Math.PI / 180f)).toFloat()
                        val dotX = center.x + dotRadius * cos(angle)
                        val dotY = center.y + dotRadius * sin(angle)
                        val isMajor = (i % 5 == 0)
                        val isDotActive = i <= activeCategoryDots

                        val dotColor = when {
                            isDotActive && isMajor -> NothingRed
                            isDotActive -> if (isDark) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.9f)
                            isMajor -> outlineColor
                            else -> outlineColor.copy(alpha = 0.3f)
                        }

                        drawCircle(
                            color = dotColor,
                            radius = if (isMajor) 2.2.dp.toPx() else 1.2.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }

                    // 2. INNER RING: Active Sub-task Countdown Track
                    val innerStroke = 12.dp.toPx()
                    val innerDiameter = outerDiameter - 38.dp.toPx()
                    val innerTopLeft = Offset((size.width - innerDiameter) / 2, (size.height - innerDiameter) / 2)
                    val innerArcSize = Size(innerDiameter, innerDiameter)

                    // Inner Track
                    drawArc(
                        color = surfaceVariantColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = innerTopLeft,
                        size = innerArcSize,
                        style = Stroke(width = innerStroke, cap = StrokeCap.Round)
                    )

                    // Inner Active Arc (Subtask remaining)
                    val subtaskSweepAngle = 360f * animatedStepProgress
                    drawArc(
                        color = arcActiveColor,
                        startAngle = -90f,
                        sweepAngle = subtaskSweepAngle,
                        useCenter = false,
                        topLeft = innerTopLeft,
                        size = innerArcSize,
                        style = Stroke(width = innerStroke, cap = StrokeCap.Round)
                    )

                    // Luminous Nothing Red glowing bead at leading tip of active subtask arc
                    if (animatedStepProgress > 0.005f) {
                        val tipAngleRad = ((-90f + subtaskSweepAngle) * (Math.PI / 180.0)).toFloat()
                        val innerRadius = innerDiameter / 2f
                        val beadX = center.x + innerRadius * kotlin.math.cos(tipAngleRad)
                        val beadY = center.y + innerRadius * kotlin.math.sin(tipAngleRad)
                        val beadCenter = Offset(beadX, beadY)

                        // Outer luminous Nothing Red aura glow
                        drawCircle(
                            color = NothingRed.copy(alpha = 0.4f),
                            radius = 12.dp.toPx(),
                            center = beadCenter
                        )
                        // Radiant core bead
                        drawCircle(
                            color = NothingRed,
                            radius = 6.5.dp.toPx(),
                            center = beadCenter
                        )
                        // Specular white center spark
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = beadCenter
                        )
                    }
                }

                // Center Digital Chronograph Readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    // Subtask phase badge (Nothing Pill)
                    NothingPillTag(
                        text = "${stepType.displayName()} // STEP ${(uiState.currentStepIndex + 1)}/${uiState.routine?.steps?.size ?: 1}",
                        isHighlight = true,
                        leadingDotColor = NothingRed
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Primary Sub-task Countdown Clock with Smooth Slide Transition
                    AnimatedContent(
                        targetState = uiState.formattedRemainingTime(),
                        transitionSpec = {
                            (slideInVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { height -> height / 3 } + fadeIn(animationSpec = tween(200)))
                                .togetherWith(slideOutVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { height -> -height / 3 } + fadeOut(animationSpec = tween(200)))
                        },
                        label = "TimerDigitRoll"
                    ) { formattedTime ->
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 54.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Active Step Title
                    Text(
                        text = currentStep?.title ?: "Ready",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Category Parallel Countdown Pill (Nothing Bento Pill)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(NothingRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CAT // ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AnimatedContent(
                                targetState = uiState.formattedCategoryRemainingTime(),
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                                },
                                label = "CatTimerRoll"
                            ) { catTime ->
                                Text(
                                    text = "$catTime LEFT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
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
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous Sub-task",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Big Glowing Play / Pause Button with Rotating Nothing Red Halo
                val infinitePulse = rememberInfiniteTransition(label = "haloTransition")
                val haloRotation by infinitePulse.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 3500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "haloRotation"
                )

                val haloSurfaceColor = MaterialTheme.colorScheme.onSurface

                Box(
                    modifier = Modifier.size(86.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isPlaying) {
                        Canvas(
                            modifier = Modifier
                                .size(86.dp)
                                .graphicsLayer { rotationZ = haloRotation }
                        ) {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        NothingRed.copy(alpha = 0.8f),
                                        haloSurfaceColor.copy(alpha = 0.15f),
                                        NothingRedLight.copy(alpha = 0.6f),
                                        NothingRed.copy(alpha = 0.8f)
                                    )
                                ),
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(NothingRed)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { viewModel.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = uiState.isPlaying,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                            },
                            label = "PlayPauseMorph"
                        ) { isPlaying ->
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                // Next Subtask Step (Skip)
                IconButton(
                    onClick = { viewModel.skipToNextStep() },
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next Sub-task",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp)
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
    val cardScale by animateFloatAsState(
        targetValue = if (isActive) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "SubtaskCardScale"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isActive -> MaterialTheme.colorScheme.surface
                isPassed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = Modifier
            .width(150.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = when {
                    isActive -> NothingRed
                    isPassed -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.outline
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
                Text(
                    text = String.format("%02d", index + 1),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isActive) NothingRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                if (isPassed) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Done",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(15.dp)
                    )
                } else if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(NothingRed)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = step.title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = step.formattedDuration(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) NothingRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
