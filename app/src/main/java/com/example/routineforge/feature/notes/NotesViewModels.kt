package com.example.routineforge.feature.notes

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewModelScope
import com.example.routineforge.AppContainer
import com.example.routineforge.core.database.AttachmentKind
import com.example.routineforge.core.database.Note
import com.example.routineforge.core.database.NoteAttachment
import com.example.routineforge.core.database.Subject
import com.example.routineforge.core.ui.FFViewModel
import com.example.routineforge.data.CheckItem
import com.example.routineforge.data.ChecklistBlock
import com.example.routineforge.data.DrawingBlock
import com.example.routineforge.data.DrawingStrokes
import com.example.routineforge.data.FileBlock
import com.example.routineforge.data.ImageBlock
import com.example.routineforge.data.NoteBlock
import com.example.routineforge.data.NoteContent
import com.example.routineforge.data.Stroke
import com.example.routineforge.data.TextBlock
import com.example.routineforge.data.newKey
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class NoteFilter(val label: String) { ALL("All"), FAVORITES("Important"), SUBJECTS("By subject") }

/** List row: the note plus what its card needs to preview. */
@Immutable
data class NoteRow(val note: Note, val subject: Subject?, val imagePath: String?, val drawing: List<Stroke>?, val checklist: Pair<Int, Int>?)

@Immutable
data class NotesUi(
    val loading: Boolean = true,
    val query: String = "",
    val filter: NoteFilter = NoteFilter.ALL,
    val subjectId: Long? = null,
    val subjects: List<Subject> = emptyList(),
    val rows: List<NoteRow> = emptyList(),
    val total: Int = 0
)

class NotesViewModel(private val c: AppContainer) : FFViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(NoteFilter.ALL)
    private val subjectFilter = MutableStateFlow<Long?>(null)

    private val previews = combine(c.notes.attachments, c.notes.drawings) { atts, drawings ->
        atts.filter { it.kind == AttachmentKind.IMAGE }.groupBy { it.noteId }.mapValues { it.value.first().path } to
            drawings.groupBy { it.noteId }.mapValues { DrawingStrokes.decode(it.value.first().strokes) }
    }

    val ui: StateFlow<NotesUi> = combine(
        c.notes.notes, c.subjects.subjects, previews,
        combine(query, filter, subjectFilter) { q, f, s -> Triple(q, f, s) }
    ) { notes, subjects, (images, drawings), (q, f, sid) ->
        val byId = subjects.associateBy { it.id }
        val needle = q.trim()
        val rows = notes.filter { n ->
            (needle.isEmpty() || n.title.contains(needle, true) || n.plainText.contains(needle, true) || n.tags.contains(needle, true)) &&
                when (f) {
                    NoteFilter.ALL -> true
                    NoteFilter.FAVORITES -> n.isFavorite
                    NoteFilter.SUBJECTS -> if (sid == null) n.subjectId != null else n.subjectId == sid
                }
        }.map { n ->
            NoteRow(n, n.subjectId?.let(byId::get), images[n.id], drawings[n.id], NoteContent.checklistProgress(NoteContent.decode(n.content)))
        }
        NotesUi(false, q, f, sid, subjects.filter { !it.isArchived }, rows, notes.size)
    }.stateIn(NotesUi())

    fun setQuery(q: String) { query.value = q.take(100) }
    fun setFilter(f: NoteFilter) { filter.value = f }
    fun setSubject(id: Long?) { subjectFilter.value = id }
    fun toggleFavorite(n: Note) = launchSafe { c.notes.setFavorite(n.id, !n.isFavorite) }
    fun togglePinned(n: Note) = launchSafe { c.notes.setPinned(n.id, !n.isPinned) }
    fun delete(n: Note) = launchSafe {
        c.notes.delete(n.id)
        say("Note deleted")
    }
}

/** Editable block; text blocks own a RichTextState so formatting survives until saved as HTML. */
sealed interface EditorBlock {
    val key: String

    class Text(override val key: String, val state: RichTextState) : EditorBlock
    class Checklist(override val key: String, items: List<CheckItem>) : EditorBlock {
        val items = mutableStateListOf<CheckItem>().apply { addAll(items) }
    }
    class Image(override val key: String, val attachment: NoteAttachment?) : EditorBlock
    class File(override val key: String, val attachment: NoteAttachment?) : EditorBlock
    class Drawing(override val key: String, val drawingId: Long, strokes: List<Stroke>) : EditorBlock {
        var strokes by mutableStateOf(strokes)
    }
}

@OptIn(FlowPreview::class)
class NoteEditorViewModel(private val c: AppContainer, noteId: Long, subjectId: Long?) : FFViewModel() {
    private var note = Note(subjectId = subjectId)
    val loaded = MutableStateFlow(false)
    val missing = MutableStateFlow(false)

