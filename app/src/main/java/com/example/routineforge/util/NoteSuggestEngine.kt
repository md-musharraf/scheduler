package com.example.routineforge.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * On-device autosuggest for notes. Learns from the user's own notes:
 * - word completion (frequency ranked, falls back to a small common-word list)
 * - next-word prediction (bigrams)
 * - #tag completion (existing tags + routine category names)
 * - list starters on empty lines and auto-continued bullets / checklists / numbering
 */
object NoteSuggestEngine {

    data class Suggestion(val label: String, val insert: String, val replaceFrom: Int)

    data class Template(val name: String, val body: String)

    class Model internal constructor(
        val words: Map<String, Int>,
        val bigrams: Map<String, Map<String, Int>>,
        val tags: Map<String, Int>
    )

    private val WORD = Regex("#?[\\p{L}\\p{N}']+")
    private val LIST_PREFIX = Regex("^(\\s*)(- \\[[ xX]] |• |- |\\* |(\\d+)\\. )")

    private val COMMON_WORDS = listOf(
        "about", "action", "after", "again", "agenda", "already", "also", "always", "another", "answer",
        "appointment", "assignment", "because", "before", "better", "between", "breakfast", "budget",
        "call", "change", "check", "chapter", "class", "complete", "could", "daily", "deadline", "decide",
        "dinner", "during", "email", "evening", "every", "exercise", "family", "feedback", "finish",
        "focus", "follow", "friday", "goal", "goals", "grateful", "groceries", "habit", "health", "idea",
        "ideas", "important", "improve", "interview", "journal", "later", "learn", "lesson", "lunch",
        "meeting", "message", "monday", "month", "morning", "next", "night", "notes", "office", "people",
        "personal", "plan", "practice", "prepare", "presentation", "priority", "problem", "progress",
        "project", "question", "questions", "reading", "remember", "reminder", "report", "research",
        "review", "revision", "routine", "saturday", "schedule", "session", "should", "something",
        "start", "study", "summary", "sunday", "task", "tasks", "thursday", "today", "tomorrow",
        "tuesday", "wednesday", "week", "weekend", "while", "workout", "would", "write", "yesterday"
    )

    val EMPTY_MODEL = Model(emptyMap(), emptyMap(), emptyMap())

    fun buildModel(texts: List<String>, extraTags: List<String> = emptyList()): Model {
        val words = HashMap<String, Int>()
        val bigrams = HashMap<String, HashMap<String, Int>>()
        val tags = HashMap<String, Int>()
        extraTags.map(::toTag).filter { it.length > 1 }.forEach { tags.merge(it, 1, Int::plus) }
        for (text in texts) {
            for (line in text.lineSequence()) {
                var prev: String? = null
                for (m in WORD.findAll(line)) {
                    val raw = m.value.lowercase(Locale.ROOT)
                    if (raw.startsWith("#")) {
                        if (raw.length > 1) tags.merge(raw.drop(1), 1, Int::plus)
                        prev = null
                        continue
                    }
                    if (raw.length >= 3) words.merge(raw, 1, Int::plus)
                    if (prev != null) bigrams.getOrPut(prev) { HashMap() }.merge(raw, 1, Int::plus)
                    prev = raw
                }
            }
        }
        return Model(words, bigrams, tags)
    }

