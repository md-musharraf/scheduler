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

