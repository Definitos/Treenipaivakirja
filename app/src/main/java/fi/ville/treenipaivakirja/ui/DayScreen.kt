package fi.ville.treenipaivakirja.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.ville.treenipaivakirja.ExerciseGroup
import fi.ville.treenipaivakirja.WorkoutViewModel
import fi.ville.treenipaivakirja.data.WorkoutSet
import fi.ville.treenipaivakirja.epley
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle as DayNameStyle

/** Lisäysdialogin esitäytöt. lockName = lisätään sarja jo olemassa olevaan liikkeeseen. */
data class AddPrefill(
    val name: String = "",
    val reps: String = "",
    val weight: String = "",
    val lockName: Boolean = false,
    val count: Int = 1
)

/** Esitäyttö: tämän päivän viimeisin sarja > tavoite > edellisen kerran vastaava sarja. */
private fun prefillFor(g: ExerciseGroup): AddPrefill {
    val last = g.sets.lastOrNull()
    val prev = g.previous.getOrNull(g.sets.size) ?: g.previous.lastOrNull()
    val reps = last?.reps ?: g.targetReps.takeIf { it > 0 } ?: prev?.reps
    val weight = last?.weight ?: prev?.weight
    val remaining = g.targetSets - g.sets.size
    return AddPrefill(
        name = g.exercise.name,
        reps = reps?.toString() ?: "",
        weight = weight?.let(::editable) ?: "",
        lockName = true,
        count = if (remaining > 0) remaining else 1
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScreen(vm: WorkoutViewModel, snackbar: SnackbarHostState) {
    val day by vm.selectedDay.collectAsStateWithLifecycle()
    val groups by vm.dayGroups.collectAsStateWithLifecycle()
    val trainingDays by vm.trainingDays.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val templates by vm.templates.collectAsStateWithLifecycle()
    var showTemplates by remember { mutableStateOf(false) }
    var showSaveTemplate by remember { mutableStateOf(false) }

    var dialog by remember { mutableStateOf<AddPrefill?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                DayHeader(
                    day = day,
                    onPrev = { vm.selectedDay.value = day.minusDays(1) },
                    onNext = { vm.selectedDay.value = day.plusDays(1) },
                    onToday = { vm.selectedDay.value = LocalDate.now() },
                    onPick = { showPicker = true }
                )
            }
            item { WeekStrip(day, trainingDays) { vm.selectedDay.value = it } }
            item { SummaryCard(groups) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showTemplates = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Bookmarks, null, tint = Lime, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Lataa ohjelma", color = Color.White)
                    }
                    if (groups.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showSaveTemplate = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.BookmarkAdd, null, tint = Cyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Tallenna ohjelmaksi", color = Color.White, maxLines = 1)
                        }
                    }
                }
            }
            if (groups.isEmpty()) item { EmptyDay() }
            items(groups, key = { it.exercise.id }) { g ->
                ExerciseCard(
                    group = g,
                    onAddSet = { dialog = prefillFor(g) },
                    onRemove = { vm.removeExerciseFromDay(g.exercise.id) },
                    onDelete = { set ->
                        vm.deleteSet(set)
                        scope.launch {
                            val r = snackbar.showSnackbar(
                                message = "Sarja poistettu",
                                actionLabel = "Kumoa",
                                duration = SnackbarDuration.Short
                            )
                            if (r == SnackbarResult.ActionPerformed) vm.restoreSet(set)
                        }
                    }
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = { dialog = AddPrefill() },
            containerColor = Lime,
            contentColor = Color.Black,
            shape = RoundedCornerShape(18.dp),
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Lisää liike", fontWeight = FontWeight.Bold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    dialog?.let { prefill ->
        AddSetDialog(
            prefill = prefill,
            allNames = exercises.map { it.name },
            onDismiss = { dialog = null },
            onSave = { name, reps, weight, count ->
                vm.addSets(name, reps, weight, count)
                dialog = null
            }
        )
    }

    if (showTemplates) {
        ModalBottomSheet(
            onDismissRequest = { showTemplates = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Bg
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Valitse treeniohjelma", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                Text(day.fiLong(), color = Muted, fontSize = 14.sp)
                if (templates.isEmpty()) {
                    Text(
                        "Kirjastossa ei ole vielä ohjelmia. Luo ohjelma Ohjelmat-välilehdellä tai tallenna päivän treeni ohjelmaksi.",
                        color = Muted
                    )
                }
                templates.forEach { t ->
                    TemplateCard(t, onClick = {
                        vm.applyTemplate(t.template.id)
                        showTemplates = false
                        scope.launch { snackbar.showSnackbar("${t.template.name} lisätty päivälle") }
                    })
                }
            }
        }
    }

    if (showSaveTemplate) {
        var tName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveTemplate = false },
            containerColor = CardBg,
            title = { Text("Tallenna ohjelmaksi", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Päivän ${groups.size} liikettä tallennetaan kirjastoon.", color = Muted, fontSize = 14.sp)
                    OutlinedTextField(
                        value = tName,
                        onValueChange = { tName = it },
                        label = { Text("Ohjelman nimi") },
                        placeholder = { Text("esim. Yläkroppa 1") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.saveDayAsTemplate(tName)
                        showSaveTemplate = false
                        scope.launch { snackbar.showSnackbar("Ohjelma \"${tName.trim()}\" tallennettu") }
                    },
                    enabled = tName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black)
                ) { Text("Tallenna", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showSaveTemplate = false }) { Text("Peruuta") } }
        )
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        vm.selectedDay.value = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Peruuta") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun DayHeader(
    day: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onPick: () -> Unit
) {
    val today = LocalDate.now()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            val top = when (day) {
                today -> "Tänään"
                today.minusDays(1) -> "Eilen"
                else -> "Palaa tähän päivään ›"
            }
            Text(
                top,
                color = Lime,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                modifier = if (day != today) Modifier.clickable(onClick = onToday) else Modifier
            )
            Text(
                day.fiLong(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.clickable(onClick = onPick)
            )
        }
        RoundIcon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Edellinen päivä", onPrev)
        Spacer(Modifier.width(6.dp))
        RoundIcon(Icons.Filled.CalendarMonth, "Valitse päivä", onPick)
        Spacer(Modifier.width(6.dp))
        RoundIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Seuraava päivä", onNext)
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(CardBg2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = desc, tint = Color.White, modifier = Modifier.size(22.dp)) }
}

