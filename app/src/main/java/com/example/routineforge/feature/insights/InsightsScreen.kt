package com.example.routineforge.feature.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routineforge.AppContainer
import com.example.routineforge.core.theme.FF
import com.example.routineforge.core.ui.EmptyState
import com.example.routineforge.core.ui.FFCard
import com.example.routineforge.core.ui.FFViewModel
import com.example.routineforge.core.ui.IconBadge
import com.example.routineforge.core.ui.ScreenHeader
import com.example.routineforge.core.ui.appViewModel
import com.example.routineforge.core.ui.todayFlow
import com.example.routineforge.domain.DateRange
import com.example.routineforge.domain.FocusAnalytics
import com.example.routineforge.domain.Insight
import com.example.routineforge.domain.InsightEngine
import com.example.routineforge.domain.InsightInput
import com.example.routineforge.domain.InsightKind
import com.example.routineforge.domain.StatsPeriod
import com.example.routineforge.navigation.AppNav
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.ZoneId

@Immutable
data class InsightsUi(val loading: Boolean = true, val insights: List<Insight> = emptyList())

class InsightsViewModel(c: AppContainer) : FFViewModel() {
    val ui: StateFlow<InsightsUi> = combine(c.sessions.allFocus, c.subjects.subjects, c.goals.goals, c.profiles.profile, todayFlow()) { all, subjects, goals, profile, today ->
        val zone = ZoneId.systemDefault()
        val week = StatsPeriod.WEEK.range(today)
        val lastWeek = StatsPeriod.WEEK.range(today.minusWeeks(1))
        val last30 = DateRange(today.minusDays(29), today.plusDays(1))
        val input = InsightInput(
            today = today,
            dayTotals = FocusAnalytics.dayTotals(all, zone),
            last30 = FocusAnalytics.stats(all, last30, zone),
            thisWeek = FocusAnalytics.stats(all, week, zone),
            lastWeek = FocusAnalytics.stats(all, lastWeek, zone),
            lifetimeSessions = all.size,
            lifetimeMs = all.sumOf { it.actualDurationMs },
            subjectNames = subjects.associate { it.id to it.name },
            weeklyTargetMinutes = FocusAnalytics.overallTarget(goals, subjects, StatsPeriod.WEEK, week),
            minActiveMs = profile.streakMinMinutes * 60_000L
        )
        InsightsUi(false, InsightEngine.build(input))
    }.flowOn(Dispatchers.Default).stateIn(InsightsUi())
}

@Composable
fun InsightsScreen(nav: AppNav) {
    val vm = appViewModel { InsightsViewModel(it) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Productivity Insights", onBack = nav::back)
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!ui.loading && ui.insights.isEmpty()) {
                item { EmptyState(Icons.Filled.Insights, "Not enough data yet", "Complete a few focus sessions and your patterns will show up here.") }
            }
            items(ui.insights, key = { it.kind }) { i ->
                val (icon, tint) = when (i.kind) {
                    InsightKind.PEAK_TIME -> Icons.Filled.BarChart to Color(0xFF4C8DF6)
                    InsightKind.AVERAGE_SESSION -> Icons.Filled.Timer to Color(0xFF22B59A)
                    InsightKind.WEEK_CHANGE -> (if (i.positive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown) to (if (i.positive) FF.colors.success else FF.colors.danger)
                    InsightKind.TOP_SUBJECT -> Icons.AutoMirrored.Filled.MenuBook to Color(0xFF8B5CF6)
                    InsightKind.WEEKEND -> Icons.Filled.Weekend to (if (i.positive) FF.colors.success else FF.colors.danger)
                    InsightKind.STREAK -> Icons.Filled.LocalFireDepartment to FF.colors.warning
                    InsightKind.ACTIVE_DAYS -> Icons.Filled.CalendarMonth to Color(0xFF5B5FEF)
                    InsightKind.GOAL_GAP -> Icons.Filled.EmojiEvents to FF.colors.warning
                    InsightKind.TOTAL -> Icons.Filled.AccessTime to Color(0xFF14A3B8)
                }
                FFCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon, tint, size = 42.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(i.title, style = MaterialTheme.typography.titleSmall)
                            Text(i.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
