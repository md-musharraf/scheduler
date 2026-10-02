package com.example.routineforge.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Bottom-navigation tabs
@Serializable data object HomeNav : NavKey
@Serializable data object SubjectsNav : NavKey
@Serializable data object TimerNav : NavKey
@Serializable data object NotesNav : NavKey
@Serializable data object CalendarNav : NavKey

// Secondary screens
@Serializable data class SubjectDetailNav(val subjectId: Long) : NavKey
/** subjectId = 0 → new subject. */
@Serializable data class SubjectEditorNav(val subjectId: Long = 0) : NavKey
/** noteId = 0 → new note (optionally filed under [subjectId]). */
@Serializable data class NoteEditorNav(val noteId: Long = 0, val subjectId: Long? = null) : NavKey
@Serializable data class DrawingNav(val drawingId: Long) : NavKey
/** eventId = 0 → new event on [dateEpochDay] (today when null). */
@Serializable data class EventEditorNav(val eventId: Long = 0, val dateEpochDay: Long? = null) : NavKey
@Serializable data object RemindersNav : NavKey
@Serializable data class ReminderEditorNav(val reminderId: Long = 0) : NavKey
@Serializable data object ProgressNav : NavKey
@Serializable data object GoalsNav : NavKey
@Serializable data object InsightsNav : NavKey
@Serializable data object SettingsNav : NavKey
