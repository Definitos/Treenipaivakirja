package fi.ville.treenipaivakirja.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import fi.ville.treenipaivakirja.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.ville.treenipaivakirja.DraftItem
import fi.ville.treenipaivakirja.TemplateDraft
import fi.ville.treenipaivakirja.TemplateWithItems
import fi.ville.treenipaivakirja.WorkoutViewModel
import fi.ville.treenipaivakirja.newKey
import fi.ville.treenipaivakirja.repsRange

@Composable
fun TemplatesScreen(vm: WorkoutViewModel) {
    val templates by vm.templates.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<TemplateDraft?>(null) }
    var deleting by remember { mutableStateOf<TemplateWithItems?>(null) }

    val draft = editing
    if (draft != null) {
        TemplateEditor(
            initial = draft,
            allNames = exercises.map { it.name },
            onCancel = { editing = null },
            onSave = { vm.saveTemplate(it); editing = null }
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(stringResource(R.string.library), color = Lime, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Text(
                        stringResource(R.string.programs_title),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
            if (templates.isEmpty()) item { EmptyTemplates() }
            items(templates, key = { it.template.id }) { t ->
                TemplateCard(
                    t = t,
                    onEdit = {
                        editing = TemplateDraft(
                            id = t.template.id,
                            name = t.template.name,
                            items = t.items.map { DraftItem(newKey(), it.exerciseName, it.targetSets, it.targetReps, maxOf(it.targetReps, it.targetRepsMax)) }
                        )
                    },
                    onDelete = { deleting = t }
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = { editing = TemplateDraft(null, "", emptyList()) },
            containerColor = Lime,
            contentColor = Color.Black,
            shape = RoundedCornerShape(18.dp),
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.new_program), fontWeight = FontWeight.Bold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    deleting?.let { t ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = CardBg,
            title = { Text(stringResource(R.string.delete_program_title, t.template.name), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_program_body), color = Muted) },
            confirmButton = {
                TextButton(onClick = { vm.deleteTemplate(t.template.id); deleting = null }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

/** Ohjelmakortti; käytetään myös päivänäkymän valitsimessa (onClick). */
@Composable
fun TemplateCard(
    t: TemplateWithItems,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBg)
            .border(1.dp, CardBg2, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentBrush),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Bookmarks, null, tint = Color.Black, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.template.name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.program_summary, t.items.size, t.items.sumOf { it.targetSets }),
                    color = Muted, fontSize = 13.sp
                )
            }
            if (onEdit != null) IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, stringResource(R.string.edit), tint = Muted) }
            if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, stringResource(R.string.delete), tint = Muted) }
        }
        Spacer(Modifier.height(10.dp))
        t.items.forEach { item ->
            Row(Modifier.padding(vertical = 2.dp, horizontal = 2.dp)) {
                Text(item.exerciseName, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("${item.targetSets} × ${repsRange(item.targetReps, item.targetRepsMax)}", color = Lime, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun EmptyTemplates() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardBg)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Bookmarks, null, tint = Lime, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.no_programs_title), fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            stringResource(R.string.no_programs_body),
            color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TemplateEditor(
    initial: TemplateDraft,
    allNames: List<String>,
    onCancel: () -> Unit,
    onSave: (TemplateDraft) -> Unit
) {
    BackHandler(onBack = onCancel)

    var name by remember { mutableStateOf(initial.name) }
    val items = remember { mutableStateListOf<DraftItem>().apply { addAll(initial.items) } }
    var newExercise by remember { mutableStateOf("") }

    fun addItem(n: String) {
        val clean = n.trim()
        if (clean.isEmpty()) return
        items.add(DraftItem(newKey(), clean, 3, 8, 10))
        newExercise = ""
    }

    val suggestions = remember(newExercise, allNames, items.size) {
        val q = newExercise.trim()
        val used = items.map { it.name.lowercase() }.toSet()
        allNames.filter { it.lowercase() !in used && it.contains(q, ignoreCase = true) }.take(10)
    }
    val valid = name.isNotBlank() && items.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
                }
                Text(
                    stringResource(if (initial.id == null) R.string.new_program else R.string.edit_program),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.program_name)) },
                placeholder = { Text(stringResource(R.string.program_name_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
        }

        itemsIndexed(items, key = { _, it -> it.key }) { i, item ->
            EditorRow(
                index = i,
                item = item,
                canUp = i > 0,
                canDown = i < items.lastIndex,
                onChange = { items[i] = it },
                onUp = { val t = items[i]; items[i] = items[i - 1]; items[i - 1] = t },
                onDown = { val t = items[i]; items[i] = items[i + 1]; items[i + 1] = t },
                onRemove = { items.removeAt(i) }
            )
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(CardBg)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.add_exercise), color = Color.White, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newExercise,
                        onValueChange = { newExercise = it },
                        placeholder = { Text(stringResource(R.string.exercise_name)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { addItem(newExercise) }),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (newExercise.isNotBlank()) Lime else CardBg2)
                            .clickable { addItem(newExercise) },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Add, stringResource(R.string.add), tint = if (newExercise.isNotBlank()) Color.Black else Muted) }
                }
                if (suggestions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(suggestions) { s -> AssistChip(onClick = { addItem(s) }, label = { Text(s) }) }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { onSave(TemplateDraft(initial.id, name, items.toList())) },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text(stringResource(R.string.save_program), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

@Composable
private fun EditorRow(
    index: Int,
    item: DraftItem,
    canUp: Boolean,
    canDown: Boolean,
    onChange: (DraftItem) -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(AccentBrush),
                contentAlignment = Alignment.Center
            ) { Text("${index + 1}", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
            Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onUp, enabled = canUp) {
                Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.move_up), tint = if (canUp) Muted else CardBg2)
            }
            IconButton(onClick = onDown, enabled = canDown) {
                Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.move_down), tint = if (canDown) Muted else CardBg2)
            }
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, stringResource(R.string.remove), tint = Muted) }
        }
        Row(Modifier.padding(top = 4.dp)) {
            Stepper(stringResource(R.string.sets), item.sets, 1..10, Modifier.weight(1f)) { onChange(item.copy(sets = it)) }
            Spacer(Modifier.weight(1f).padding(end = 10.dp))
        }
        Row(Modifier.padding(top = 8.dp)) {
            // Alaraja: jos nousee ylärajan yli, yläraja seuraa mukana
            Stepper(stringResource(R.string.reps_min), item.reps, 1..50, Modifier.weight(1f)) {
                onChange(item.copy(reps = it, repsMax = maxOf(it, item.repsMax)))
            }
            Stepper(stringResource(R.string.reps_max), item.repsMax, 1..50, Modifier.weight(1f)) {
                onChange(item.copy(repsMax = it, reps = minOf(it, item.reps)))
            }
        }
        Text(
            "${item.sets} × ${repsRange(item.reps, item.repsMax)}",
            color = Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 2.dp, top = 6.dp)
        )
    }
}

@Composable
private fun Stepper(label: String, value: Int, range: IntRange, modifier: Modifier, onValue: (Int) -> Unit) {
    Row(
        modifier
            .padding(end = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { if (value > range.first) onValue(value - 1) }) {
            Icon(Icons.Filled.Remove, stringResource(R.string.fewer), tint = Color.White)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", color = Lime, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(label, color = Muted, fontSize = 11.sp)
        }
        IconButton(onClick = { if (value < range.last) onValue(value + 1) }) {
            Icon(Icons.Filled.Add, stringResource(R.string.more), tint = Color.White)
        }
    }
}
