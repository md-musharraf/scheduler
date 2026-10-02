package com.example.routineforge.core.database

import android.content.Context
import com.example.routineforge.AppContainer
import com.example.routineforge.data.HtmlText
import com.example.routineforge.data.NoteContent
import com.example.routineforge.data.TextBlock
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/**
 * One-time import of data saved by the previous RoutineForge version (SharedPreferences JSON):
 * notes, daily goals (→ subjects), the hourly focus log (→ completed sessions) and scheduled
 * reminders. Unreadable parts are skipped; the old data is left in place untouched.
 */
object LegacyImport {
    private const val PREFS = "routine_forge_prefs"
    private const val FLAG = "focusflow_import_done_v1"

    suspend fun runOnce(context: Context, c: AppContainer) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(FLAG, false)) return
        if (prefs.all.isEmpty()) {
            prefs.edit().putBoolean(FLAG, true).apply()
            return
        }
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val subjectIds = HashMap<String, Long>()

        suspend fun subjectFor(name: String, minutes: Int = 60, color: Long = 0xFF5B5FEF): Long =
            subjectIds.getOrPut(name.lowercase()) {
                c.db.subjectDao().insert(Subject(name = name.take(40), dailyTargetMinutes = minutes, color = color, createdAt = now, updatedAt = now))
            }

        runCatching {
            val arr = JSONArray(prefs.getString("focus_goals_json", null) ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val title = o.optString("title").trim()
                if (title.isNotEmpty()) subjectFor(title, o.optInt("minutes", 60).coerceIn(0, 1440), o.optLong("colorHex", 0xFF5B5FEF))
            }
        }

        runCatching {
            // {"hourKey": {"Math": seconds}} with hourKey = epochDay * 24 + local hour.
            val root = JSONObject(prefs.getString("focus_log_json", null) ?: "{}")
            val sessions = ArrayList<FocusSession>()
            for (key in root.keys()) {
                val hourKey = key.toLong()
                val start = LocalDate.ofEpochDay(Math.floorDiv(hourKey, 24L)).atTime(Math.floorMod(hourKey, 24L).toInt(), 0)
                    .atZone(zone).toInstant().toEpochMilli()
                val hour = root.getJSONObject(key)
                for (name in hour.keys()) {
                    val ms = hour.getInt(name).coerceIn(0, 3600) * 1000L
                    if (ms <= 0 || name.isBlank()) continue
                    val sid = subjectFor(name.trim(), 60)
                    sessions += FocusSession(
                        id = UUID.randomUUID().toString(), subjectId = sid, sessionType = SessionType.FOCUS,
                        status = SessionStatus.COMPLETED, plannedDurationMs = ms, actualDurationMs = ms,
                        startTime = start, endTime = start + ms, createdAt = now, updatedAt = now
                    )
                }
            }
            if (sessions.isNotEmpty()) c.db.sessionDao().insertAll(sessions)
        }

        runCatching {
            val arr = JSONArray(prefs.getString("notes_json", null) ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val text = o.optString("text")
                if (text.isBlank()) continue
                val lines = text.lines()
                val title = lines.firstOrNull { it.isNotBlank() }?.trim()?.take(120).orEmpty()
                val body = lines.dropWhile { it.isBlank() }.drop(1).joinToString("\n")
                val blocks = listOf(TextBlock(html = HtmlText.fromPlain(body)))
                val updated = o.optLong("updatedAt", now)
                c.db.noteDao().insert(
                    Note(title = title, content = NoteContent.encode(blocks), plainText = NoteContent.plainText(blocks),
                        isPinned = o.optBoolean("pinned"), createdAt = updated, updatedAt = updated)
                )
            }
        }

        runCatching {
            val arr = JSONArray(prefs.getString("scheduled_routines_json", null) ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val time = runCatching { LocalTime.parse(o.optString("timeOfDay", "09:00")) }.getOrDefault(LocalTime.of(9, 0))
                val trigger = LocalDate.ofEpochDay(o.getLong("dateEpochDay")).atTime(time).atZone(zone).toInstant().toEpochMilli()
                val repeat = runCatching { RepeatRule.valueOf(o.optString("repeat", "NONE")) }.getOrDefault(RepeatRule.NONE)
                c.db.reminderDao().insert(
                    Reminder(
                        title = o.optString("routineTitle").ifBlank { o.optString("categoryName", "Reminder") }.take(80),
                        description = o.optString("note").take(300),
                        triggerTime = trigger, repeatRule = repeat, isDone = o.optBoolean("isCompleted"),
                        lastFiredAt = if (trigger < now) trigger else null, createdAt = now, updatedAt = now
                    )
                )
            }
        }

        runCatching {
            when (prefs.getString("theme_mode_key", null)) {
                "DARK" -> c.profiles.update { it.copy(themeMode = UserProfile.THEME_DARK) }
                "LIGHT" -> c.profiles.update { it.copy(themeMode = UserProfile.THEME_LIGHT) }
            }
        }
        prefs.edit().putBoolean(FLAG, true).apply()
    }
}
