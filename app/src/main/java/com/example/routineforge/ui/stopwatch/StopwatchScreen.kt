package com.example.routineforge.ui.stopwatch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routineforge.theme.NothingRed
import com.example.routineforge.theme.NothingRedLight
import kotlin.math.cos
import kotlin.math.sin

private fun formatMillisToTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    val ms = (millis % 1000) / 10
    return "%02d:%02d.%02d".format(m, s, ms)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopwatchScreen(
    viewModel: StopwatchViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "STOPWATCH",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        )
                        Text(
                            text = "// 10MS HIGH-PRECISION ENGINE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = NothingRed
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Spacer(modifier = Modifier.height(16.dp))

            // Dial Visualizer (60-second sweep with 60 ambient dots)
            Box(
                modifier = Modifier
                    .size(270.dp),
                contentAlignment = Alignment.Center
            ) {
                val dotColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                val activeRingColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                val redColor = NothingRed

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = (size.minDimension / 2f) - 16.dp.toPx()

                    // Ambient 60 Dots
                    for (i in 0 until 60) {
                        val angleDeg = i * 6f - 90f
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val x = center.x + radius * cos(angleRad).toFloat()
                        val y = center.y + radius * sin(angleRad).toFloat()

                        when {
                            i == 0 -> {
                                drawCircle(color = redColor, radius = 4.5.dp.toPx(), center = Offset(x, y))
                            }
                            i % 15 == 0 -> {
                                drawCircle(color = activeRingColor, radius = 3.dp.toPx(), center = Offset(x, y))
                            }
                            else -> {
                                drawCircle(color = dotColor, radius = 1.5.dp.toPx(), center = Offset(x, y))
                            }
                        }
                    }

                    // Smooth 60-second sweep track
                    val sweepFraction = ((uiState.elapsedMillis % 60000L) / 60000f)
                    val sweepAngle = sweepFraction * 360f

                    if (uiState.elapsedMillis > 0) {
                        drawArc(
                            color = redColor.copy(alpha = 0.9f),
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Bead at sweep head
                        val headRad = Math.toRadians((sweepAngle - 90f).toDouble())
                        val beadX = center.x + radius * cos(headRad).toFloat()
                        val beadY = center.y + radius * sin(headRad).toFloat()
                        drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(beadX, beadY))
                        drawCircle(color = redColor, radius = 3.dp.toPx(), center = Offset(beadX, beadY))
                    }
                }

                // Digits Display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.formattedTime,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = ".${uiState.formattedMillis}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NothingRed,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = if (uiState.isRunning) "● RUNNING" else if (uiState.elapsedMillis > 0) "⏸ PAUSED" else "IDLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isRunning) NothingRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lap / Reset Button
                val isReset = !uiState.isRunning && uiState.elapsedMillis > 0
                val canLap = uiState.isRunning

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                        .clickable(enabled = canLap || isReset) {
                            if (isReset) {
                                viewModel.reset()
                            } else if (canLap) {
                                viewModel.lap()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isReset) Icons.Filled.Refresh else Icons.Filled.Flag,
                        contentDescription = if (isReset) "Reset" else "Lap",
                        tint = if (canLap || isReset) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Primary Start / Pause Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(NothingRed)
                        .border(1.dp, NothingRedLight.copy(alpha = 0.6f), CircleShape)
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = uiState.isRunning,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
                        label = "StopwatchPlayPause"
                    ) { isRunning ->
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Start",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Laps History Card
            if (uiState.laps.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "LAP",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "LAP TIME",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "SPLIT TOTAL",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        val minLap = uiState.laps.minByOrNull { it.lapTimeMillis }?.lapTimeMillis
                        val maxLap = uiState.laps.maxByOrNull { it.lapTimeMillis }?.lapTimeMillis
                        val hasMultiLaps = uiState.laps.size >= 2

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.laps, key = { it.lapNumber }) { lap ->
                                val isBest = hasMultiLaps && lap.lapTimeMillis == minLap
                                val isWorst = hasMultiLaps && lap.lapTimeMillis == maxLap
                                val lapColor = when {
                                    isBest -> Color(0xFF10B981) // Emerald Green
                                    isWorst -> NothingRed
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "%02d".format(lap.lapNumber),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "+${formatMillisToTime(lap.lapTimeMillis)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        fontFamily = FontFamily.Monospace,
                                        color = lapColor
                                    )
                                    Text(
                                        text = formatMillisToTime(lap.splitTimeMillis),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "// TAP FLAG TO RECORD LAPS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}
