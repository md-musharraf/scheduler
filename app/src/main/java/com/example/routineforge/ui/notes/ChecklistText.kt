package com.example.routineforge.ui.notes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import com.example.routineforge.util.NoteSuggestEngine

/**
 * Draws "- [ ] " as ☐ and "- [x] " as ☑ with the item struck through. The stored text is untouched,
 * so search, previews and list continuation keep working on plain text.
 * [boxes] maps each checkbox glyph's offset in the shown text to its source line start (for taps).
 */
class ChecklistText(val source: String, box: Color, done: Color) {
    val transformed: TransformedText
    val boxes: List<Pair<Int, Int>>

    init {
        val items = NoteSuggestEngine.checkItems(source).associateBy { it.boxAt }
        val out = AnnotatedString.Builder()
        val o2t = IntArray(source.length + 1)
        val t2o = ArrayList<Int>(source.length + 1)
        val found = mutableListOf<Pair<Int, Int>>()
        var i = 0
        var strikeUntil = -1
        while (i < source.length) {
            val item = items[i]
            if (item != null) {
                val at = out.length
                out.pushStyle(SpanStyle(color = box, fontWeight = FontWeight.Bold))
                out.append(if (item.done) "☑ " else "☐ ")
                out.pop()
                for (k in i until i + item.markerLength) o2t[k] = at
                t2o += i
                t2o += i + item.markerLength
                found += at to item.lineStart
                i += item.markerLength
                if (item.done) strikeUntil = source.indexOf('\n', i).let { if (it < 0) source.length else it }
                continue
            }
            o2t[i] = out.length
            t2o += i
            if (i < strikeUntil) {
                out.pushStyle(SpanStyle(color = done, textDecoration = TextDecoration.LineThrough))
                out.append(source[i])
                out.pop()
            } else {
                out.append(source[i])
            }
            i++
        }
        o2t[source.length] = out.length
        t2o += source.length
        boxes = found
        transformed = TransformedText(out.toAnnotatedString(), object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = o2t[offset.coerceIn(0, source.length)]
            override fun transformedToOriginal(offset: Int) = t2o[offset.coerceIn(0, t2o.lastIndex)]
        })
    }

    fun asVisualTransformation() = VisualTransformation { text ->
        if (text.text == source) transformed else ChecklistText(text.text, Color.Unspecified, Color.Unspecified).transformed
    }
}
