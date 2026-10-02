package com.example.routineforge.feature.notes

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routineforge.core.database.AttachmentKind
import com.example.routineforge.core.theme.FF
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.core.ui.EmptyState
import com.example.routineforge.core.ui.ShowMessages
import com.example.routineforge.core.ui.SubjectBadge
import com.example.routineforge.core.ui.appViewModel
import com.example.routineforge.data.CheckItem
import com.example.routineforge.data.DrawingStrokes
import com.example.routineforge.navigation.AppNav
import com.example.routineforge.navigation.DrawingNav
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import java.io.File

private val HIGHLIGHT = Color(0x80FFD54F)
private val TEXT_COLORS = listOf(Color(0xFFE5484D), Color(0xFF5B5FEF), Color(0xFF22B59A), Color(0xFFF2994A), Color(0xFF8B5CF6))

@Composable
fun NoteEditorScreen(noteId: Long, subjectId: Long?, nav: AppNav) {
    val vm = appViewModel(key = "note-$noteId-$subjectId") { NoteEditorViewModel(it, noteId, subjectId) }
    ShowMessages(vm)
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val missing by vm.missing.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editTags by remember { mutableStateOf(false) }
    var pendingFocus by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.refreshDrawings() }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.addImage(it, AttachmentKind.IMAGE) } }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.addImage(it, AttachmentKind.FILE) } }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp -> bmp?.let(vm::addPhoto) }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { vm.focusedText()?.addTextAfterSelection("$it ") }
    }
    fun launchSafely(block: () -> Unit, unavailable: String) = try { block() } catch (_: ActivityNotFoundException) { vm.reportError(unavailable) }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = nav::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(if (vm.updatedAt > 0) "Saved · ${TimeFormat.relative(vm.updatedAt)}" else "New note", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.focusedText()?.history?.undo() }) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo") }
            IconButton(onClick = { vm.focusedText()?.history?.redo() }) { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo") }
            IconToggleButton(checked = vm.favorite, onCheckedChange = { vm.favorite = it }) {
                Icon(if (vm.favorite) Icons.Filled.Star else Icons.Filled.StarBorder, contentDescription = if (vm.favorite) "Remove from important" else "Mark important", tint = if (vm.favorite) FF.colors.warning else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More options") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Edit tags") }, { menu = false; editTags = true }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null) })
                    DropdownMenuItem({ Text("Delete note", color = FF.colors.danger) }, { menu = false; confirmDelete = true }, leadingIcon = { Icon(Icons.Filled.Delete, null, tint = FF.colors.danger) })
                }
            }
        }
        if (missing) {
            EmptyState(Icons.Filled.Delete, "Note not found", "It may have been deleted.", actionLabel = "Go back", onAction = nav::back)
            return@Column
        }
        if (!loaded) return@Column

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                BasicTextField(
                    value = vm.title,
                    onValueChange = { vm.title = it.replace("\n", "").take(120) },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box { if (vm.title.isEmpty()) Text("Title", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)); inner() }
                    }
                )
            }
            item { MetaRow(vm, subjects) }
            items(vm.blocks, key = { it.key }) { b ->
                when (b) {
                    is EditorBlock.Text -> TextBlockEditor(
                        b,
                        placeholder = when {
                            vm.blocks.size == 1 -> "Start writing…"
                            b === vm.blocks.last() -> "Continue writing…"
                            else -> null
                        },
                        onFocus = { vm.focusedKey = b.key }
                    )
                    is EditorBlock.Checklist -> ChecklistEditor(b, pendingFocus, { pendingFocus = it }, onFocus = { vm.focusedKey = b.key }, onRemoveBlock = { vm.removeBlock(b.key) })
                    is EditorBlock.Image -> RemovableBlock(onRemove = { vm.removeBlock(b.key) }) {
                        val bmp = b.attachment?.let { rememberBitmap(it.path, 1280) }
                        if (bmp != null) Image(bmp, contentDescription = "Image in note", contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)))
                        else Text("Image unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                    }
                    is EditorBlock.File -> RemovableBlock(onRemove = { vm.removeBlock(b.key) }) {
                        val a = b.attachment
                        Surface(
                            onClick = {
                                if (a == null) return@Surface
                                try {
                                    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", File(a.path))
                                    context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, a.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                                } catch (_: ActivityNotFoundException) {
                                    vm.reportError("No app on this device can open this file")
                                } catch (_: IllegalArgumentException) {
                                    vm.reportError("This file is no longer available")
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(a?.fileName ?: "File unavailable", style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (a != null) Text(a.mimeType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    is EditorBlock.Drawing -> RemovableBlock(onRemove = { vm.removeBlock(b.key) }) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(DrawingStrokes.ASPECT).clip(RoundedCornerShape(14.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                                .clickable(onClickLabel = "Edit drawing") { vm.saveNow(); nav.push(DrawingNav(b.drawingId)) },
                            contentAlignment = Alignment.Center
                        ) {
                            StrokesPreview(b.strokes, Modifier.fillMaxSize())
                            if (b.strokes.isEmpty()) Text("Tap to draw", color = Color(0xFF6B7280))
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Toolbar(
            state = vm.focusedText(),
            onChecklist = vm::addChecklist,
            onImage = { launchSafely({ pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, "No photo picker available") },
            onCamera = { launchSafely({ camera.launch(null) }, "No camera app available") },
            onFile = { launchSafely({ pickFile.launch(arrayOf("*/*")) }, "No file picker available") },
            onVoice = {
                launchSafely({
                    voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM))
                }, "Voice input isn't available on this device")
            },
            onDrawing = { vm.addDrawing { id -> nav.push(DrawingNav(id)) } }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete note?") },
            text = { Text("This note, its images and drawings will be deleted.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete { nav.back() } }) { Text("Delete", color = FF.colors.danger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
    if (editTags) {
        var text by rememberSaveable { mutableStateOf(vm.tags.replace(",", ", ")) }
        AlertDialog(
            onDismissRequest = { editTags = false },
            title = { Text("Tags") },
            text = { OutlinedTextField(text, { text = it.take(200) }, placeholder = { Text("exam, revision") }, supportingText = { Text("Separate tags with commas") }) },
            confirmButton = { TextButton(onClick = { vm.tags = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(","); editTags = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editTags = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MetaRow(vm: NoteEditorViewModel, subjects: List<com.example.routineforge.core.database.Subject>) {
    var open by remember { mutableStateOf(false) }
    val subject = subjects.firstOrNull { it.id == vm.subject }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.horizontalScroll(rememberScrollState())) {
        Box {
            Surface(onClick = { open = true }, shape = CircleShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (subject != null) { SubjectBadge(subject.icon, subject.color, size = 22.dp); Spacer(Modifier.width(6.dp)) }
                    Text(subject?.name ?: "No subject", style = MaterialTheme.typography.labelLarge)
                }
            }
            DropdownMenu(open, { open = false }) {
                DropdownMenuItem({ Text("No subject") }, { vm.subject = null; open = false })
                subjects.filter { !it.isArchived }.forEach { s -> DropdownMenuItem({ Text(s.name) }, { vm.subject = s.id; open = false }, leadingIcon = { SubjectBadge(s.icon, s.color, size = 24.dp) }) }
            }
        }
        vm.tags.split(',').filter { it.isNotBlank() }.forEach { t ->
            Spacer(Modifier.width(6.dp))
            Text("#$t", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TextBlockEditor(b: EditorBlock.Text, placeholder: String?, onFocus: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    LaunchedEffect(primary) { b.state.config.linkColor = primary }
    BasicRichTextEditor(
        state = b.state,
        modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) onFocus() },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(primary),
        decorationBox = { inner ->
            Box {
                if (placeholder != null && b.state.annotatedString.text.isEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
                inner()
            }
        }
    )
}

@Composable
private fun ChecklistEditor(b: EditorBlock.Checklist, pendingFocus: String?, setPendingFocus: (String?) -> Unit, onFocus: () -> Unit, onRemoveBlock: () -> Unit) {
    Column {
        b.items.forEachIndexed { i, item ->
            val requester = remember(item.key) { FocusRequester() }
            LaunchedEffect(pendingFocus) {
                if (pendingFocus == item.key) {
                    requester.requestFocus()
                    setPendingFocus(null)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item.done, onCheckedChange = { b.items[i] = item.copy(done = it) })
                BasicTextField(
                    value = item.text,
                    onValueChange = { v ->
                        if ('\n' in v) {
                            // Enter starts a new item below, like any checklist app.
                            val next = CheckItem(v.substringAfter('\n').replace("\n", ""))
                            b.items[i] = item.copy(text = v.substringBefore('\n'))
                            b.items.add(i + 1, next)
                            setPendingFocus(next.key)
                        } else b.items[i] = item.copy(text = v.take(300))
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (item.done) TextDecoration.LineThrough else null
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.weight(1f).focusRequester(requester).onFocusChanged { if (it.isFocused) onFocus() },
                    decorationBox = { inner -> Box { if (item.text.isEmpty()) Text("List item", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)); inner() } }
                )
                IconButton(onClick = { if (b.items.size > 1) b.items.removeAt(i) else onRemoveBlock() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove item", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        TextButton(onClick = { val n = CheckItem(); b.items.add(n); setPendingFocus(n.key) }) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add item")
        }
    }
}

@Composable
private fun RemovableBlock(onRemove: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        content()
        Surface(onClick = onRemove, shape = CircleShape, color = Color.Black.copy(alpha = 0.55f), contentColor = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(32.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
private fun Toolbar(
    state: RichTextState?,
    onChecklist: () -> Unit,
    onImage: () -> Unit,
    onCamera: () -> Unit,
    onFile: () -> Unit,
    onVoice: () -> Unit,
    onDrawing: () -> Unit
) {
    var colorMenu by remember { mutableStateOf(false) }
    var sizeMenu by remember { mutableStateOf(false) }
    var linkDialog by remember { mutableStateOf(false) }
    val style = state?.currentSpanStyle
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        FormatButton(Icons.Filled.FormatBold, "Bold", style?.fontWeight == FontWeight.Bold) { state?.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
        FormatButton(Icons.Filled.FormatItalic, "Italic", style?.fontStyle == FontStyle.Italic) { state?.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
        FormatButton(Icons.Filled.FormatUnderlined, "Underline", style?.textDecoration?.contains(TextDecoration.Underline) == true) { state?.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) }
        FormatButton(Icons.Filled.BorderColor, "Highlight", style?.background == HIGHLIGHT) { state?.toggleSpanStyle(SpanStyle(background = HIGHLIGHT)) }
        Box {
            FormatButton(Icons.Filled.FormatColorText, "Text color", style?.color?.let { it in TEXT_COLORS } == true) { colorMenu = true }
            DropdownMenu(colorMenu, { colorMenu = false }) {
                DropdownMenuItem({ Text("Default") }, { TEXT_COLORS.forEach { state?.removeSpanStyle(SpanStyle(color = it)) }; colorMenu = false })
                TEXT_COLORS.forEachIndexed { i, c ->
                    DropdownMenuItem(
                        { Text(listOf("Red", "Indigo", "Green", "Orange", "Purple")[i], color = c) },
                        { TEXT_COLORS.forEach { state?.removeSpanStyle(SpanStyle(color = it)) }; state?.addSpanStyle(SpanStyle(color = c)); colorMenu = false },
                        leadingIcon = { Box(Modifier.size(16.dp).clip(CircleShape).background(c)) }
                    )
                }
            }
        }
        Box {
            FormatButton(Icons.Filled.FormatSize, "Text size", false) { sizeMenu = true }
            DropdownMenu(sizeMenu, { sizeMenu = false }) {
                DropdownMenuItem({ Text("Heading", style = MaterialTheme.typography.titleLarge) }, { state?.setHeadingStyle(HeadingStyle.H2); sizeMenu = false })
                DropdownMenuItem({ Text("Subheading", style = MaterialTheme.typography.titleMedium) }, { state?.setHeadingStyle(HeadingStyle.H4); sizeMenu = false })
                DropdownMenuItem({ Text("Normal text") }, { state?.setHeadingStyle(HeadingStyle.Normal); SIZES.forEach { s -> state?.removeSpanStyle(SpanStyle(fontSize = s)) }; sizeMenu = false })
                DropdownMenuItem({ Text("Large text", fontSize = 20.sp) }, { SIZES.forEach { s -> state?.removeSpanStyle(SpanStyle(fontSize = s)) }; state?.addSpanStyle(SpanStyle(fontSize = 20.sp)); sizeMenu = false })
                DropdownMenuItem({ Text("Small text", fontSize = 13.sp) }, { SIZES.forEach { s -> state?.removeSpanStyle(SpanStyle(fontSize = s)) }; state?.addSpanStyle(SpanStyle(fontSize = 13.sp)); sizeMenu = false })
            }
        }
        FormatButton(Icons.AutoMirrored.Filled.FormatListBulleted, "Bullet list", state?.isUnorderedList == true) { state?.toggleUnorderedList() }
        FormatButton(Icons.Filled.FormatListNumbered, "Numbered list", state?.isOrderedList == true) { state?.toggleOrderedList() }
        FormatButton(Icons.Filled.CheckBox, "Checklist", false, onChecklist)
        FormatButton(Icons.Filled.Link, "Link", state?.isLink == true) { linkDialog = true }
        FormatButton(Icons.Filled.Image, "Insert image", false, onImage)
        FormatButton(Icons.Filled.CameraAlt, "Take photo", false, onCamera)
        FormatButton(Icons.Filled.AttachFile, "Attach file", false, onFile)
        FormatButton(Icons.Filled.Mic, "Voice typing", false, onVoice)
        FormatButton(Icons.Filled.Brush, "Drawing", false, onDrawing)
    }
    if (linkDialog && state != null) {
        val selected = state.selection.let { !it.collapsed }
        var text by rememberSaveable { mutableStateOf("") }
        var url by rememberSaveable { mutableStateOf("https://") }
        AlertDialog(
            onDismissRequest = { linkDialog = false },
            title = { Text("Add link") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!selected) OutlinedTextField(text, { text = it }, label = { Text("Text") }, singleLine = true)
                    OutlinedTextField(url, { url = it.trim() }, label = { Text("URL") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = url.length > 8 && (selected || text.isNotBlank()),
                    onClick = {
                        if (selected) state.addLinkToSelection(url) else state.addLink(text, url)
                        linkDialog = false
                    }
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { linkDialog = false }) { Text("Cancel") } }
        )
    }
}

private val SIZES = listOf(13.sp, 20.sp)

@Composable
private fun FormatButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    IconToggleButton(checked = active, onCheckedChange = { onClick() }) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = label, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
