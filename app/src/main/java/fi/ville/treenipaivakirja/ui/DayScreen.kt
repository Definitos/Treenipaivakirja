package fi.ville.treenipaivakirja.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.ville.treenipaivakirja.ExerciseGroup
import fi.ville.treenipaivakirja.R
import fi.ville.treenipaivakirja.WorkoutViewModel
import fi.ville.treenipaivakirja.data.WorkoutSet
import fi.ville.treenipaivakirja.epley
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.text.input.ImeAction
import fi.ville.treenipaivakirja.repsRange
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle as DayNameStyle

/** Lisäysdialogin esitäytöt (paino valitussa yksikössä). lockName = sarja olemassa olevaan liikkeeseen. */
data class AddPrefill(
    val name: String = "",
    val reps: String = "",
    val weight: String = "",
    val lockName: Boolean = false,
    val count: Int = 1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScreen(vm: WorkoutViewModel, snackbar: SnackbarHostState) {
    val day by vm.selectedDay.collectAsStateWithLifecycle()
    val groups by vm.dayGroups.collectAsStateWithLifecycle()
    val trainingDays by vm.trainingDays.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val templates by vm.templates.collectAsStateWithLifecycle()
    val unit = LocalUnit.current
    val context = LocalContext.current

    var dialog by remember { mutableStateOf<AddPrefill?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var showSaveTemplate by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
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
                    onPick = { showPicker = true },
                    onSettings = { showSettings = true }
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
                        Text(stringResource(R.string.load_program), color = Color.White, maxLines = 1)
                    }
                    if (groups.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showSaveTemplate = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.BookmarkAdd, null, tint = Cyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.save_as_program), color = Color.White, maxLines = 1)
                        }
                    }
                }
            }
            if (groups.isEmpty()) item { EmptyDay() }
            items(groups, key = { it.exercise.id }) { g ->
                ExerciseCard(
                    group = g,
                    onLog = { reps, kg -> vm.logSet(g.exercise.id, reps, kg) },
                    onRemove = { vm.removeExerciseFromDay(g.exercise.id) },
                    onDelete = { set ->
                        vm.deleteSet(set)
                        scope.launch {
                            val r = snackbar.showSnackbar(
                                message = context.getString(R.string.set_deleted),
                                actionLabel = context.getString(R.string.undo),
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
            text = { Text(stringResource(R.string.add_exercise), fontWeight = FontWeight.Bold) },
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
            onSave = { name, reps, weightInUnit, count ->
                vm.addSets(name, reps, unit.toKg(weightInUnit), count)
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
                Text(
                    stringResource(R.string.choose_program),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(day.longLabel(), color = Muted, fontSize = 14.sp)
                if (templates.isEmpty()) {
                    Text(stringResource(R.string.no_programs_hint), color = Muted)
                }
                templates.forEach { t ->
                    TemplateCard(t, onClick = {
                        vm.applyTemplate(t.template.id)
                        showTemplates = false
                        scope.launch {
                            snackbar.showSnackbar(context.getString(R.string.program_added, t.template.name))
                        }
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
            title = { Text(stringResource(R.string.save_as_program), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.save_program_body, groups.size), color = Muted, fontSize = 14.sp)
                    OutlinedTextField(
                        value = tName,
                        onValueChange = { tName = it },
                        label = { Text(stringResource(R.string.program_name)) },
                        placeholder = { Text(stringResource(R.string.program_name_hint)) },
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
                        scope.launch {
                            snackbar.showSnackbar(context.getString(R.string.program_saved, tName.trim()))
                        }
                    },
                    enabled = tName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black)
                ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplate = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showSettings) {
        SettingsDialog(
            unit = unit,
            onUnit = vm::setUnit,
            onDismiss = { showSettings = false }
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
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun SettingsDialog(unit: WeightUnit, onUnit: (WeightUnit) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.weight_unit), color = Color.White, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeightUnit.entries.forEach { u ->
                        FilterChip(
                            selected = u == unit,
                            onClick = { onUnit(u) },
                            label = { Text(u.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Lime,
                                selectedLabelColor = Color.Black,
                                labelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.language), color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.language_system), color = Muted, fontSize = 14.sp)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    OutlinedButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                                .setData(Uri.fromParts("package", context.packageName, null))
                        )
                    }) { Text(stringResource(R.string.change_language), color = Color.White) }
                }
                HorizontalDivider(color = CardBg2, modifier = Modifier.padding(top = 8.dp))
                val version = remember {
                    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
                        .getOrNull() ?: ""
                }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleSmall.copy(brush = AccentBrush),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(stringResource(R.string.version, version), color = Muted, fontSize = 12.sp)
                    Text(stringResource(R.string.copyright), color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } }
    )
}

@Composable
private fun DayHeader(
    day: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onPick: () -> Unit,
    onSettings: () -> Unit
) {
    val today = LocalDate.now()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            val top = when (day) {
                today -> stringResource(R.string.today)
                today.minusDays(1) -> stringResource(R.string.yesterday)
                else -> stringResource(R.string.back_to_today)
            }
            Text(
                top,
                color = Lime,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                modifier = if (day != today) Modifier.clickable(onClick = onToday) else Modifier
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onPick)
            ) {
                Text(
                    day.longLabel(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Filled.CalendarMonth,
                    stringResource(R.string.pick_day),
                    tint = Muted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        RoundIcon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.prev_day), onPrev)
        Spacer(Modifier.width(6.dp))
        RoundIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_day), onNext)
        Spacer(Modifier.width(6.dp))
        RoundIcon(Icons.Filled.Settings, stringResource(R.string.settings), onSettings)
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
    val locale = appLocale()
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
                    d.dayOfWeek.getDisplayName(DayNameStyle.SHORT, locale),
                    fontSize = 12.sp,
                    maxLines = 1,
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
    val volumeKg = groups.sumOf { g -> g.sets.sumOf { it.weight * it.reps } }
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBg)
            .border(1.dp, AccentBrush, shape)
            .padding(vertical = 18.dp)
    ) {
        BigStat(stringResource(R.string.exercises), groups.size.toString(), Modifier.weight(1f))
        BigStat(stringResource(R.string.sets), setCount.toString(), Modifier.weight(1f))
        BigStat(stringResource(R.string.volume), wt(volumeKg), Modifier.weight(1f))
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
        Text(stringResource(R.string.empty_day_title), fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            stringResource(R.string.empty_day_body),
            color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center
        )
    }
}