    fun templates(now: LocalDateTime = LocalDateTime.now()): List<Template> {
        val date = now.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()))
        return listOf(
            Template("✅ To-do", "To-do — $date\n- [ ] "),
            Template("🗓 Meeting", "Meeting — $date\nAttendees: \nAgenda:\n• \nAction items:\n- [ ] "),
            Template("💪 Workout", "Workout — $date\n• Exercise: \n• Sets × reps: \n• How it felt: "),
            Template("📚 Study", "Study — $date\nTopic: \nKey points:\n• \nQuestions:\n• "),
            Template("📝 Journal", "Journal — $date\nGrateful for: \nToday I: \nTomorrow I will: ")
        )
    }

    /**
     * @param model built from the user's *other* notes (cache it; the current note is scanned here).
     */
    fun suggest(
        text: String,
        cursor: Int,
        model: Model,
        now: LocalDateTime = LocalDateTime.now(),
        max: Int = 3
    ): List<Suggestion> {
        val c = cursor.coerceIn(0, text.length)
        val before = text.substring(0, c)
        val tokenStart = before.indexOfLast { it.isWhitespace() } + 1
        val token = before.substring(tokenStart)
        // Current note minus the half-typed token, so partial words never get suggested back.
        val local = buildModel(listOf(before.substring(0, tokenStart) + " " + text.substring(c)))

        if (token.startsWith("#")) {
            val prefix = token.drop(1).lowercase(Locale.ROOT)
            return rank(model.tags, local.tags) { it.startsWith(prefix) && it != prefix }
                .take(max).map { Suggestion("#$it", "#$it ", tokenStart) }
        }

        if (token.length >= 2) {
            val prefix = token.lowercase(Locale.ROOT)
            val common = COMMON_WORDS.associateWith { 0 }
            return rank(model.words, local.words, common) { it.length > prefix.length && it.startsWith(prefix) }
                .take(max)
                .map { val w = matchCase(token, it); Suggestion(w, "$w ", tokenStart) }
        }

        if (token.isNotEmpty()) return emptyList()

        val line = before.substring(before.lastIndexOf('\n') + 1)
        if (line.isBlank()) {
            val time = now.format(DateTimeFormatter.ofPattern("HH:mm"))
            return listOf(
                Suggestion("☐ Checklist", "- [ ] ", c),
                Suggestion("• Bullet", "• ", c),
                Suggestion("🕒 $time", "$time — ", c)
            )
        }

        val prev = WORD.findAll(line).lastOrNull()?.value?.lowercase(Locale.ROOT) ?: return emptyList()
        return rank(model.bigrams[prev].orEmpty(), local.bigrams[prev].orEmpty()) { true }
            .take(max).map { Suggestion(it, "$it ", c) }
    }

    /** Applies a suggestion; returns the new text and cursor. */
    fun apply(text: String, cursor: Int, s: Suggestion): Pair<String, Int> {
        val c = cursor.coerceIn(0, text.length)
        val from = s.replaceFrom.coerceIn(0, c)
        return (text.substring(0, from) + s.insert + text.substring(c)) to (from + s.insert.length)
    }

    /**
     * If the user just pressed Enter on a list line, continue the list (or end it on an empty item).
     * Returns null when nothing should change.
     */
    fun continueList(oldText: String, newText: String, cursor: Int): Pair<String, Int>? {
        if (newText.length != oldText.length + 1 || cursor < 1 || cursor > newText.length) return null
        if (newText[cursor - 1] != '\n' || newText.removeRange(cursor - 1, cursor) != oldText) return null

        val lineStart = newText.lastIndexOf('\n', cursor - 2) + 1
        val prevLine = newText.substring(lineStart, cursor - 1)
        val m = LIST_PREFIX.find(prevLine) ?: return null

        if (prevLine.length == m.value.length) {
            // Empty item: end the list instead of adding another bullet.
            return (newText.substring(0, lineStart) + newText.substring(cursor)) to lineStart
        }
        val indent = m.groupValues[1]
        val marker = m.groupValues[2]
        val next = when {
            marker.startsWith("- [") -> "- [ ] "
            m.groupValues[3].isNotEmpty() -> "${m.groupValues[3].toInt() + 1}. "
            else -> marker
        }
        val insert = indent + next
        return (newText.substring(0, cursor) + insert + newText.substring(cursor)) to (cursor + insert.length)
    }

    private fun rank(vararg sources: Map<String, Int>, keep: (String) -> Boolean): List<String> {
        val scores = HashMap<String, Int>()
        sources.forEach { src -> src.forEach { (k, v) -> if (keep(k)) scores.merge(k, v, Int::plus) } }
        return scores.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.length }.thenBy { it.key })
            .map { it.key }
    }

    private fun toTag(name: String) = name.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

    private fun matchCase(typed: String, word: String): String = when {
        typed.length > 1 && typed.all { !it.isLetter() || it.isUpperCase() } -> word.uppercase(Locale.ROOT)
        typed.first().isUpperCase() -> word.replaceFirstChar { it.titlecase(Locale.ROOT) }
        else -> word
    }
}
