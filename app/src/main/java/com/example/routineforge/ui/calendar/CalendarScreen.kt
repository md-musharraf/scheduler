package com.example.routineforge.ui.calendar

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routineforge.data.CompletedSession
import com.example.routineforge.data.Routine
import com.example.routineforge.data.ScheduledRoutine
import com.example.routineforge.theme.BrandPrimary
import com.example.routineforge.theme.BrandSecondary
import com.example.routineforge.theme.BrandTertiary
import com.example.routineforge.theme.DarkBg
import com.example.routineforge.theme.NothingDottedDivider
import com.example.routineforge.theme.NothingPillTag
import com.example.routineforge.theme.NothingRed
import com.example.routineforge.theme.NothingSectionHeader
import com.example.routineforge.theme.WorkColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onStartRoutine: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showScheduleDialog by remember { mutableStateOf(false) }
    var isWeekView by remember { mutableStateOf(false) }

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault()) }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = NothingRed
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "CALENDAR",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Text(
                                    text = "// PROTOCOL PLANNER",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = NothingRed
                                    )
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Today Quick Button
                        TextButton(onClick = { viewModel.jumpToToday() }) {
                            NothingPillTag(text = "TODAY", isHighlight = true, leadingDotColor = NothingRed)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                NothingDottedDivider()
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Header Controls Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Month Selector Row (< September 2026 >)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                if (isWeekView) {
                                    viewModel.selectDate(uiState.selectedDate.minusWeeks(1))
                                } else {
                                    viewModel.changeMonth(-1)
                                }
                            }) {
                                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous")
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isWeekView) {
                                        uiState.selectedDate.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())).uppercase()
                                    } else {
                                        uiState.displayedMonth.format(monthFormatter).uppercase()
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 15.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                )

                                NothingPillTag(
                                    text = if (isWeekView) "7D" else "30D",
                                    isHighlight = isWeekView,
                                    leadingDotColor = if (isWeekView) NothingRed else null,
                                    modifier = Modifier.clickable { isWeekView = !isWeekView }
                                )
                            }

                            IconButton(onClick = {
                                if (isWeekView) {
                                    viewModel.selectDate(uiState.selectedDate.plusWeeks(1))
                                } else {
                                    viewModel.changeMonth(1)
                                }
                            }) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Next")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Weekday Headers (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            val daysOfWeek = listOf(
                                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
                            )
                            daysOfWeek.forEach { day ->
                                Text(
                                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Month / Week Day Grid
                        CalendarGrid(
                            displayedMonth = uiState.displayedMonth,
                            selectedDate = uiState.selectedDate,
                            isWeekView = isWeekView,
                            scheduledDateSet = uiState.scheduledDateSet,
                            completedDateSet = uiState.completedDateSet,
                            onDateSelected = { viewModel.selectDate(it) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Legend (Scheduled Dot vs Completed Dot)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NothingRed)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "SCHEDULED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "COMPLETED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Selected Day Header & Schedule Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = uiState.selectedDate.format(dayFormatter),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${uiState.scheduledForSelectedDate.size} SCHEDULED • ${uiState.completedSessionsForSelectedDate.size} COMPLETED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { showScheduleDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NothingRed,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PLAN",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }

            // Scheduled Routines Section
            if (uiState.scheduledForSelectedDate.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No routines scheduled for this date",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Plan ahead! Add your Gym, Study, or Communication routine for this day.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { showScheduleDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("+ Schedule for ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))}")
                            }
                        }
                    }
                }
            } else {
                items(uiState.scheduledForSelectedDate, key = { it.id }) { scheduled ->
                    ScheduledRoutineCard(
                        item = scheduled,
                        onStart = { onStartRoutine(scheduled.routineId) },
                        onToggleCompleted = { viewModel.toggleScheduledCompleted(scheduled.id) },
                        onDelete = { viewModel.deleteScheduledRoutine(scheduled.id) }
                    )
                }
            }

            // Completed Sessions On Selected Day Section
            if (uiState.completedSessionsForSelectedDate.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Completed Sessions On This Day (${uiState.completedSessionsForSelectedDate.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = WorkColor
                    )
                }

                items(uiState.completedSessionsForSelectedDate, key = { it.id }) { session ->
                    CompletedDaySessionCard(session = session)
                }
            }
        }
    }

    // Schedule Routine Dialog Modal
    if (showScheduleDialog) {
        ScheduleRoutineDialog(
            availableRoutines = uiState.availableRoutines,
            initialDate = uiState.selectedDate,
            onDismiss = { showScheduleDialog = false },
            onSchedule = { routine, date, timeStr ->
                viewModel.scheduleRoutine(routine, date, timeStr)
                showScheduleDialog = false
            }
        )
    }
}

