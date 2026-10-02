package com.example.routineforge.feature.notes

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routineforge.core.database.Note
import com.example.routineforge.core.database.Subject
import com.example.routineforge.core.theme.FF
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.core.ui.EmptyState
import com.example.routineforge.core.ui.FFCard
import com.example.routineforge.core.ui.IconBadge
import com.example.routineforge.core.ui.ScreenHeader
import com.example.routineforge.core.ui.ShowMessages
import com.example.routineforge.core.ui.SubjectBadge
import com.example.routineforge.core.ui.appViewModel
import com.example.routineforge.data.DrawingStrokes
import com.example.routineforge.data.Stroke
import com.example.routineforge.navigation.AppNav
import com.example.routineforge.navigation.NoteEditorNav
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NotesScreen(nav: AppNav) {
    val vm = appViewModel { NotesViewModel(it) }
    ShowMessages(vm)
    val ui by vm.ui.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Note?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Notes", actions = {
                IconButton(onClick = { searching = !searching; if (!searching) vm.setQuery("") }) {
                    Icon(if (searching) Icons.Filled.Close else Icons.Filled.Search, contentDescription = if (searching) "Close search" else "Search notes")
                }
            })
            if (searching) {
                OutlinedTextField(
                    value = ui.query, onValueChange = vm::setQuery, placeholder = { Text("Search titles, text and tags") },
                    singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(NoteFilter.entries) { f -> FilterChip(ui.filter == f, { vm.setFilter(f); vm.setSubject(null) }, { Text(f.label) }) }
            }
            if (ui.filter == NoteFilter.SUBJECTS && ui.subjects.isNotEmpty()) {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(ui.subjectId == null, { vm.setSubject(null) }, { Text("Any subject") }) }
                    items(ui.subjects, key = { it.id }) { s -> FilterChip(ui.subjectId == s.id, { vm.setSubject(s.id) }, { Text(s.name) }) }
                }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!ui.loading && ui.rows.isEmpty()) {
                    item {
                        if (ui.total == 0) EmptyState(Icons.Filled.EditNote, "Create your first note", "Write, make checklists, add images and draw diagrams.", actionLabel = "New note", onAction = { nav.push(NoteEditorNav()) })
                        else EmptyState(Icons.Filled.Search, "No matching notes", "Try another search or filter.")
                    }
                }
                items(ui.rows, key = { it.note.id }) { row ->
                    NoteCard(
                        row.note, row.subject, imagePath = row.imagePath, drawing = row.drawing, checklist = row.checklist,
                        onClick = { nav.push(NoteEditorNav(row.note.id)) },
                        onFavorite = { vm.toggleFavorite(row.note) },
                        onPin = { vm.togglePinned(row.note) },
                        onDelete = { confirmDelete = row.note }
                    )
                }
            }
        }
        FloatingActionButton(onClick = { nav.push(NoteEditorNav()) }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "New note")
        }
    }

    confirmDelete?.let { n ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete note?") },
            text = { Text("\"${n.title.ifBlank { "Untitled" }}\" and its images and drawings will be deleted.") },
            confirmButton = { TextButton(onClick = { vm.delete(n); confirmDelete = null }) { Text("Delete", color = FF.colors.danger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun NoteCard(
    note: Note,
    subject: Subject?,
    modifier: Modifier = Modifier,
    imagePath: String? = null,
    drawing: List<Stroke>? = null,
    checklist: Pair<Int, Int>? = null,
    onClick: () -> Unit,
    onFavorite: (() -> Unit)? = null,
    onPin: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var menu by remember { mutableStateOf(false) }
    FFCard(modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (subject != null) SubjectBadge(subject.icon, subject.color, size = 42.dp)
            else IconBadge(Icons.Filled.EditNote, MaterialTheme.colorScheme.primary, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) Icon(Icons.Filled.PushPin, contentDescription = "Pinned", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(note.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (note.isFavorite) Icon(Icons.Filled.Star, contentDescription = "Important", modifier = Modifier.padding(start = 4.dp).size(16.dp), tint = FF.colors.warning)
                }
                val preview = note.plainText.lineSequence().firstOrNull { it.isNotBlank() }
                if (preview != null) Text(preview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(subject?.name, checklist?.let { "${it.first}/${it.second} done" }, TimeFormat.relative(note.updatedAt)).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                imagePath != null -> {
                    val bmp = rememberBitmap(imagePath, 160)
                    if (bmp != null) Image(bmp, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.padding(start = 8.dp).size(width = 64.dp, height = 52.dp).clip(RoundedCornerShape(10.dp)))
                }
                !drawing.isNullOrEmpty() -> StrokesPreview(drawing, Modifier.padding(start = 8.dp).width(64.dp).aspectRatio(DrawingStrokes.ASPECT))
                checklist != null -> Icon(Icons.Filled.CheckBox, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
            }
            if (onDelete != null) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More options") }
                    DropdownMenu(menu, { menu = false }) {
                        onFavorite?.let { DropdownMenuItem({ Text(if (note.isFavorite) "Remove from important" else "Mark important") }, { menu = false; it() }, leadingIcon = { Icon(if (note.isFavorite) Icons.Filled.StarBorder else Icons.Filled.Star, null) }) }
                        onPin?.let { DropdownMenuItem({ Text(if (note.isPinned) "Unpin" else "Pin to top") }, { menu = false; it() }, leadingIcon = { Icon(Icons.Outlined.PushPin, null) }) }
                        DropdownMenuItem({ Text("Delete", color = FF.colors.danger) }, { menu = false; onDelete() }, leadingIcon = { Icon(Icons.Filled.Delete, null, tint = FF.colors.danger) })
                    }
                }
            }
        }
    }
}

/** Decodes a local image off the main thread, downsampled to about [maxPx]. */
@Composable
fun rememberBitmap(path: String, maxPx: Int): ImageBitmap? {
    val state = produceState<ImageBitmap?>(null, path, maxPx) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= maxPx && bounds.outHeight / (sample * 2) >= maxPx) sample *= 2
                BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }.getOrNull()
        }
    }
    return state.value
}

/** Paper-backed, read-only rendering of a drawing. */
@Composable
fun StrokesPreview(strokes: List<Stroke>, modifier: Modifier = Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(10.dp)).background(Color.White).border(1.dp, FF.colors.cardBorder, RoundedCornerShape(10.dp))) { drawStrokes(strokes) }
}

fun DrawScope.drawStrokes(strokes: List<Stroke>, live: Stroke? = null) {
    val w = size.width
    val h = size.height
    for (s in if (live != null) strokes + live else strokes) {
        val color = Color(s.color)
        val width = (s.width * w).coerceAtLeast(1f)
        if (s.points.size == 2) {
            drawCircle(color, width / 2, Offset(s.points[0] * w, s.points[1] * h))
            continue
        }
        val path = Path()
        var i = 0
        while (i + 1 < s.points.size) {
            val x = s.points[i] * w
            val y = s.points[i + 1] * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            i += 2
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
