package com.example.routineforge

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.routineforge.core.database.AppDatabase
import com.example.routineforge.core.database.CalendarEvent
import com.example.routineforge.core.database.Reminder
import com.example.routineforge.core.database.RepeatRule
import com.example.routineforge.core.database.SessionStatus
import com.example.routineforge.core.database.SessionType
import com.example.routineforge.core.database.Subject
import com.example.routineforge.core.notifications.Alarms
import com.example.routineforge.core.notifications.ReminderReceiver
import com.example.routineforge.data.CalendarRepository
import com.example.routineforge.data.CheckItem
import com.example.routineforge.data.ChecklistBlock
import com.example.routineforge.data.DrawingBlock
import com.example.routineforge.data.DrawingStrokes
import com.example.routineforge.data.FocusTimer
import com.example.routineforge.data.NoteContent
import com.example.routineforge.data.NoteRepository
import com.example.routineforge.data.ProfileRepository
import com.example.routineforge.data.ReminderRepository
import com.example.routineforge.data.SessionRepository
import com.example.routineforge.data.Stroke
import com.example.routineforge.data.SubjectRepository
import com.example.routineforge.data.TextBlock
import com.example.routineforge.data.ValidationException
import com.example.routineforge.domain.StatsPeriod
import com.example.routineforge.data.TimerEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

private const val MIN = 60_000L