@Composable
fun CalendarGrid(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    isWeekView: Boolean,
    scheduledDateSet: Set<Long>,
    completedDateSet: Set<Long>,
    onDateSelected: (LocalDate) -> Unit
) {
    if (isWeekView) {
        val dayOfWeek = selectedDate.dayOfWeek.value
        val startOfWeek = selectedDate.minusDays((dayOfWeek - 1).toLong())
        val today = LocalDate.now()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            for (i in 0 until 7) {
                val cellDate = startOfWeek.plusDays(i.toLong())
                val cellEpoch = cellDate.toEpochDay()
                val isSelected = cellDate == selectedDate
                val isToday = cellDate == today
                val hasScheduled = scheduledDateSet.contains(cellEpoch)
                val hasCompleted = completedDateSet.contains(cellEpoch)

                DayCell(
                    dayNumber = cellDate.dayOfMonth,
                    isSelected = isSelected,
                    isToday = isToday,
                    hasScheduled = hasScheduled,
                    hasCompleted = hasCompleted,
                    onClick = { onDateSelected(cellDate) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        MonthGrid(
            displayedMonth = displayedMonth,
            selectedDate = selectedDate,
            scheduledDateSet = scheduledDateSet,
            completedDateSet = completedDateSet,
            onDateSelected = onDateSelected
        )
    }
}

@Composable
fun MonthGrid(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    scheduledDateSet: Set<Long>,
    completedDateSet: Set<Long>,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = displayedMonth.atDay(1)
    val dayOfWeekOfFirst = firstDayOfMonth.dayOfWeek.value // 1 = Monday, 7 = Sunday
    val daysInMonth = displayedMonth.lengthOfMonth()
    val today = LocalDate.now()

    // Generate grid items (total 35 or 42 cells)
    val totalCells = ((dayOfWeekOfFirst - 1 + daysInMonth + 6) / 7) * 7

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in 0 until totalCells / 7) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - (dayOfWeekOfFirst - 1) + 1

                    if (dayNumber in 1..daysInMonth) {
                        val cellDate = displayedMonth.atDay(dayNumber)
                        val cellEpoch = cellDate.toEpochDay()
                        val isSelected = cellDate == selectedDate
                        val isToday = cellDate == today
                        val hasScheduled = scheduledDateSet.contains(cellEpoch)
                        val hasCompleted = completedDateSet.contains(cellEpoch)

                        DayCell(
                            dayNumber = dayNumber,
                            isSelected = isSelected,
                            isToday = isToday,
                            hasScheduled = hasScheduled,
                            hasCompleted = hasCompleted,
                            onClick = { onDateSelected(cellDate) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Empty cell outside current month
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasScheduled: Boolean,
    hasCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    val selectedBg = if (isDark) Color.White else Color.Black
    val selectedFg = if (isDark) Color.Black else Color.White

    Box(
        modifier = modifier
            .aspectRatio(1.05f)
            .padding(1.5.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> selectedBg
                    isToday -> MaterialTheme.colorScheme.surfaceVariant
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isToday && !isSelected) 1.dp else if (isSelected) 0.dp else 0.5.dp,
                color = if (isToday && !isSelected) NothingRed else if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$dayNumber",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                ),
                color = when {
                    isSelected -> selectedFg
                    isToday -> NothingRed
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            // Indicators row (scheduled dot in NothingRed)
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(6.dp)
            ) {
                if (hasScheduled) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) selectedFg else NothingRed)
                    )
                }
                if (hasCompleted) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) selectedFg else MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduledRoutineCard(
    item: ScheduledRoutine,
    onStart: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (item.isCompleted) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Completed Checkbox
                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = { onToggleCompleted() },
                    colors = CheckboxDefaults.colors(checkedColor = NothingRed)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Routine Title & Category Badge
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.routineTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.categoryName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = NothingRed
                        )
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            NothingDottedDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: Time badge & Start Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NothingPillTag(
                    text = "[ ${item.timeOfDay} ]",
                    isHighlight = true
                )

                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "START",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
        }
    }
}
}

@Composable
fun CompletedDaySessionCard(session: CompletedSession) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = NothingRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = session.routineTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${session.categoryName.uppercase()} • ${session.stepsCompleted} STEPS DONE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            NothingPillTag(
                text = "[ ${session.formattedDuration()} ]",
                isHighlight = true,
                leadingDotColor = NothingRed
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleRoutineDialog(
    availableRoutines: List<Routine>,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSchedule: (Routine, LocalDate, String) -> Unit
) {
    var selectedRoutine by remember { mutableStateOf(availableRoutines.firstOrNull()) }
    var timeOfDay by remember { mutableStateOf("09:00") }
    var selectedDate by remember { mutableStateOf(initialDate) }

    val quickTimes = listOf("07:00", "09:00", "14:00", "18:00", "21:00")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Plan & Schedule Routine",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select Routine:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (availableRoutines.isEmpty()) {
                    Text(text = "No routines available. Please create a routine first.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableRoutines.forEach { routine ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selectedRoutine?.id == routine.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .clickable { selectedRoutine = routine }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedRoutine?.id == routine.id,
                                    onClick = { selectedRoutine = routine }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = routine.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "${routine.steps.size} steps • ${routine.formattedDuration}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Scheduled Time (HH:mm):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Quick time chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickTimes.forEach { qTime ->
                        FilterChip(
                            selected = timeOfDay == qTime,
                            onClick = { timeOfDay = qTime },
                            label = { Text(qTime, fontSize = 11.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = timeOfDay,
                    onValueChange = { timeOfDay = it },
                    label = { Text("Custom Time (e.g. 07:30, 18:00)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Scheduled For: ${selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedRoutine?.let { routine ->
                        onSchedule(routine, selectedDate, timeOfDay)
                    }
                },
                enabled = selectedRoutine != null,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Schedule Routine")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