@Composable
private fun WeekStrip(day: LocalDate, trainingDays: Set<Long>, onSelect: (LocalDate) -> Unit) {
    val monday = day.with(DayOfWeek.MONDAY)
    val today = LocalDate.now()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0..6) {
            val d = monday.plusDays(i.toLong())
            val selected = d == day
            val bg = if (selected) Modifier.background(AccentBrush) else Modifier.background(CardBg)
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .then(bg)
                    .clickable { onSelect(d) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    d.dayOfWeek.getDisplayName(DayNameStyle.SHORT, FI),
                    fontSize = 12.sp,
                    color = if (selected) Color.Black else Muted
                )
                Text(
                    d.dayOfMonth.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        selected -> Color.Black
                        d == today -> Lime
                        else -> Color.White
                    }
                )
                Spacer(Modifier.height(4.dp))
                val trained = trainingDays.contains(d.toEpochDay())
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                !trained -> Color.Transparent
                                selected -> Color.Black
                                else -> Lime
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(groups: List<ExerciseGroup>) {
    val setCount = groups.sumOf { it.sets.size }
    val volume = groups.sumOf { g -> g.sets.sumOf { it.weight * it.reps } }
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBg)
            .border(1.dp, AccentBrush, shape)
            .padding(vertical = 18.dp)
    ) {
        BigStat("Liikkeet", groups.size.toString(), Modifier.weight(1f))
        BigStat("Sarjat", setCount.toString(), Modifier.weight(1f))
        BigStat("Volyymi", "${num(volume)} kg", Modifier.weight(1f))
    }
}

