package com.example.routineforge

import com.example.routineforge.util.NoteSuggestEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSuggestEngineTest {

    private val model = NoteSuggestEngine.buildModel(
        listOf(
            "Bench press felt heavy today #gym",
            "Bench press again, then squats #gym #legs",
            "Benchmark the app"
        ),
        extraTags = listOf("Study")
    )

    @Test
    fun completesFrequentWordsAndKeepsCase() {
        val s = NoteSuggestEngine.suggest("Ben", 3, model)
        assertEquals("Bench", s.first().label)
        val (text, cursor) = NoteSuggestEngine.apply("Ben", 3, s.first())
        assertEquals("Bench ", text)
        assertEquals(6, cursor)
    }

    @Test
    fun neverSuggestsTheHalfTypedWordBack() {
        val s = NoteSuggestEngine.suggest("zzq", 3, NoteSuggestEngine.EMPTY_MODEL)
        assertTrue(s.none { it.label.equals("zzq", true) })
    }

    @Test
    fun predictsNextWordFromBigrams() {
        val s = NoteSuggestEngine.suggest("bench ", 6, model)
        assertEquals("press", s.first().label)
    }

    @Test
    fun completesTagsIncludingCategories() {
        val labels = NoteSuggestEngine.suggest("#", 1, model).map { it.label }
        assertEquals("#gym", labels.first())
        assertTrue("#study" in NoteSuggestEngine.suggest("#st", 3, model).map { it.label })
    }

    @Test
    fun continuesAndEndsLists() {
        val old = "- [x] milk"
        val typed = "$old\n"
        assertEquals("$old\n- [ ] " to (typed.length + 6), NoteSuggestEngine.continueList(old, typed, typed.length))

        val numbered = NoteSuggestEngine.continueList("1. a", "1. a\n", 5)
        assertEquals("1. a\n2. " to 8, numbered)

        val emptyItem = NoteSuggestEngine.continueList("a\n• ", "a\n• \n", 5)
        assertEquals("a\n" to 2, emptyItem)

        assertNull(NoteSuggestEngine.continueList("plain", "plain\n", 6))
    }

    @Test
    fun toolbarTogglesLineStyleFromAnywhereOnTheLine() {
        val check = NoteSuggestEngine.LineStyle.CHECK
        // Mid-line: marker goes to the line start, cursor keeps its place in the words.
        assertEquals("a\n- [ ] milk" to 10, NoteSuggestEngine.toggleLineStyle("a\nmilk", 4, check))
        // Same style again removes it; another style swaps it.
        assertEquals("milk" to 2, NoteSuggestEngine.toggleLineStyle("- [ ] milk", 8, check))
        assertEquals("• milk" to 4, NoteSuggestEngine.toggleLineStyle("- [x] milk", 8, NoteSuggestEngine.LineStyle.BULLET))
        // Numbering follows the previous line.
        assertEquals("1. a\n2. b" to 9, NoteSuggestEngine.toggleLineStyle("1. a\nb", 6, NoteSuggestEngine.LineStyle.NUMBER))
    }

    @Test
    fun checkboxesTickAndCount() {
        val text = "Shop\n- [ ] milk\n  - [x] eggs"
        val items = NoteSuggestEngine.checkItems(text)
        assertEquals(listOf(false, true), items.map { it.done })
        val ticked = NoteSuggestEngine.toggleCheck(text, items[0].lineStart)
        assertEquals("Shop\n- [x] milk\n  - [x] eggs", ticked)
        assertEquals(text, NoteSuggestEngine.toggleCheck(ticked, items[0].lineStart))
        assertEquals("☑ eggs", NoteSuggestEngine.prettyLine("- [x] eggs"))
    }
}
