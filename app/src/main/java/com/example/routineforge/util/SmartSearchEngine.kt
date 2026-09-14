package com.example.routineforge.util

import com.example.routineforge.data.Routine
import com.example.routineforge.data.RoutineCategory
import java.util.Locale

/**
 * Intelligent Semantic Search & Intent Engine for RoutineForge.
 * Features:
 * - Natural language intent mapping (e.g. "workout" -> Gym, "coding" -> Study/Dev)
 * - Duration constraints parsing (e.g. "quick", "< 30m", "deep work", "long")
 * - Multi-tier hierarchical search across Titles, Steps, Instructions, and Categories
 * - Weighted relevance scoring & ranking
 */
object SmartSearchEngine {

    data class SearchSuggestion(
        val label: String,
        val query: String,
        val categoryId: String? = null
    )

    val QUICK_SUGGESTIONS = listOf(
        SearchSuggestion("✨ Quick (< 30m)", "quick"),
        SearchSuggestion("💪 Workout", "workout"),
        SearchSuggestion("🧠 Deep Study", "study"),
        SearchSuggestion("🎙️ Speech & English", "english"),
        SearchSuggestion("🧘 Calm & Reset", "calm"),
        SearchSuggestion("💻 Coding Sprint", "coding")
    )

    private val INTENT_SYNONYMS: Map<String, List<String>> = mapOf(
        "workout" to listOf("gym", "exercise", "lifting", "chest", "arms", "pushups", "squats", "hiit", "fitness", "cardio", "bodybuilding"),
        "coding" to listOf("code", "programming", "developer", "dev", "dsa", "leetcode", "software", "algorithms", "bug"),
        "study" to listOf("read", "book", "learning", "revision", "exam", "dsa", "deep work", "focus", "research"),
        "english" to listOf("speak", "communication", "voice", "podcast", "fluency", "conversation", "talk", "record"),
        "calm" to listOf("mindfulness", "breathing", "meditate", "relax", "peace", "zen", "body scan", "gratitude", "stress"),
        "quick" to listOf("short", "fast", "speed", "mini", "15m", "20m", "25m", "30m"),
        "long" to listOf("deep work", "marathon", "intensive", "2h", "3h", "4h")
    )

    /**
     * Executes intelligent semantic search and returns routines ranked by match score.
     */
    fun search(
        routines: List<Routine>,
        categories: List<RoutineCategory>,
        rawQuery: String,
        selectedCategoryId: String?
    ): List<Routine> {
        val query = rawQuery.trim().lowercase(Locale.ROOT)

        // 1) Filter by category first if specified
        val categoryFiltered = if (selectedCategoryId != null) {
            routines.filter { it.categoryId == selectedCategoryId }
        } else {
            routines
        }

        if (query.isBlank()) {
            return categoryFiltered
        }

        // 2) Score each routine based on hierarchical matching & intent expansion
        val scoredList = categoryFiltered.mapNotNull { routine ->
            val score = calculateRelevanceScore(routine, categories, query)
            if (score > 0) Pair(routine, score) else null
        }

        // 3) Rank descending by score
        return scoredList
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun calculateRelevanceScore(
        routine: Routine,
        categories: List<RoutineCategory>,
        query: String
    ): Int {
        var score = 0
        val lowerTitle = routine.title.lowercase(Locale.ROOT)
        val lowerDesc = routine.description.lowercase(Locale.ROOT)
        val category = categories.firstOrNull { it.id == routine.categoryId }
        val categoryName = category?.name?.lowercase(Locale.ROOT) ?: ""

        // A. Exact or Prefix Title Match (Highest Weight: 100 - 50)
        if (lowerTitle == query) {
            score += 100
        } else if (lowerTitle.startsWith(query)) {
            score += 50
        } else if (lowerTitle.contains(query)) {
            score += 30
        }

        // B. Category Name & Emoji Match (Weight: 25)
        if (categoryName.contains(query) || category?.emoji?.contains(query) == true) {
            score += 25
        }

        // C. Routine Description Match (Weight: 15)
        if (lowerDesc.contains(query)) {
            score += 15
        }

        // D. Step Titles & Instructions Match (Weight: 20 per matching step)
        routine.steps.forEach { step ->
            val stepTitle = step.title.lowercase(Locale.ROOT)
            val stepInstruction = step.instruction.lowercase(Locale.ROOT)
            if (stepTitle.contains(query)) {
                score += 20
            } else if (stepInstruction.contains(query)) {
                score += 10
            }
        }

        // E. Semantic Intent & Synonym Expansion (Weight: 25)
        INTENT_SYNONYMS.forEach { (intentKey, synonyms) ->
            val queryMatchesIntent = query.contains(intentKey) || synonyms.any { it.contains(query) || query.contains(it) }
            if (queryMatchesIntent) {
                val routineMatchesIntent = lowerTitle.contains(intentKey) ||
                        lowerDesc.contains(intentKey) ||
                        categoryName.contains(intentKey) ||
                        synonyms.any { syn ->
                            lowerTitle.contains(syn) ||
                            categoryName.contains(syn) ||
                            lowerDesc.contains(syn) ||
                            routine.steps.any { it.title.lowercase(Locale.ROOT).contains(syn) }
                        }
                if (routineMatchesIntent) {
                    score += 25
                }
            }
        }

        // F. Duration Intent Parsing
        if (query.contains("quick") || query.contains("short") || query.contains("< 30m")) {
            if (routine.totalDurationSeconds <= 1800) { // <= 30 minutes
                score += 35
            }
        } else if (query.contains("long") || query.contains("deep") || query.contains("> 1h") || query.contains("marathon")) {
            if (routine.totalDurationSeconds >= 3600) { // >= 1 hour
                score += 35
            }
        }

        return score
    }
}
