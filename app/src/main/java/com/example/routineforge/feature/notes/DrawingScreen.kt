package com.example.routineforge.feature.notes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routineforge.core.theme.FF
import com.example.routineforge.core.ui.ScreenHeader
import com.example.routineforge.core.ui.ShowMessages
import com.example.routineforge.core.ui.appViewModel
import com.example.routineforge.data.DrawingStrokes
import com.example.routineforge.data.Stroke
import com.example.routineforge.navigation.AppNav

private val INKS = listOf(0xFF1E2A5A, 0xFF111827, 0xFF5B5FEF, 0xFFE5484D, 0xFF22B59A, 0xFFF2994A, 0xFF8B5CF6)
/** Stroke widths as a fraction of the canvas width. */
private val WIDTHS = listOf(0.005f, 0.01f, 0.02f)

@Composable
fun DrawingScreen(drawingId: Long, nav: AppNav) {
    val vm = appViewModel(key = "drawing-$drawingId") { DrawingViewModel(it, drawingId) }
    ShowMessages(vm)
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    var color by rememberSaveable { mutableLongStateOf(INKS[0]) }
    var width by rememberSaveable { mutableFloatStateOf(WIDTHS[1]) }
    var eraser by rememberSaveable { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var live by remember { mutableStateOf<Stroke?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Drawing", onBack = nav::back, actions = {
            IconButton(onClick = vm::undo, enabled = vm.canUndo) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo") }
            IconButton(onClick = vm::redo, enabled = vm.canRedo) { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo") }
            IconButton(onClick = { confirmClear = true }, enabled = vm.strokes.isNotEmpty()) { Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear drawing") }
            IconButton(onClick = nav::back) { Icon(Icons.Filled.Check, contentDescription = "Done") }
        })
        Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            if (loaded) {
                Canvas(
                    Modifier.aspectRatio(DrawingStrokes.ASPECT, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, FF.colors.cardBorder, RoundedCornerShape(16.dp))
                        .clipToBounds()
                        .semantics { contentDescription = "Drawing canvas" }
                        .pointerInput(eraser, color, width) {
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            detectTapGestures { p ->
                                if (eraser) {
                                    vm.beginErase(); vm.eraseAt(p.x / w, p.y / h, 0.03f)
                                } else vm.add(Stroke(color, width, listOf(p.x / w, p.y / h)))
                            }
                        }
                        .pointerInput(eraser, color, width) {
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            detectDragGestures(
                                onDragStart = { p ->
                                    if (eraser) vm.beginErase() else live = Stroke(color, width, listOf(p.x / w, p.y / h))
                                },
                                onDrag = { change, _ ->
                                    val x = (change.position.x / w).coerceIn(0f, 1f)
                                    val y = (change.position.y / h).coerceIn(0f, 1f)
                                    if (eraser) vm.eraseAt(x, y, 0.03f)
                                    else live = live?.let { it.copy(points = it.points + x + y) }
                                    change.consume()
                                },
                                onDragEnd = { live?.let(vm::add); live = null },
                                onDragCancel = { live?.let(vm::add); live = null }
                            )
                        }
                ) { drawStrokes(vm.strokes, live) }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    INKS.forEachIndexed { i, c ->
                        val selected = !eraser && color == c
                        Surface(
                            onClick = { color = c; eraser = false },
                            shape = CircleShape,
                            color = Color(c),
                            modifier = Modifier.size(34.dp).semantics { this.selected = selected; contentDescription = "Ink ${i + 1}" }
                                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape) else Modifier)
                        ) {}
                    }
                }
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Width", style = MaterialTheme.typography.labelLarge)
                    WIDTHS.forEachIndexed { i, wv ->
                        val selected = width == wv
                        Surface(
                            onClick = { width = wv; eraser = false },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected && !eraser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(44.dp, 36.dp).semantics { this.selected = selected; contentDescription = listOf("Thin", "Medium", "Thick")[i] }
                        ) {
                            Box(contentAlignment = Alignment.Center) { Box(Modifier.size((6 + i * 5).dp).clip(CircleShape).background(Color(color))) }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = { eraser = !eraser },
                        shape = RoundedCornerShape(10.dp),
                        color = if (eraser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.semantics { selected = eraser }
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoFixNormal, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Eraser")
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear drawing?") },
            text = { Text("You can undo this.") },
            confirmButton = { TextButton(onClick = { vm.clear(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}
