package com.example.routineforge.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import com.example.routineforge.core.database.AttachmentKind
import com.example.routineforge.core.database.Drawing
import com.example.routineforge.core.database.Note
import com.example.routineforge.core.database.NoteAttachment
import com.example.routineforge.core.database.NoteDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class NoteRepository(private val context: Context, private val dao: NoteDao) {
    val notes: Flow<List<Note>> = dao.observeAll()
    val attachments: Flow<List<NoteAttachment>> = dao.observeAttachments()
    val drawings: Flow<List<Drawing>> = dao.observeDrawings()
    fun forSubject(subjectId: Long): Flow<List<Note>> = dao.observeForSubject(subjectId)

    suspend fun get(id: Long): Note? = dao.get(id)
    suspend fun attachment(id: Long): NoteAttachment? = dao.attachment(id)
    suspend fun drawing(id: Long): Drawing? = dao.drawing(id)

    private val dir: File get() = File(context.filesDir, "attachments").apply { mkdirs() }

    /** Saves the note; returns its id. Attachments and drawings no longer referenced are removed. */
    suspend fun save(note: Note, blocks: List<NoteBlock>): Long {
        val now = System.currentTimeMillis()
        val plain = NoteContent.plainText(blocks)
        val title = note.title.trim().take(120)
        val clean = note.copy(
            title = title,
            content = NoteContent.encode(blocks),
            plainText = plain.take(20_000),
            tags = note.tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(","),
            updatedAt = now
        )
        val id = if (clean.id == 0L) dao.insert(clean.copy(createdAt = now)) else clean.id.also { dao.update(clean) }
        removeUnreferenced(id, blocks)
        return id
    }

    private suspend fun removeUnreferenced(noteId: Long, blocks: List<NoteBlock>) {
        val usedAttachments = blocks.mapNotNull { (it as? ImageBlock)?.attachmentId ?: (it as? FileBlock)?.attachmentId }.toSet()
        dao.attachments(noteId).filter { it.id !in usedAttachments }.forEach { deleteAttachment(it) }
        val usedDrawings = blocks.filterIsInstance<DrawingBlock>().map { it.drawingId }.toSet()
        dao.drawingsFor(noteId).filter { it.id !in usedDrawings }.forEach { dao.deleteDrawing(it.id) }
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)
    suspend fun setPinned(id: Long, pinned: Boolean) = dao.setPinned(id, pinned)

    suspend fun delete(id: Long) {
        dao.attachments(id).forEach { withContext(Dispatchers.IO) { File(it.path).delete() } }
        dao.delete(id) // attachments and drawings cascade
    }

    private suspend fun deleteAttachment(a: NoteAttachment) {
        withContext(Dispatchers.IO) { File(a.path).delete() }
        dao.deleteAttachment(a.id)
    }

    /** Copies a picked file into private storage so the note keeps working if the original moves. */
    suspend fun addAttachment(noteId: Long, uri: Uri, kind: AttachmentKind): NoteAttachment = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "attachment"
        val size = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
        }
        validate(size == null || size <= MAX_BYTES) { "That file is too large (max 25 MB)" }
        val file = File(dir, UUID.randomUUID().toString() + name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" })
        val input = resolver.openInputStream(uri) ?: throw ValidationException("Couldn't open that file")
        input.use { src -> file.outputStream().use { src.copyTo(it) } }
        insert(noteId, kind, name.take(120), file, mime)
    }

    suspend fun addPhoto(noteId: Long, bitmap: Bitmap): NoteAttachment = withContext(Dispatchers.IO) {
        val file = File(dir, UUID.randomUUID().toString() + ".jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        insert(noteId, AttachmentKind.IMAGE, "Photo.jpg", file, "image/jpeg")
    }

    private suspend fun insert(noteId: Long, kind: AttachmentKind, name: String, file: File, mime: String): NoteAttachment {
        val a = NoteAttachment(noteId = noteId, kind = kind, fileName = name, path = file.absolutePath, mimeType = mime, createdAt = System.currentTimeMillis())
        return a.copy(id = dao.insertAttachment(a))
    }

    suspend fun saveDrawing(noteId: Long, drawingId: Long?, strokes: List<Stroke>): Long {
        val now = System.currentTimeMillis()
        val json = DrawingStrokes.encode(strokes)
        val existing = drawingId?.let { dao.drawing(it) }
        return if (existing != null) existing.id.also { dao.updateDrawing(existing.copy(strokes = json, updatedAt = now)) }
        else dao.insertDrawing(Drawing(noteId = noteId, strokes = json, createdAt = now, updatedAt = now))
    }

    private companion object {
        const val MAX_BYTES = 25L * 1024 * 1024
    }
}
