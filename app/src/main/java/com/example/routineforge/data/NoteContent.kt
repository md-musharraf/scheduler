package com.example.routineforge.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

/** A note is an ordered list of blocks so text, checklists, images and drawings can be mixed. */
@Serializable
sealed interface NoteBlock {
    val key: String
}

@Immutable
@Serializable
@SerialName("text")
data class TextBlock(override val key: String = newKey(), val html: String = "") : NoteBlock

@Immutable
@Serializable
data class CheckItem(val text: String = "", val done: Boolean = false, val key: String = newKey())

@Immutable
@Serializable
@SerialName("check")
data class ChecklistBlock(override val key: String = newKey(), val items: List<CheckItem> = listOf(CheckItem())) : NoteBlock

@Immutable
@Serializable
@SerialName("image")
data class ImageBlock(override val key: String = newKey(), val attachmentId: Long) : NoteBlock

@Immutable
@Serializable
@SerialName("file")
data class FileBlock(override val key: String = newKey(), val attachmentId: Long) : NoteBlock

@Immutable
@Serializable
@SerialName("drawing")
data class DrawingBlock(override val key: String = newKey(), val drawingId: Long) : NoteBlock

fun newKey(): String = UUID.randomUUID().toString()

object NoteContent {
    private val json = Json { ignoreUnknownKeys = true; classDiscriminator = "t" }

    fun encode(blocks: List<NoteBlock>): String = json.encodeToString(blocks)

    /** Never throws: unreadable content becomes a single text block so nothing is lost silently. */
    fun decode(raw: String): List<NoteBlock> {
        if (raw.isBlank()) return emptyList()
        return try {
            json.decodeFromString<List<NoteBlock>>(raw)
        } catch (_: Exception) {
            listOf(TextBlock(html = HtmlText.fromPlain(raw)))
        }
    }

    fun plainText(blocks: List<NoteBlock>): String = blocks.joinToString("\n") { b ->
        when (b) {
            is TextBlock -> HtmlText.toPlain(b.html)
            is ChecklistBlock -> b.items.joinToString("\n") { (if (it.done) "☑ " else "☐ ") + it.text }
            else -> ""
        }
    }.trim()

    /** (done, total) over all checklist items, or null without checklists. */
    fun checklistProgress(blocks: List<NoteBlock>): Pair<Int, Int>? {
        val items = blocks.filterIsInstance<ChecklistBlock>().flatMap { it.items }.filter { it.text.isNotBlank() }
        return if (items.isEmpty()) null else items.count { it.done } to items.size
    }
}

/** Tiny HTML helpers for the rich-text blocks (no android.text so they work in unit tests). */
object HtmlText {
    private val BREAKS = Regex("(?i)<br\\s*/?>|</p>|</li>|</h[1-6]>")
    private val TAGS = Regex("<[^>]+>")

    fun toPlain(html: String): String = html
        .replace(BREAKS, "\n")
        .replace(TAGS, "")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
        .lines().joinToString("\n") { it.trimEnd() }
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()

    fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    fun fromPlain(text: String): String =
        text.lines().joinToString("") { "<p>${escape(it)}</p>" }
}

/** Vector strokes; coordinates are normalized to the canvas (0..1) so drawings scale anywhere. */
@Immutable
@Serializable
data class Stroke(val color: Long, val width: Float, val points: List<Float>, val eraser: Boolean = false)

object DrawingStrokes {
    /** Width / height of every drawing canvas. */
    const val ASPECT = 0.8f
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(strokes: List<Stroke>): String = json.encodeToString(strokes)
    fun decode(raw: String): List<Stroke> = try {
        json.decodeFromString<List<Stroke>>(raw)
    } catch (_: Exception) {
        emptyList()
    }
}