    var title by mutableStateOf("")
    var subject by mutableStateOf(subjectId)
    var favorite by mutableStateOf(false)
    var tags by mutableStateOf("")
    var updatedAt by mutableStateOf(0L)
    val blocks = mutableStateListOf<EditorBlock>()
    /** The text block formatting commands apply to. */
    var focusedKey by mutableStateOf<String?>(null)

    val subjects: StateFlow<List<Subject>> = c.subjects.subjects.stateIn(emptyList())

    private val saveLock = Mutex()

    init {
        viewModelScope.launch {
            if (noteId != 0L) {
                val n = c.notes.get(noteId)
                if (n == null) {
                    missing.value = true
                    return@launch
                }
                note = n
                title = n.title
                subject = n.subjectId
                favorite = n.isFavorite
                tags = n.tags
                updatedAt = n.updatedAt
                NoteContent.decode(n.content).forEach { blocks += toEditor(it) }
            }
            if (blocks.none { it is EditorBlock.Text }) blocks += textBlock("")
            focusedKey = blocks.firstOrNull { it is EditorBlock.Text }?.key
            loaded.value = true
            // Autosave shortly after typing stops.
            snapshotFlow { snapshot() }
                .drop(1)
                .distinctUntilChanged()
                .debounce(700)
                .collect { save(it) }
        }
    }

    private suspend fun toEditor(b: NoteBlock): EditorBlock = when (b) {
        is TextBlock -> textBlock(b.html, b.key)
        is ChecklistBlock -> EditorBlock.Checklist(b.key, b.items)
        is ImageBlock -> EditorBlock.Image(b.key, c.notes.attachment(b.attachmentId))
        is FileBlock -> EditorBlock.File(b.key, c.notes.attachment(b.attachmentId))
        is DrawingBlock -> EditorBlock.Drawing(b.key, b.drawingId, c.notes.drawing(b.drawingId)?.let { DrawingStrokes.decode(it.strokes) } ?: emptyList())
    }

    private fun textBlock(html: String, key: String = newKey()) =
        EditorBlock.Text(key, RichTextState().apply { if (html.isNotBlank()) setHtml(html) })

    private data class Snapshot(val title: String, val subject: Long?, val favorite: Boolean, val tags: String, val blocks: List<NoteBlock>)

    private fun snapshot() = Snapshot(title, subject, favorite, tags, blocks.map { b ->
        when (b) {
            is EditorBlock.Text -> TextBlock(b.key, if (b.state.annotatedString.text.isBlank()) "" else b.state.toHtml())
            is EditorBlock.Checklist -> ChecklistBlock(b.key, b.items.toList())
            is EditorBlock.Image -> ImageBlock(b.key, b.attachment?.id ?: 0)
            is EditorBlock.File -> FileBlock(b.key, b.attachment?.id ?: 0)
            is EditorBlock.Drawing -> DrawingBlock(b.key, b.drawingId)
        }
    })

    private fun Snapshot.isEmpty(): Boolean = title.isBlank() && blocks.all {
        (it is TextBlock && it.html.isBlank()) || (it is ChecklistBlock && it.items.all { i -> i.text.isBlank() })
    }

    private suspend fun save(s: Snapshot) = saveLock.withLock {
        if (note.id == 0L && s.isEmpty()) return@withLock
        val id = c.notes.save(note.copy(title = s.title, subjectId = s.subject, isFavorite = s.favorite, tags = s.tags), s.blocks)
        note = c.notes.get(id) ?: note.copy(id = id)
        updatedAt = note.updatedAt
    }

    /** Makes sure the note has a row, so attachments and drawings can point at it. */
    private suspend fun ensureSaved(): Long {
        if (note.id == 0L) saveLock.withLock {
            if (note.id == 0L) {
                val s = snapshot()
                val id = c.notes.save(note.copy(title = s.title, subjectId = s.subject, isFavorite = s.favorite, tags = s.tags), s.blocks)
                note = c.notes.get(id) ?: note.copy(id = id)
            }
        }
        return note.id
    }

    fun saveNow() = launchSafe { save(snapshot()) }

    /** Final save when the editor closes; an untouched new note leaves nothing behind. */
    override fun onCleared() {
        val s = snapshot()
        val current = note
        c.scope.launch {
            saveLock.withLock {
                if (current.id != 0L && s.isEmpty() && c.db.noteDao().attachments(current.id).isEmpty() && s.blocks.none { it is DrawingBlock }) {
                    c.notes.delete(current.id)
                } else if (!(current.id == 0L && s.isEmpty())) {
                    c.notes.save(current.copy(title = s.title, subjectId = s.subject, isFavorite = s.favorite, tags = s.tags), s.blocks)
                }
            }
        }
    }

    private fun insertAfterFocus(block: EditorBlock) {
        val i = blocks.indexOfFirst { it.key == focusedKey }.takeIf { it >= 0 } ?: blocks.lastIndex
        blocks.add(i + 1, block)
        // Keep a text block after inserted content so typing can continue below it.
        if (blocks.getOrNull(i + 2) !is EditorBlock.Text) blocks.add(i + 2, textBlock(""))
    }