/** Kirjaamattoman sarjarivin syötteet (paino valitussa yksikössä). */
private data class PendingInput(val weight: String, val reps: String, val weightTouched: Boolean = false)

private val Warn = Color(0xFFFFB04D)

@Composable
private fun ExerciseCard(
    group: ExerciseGroup,
    onLog: (reps: Int, weightKg: Double) -> Unit,
    onRemove: () -> Unit,
    onDelete: (WorkoutSet) -> Unit
) {
    val unit = LocalUnit.current
    val best = group.sets.maxByOrNull { epley(it.weight, it.reps) }
    var menu by remember { mutableStateOf(false) }
    val hasTarget = group.targetSets > 0
    val logged = group.sets.size
    val done = hasTarget && logged >= group.targetSets
    val topReps = maxOf(group.targetReps, group.targetRepsMax)
    val goHeavier = done && group.targetReps > 0 && group.sets.all { it.reps >= topReps }

    // Kirjaamattomat sarjarivit: ohjelman jäljellä olevat + käyttäjän lisäämät
    var extra by remember(group.exercise.id) { mutableIntStateOf(0) }
    val pending = remember(group.exercise.id) { mutableStateMapOf<Int, PendingInput>() }
    val slots = if (!hasTarget && logged == 0) maxOf(extra, 1) else maxOf(group.targetSets - logged, 0) + extra

    /** Paino: tämän päivän viimeisin > edellisen kerran vastaava sarja > edellisen kerran viimeinen. */
    fun initialFor(n: Int): PendingInput {
        val kg = group.sets.lastOrNull()?.weight
            ?: group.previous.getOrNull(n)?.weight
            ?: group.previous.lastOrNull()?.weight
        return PendingInput(weight = kg?.let { editable(unit.fromKg(it)) } ?: "", reps = "")
    }

    /** Toistokentän vihje: ohjelman haarukka tai edellisen kerran toistot. */
    fun repsHint(n: Int): String = when {
        group.targetReps > 0 -> repsRange(group.targetReps, group.targetRepsMax)
        else -> group.previous.getOrNull(n)?.reps?.toString() ?: ""
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardBg)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)
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
                    "$logged/${group.targetSets} × ${repsRange(group.targetReps, group.targetRepsMax)}",
                    color = if (done) Color.Black else Lime,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .then(if (done) Modifier.background(AccentBrush) else Modifier.background(Lime.copy(alpha = 0.12f)))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            } else if (best != null) {
                Text(
                    "${wt(best.weight)} × ${best.reps}",
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
                    Icon(Icons.Filled.MoreVert, stringResource(R.string.menu), tint = Muted)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.remove_from_day)) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                        onClick = { menu = false; onRemove() }
                    )
                }
            }
        }
        if (group.previous.isNotEmpty()) {
            val prevText = group.previous.joinToString(" · ") { "${num(unit.fromKg(it.weight))}×${it.reps}" }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 14.dp, top = 2.dp)) {
                Icon(Icons.Filled.History, null, tint = Muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.last_time, prevText), color = Muted, fontSize = 12.sp, maxLines = 1)
            }
        }
        if (goHeavier) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 14.dp, top = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Cyan.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Filled.ArrowUpward, null, tint = Cyan, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.go_heavier), color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Kirjatut sarjat
        group.sets.forEachIndexed { i, s ->
            val repsColor = when {
                group.targetReps > 0 && s.reps < group.targetReps -> Warn
                group.targetReps > 0 && s.reps >= topReps -> Lime
                else -> Muted
            }
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
                        .background(AccentBrush),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp)) }
                Spacer(Modifier.width(12.dp))
                Text(
                    wt(s.weight),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(stringResource(R.string.reps_count, s.reps), color = repsColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { onDelete(s) }) {
                    Icon(Icons.Outlined.Delete, stringResource(R.string.delete_set), tint = Muted, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Kirjaamattomat sarjat: kg + toistot + ✓
        repeat(slots) { j ->
            val n = logged + j
            key(n) {
                val input = pending[n] ?: initialFor(n)
                val hint = repsHint(n)
                PendingSetRow(
                    number = n + 1,
                    input = input,
                    unitLabel = unit.label,
                    repsHint = hint,
                    onChange = { pending[n] = it },
                    onSave = { reps, weightInUnit ->
                        onLog(reps, unit.toKg(weightInUnit))
                        pending.remove(n)
                        // Päivitä seuraavien koskemattomien rivien paino juuri käytettyyn
                        for (k in n + 1 until logged + slots) {
                            val cur = pending[k] ?: initialFor(k)
                            if (!cur.weightTouched) pending[k] = cur.copy(weight = editable(weightInUnit))
                        }
                        if (hasTarget && n >= group.targetSets || !hasTarget) extra = maxOf(extra - 1, 0)
                    }
                )
            }
        }

        TextButton(onClick = { extra++ }) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.add_set), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PendingSetRow(
    number: Int,
    input: PendingInput,
    unitLabel: String,
    repsHint: String,
    onChange: (PendingInput) -> Unit,
    onSave: (reps: Int, weightInUnit: Double) -> Unit
) {
    val weight = parseDecimal(input.weight)
    // Tyhjä toistokenttä hyväksytään, jos vihje on yksittäinen luku (esim. viime kerran toistot)
    val reps = input.reps.toIntOrNull() ?: if (input.reps.isBlank()) repsHint.toIntOrNull() else null
    val valid = weight != null && weight >= 0 && reps != null && reps > 0
    val save = { if (valid) onSave(reps!!, weight!!) }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(CardBg2),
            contentAlignment = Alignment.Center
        ) { Text("$number", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Muted) }
        Spacer(Modifier.width(10.dp))
        OutlinedTextField(
            value = input.weight,
            onValueChange = { v ->
                onChange(input.copy(weight = v.filter { it.isDigit() || it == ',' || it == '.' }, weightTouched = true))
            },
            suffix = { Text(unitLabel, fontSize = 13.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1.1f)
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = input.reps,
            onValueChange = { v -> onChange(input.copy(reps = v.filter { it.isDigit() })) },
            placeholder = { Text(repsHint, color = Muted) },
            suffix = { Text("×", fontSize = 13.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .then(if (valid) Modifier.background(AccentBrush) else Modifier.background(CardBg2))
                .clickable(enabled = valid, onClick = save),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Check,
                stringResource(R.string.save_set),
                tint = if (valid) Color.Black else Muted
            )
        }
    }
}

@Composable
private fun AddSetDialog(
    prefill: AddPrefill,
    allNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, reps: Int, weightInUnit: Double, count: Int) -> Unit
) {
    val unit = LocalUnit.current
    var name by remember { mutableStateOf(prefill.name) }
    var reps by remember { mutableStateOf(prefill.reps) }
    var weight by remember { mutableStateOf(prefill.weight) }
    var count by remember { mutableIntStateOf(prefill.count) }

    val repsInt = reps.toIntOrNull()
    val weightD = parseDecimal(weight)
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
                if (prefill.lockName) prefill.name else stringResource(R.string.add_exercise),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!prefill.lockName) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.exercise)) },
                        placeholder = { Text(stringResource(R.string.exercise_hint)) },
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
                        label = { Text(stringResource(R.string.weight)) },
                        suffix = { Text(unit.label) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { v -> reps = v.filter { it.isDigit() } },
                        label = { Text(stringResource(R.string.reps)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.sets), color = Color.White, modifier = Modifier.weight(1f))
                    IconButton(onClick = { if (count > 1) count-- }) {
                        Icon(Icons.Filled.Remove, stringResource(R.string.fewer))
                    }
                    Text("$count", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    IconButton(onClick = { if (count < 10) count++ }) {
                        Icon(Icons.Filled.Add, stringResource(R.string.more))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (valid) onSave(name, repsInt!!, weightD!!, count) },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black)
            ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
