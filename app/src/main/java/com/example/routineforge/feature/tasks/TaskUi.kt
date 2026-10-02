package com.example.routineforge.feature.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.routineforge.core.database.Subject
import com.example.routineforge.core.database.TaskItem
import com.example.routineforge.core.theme.FF
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.core.ui.DatePickerDialogFF
import java.time.LocalDate

@Composable
fun TaskRow(task: TaskItem, subject: Subject?, onToggle: (Boolean) -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val today = LocalDate.now().toEpochDay()
    Row(modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = task.isDone, onCheckedChange = onToggle)
        Column(Modifier.weight(1f)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val due = task.dueDate
            val meta = listOfNotNull(
                subject?.name,
                due?.let {
                    when (it) {
                        today -> "Today"
                        today + 1 -> "Tomorrow"
                        else -> TimeFormat.day(LocalDate.ofEpochDay(it))
                    }
                }
            )
            if (meta.isNotEmpty()) {
                Text(
                    meta.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!task.isDone && due != null && due < today) FF.colors.danger else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, contentDescription = "Delete task ${task.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** Title, optional subject and due date. [onSave] gets (title, subjectId, dueEpochDay). */
@Composable
fun AddTaskDialog(
    subjects: List<Subject>,
    initialSubjectId: Long? = null,
    initialDate: LocalDate? = LocalDate.now(),
    onDismiss: () -> Unit,
    onSave: (String, Long?, Long?) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var subjectId by rememberSaveable { mutableStateOf(initialSubjectId) }
    var due by rememberSaveable { mutableStateOf(initialDate?.toEpochDay()) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now()
    val submit = { if (title.isNotBlank()) onSave(title, subjectId, due) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    placeholder = { Text("What needs doing?") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Due", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { FilterChip(due == today.toEpochDay(), { due = today.toEpochDay() }, { Text("Today") }) }
                    item { FilterChip(due == today.plusDays(1).toEpochDay(), { due = today.plusDays(1).toEpochDay() }, { Text("Tomorrow") }) }
                    item {
                        val custom = due != null && due != today.toEpochDay() && due != today.plusDays(1).toEpochDay()
                        FilterChip(custom, { pickDate = true }, { Text(if (custom) TimeFormat.day(LocalDate.ofEpochDay(due!!)) else "Pick date") }, leadingIcon = { Icon(Icons.Filled.Event, null) })
                    }
                    item { FilterChip(due == null, { due = null }, { Text("No date") }) }
                }
                if (subjects.isNotEmpty()) {
                    Text("Subject", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item { FilterChip(subjectId == null, { subjectId = null }, { Text("None") }) }
                        items(subjects, key = { it.id }) { s -> FilterChip(subjectId == s.id, { subjectId = s.id }, { Text(s.name) }) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = submit, enabled = title.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
    if (pickDate) DatePickerDialogFF(due?.let { LocalDate.ofEpochDay(it) }, { pickDate = false }) { due = it.toEpochDay() }
}