    fun addChecklist() {
        insertAfterFocus(EditorBlock.Checklist(newKey(), listOf(CheckItem())))
    }

    fun addImage(uri: Uri, kind: AttachmentKind) = launchSafe {
        val id = ensureSaved()
        val a = c.notes.addAttachment(id, uri, kind)
        insertAfterFocus(if (kind == AttachmentKind.IMAGE) EditorBlock.Image(newKey(), a) else EditorBlock.File(newKey(), a))
        save(snapshot())
    }

    fun addPhoto(bitmap: Bitmap) = launchSafe {
        val id = ensureSaved()
        insertAfterFocus(EditorBlock.Image(newKey(), c.notes.addPhoto(id, bitmap)))
        save(snapshot())
    }

    /** Creates an empty drawing in the note and returns its id so the canvas can open it. */
    fun addDrawing(open: (Long) -> Unit) = launchSafe {
        val id = ensureSaved()
        val drawingId = c.notes.saveDrawing(id, null, emptyList())
        insertAfterFocus(EditorBlock.Drawing(newKey(), drawingId, emptyList()))
        save(snapshot())
        open(drawingId)
    }

    /** Reloads drawing previews after returning from the canvas. */
    fun refreshDrawings() = launchSafe {
        blocks.filterIsInstance<EditorBlock.Drawing>().forEach { d ->
            d.strokes = c.notes.drawing(d.drawingId)?.let { DrawingStrokes.decode(it.strokes) } ?: emptyList()
        }
    }

    fun removeBlock(key: String) {
        blocks.removeAll { it.key == key }
        if (blocks.none { it is EditorBlock.Text }) blocks += textBlock("")
    }

    fun delete(done: () -> Unit) = launchSafe {
        saveLock.withLock {
            if (note.id != 0L) c.notes.delete(note.id)
            note = Note()
            blocks.clear()
            title = ""
        }
        say("Note deleted")
        done()
    }

    fun focusedText(): RichTextState? = (blocks.firstOrNull { it.key == focusedKey } as? EditorBlock.Text)?.state
        ?: (blocks.lastOrNull { it is EditorBlock.Text } as? EditorBlock.Text)?.state

    fun reportError(message: String) = say(message)
}

class DrawingViewModel(private val c: AppContainer, private val drawingId: Long) : FFViewModel() {
    val strokes = mutableStateListOf<Stroke>()
    private val undo = ArrayDeque<List<Stroke>>()
    private val redo = ArrayDeque<List<Stroke>>()
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set
    val loaded = MutableStateFlow(false)
    private var noteId = 0L

    init {
        viewModelScope.launch {
            val d = c.notes.drawing(drawingId)
            if (d == null) say("This drawing no longer exists") else {
                noteId = d.noteId
                strokes.addAll(DrawingStrokes.decode(d.strokes))
            }
            loaded.value = d != null
        }
    }

    private fun record() {
        undo.addLast(strokes.toList())
        if (undo.size > 100) undo.removeFirst()
        redo.clear()
        refreshFlags()
    }

    private fun refreshFlags() {
        canUndo = undo.isNotEmpty()
        canRedo = redo.isNotEmpty()
    }

    fun add(stroke: Stroke) {
        record()
        strokes.add(stroke)
        persist()
    }

    private var gestureRecorded = false

    fun beginErase() {
        gestureRecorded = false
    }

    /** Stroke eraser: removes whole strokes near the touch point (keeps the data vector-based). */
    fun eraseAt(x: Float, y: Float, radius: Float) {
        val hit = strokes.filter { s -> s.points.chunked(2).any { (px, py) -> (px - x) * (px - x) + (py - y) * (py - y) <= radius * radius } }
        if (hit.isEmpty()) return
        if (!gestureRecorded) {
            record()
            gestureRecorded = true
        }
        strokes.removeAll(hit)
        persist()
    }

    fun undo() {
        val prev = undo.removeLastOrNull() ?: return
        redo.addLast(strokes.toList())
        strokes.clear(); strokes.addAll(prev)
        refreshFlags(); persist()
    }

    fun redo() {
        val next = redo.removeLastOrNull() ?: return
        undo.addLast(strokes.toList())
        strokes.clear(); strokes.addAll(next)
        refreshFlags(); persist()
    }

    fun clear() {
        if (strokes.isEmpty()) return
        record()
        strokes.clear()
        persist()
    }

    private val saveLock = Mutex()

    /** Saves after every change; the lock makes the newest strokes the last write. */
    private fun persist() {
        if (noteId == 0L) return
        launchSafe { saveLock.withLock { c.notes.saveDrawing(noteId, drawingId, strokes.toList()) } }
    }
}
