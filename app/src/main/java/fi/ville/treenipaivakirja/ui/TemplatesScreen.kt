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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.ville.treenipaivakirja.DraftItem
import fi.ville.treenipaivakirja.TemplateDraft
import fi.ville.treenipaivakirja.TemplateWithItems
import fi.ville.treenipaivakirja.WorkoutViewModel
import fi.ville.treenipaivakirja.newKey

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
                    Text("Kirjasto", color = Lime, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Treeniohjelmat",
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
                            items = t.items.map { DraftItem(newKey(), it.exerciseName, it.targetSets, it.targetReps) }
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
            text = { Text("Uusi ohjelma", fontWeight = FontWeight.Bold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    deleting?.let { t ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = CardBg,
            title = { Text("Poistetaanko ${t.template.name}?", fontWeight = FontWeight.Bold) },
            text = { Text("Ohjelma poistuu kirjastosta. Jo kirjatut treenit säilyvät.", color = Muted) },
            confirmButton = {
                TextButton(onClick = { vm.deleteTemplate(t.template.id); deleting = null }) {
                    Text("Poista", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Peruuta") } }
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
                    "${t.items.size} liikettä · ${t.items.sumOf { it.targetSets }} sarjaa",
                    color = Muted, fontSize = 13.sp
                )
            }
            if (onEdit != null) IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Muokkaa", tint = Muted) }
            if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Poista", tint = Muted) }
        }
        Spacer(Modifier.height(10.dp))
        t.items.forEach { item ->
            Row(Modifier.padding(vertical = 2.dp, horizontal = 2.dp)) {
                Text(item.exerciseName, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("${item.targetSets} × ${item.targetReps}", color = Lime, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
        Text("Ei vielä ohjelmia", fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            "Luo esim. \"Yläkroppa 1\" ja lataa se treenipäivälle yhdellä napautuksella.",
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
        items.add(DraftItem(newKey(), clean, 3, 10))
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Takaisin", tint = Color.White)
                }
                Text(
                    if (initial.id == null) "Uusi ohjelma" else "Muokkaa ohjelmaa",
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
                label = { Text("Ohjelman nimi") },
                placeholder = { Text("esim. Yläkroppa 1") },
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
                Text("Lisää liike", color = Color.White, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newExercise,
                        onValueChange = { newExercise = it },
                        placeholder = { Text("Liikkeen nimi") },
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
                    ) { Icon(Icons.Filled.Add, "Lisää", tint = if (newExercise.isNotBlank()) Color.Black else Muted) }
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
            ) { Text("Tallenna ohjelma", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
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
                Icon(Icons.Filled.KeyboardArrowUp, "Ylös", tint = if (canUp) Muted else CardBg2)
            }
            IconButton(onClick = onDown, enabled = canDown) {
                Icon(Icons.Filled.KeyboardArrowDown, "Alas", tint = if (canDown) Muted else CardBg2)
            }
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, "Poista", tint = Muted) }
        }
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stepper("Sarjat", item.sets, 1..10, Modifier.weight(1f)) { onChange(item.copy(sets = it)) }
            Stepper("Toistot", item.reps, 1..50, Modifier.weight(1f)) { onChange(item.copy(reps = it)) }
        }
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
            Icon(Icons.Filled.Remove, "Vähemmän", tint = Color.White)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", color = Lime, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(label, color = Muted, fontSize = 11.sp)
        }
        IconButton(onClick = { if (value < range.last) onValue(value + 1) }) {
            Icon(Icons.Filled.Add, "Enemmän", tint = Color.White)
        }
    }
}
