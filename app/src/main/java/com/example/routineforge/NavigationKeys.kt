package com.example.routineforge

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object RoutineListNav : NavKey

@Serializable
data class RoutineEditorNav(val routineId: String? = null) : NavKey

@Serializable
data class RoutinePlayerNav(val routineId: String) : NavKey

@Serializable
data object HistoryStatsNav : NavKey

@Serializable
data object CalendarNav : NavKey

@Serializable
data object StopwatchNav : NavKey

@Serializable
data object SpecialTimerNav : NavKey


@Serializable
data object NotesNav : NavKey

/** noteId is generated up-front for new notes so the editor survives recreation. */
@Serializable
data class NoteEditorNav(val noteId: String) : NavKey

/** reminderId = null → new reminder; routineId pre-fills it from a routine. */
@Serializable
data class ReminderEditorNav(val reminderId: String? = null, val routineId: String? = null) : NavKey