/** Runs the real repositories against an in-memory Room database on a device. */
@RunWith(AndroidJUnit4::class)
class DataLayerTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private var now = 0L
    private lateinit var timer: FocusTimer
    private lateinit var sessions: SessionRepository
    private lateinit var subjects: SubjectRepository

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        now = LocalDate.now().atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        timer = FocusTimer(context, db.sessionDao(), db.subjectDao(), ProfileRepository(db.profileDao())) { now }
        sessions = SessionRepository(db.sessionDao())
        subjects = SubjectRepository(db.subjectDao())
    }

    @After fun tearDown() {
        Alarms.cancelTimer(context)
        db.close()
    }

    private suspend fun todayMs(subjectId: Long? = null): Long {
        val st = sessions.stats(StatsPeriod.DAY.range(LocalDate.now())).first()
        return if (subjectId == null) st.totalMs else st.bySubject[subjectId] ?: 0L
    }

    @Test fun pauseTimeIsExcludedAndSessionFeedsProgress() = runTest {
        val dsa = subjects.save(Subject(name = "Data Structures", dailyTargetMinutes = 180))
        val s = timer.start(SessionType.FOCUS, dsa, 45 * MIN)          // 10:00
        now += 20 * MIN; timer.pause(s.id)                                // 10:20
        now += 15 * MIN; timer.resume(s.id)                               // 10:35
        now += 10 * MIN; val saved = timer.stop(s.id, save = true)        // 10:45
        assertEquals(30 * MIN, saved)
        val row = db.sessionDao().get(s.id)!!
        assertEquals(SessionStatus.COMPLETED, row.status)
        assertEquals(30 * MIN, row.actualDurationMs)
        assertEquals(30 * MIN, todayMs(dsa))
        assertEquals(30 * MIN, sessions.stats(StatsPeriod.YEAR.range(LocalDate.now())).first().totalMs)
    }

    @Test fun completionIsIdempotent() = runTest {
        val s = timer.start(SessionType.FOCUS, null, 25 * MIN)
        now += 30 * MIN
        // Alarm, UI tick and app start all racing to complete the same session.
        (1..5).map { async { timer.completeIfDue() } }.awaitAll()
        timer.stop(s.id, save = true)
        timer.completeIfDue()
        val row = db.sessionDao().get(s.id)!!
        assertEquals(SessionStatus.COMPLETED, row.status)
        assertEquals(25 * MIN, row.actualDurationMs)
        // Ended when the time ran out, not when it was noticed.
        assertEquals(s.startTime + 25 * MIN, row.endTime)
        assertEquals(25 * MIN, todayMs())
    }

    /** Regression: completing from collectLatest used to cancel itself before the finish event. */
    @Test fun completionTriggeredFromCollectLatestStillFinishes() = runBlocking {
        val events = java.util.Collections.synchronizedList(mutableListOf<TimerEvent>())
        val listener = launch(Dispatchers.Default) { timer.events.collect { events += it } }
        delay(100)
        timer.start(SessionType.FOCUS, null, 25 * MIN)
        now += 26 * MIN
        val watcher = launch(Dispatchers.Default) { timer.active.collectLatest { s -> if (s != null) timer.completeIfDue() } }
        withTimeout(5_000) { while (events.isEmpty()) delay(20) }
        delay(200)
        watcher.cancel(); listener.cancel()
        assertEquals(1, events.size)
        assertTrue(events.single() is TimerEvent.Finished && (events.single() as TimerEvent.Finished).natural)
    }

    @Test fun overdueSessionIsCompletedAfterProcessDeath() = runTest {
        timer.start(SessionType.FOCUS, null, 25 * MIN)
        now += 3 * 60 * MIN
        // A new FocusTimer over the same database = the app restarting.
        val restarted = FocusTimer(context, db.sessionDao(), db.subjectDao(), ProfileRepository(db.profileDao())) { now }
        restarted.restore()
        assertNull(db.sessionDao().getActive().firstOrNull())
        assertEquals(25 * MIN, todayMs())
    }

    @Test fun onlyOneActiveSession() = runTest {
        val a = timer.start(SessionType.FOCUS, null, 25 * MIN)
        val b = timer.start(SessionType.FOCUS, null, 50 * MIN)
        assertEquals(a.id, b.id)
        assertEquals(1, db.sessionDao().getActive().size)
    }

    @Test fun cancelledAndShortSessionsDontCount() = runTest {
        val a = timer.start(SessionType.FOCUS, null, 25 * MIN)
        now += 10 * MIN
        assertEquals(0L, timer.stop(a.id, save = false))
        assertEquals(SessionStatus.CANCELLED, db.sessionDao().get(a.id)!!.status)
        val b = timer.start(SessionType.FOCUS, null, 25 * MIN)
        now += 30_000
        assertEquals(0L, timer.stop(b.id, save = true)) // under a minute
        val br = timer.start(SessionType.SHORT_BREAK, null, 5 * MIN)
        now += 6 * MIN
        timer.completeIfDue()
        assertEquals(SessionStatus.COMPLETED, db.sessionDao().get(br.id)!!.status)
        assertEquals(0L, todayMs()) // breaks never count as focus
    }

    @Test fun pausedSessionNeverCompletes() = runTest {
        val s = timer.start(SessionType.FOCUS, null, 25 * MIN)
        now += 5 * MIN
        timer.pause(s.id)
        now += 10 * 60 * MIN
        timer.completeIfDue()
        assertEquals(SessionStatus.PAUSED, db.sessionDao().get(s.id)!!.status)
        timer.resume(s.id)
        now += 20 * MIN
        timer.completeIfDue()
        assertEquals(25 * MIN, db.sessionDao().get(s.id)!!.actualDurationMs)
    }

    @Test fun deletingSubjectKeepsFocusTime() = runTest {
        val id = subjects.save(Subject(name = "OS"))
        val s = timer.start(SessionType.FOCUS, id, 25 * MIN)
        now += 25 * MIN
        timer.completeIfDue()
        subjects.delete(id)
        assertNull(db.sessionDao().get(s.id)!!.subjectId)
        assertEquals(25 * MIN, todayMs())
    }

    @Test fun subjectValidation() = runTest {
        try {
            subjects.save(Subject(name = "   "))
            fail("blank name accepted")
        } catch (_: ValidationException) {
        }
        val id = subjects.save(Subject(name = "  Maths  "))
        assertEquals("Maths", subjects.get(id)!!.name)
        subjects.setArchived(id, true)
        assertTrue(subjects.get(id)!!.isArchived)
    }

    private fun alarmSet(id: Long): Boolean = PendingIntent.getBroadcast(
        context, Alarms.requestCode(id, Alarms.SLOT_FIRE),
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_FIRE).putExtra(ReminderReceiver.EXTRA_ID, id),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    ) != null

    @Test fun reminderCrudKeepsAlarmsInSync() = runTest {
        val reminders = ReminderRepository(context, db.reminderDao())
        val inAnHour = System.currentTimeMillis() + 60 * MIN
        val id = reminders.save(Reminder(title = "Math revision", triggerTime = inAnHour))
        assertTrue("scheduled on create", alarmSet(id))
        reminders.save(reminders.get(id)!!.copy(triggerTime = inAnHour + 30 * MIN))
        assertTrue("rescheduled on edit", alarmSet(id))
        reminders.setEnabled(id, false)
        assertTrue("cancelled on disable", !alarmSet(id))
        reminders.setEnabled(id, true)
        assertTrue(alarmSet(id))
        reminders.delete(id)
        assertTrue("cancelled on delete", !alarmSet(id))
        assertNull(reminders.get(id))
        try {
            reminders.save(Reminder(title = "Past", triggerTime = System.currentTimeMillis() - MIN))
            fail("past one-off reminder accepted")
        } catch (_: ValidationException) {
        }
        val daily = reminders.save(Reminder(title = "Daily", triggerTime = System.currentTimeMillis() - 24 * 60 * MIN, repeatRule = RepeatRule.DAILY))
        assertTrue("repeating reminder in the past schedules its next occurrence", alarmSet(daily))
        reminders.delete(daily)
    }

    @Test fun eventReminderFollowsTheEvent() = runTest {
        val reminders = ReminderRepository(context, db.reminderDao())
        val calendar = CalendarRepository(db.eventDao(), reminders)
        val start = System.currentTimeMillis() + 2 * 60 * MIN
        val id = calendar.save(CalendarEvent(title = "DSA Practice", startTime = start, endTime = start + 60 * MIN), remindMinutesBefore = 10)
        val event = calendar.get(id)!!
        val reminder = reminders.get(event.reminderId!!)!!
        assertEquals(start - 10 * MIN, reminder.triggerTime)
        assertEquals(10, calendar.reminderOffset(event))
        val today = LocalDate.now()
        assertEquals(1, calendar.occurrences(today, today.plusDays(2)).first().size)
        calendar.save(event, remindMinutesBefore = null)
        assertNull(reminders.get(reminder.id))
        calendar.delete(id)
        assertNull(calendar.get(id))
        try {
            calendar.save(CalendarEvent(title = "Bad", startTime = start, endTime = start - MIN), null)
            fail("end before start accepted")
        } catch (_: ValidationException) {
        }
    }

    @Test fun noteWithChecklistAndDrawingPersists() = runTest {
        val notes = NoteRepository(context, db.noteDao())
        val id = notes.save(com.example.routineforge.core.database.Note(title = "Binary Tree"), listOf(TextBlock(html = "<p>Each node has two children</p>")))
        val drawingId = notes.saveDrawing(id, null, listOf(Stroke(0xFF000000, 0.01f, listOf(0.1f, 0.1f, 0.5f, 0.5f))))
        val blocks = listOf(
            TextBlock(html = "<p>Each node has <b>two</b> children</p>"),
            ChecklistBlock(items = listOf(CheckItem("Inorder", true))),
            DrawingBlock(drawingId = drawingId)
        )
        notes.save(notes.get(id)!!, blocks)
        val reopened = notes.get(id)!!
        assertEquals(blocks, NoteContent.decode(reopened.content))
        assertTrue(reopened.plainText.contains("Inorder"))
        assertEquals(1, DrawingStrokes.decode(notes.drawing(drawingId)!!.strokes).size)
        // Removing the drawing block removes the drawing.
        notes.save(reopened, blocks.take(2))
        assertNull(notes.drawing(drawingId))
        notes.delete(id)
        assertNull(notes.get(id))
        assertNotNull(db) // database still usable
    }
}
