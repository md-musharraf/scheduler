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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material.icons.filled.MoreVert

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
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
    onScheduleRoutine: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var routineToDelete by remember { mutableStateOf<Routine?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Routines", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEditRoutine(null) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New routine") }
            )
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
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search routines") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                CategoryChipsRow(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onSelectCategory = { viewModel.selectCategory(it) },
                    onAddCategoryClick = { showAddCategoryDialog = true },
                    onDeleteCategory = { viewModel.deleteCategory(it) }
                )

                if (uiState.filteredRoutines.isEmpty()) {
                    EmptyRoutineState(
                        selectedCategory = uiState.categories.firstOrNull { it.id == uiState.selectedCategoryId },
                        onCreateRoutine = { onEditRoutine(null) }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.filteredRoutines, key = { it.id }) { routine ->
                            RoutineCard(
                                routine = routine,
                                category = uiState.categories.firstOrNull { it.id == routine.categoryId },
                                onPlay = { onStartRoutine(routine.id) },
                                onEdit = { onEditRoutine(routine.id) },
                                onDuplicate = { viewModel.duplicateRoutine(routine) },
                                onSchedule = { onScheduleRoutine(routine.id) },
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
    var showMenu by remember { mutableStateOf(false) }
    val accent = Color(category?.colorHex ?: routine.colorHex)
    val stepCount = routine.steps.size

    Card(
        onClick = onPlay,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(category?.emoji ?: "⏱", fontSize = 20.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = routine.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        routine.formattedDuration,
                        if (stepCount == 1) "1 step" else "$stepCount steps",
                        category?.name
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FilledIconButton(onClick = onPlay) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Start ${routine.title}")
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Routine options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Schedule") },
                        leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                        onClick = { showMenu = false; onSchedule() }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                        onClick = { showMenu = false; onDuplicate() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
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
