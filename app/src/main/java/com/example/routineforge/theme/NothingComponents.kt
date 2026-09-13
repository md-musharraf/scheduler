package com.example.routineforge.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Nothing OS Micro-Dotted Divider Line
 */
@Composable
fun NothingDottedDivider(
    modifier: Modifier = Modifier,
    dotColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
    dotRadius: Dp = 1.2.dp,
    spacing: Dp = 6.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        val radiusPx = dotRadius.toPx()
        val spacingPx = spacing.toPx()
        var currentX = radiusPx
        val y = size.height / 2f

        while (currentX < size.width) {
            drawCircle(
                color = dotColor,
                radius = radiusPx,
                center = Offset(currentX, y)
            )
            currentX += spacingPx
        }
    }
}

/**
 * Nothing OS 1-Tap Theme Switcher Pill [ ● DARK ] / [ ○ LIGHT ]
 */
@Composable
fun NothingThemeTogglePill(
    currentMode: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = currentMode == "DARK"
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable { onToggle() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            // Status Dot (Red when dark, white/black when light)
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isDark) NothingRed else MaterialTheme.colorScheme.onSurface)
            )
            Spacer(modifier = Modifier.width(6.dp))
            AnimatedContent(
                targetState = if (isDark) "DARK" else "LIGHT",
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ThemeToggleText"
            ) { modeText ->
                Text(
                    text = modeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

/**
 * Nothing OS Monospace Pill Tag
 */
@Composable
fun NothingPillTag(
    text: String,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false,
    leadingDotColor: Color? = null
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isHighlight) NothingRedSubtle else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (isHighlight) NothingRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            if (leadingDotColor != null) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(leadingDotColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    color = if (isHighlight) NothingRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Nothing OS Tracked Monospace Section Header
 */
@Composable
fun NothingSectionHeader(
    index: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "( $index )",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = NothingRed
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
