package com.example.routineforge.ui.routines

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History

import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routineforge.data.Routine
import com.example.routineforge.data.RoutineCategory
import com.example.routineforge.data.StepType
import com.example.routineforge.theme.BrandPrimary
import com.example.routineforge.theme.BrandSecondary
import com.example.routineforge.theme.BrandTertiary
import com.example.routineforge.theme.DarkBg
import com.example.routineforge.theme.NothingDottedDivider
import com.example.routineforge.theme.NothingPillTag
import com.example.routineforge.theme.NothingRed
import com.example.routineforge.theme.NothingSectionHeader
import com.example.routineforge.theme.NothingThemeTogglePill
import com.example.routineforge.theme.RestColor
import com.example.routineforge.theme.WorkColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineListScreen(
    viewModel: RoutineListViewModel,
    onStartRoutine: (String) -> Unit,
    onEditRoutine: (String?) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenStopwatch: () -> Unit,
    onOpenSpecialTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showClearDbDialog by remember { mutableStateOf(false) }
    var routineToDelete by remember { mutableStateOf<Routine?>(null) }
    var routineToSchedule by remember { mutableStateOf<Routine?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(com.example.routineforge.R.drawable.app_brand_logo),
                            contentDescription = "RoutineForge Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(7.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "ROUTINEFORGE",
                                maxLines = 1,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.sp
                                )
                            )
                            Text(
                                text = "// PROTOCOL",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = NothingRed
                                )
                            )
                        }
                    }
                },
                actions = {
                    // Nothing 1-Tap Theme Switcher
                    NothingThemeTogglePill(
                        currentMode = uiState.themeMode,
                        onToggle = { viewModel.toggleThemeMode() }
                    )

                    Spacer(modifier = Modifier.width(2.dp))

                    // Calendar / Schedule Planner Button
                    IconButton(
                        onClick = onOpenCalendar
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = "Calendar Planner",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // History / Analytics Button
                    IconButton(
                        onClick = onOpenHistory
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = "History",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            if (uiState.completedSessions.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(NothingRed)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }

                    // Clear Database Action Button
                    IconButton(
                        onClick = { showClearDbDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear Database",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEditRoutine(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "New Routine", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
            ) {
            // 3-Tab Mode Switcher (Protocols, Special Timer, Stopwatch)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeTabPill(
                    label = "PROTOCOLS",
                    count = uiState.routines.size,
                    isSelected = true,
                    onClick = { /* already here */ },
                    modifier = Modifier.weight(1f)
                )
                ModeTabPill(
                    label = "TIMER",
                    count = null,
                    isSelected = false,
                    onClick = onOpenSpecialTimer,
                    modifier = Modifier.weight(1f)
                )
                ModeTabPill(
                    label = "STOPWATCH",
                    count = null,
                    isSelected = false,
                    onClick = onOpenStopwatch,
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search routines, exercises, topics...") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // AI Smart Search Prompt Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                uiState.smartSuggestions.forEach { suggestion ->
                    val isSelected = uiState.searchQuery.equals(suggestion.query, ignoreCase = true)
                    NothingPillTag(
                        text = suggestion.label,
                        isHighlight = isSelected,
                        leadingDotColor = if (isSelected) NothingRed else null,
                        modifier = Modifier.clickable {
                            if (isSelected) {
                                viewModel.updateSearchQuery("")
                            } else {
                                viewModel.updateSearchQuery(suggestion.query)
                            }
                        }
                    )
                }
            }

            // Category Filter Chips
            CategoryChipsRow(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onSelectCategory = { viewModel.selectCategory(it) },
                onAddCategoryClick = { showAddCategoryDialog = true },
                onDeleteCategory = { viewModel.deleteCategory(it) }
            )

            if (uiState.searchQuery.isNotBlank()) {
                NothingSectionHeader(
                    index = "${uiState.filteredRoutines.size}",
                    title = "AI Matches: \"${uiState.searchQuery}\""
                )
            }

            // Routine List
            if (uiState.filteredRoutines.isEmpty()) {
                EmptyRoutineState(
                    selectedCategory = uiState.categories.firstOrNull { it.id == uiState.selectedCategoryId },
                    onCreateRoutine = { onEditRoutine(null) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.filteredRoutines, key = { it.id }) { routine ->
                        val category = uiState.categories.firstOrNull { it.id == routine.categoryId }
                        RoutineCard(
                            routine = routine,
                            category = category,
                            onPlay = { onStartRoutine(routine.id) },
                            onEdit = { onEditRoutine(routine.id) },
                            onDuplicate = { viewModel.duplicateRoutine(routine) },
                            onSchedule = { routineToSchedule = routine },
                            onDelete = { routineToDelete = routine }
                        )
                    }
                }
            }
        }
    }
}


    // Delete Confirmation Dialog
    if (routineToDelete != null) {
        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            title = { Text("Delete Routine?") },
            text = { Text("Are you sure you want to delete \"${routineToDelete?.title}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        routineToDelete?.let { viewModel.deleteRoutine(it.id) }
                        routineToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { routineToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Custom Category Dialog
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onAdd = { name, emoji, colorHex ->
                viewModel.addCustomCategory(name, emoji, colorHex)
                showAddCategoryDialog = false
            }
        )
    }

    // Quick Schedule Dialog
    if (routineToSchedule != null) {
        val routine = routineToSchedule!!
        val context = androidx.compose.ui.platform.LocalContext.current
        QuickScheduleDialog(
            routine = routine,
            onDismiss = { routineToSchedule = null },
            onConfirm = { dateEpochDay, timeStr ->
                viewModel.scheduleRoutine(context, routine, dateEpochDay, timeStr)
                routineToSchedule = null
            }
        )
    }

    // Clear All Data Confirmation Dialog
    if (showClearDbDialog) {
        AlertDialog(
            onDismissRequest = { showClearDbDialog = false },
            title = {
                Text(
                    text = "CLEAR DATABASE?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all routines, sessions, and schedules so you can start completely fresh. Are you sure?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDbDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed)
                ) {
                    Text("Clear All Data", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDbDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ModeTabPill(
    label: String,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(NothingRed)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            )
            if (count != null && count > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "($count)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

@Composable
fun StatsSummaryBanner(
    totalRoutines: Int,
    completedSessions: Int,
    onOpenHistory: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .clickable { onOpenHistory() }
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NothingRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "$totalRoutines PROTOCOLS",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (completedSessions > 0) "$completedSessions SESSIONS FINISHED" else "SYS // READY",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { onOpenCalendar() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "CALENDAR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { onOpenHistory() }
                ) {
                    Text(
                        text = "STATS →",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChipsRow(
    categories: List<RoutineCategory>,
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
    onAddCategoryClick: () -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isDark = MaterialTheme.colorScheme.background == DarkBg

        // "All" chip
        FilterChip(
            selected = selectedCategoryId == null,
            onClick = { onSelectCategory(null) },
            label = {
                Text(
                    "ALL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (selectedCategoryId == null) NothingRed else MaterialTheme.colorScheme.outline
            ),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = if (isDark) Color.White else Color.Black,
                selectedLabelColor = if (isDark) Color.Black else Color.White,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // Predefined & custom categories
        categories.forEach { category ->
            val isSelected = selectedCategoryId == category.id
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (isSelected) {
                        onSelectCategory(null)
                    } else {
                        onSelectCategory(category.id)
                    }
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) NothingRed else Color(category.colorHex))
                    )
                },
                label = {
                    Text(
                        category.name.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) NothingRed else MaterialTheme.colorScheme.outline
                ),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (isDark) Color.White else Color.Black,
                    selectedLabelColor = if (isDark) Color.Black else Color.White,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // Add Category Chip Button
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                .clickable { onAddCategoryClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "NEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RoutineCard(
    routine: Routine,
    category: RoutineCategory?,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onSchedule: () -> Unit,
    onDelete: () -> Unit
) {
    val workSteps = routine.steps.count { it.stepType == StepType.WORK }
    val restSteps = routine.steps.count { it.stepType == StepType.REST }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Category badge & Action menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                category?.let {
                    Box(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                        NothingPillTag(
                            text = it.name.uppercase(),
                            leadingDotColor = NothingRed
                        )
                    }
                } ?: Spacer(modifier = Modifier.width(1.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onSchedule,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = "Schedule Routine",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "Duplicate",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Routine Title
            Text(
                text = routine.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            // Routine Description
            if (routine.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = routine.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            NothingDottedDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Steps overview chips (Responsive horizontal scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NothingPillTag(
                    text = "[ ${routine.formattedDuration} ]",
                    isHighlight = true
                )

                NothingPillTag(
                    text = "[ ${routine.steps.size} STEPS ]"
                )

                if (workSteps > 0) {
                    NothingPillTag(
                        text = "$workSteps WORK",
                        leadingDotColor = NothingRed
                    )
                }

                if (restSteps > 0) {
                    NothingPillTag(
                        text = "$restSteps REST",
                        leadingDotColor = RestColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Start Countdown Timer Button (Nothing OS Stark Pill Button)
            Button(
                onClick = onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "START PROTOCOL",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun EmptyRoutineState(
    selectedCategory: RoutineCategory?,
    onCreateRoutine: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = selectedCategory?.emoji ?: "📋",
                fontSize = 52.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (selectedCategory != null) "No routines in ${selectedCategory.name}" else "No routines found",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Create your own custom steps, intervals, and timers!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onCreateRoutine,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Custom Routine")
            }
        }
    }
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, emoji: String, colorHex: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📚") }
    var selectedColorHex by remember { mutableStateOf(0xFF6366F1) }

    val emojis = listOf("📚", "💪", "💻", "🧘", "⚡", "🏃", "🎨", "🎵", "💼", "🧠", "🍳", "🎯")
    val colors = listOf(
        0xFF6366F1, // Indigo
        0xFFEF4444, // Red
        0xFF06B6D4, // Cyan
        0xFF10B981, // Emerald
        0xFFF59E0B, // Amber
        0xFF8B5CF6, // Purple
        0xFFEC4899  // Pink
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Category", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name (e.g. Yoga, Reading, Guitar)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Select Icon / Emoji:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emojis.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selectedEmoji == emoji) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Select Accent Color:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colors.forEach { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .clickable { selectedColorHex = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColorHex == colorHex) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name, selectedEmoji, selectedColorHex)
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Category")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickScheduleDialog(
    routine: Routine,
    onDismiss: () -> Unit,
    onConfirm: (dateEpochDay: Long, timeOfDay: String) -> Unit
) {
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var timeOfDay by remember { mutableStateOf("09:00") }
    val quickTimes = listOf("07:00", "09:00", "14:00", "18:00", "21:00")
    val dayOptions = remember {
        listOf(
            "Today" to today,
            "Tomorrow" to today.plusDays(1),
            "+2 Days" to today.plusDays(2),
            "+3 Days" to today.plusDays(3),
            "Next Week" to today.plusDays(7)
        )
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Schedule Routine",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = routine.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Select Day:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dayOptions.forEach { (label, date) ->
                        FilterChip(
                            selected = selectedDate == date,
                            onClick = { selectedDate = date },
                            label = { Text(label, fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Selected: ${selectedDate.format(dateFormatter)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Select Time:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))

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
                            label = { Text(qTime, fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = timeOfDay,
                    onValueChange = { timeOfDay = it },
                    label = { Text("Custom Time (HH:mm)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedDate.toEpochDay(), timeOfDay.trim().ifEmpty { "09:00" })
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