@Composable
fun BigStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(brush = AccentBrush),
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Text(label, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun EmptyDay() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardBg)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.FitnessCenter, null, tint = Lime, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text("Ei treenejä tälle päivälle", fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            "Lataa valmis ohjelma tai lisää liike napista.",
            color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ExerciseCard(
    group: ExerciseGroup,
    onAddSet: () -> Unit,
    onRemove: () -> Unit,
    onDelete: (WorkoutSet) -> Unit
) {
    val best = group.sets.maxByOrNull { epley(it.weight, it.reps) }
    var menu by remember { mutableStateOf(false) }
    val hasTarget = group.targetSets > 0
    val done = hasTarget && group.sets.size >= group.targetSets
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardBg)
            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 4.dp, height = 22.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AccentBrush)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                group.exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            if (hasTarget) {
                Text(
                    "${group.sets.size}/${group.targetSets} × ${group.targetReps}",
                    color = if (done) Color.Black else Lime,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .then(if (done) Modifier.background(AccentBrush) else Modifier.background(Lime.copy(alpha = 0.12f)))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            } else best?.let {
                Text(
                    "${num(it.weight)} kg × ${it.reps}",
                    color = Lime,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Lime.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, "Valikko", tint = Muted)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Poista liike päivältä") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                        onClick = { menu = false; onRemove() }
                    )
                }
            }
        }
        if (group.previous.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 14.dp, top = 2.dp)) {
                Icon(Icons.Filled.History, null, tint = Muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "Viimeksi: " + group.previous.joinToString(" · ") { "${num(it.weight)}×${it.reps}" },
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        group.sets.forEachIndexed { i, s ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(CardBg2),
                    contentAlignment = Alignment.Center
                ) { Text("${i + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Muted) }
                Spacer(Modifier.width(12.dp))
                Text(
                    "${num(s.weight)} kg",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Text("${s.reps} toistoa", color = Muted, fontSize = 15.sp)
                IconButton(onClick = { onDelete(s) }) {
                    Icon(Icons.Outlined.Delete, "Poista sarja", tint = Muted, modifier = Modifier.size(20.dp))
                }
            }
        }
        if (group.sets.isEmpty()) {
            Button(
                onClick = onAddSet,
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp, top = 4.dp, bottom = 10.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Kirjaa sarjat", fontWeight = FontWeight.Bold)
            }
        } else {
            TextButton(onClick = onAddSet) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (hasTarget && !done) "Kirjaa seuraava" else "Lisää sarja", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AddSetDialog(
    prefill: AddPrefill,
    allNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, reps: Int, weight: Double, count: Int) -> Unit
) {
    var name by remember { mutableStateOf(prefill.name) }
    var reps by remember { mutableStateOf(prefill.reps) }
    var weight by remember { mutableStateOf(prefill.weight) }
    var count by remember { mutableIntStateOf(prefill.count) }

    val repsInt = reps.toIntOrNull()
    val weightD = weight.replace(',', '.').toDoubleOrNull()
    val valid = name.isNotBlank() && repsInt != null && repsInt > 0 && weightD != null && weightD >= 0

    val suggestions = remember(name, allNames) {
        val q = name.trim()
        allNames.filter { it.contains(q, ignoreCase = true) && !it.equals(q, ignoreCase = true) }.take(8)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = {
            Text(
                if (prefill.lockName) prefill.name else "Lisää liike",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!prefill.lockName) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Liike") },
                        placeholder = { Text("esim. Penkkipunnerrus") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (suggestions.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(suggestions) { s ->
                                AssistChip(onClick = { name = s }, label = { Text(s) })
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { v -> weight = v.filter { it.isDigit() || it == ',' || it == '.' } },
                        label = { Text("Kilot") },
                        suffix = { Text("kg") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { v -> reps = v.filter { it.isDigit() } },
                        label = { Text("Toistot") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sarjoja", color = Color.White, modifier = Modifier.weight(1f))
                    IconButton(onClick = { if (count > 1) count-- }) {
                        Icon(Icons.Filled.Remove, "Vähemmän")
                    }
                    Text("$count", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    IconButton(onClick = { if (count < 10) count++ }) {
                        Icon(Icons.Filled.Add, "Enemmän")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (valid) onSave(name, repsInt!!, weightD!!, count) },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black)
            ) { Text("Tallenna", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Peruuta") } }
    )
}
