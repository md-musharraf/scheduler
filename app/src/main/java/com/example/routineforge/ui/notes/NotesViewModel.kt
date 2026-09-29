package com.example.routineforge.ui.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routineforge.data.Note
import com.example.routineforge.data.RoutineRepository
import com.example.routineforge.util.NoteSuggestEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class NotesViewModel(repository: RoutineRepository) : ViewModel() {
    val query = MutableStateFlow("")

    val notes: StateFlow<List<Note>> = combine(repository.notes, query) { notes, q ->
        notes
            .filter { q.isBlank() || it.text.contains(q.trim(), ignoreCase = true) }
            .sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

/**
 * Plain state holder (not a ViewModel): NavDisplay here has no per-entry ViewModel store, so a
 * keyed ViewModel would outlive the screen and go stale. Keyed by a stable id from the nav key.
 */
class NoteEditorState(
    private val repository: RoutineRepository,
    private val id: String
) {
    private val existing = repository.notes.value.firstOrNull { it.id == id }

    val initialText = existing?.text.orEmpty()
    var text = initialText
        private set
    var pinned by mutableStateOf(existing?.pinned ?: false)
        private set

    /** Learned once from other notes + routine names; the note being edited is scanned live. */
    val model: NoteSuggestEngine.Model = run {
        val routines = repository.routines.value
        NoteSuggestEngine.buildModel(
            texts = repository.notes.value.filter { it.id != id }.map { it.text } +
                routines.map { it.title } + routines.flatMap { r -> r.steps.map { it.title } },
            extraTags = repository.categories.value.map { it.name }
        )
    }

    // Saved on every edit so nothing is lost if the app is killed; blank notes are never kept.
    fun update(newText: String) {
        text = newText
        persist()
    }

    fun togglePin() {
        pinned = !pinned
        persist()
    }

    fun delete() = repository.deleteNote(id)

    private fun persist() {
        if (text.isBlank()) repository.deleteNote(id)
        else repository.saveNote(Note(id = id, text = text, pinned = pinned))
    }
}
