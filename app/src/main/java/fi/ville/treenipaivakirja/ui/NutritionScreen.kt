package fi.ville.treenipaivakirja.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.ville.treenipaivakirja.Activity
import fi.ville.treenipaivakirja.FoodItem
import fi.ville.treenipaivakirja.Goal
import fi.ville.treenipaivakirja.KCAL_CARB
import fi.ville.treenipaivakirja.KCAL_FAT
import fi.ville.treenipaivakirja.KCAL_PROTEIN
import fi.ville.treenipaivakirja.Macros
import fi.ville.treenipaivakirja.MealGroup
import fi.ville.treenipaivakirja.NutritionProfile
import fi.ville.treenipaivakirja.NutritionViewModel
import fi.ville.treenipaivakirja.R
import fi.ville.treenipaivakirja.Sex
import fi.ville.treenipaivakirja.Targets
import fi.ville.treenipaivakirja.data.CustomFood
import fi.ville.treenipaivakirja.data.FoodEntry
import fi.ville.treenipaivakirja.macros
import fi.ville.treenipaivakirja.macrosFor
import fi.ville.treenipaivakirja.targets
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

// Makrojen värit (yhtenäiset kaikissa näkymissä)
private val ProteinColor = Lime
private val CarbColor = Cyan
private val FatColor = Color(0xFFFFB04D)
private val Over = Color(0xFFFF5A6E)

private fun kcal(d: Double) = d.roundToInt().toString()
private fun g(d: Double) = d.roundToInt().toString()

@Composable
private fun unitLabel(code: String): String = stringResource(
    when (code) {
        "KPL_S" -> R.string.unit_kpl_s
        "KPL_M" -> R.string.unit_kpl_m
        "KPL_L" -> R.string.unit_kpl_l
        "KPL_VALM" -> R.string.unit_kpl_valm
        "PORTS" -> R.string.unit_ports
        "PORTM" -> R.string.unit_portm
        "PORTL" -> R.string.unit_portl
        "DL" -> R.string.unit_dl
        "RKL" -> R.string.unit_rkl
        "TL" -> R.string.unit_tl
        else -> R.string.unit_portm
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    nvm: NutritionViewModel,
    day: LocalDate,
    onDay: (LocalDate) -> Unit,
    snackbar: SnackbarHostState
) {
    LaunchedEffect(day) { nvm.selectedDay.value = day }

    val profile by nvm.profile.collectAsStateWithLifecycle()
    val targets by nvm.targets.collectAsStateWithLifecycle()
    val meals by nvm.meals.collectAsStateWithLifecycle()
    val total by nvm.dayTotal.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showProfile by remember { mutableStateOf(false) }
    var pickerMeal by remember { mutableStateOf<Int?>(null) }
    var editing by remember { mutableStateOf<FoodEntry?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NutritionHeader(
                day = day,
                onPrev = { onDay(day.minusDays(1)) },
                onNext = { onDay(day.plusDays(1)) },
                onToday = { onDay(LocalDate.now()) },
                onProfile = { showProfile = true }
            )
        }
        val t = targets
        if (t == null) {
            item { SetupCard(onClick = { showProfile = true }) }
        } else {
            item { DaySummaryCard(total, t) }
        }
        if (meals.all { it.entries.isEmpty() }) {
            item {
                OutlinedButton(
                    onClick = {
                        nvm.copyPreviousDay { n ->
                            scope.launch {
                                snackbar.showSnackbar(
                                    if (n == 0) context.getString(R.string.nothing_to_copy)
                                    else context.getString(R.string.copied_entries, n)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.ContentCopy, null, tint = Cyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.copy_yesterday), color = Color.White)
                }
            }
        }
        items(meals, key = { it.index }) { m ->
            MealCard(
                meal = m,
                target = t?.perMeal,
                onAdd = { pickerMeal = m.index },
                onEdit = { editing = it }
            )
        }
    }

    if (showProfile) {
        ProfileSheet(
            initial = profile,
            onDismiss = { showProfile = false },
            onSave = {
                nvm.saveProfile(it)
                showProfile = false
            }
        )
    }

    pickerMeal?.let { meal ->
        FoodPickerSheet(
            nvm = nvm,
            meal = meal,
            onDismiss = { pickerMeal = null },
            onAdded = { name ->
                pickerMeal = null
                scope.launch { snackbar.showSnackbar(context.getString(R.string.food_added, name)) }
            }
        )
    }

    editing?.let { entry ->
        AmountDialog(
            title = entry.name,
            initialGrams = entry.grams,
            per100 = Macros(entry.kcal100, entry.protein100, entry.carbs100, entry.fat100),
            units = remember(entry.fineliId) { nvm.unitsFor(entry.fineliId) },
            confirmLabel = stringResource(R.string.save),
            onDismiss = { editing = null },
            onConfirm = { grams ->
                nvm.updateEntry(entry.copy(grams = grams))
                editing = null
            },
            onDelete = {
                nvm.deleteEntry(entry)
                editing = null
                scope.launch {
                    val r = snackbar.showSnackbar(
                        message = context.getString(R.string.food_deleted),
                        actionLabel = context.getString(R.string.undo),
                        duration = SnackbarDuration.Short
                    )
                    if (r == SnackbarResult.ActionPerformed) nvm.restoreEntry(entry)
                }
            }
        )
    }
}

// ---------- Otsikko ja yhteenveto ----------

@Composable
private fun NutritionHeader(
    day: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onProfile: () -> Unit
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
            Text(
                day.longLabel(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
        CircleButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.prev_day), onPrev)
        Spacer(Modifier.width(6.dp))
        CircleButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_day), onNext)
        Spacer(Modifier.width(6.dp))
        CircleButton(Icons.Filled.Tune, stringResource(R.string.nutrition_goals), onProfile)
    }
}

@Composable
private fun CircleButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
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
private fun SetupCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBg)
            .border(1.dp, AccentBrush, shape)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Restaurant, null, tint = Lime, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.setup_title), fontWeight = FontWeight.Bold, color = Color.White)
        Text(stringResource(R.string.setup_body), color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black)
        ) { Text(stringResource(R.string.set_goals), fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun DaySummaryCard(eaten: Macros, t: Targets) {
    val shape = RoundedCornerShape(22.dp)
    val left = t.daily.kcal - eaten.kcal
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBg)
            .border(1.dp, AccentBrush, shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${kcal(eaten.kcal)} / ${kcal(t.daily.kcal)}",
                    style = MaterialTheme.typography.headlineSmall.copy(brush = AccentBrush),
                    fontWeight = FontWeight.ExtraBold
                )
                Text(stringResource(R.string.kcal_eaten_of_target), color = Muted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    kcal(kotlin.math.abs(left)),
                    color = if (left >= 0) Color.White else Over,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    stringResource(if (left >= 0) R.string.kcal_left else R.string.kcal_over),
                    color = Muted, fontSize = 12.sp
                )
            }
        }
        Bar(eaten.kcal, t.daily.kcal, Lime)
        MacroRow(stringResource(R.string.protein), eaten.protein, t.daily.protein, ProteinColor)
        MacroRow(stringResource(R.string.carbs), eaten.carbs, t.daily.carbs, CarbColor)
        MacroRow(stringResource(R.string.fat), eaten.fat, t.daily.fat, FatColor)
    }
}

@Composable
private fun MacroRow(label: String, eaten: Double, target: Double, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(
                "${g(eaten)} / ${g(target)} g",
                color = if (target > 0 && eaten > target * 1.1) Over else Muted,
                fontSize = 14.sp
            )
        }
        Bar(eaten, target, color)
    }
}

@Composable
private fun Bar(value: Double, target: Double, color: Color) {
    val frac = if (target <= 0) 0f else (value / target).toFloat().coerceIn(0f, 1f)
    val over = target > 0 && value > target * 1.1
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(CardBg2)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(frac)
                .clip(RoundedCornerShape(4.dp))
                .background(if (over) Over else color)
        )
    }
}

// ---------- Ateriat ----------

@Composable
private fun MealCard(meal: MealGroup, target: Macros?, onAdd: () -> Unit, onEdit: (FoodEntry) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.meal_n, meal.index + 1),
                    color = Color.White, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                if (target != null) {
                    Text(
                        stringResource(R.string.meal_target, kcal(target.kcal), g(target.protein), g(target.carbs), g(target.fat)),
                        color = Muted, fontSize = 12.sp
                    )
                }
            }
            if (meal.entries.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("${kcal(meal.total.kcal)} kcal", color = Lime, fontWeight = FontWeight.Bold)
                    MacroLine(meal.total, 12)
                }
            }
        }
        if (meal.entries.isNotEmpty()) HorizontalDivider(color = CardBg2)
        meal.entries.forEach { e ->
            val m = e.macros()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onEdit(e) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(e.name, color = Color.White, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${num(e.grams)} g", color = Muted, fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("${kcal(m.kcal)} kcal", color = Color.White, fontSize = 14.sp)
                    MacroLine(m, 12)
                }
            }
        }
        TextButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, null, tint = Lime, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.add_food), color = Lime, fontWeight = FontWeight.Bold)
        }
    }
}

/** "P 30 · H 50 · R 10" makrojen väreillä. */
@Composable
private fun MacroLine(m: Macros, size: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("${stringResource(R.string.protein_short)} ${g(m.protein)}", color = ProteinColor, fontSize = size.sp)
        Text("${stringResource(R.string.carbs_short)} ${g(m.carbs)}", color = CarbColor, fontSize = size.sp)
        Text("${stringResource(R.string.fat_short)} ${g(m.fat)}", color = FatColor, fontSize = size.sp)
    }
}

// ---------- Ruokahaku ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodPickerSheet(
    nvm: NutritionViewModel,
    meal: Int,
    onDismiss: () -> Unit,
    onAdded: (String) -> Unit
) {
    val finnish = appLocale().language == "fi"
    val recent by nvm.recentFoods.collectAsStateWithLifecycle()
    val custom by nvm.customFoods.collectAsStateWithLifecycle()
    val hasFineli by nvm.hasFineli.collectAsStateWithLifecycle()
    val fineliInfo by nvm.fineliInfo.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<FoodItem?>(null) }
    var customDialog by remember { mutableStateOf<CustomFood?>(null) }
    val results = remember(query, custom, hasFineli) { nvm.search(query, finnish) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Bg
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.add_food_to_meal, meal + 1),
                color = Color.White, fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_food_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = Muted) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (query.isBlank()) {
                    if (recent.isNotEmpty()) {
                        item { SectionLabel(stringResource(R.string.recent_foods)) }
                        items(recent, key = { "r" + it.name }) { FoodRow(it) { chosen = it } }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel(stringResource(R.string.my_foods), Modifier.weight(1f))
                            TextButton(onClick = { customDialog = CustomFood(name = "", kcal100 = 0.0, protein100 = 0.0, carbs100 = 0.0, fat100 = 0.0) }) {
                                Icon(Icons.Filled.Add, null, tint = Lime, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.new_food), color = Lime)
                            }
                        }
                    }
                    if (custom.isEmpty()) {
                        item { Text(stringResource(R.string.my_foods_empty), color = Muted, fontSize = 13.sp) }
                    }
                    items(custom, key = { "c" + it.id }) { c ->
                        FoodRow(FoodItem(c.name, c.kcal100, c.protein100, c.carbs100, c.fat100, custom = c),
                            onLongClick = { customDialog = c }) {
                            chosen = FoodItem(c.name, c.kcal100, c.protein100, c.carbs100, c.fat100, custom = c)
                        }
                    }
                    if (!hasFineli) {
                        item { Text(stringResource(R.string.fineli_missing), color = Muted, fontSize = 13.sp) }
                    }
                } else {
                    if (results.isEmpty()) {
                        item {
                            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.no_results), color = Muted)
                                TextButton(onClick = {
                                    customDialog = CustomFood(name = query.trim().replaceFirstChar { it.uppercase() }, kcal100 = 0.0, protein100 = 0.0, carbs100 = 0.0, fat100 = 0.0)
                                }) { Text(stringResource(R.string.create_food_named, query.trim()), color = Lime) }
                            }
                        }
                    }
                    items(results, key = { (it.fineliId?.toString() ?: ("c" + it.custom?.id)) }) { item ->
                        FoodRow(item, onLongClick = item.custom?.let { c -> { customDialog = c } }) { chosen = item }
                    }
                }
                if (hasFineli) {
                    item {
                        Text(
                            stringResource(R.string.fineli_attribution, fineliInfo ?: "Fineli"),
                            color = Muted, fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }

    chosen?.let { item ->
        AmountDialog(
            title = item.name,
            initialGrams = item.units.firstOrNull { it.first == "PORTM" }?.second ?: 100.0,
            per100 = Macros(item.kcal100, item.protein100, item.carbs100, item.fat100),
            units = item.units,
            confirmLabel = stringResource(R.string.add),
            onDismiss = { chosen = null },
            onConfirm = { grams ->
                nvm.addEntry(item, grams, meal)
                chosen = null
                onAdded(item.name)
            },
            onDelete = null
        )
    }

    customDialog?.let { food ->
        CustomFoodDialog(
            initial = food,
            onDismiss = { customDialog = null },
            onSave = {
                nvm.saveCustomFood(it)
                customDialog = null
            },
            onDelete = if (food.id != 0L) ({
                nvm.deleteCustomFood(food)
                customDialog = null
            }) else null
        )
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = modifier.padding(top = 6.dp)
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FoodRow(item: FoodItem, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .then(
                if (onLongClick != null)
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                else Modifier.clickable(onClick = onClick)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.name, color = Color.White, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.kcal_per_100, kcal(item.kcal100)), color = Muted, fontSize = 12.sp)
                MacroLine(Macros(item.kcal100, item.protein100, item.carbs100, item.fat100), 12)
            }
        }
        if (item.custom != null) {
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.mine_tag), color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------- Määrä ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmountDialog(
    title: String,
    initialGrams: Double,
    per100: Macros,
    units: List<Pair<String, Double>>,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    onDelete: (() -> Unit)?
) {
    var text by remember { mutableStateOf(editable(initialGrams)) }
    val grams = parseDecimal(text)
    val m = grams?.let { macrosFor(it, per100.kcal, per100.protein, per100.carbs, per100.fat) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text(title, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.amount_grams)) },
                    suffix = { Text("g") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (units.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        units.forEach { (code, g) ->
                            FilterChip(
                                selected = grams == g,
                                onClick = { text = editable(g) },
                                label = { Text("${unitLabel(code)} · ${num(g)} g", fontSize = 12.sp) },
                                colors = chipColors()
                            )
                        }
                    }
                }
                if (m != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${kcal(m.kcal)} kcal", color = Lime, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        MacroLine(m, 14)
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, null, tint = Over, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.delete), color = Over)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { grams?.let(onConfirm) },
                enabled = grams != null && grams > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black)
            ) { Text(confirmLabel, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Lime,
    selectedLabelColor = Color.Black,
    labelColor = Color.White
)

// ---------- Oma ruoka ----------

@Composable
private fun CustomFoodDialog(
    initial: CustomFood,
    onDismiss: () -> Unit,
    onSave: (CustomFood) -> Unit,
    onDelete: (() -> Unit)?
) {
    fun init(d: Double) = if (initial.id == 0L && d == 0.0) "" else editable(d)
    var name by remember { mutableStateOf(initial.name) }
    var kcalText by remember { mutableStateOf(init(initial.kcal100)) }
    var p by remember { mutableStateOf(init(initial.protein100)) }
    var c by remember { mutableStateOf(init(initial.carbs100)) }
    var f by remember { mutableStateOf(init(initial.fat100)) }

    val pv = parseDecimal(p) ?: 0.0
    val cv = parseDecimal(c) ?: 0.0
    val fv = parseDecimal(f) ?: 0.0
    val fromMacros = pv * KCAL_PROTEIN + cv * KCAL_CARB + fv * KCAL_FAT
    val kv = parseDecimal(kcalText) ?: fromMacros

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text(stringResource(if (initial.id == 0L) R.string.new_food else R.string.edit_food), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.food_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.per_100g_hint), color = Muted, fontSize = 12.sp)
                NumField(stringResource(R.string.protein), p, "g") { p = it }
                NumField(stringResource(R.string.carbs), c, "g") { c = it }
                NumField(stringResource(R.string.fat), f, "g") { f = it }
                OutlinedTextField(
                    value = kcalText, onValueChange = { kcalText = it },
                    label = { Text(stringResource(R.string.energy)) },
                    placeholder = { Text(kcal(fromMacros)) },
                    suffix = { Text("kcal") },
                    supportingText = { Text(stringResource(R.string.energy_auto_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, null, tint = Over, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.delete), color = Over)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(initial.copy(name = name, kcal100 = kv, protein100 = pv, carbs100 = cv, fat100 = fv)) },
                enabled = name.isNotBlank() && kv > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black)
            ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun NumField(label: String, value: String, suffix: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

// ---------- Tavoitteet ----------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProfileSheet(
    initial: NutritionProfile?,
    onDismiss: () -> Unit,
    onSave: (NutritionProfile) -> Unit
) {
    val unit = LocalUnit.current
    var sex by remember { mutableStateOf(initial?.sex ?: Sex.MALE) }
    var age by remember { mutableStateOf(initial?.age?.toString() ?: "") }
    var height by remember { mutableStateOf(initial?.heightCm?.let(::editable) ?: "") }
    var weight by remember { mutableStateOf(initial?.weightKg?.let { editable(unit.fromKg(it)) } ?: "") }
    var activity by remember { mutableStateOf(initial?.activity ?: Activity.MODERATE) }
    var goal by remember { mutableStateOf(initial?.goal ?: Goal.MAINTAIN) }
    var meals by remember { mutableIntStateOf(initial?.meals ?: 4) }
    var manual by remember { mutableStateOf(initial?.manualKcal != null) }
    var mKcal by remember { mutableStateOf(initial?.manualKcal?.toString() ?: "") }
    var mProtein by remember { mutableStateOf(initial?.manualProtein?.toString() ?: "") }
    var mFat by remember { mutableStateOf(initial?.manualFat?.toString() ?: "") }

    val ageV = age.trim().toIntOrNull()
    val heightV = parseDecimal(height)
    val weightV = parseDecimal(weight)?.let(unit::toKg)
    val ageOk = ageV != null && ageV in 18..100
    val heightOk = heightV != null && heightV in 120.0..230.0
    val weightOk = weightV != null && weightV in 35.0..250.0

    val draft: NutritionProfile? = if (ageOk && heightOk && weightOk) {
        val auto = targets(NutritionProfile(sex, ageV!!, heightV!!, weightV!!, activity, goal, meals))
        NutritionProfile(
            sex, ageV, heightV, weightV, activity, goal, meals,
            manualKcal = if (manual) mKcal.trim().toIntOrNull() ?: auto.daily.kcal.roundToInt() else null,
            manualProtein = if (manual) mProtein.trim().toIntOrNull() ?: auto.daily.protein.roundToInt() else null,
            manualFat = if (manual) mFat.trim().toIntOrNull() ?: auto.daily.fat.roundToInt() else null
        )
    } else null
    val result = draft?.let(::targets)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Bg
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.nutrition_goals),
                color = Color.White, fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge
            )

            FieldLabel(stringResource(R.string.sex))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(sex == Sex.MALE, { sex = Sex.MALE }, { Text(stringResource(R.string.male)) }, colors = chipColors())
                FilterChip(sex == Sex.FEMALE, { sex = Sex.FEMALE }, { Text(stringResource(R.string.female)) }, colors = chipColors())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = age, onValueChange = { age = it },
                    label = { Text(stringResource(R.string.age)) },
                    isError = age.isNotBlank() && !ageOk,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                NumField(stringResource(R.string.height), height, "cm", Modifier.weight(1f)) { height = it }
                NumField(stringResource(R.string.weight), weight, unit.label, Modifier.weight(1f)) { weight = it }
            }
            if (age.isNotBlank() && !ageOk) {
                Text(stringResource(R.string.age_limit), color = Over, fontSize = 12.sp)
            }

            FieldLabel(stringResource(R.string.activity_level))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Activity.entries.forEach { a ->
                    val sel = a == activity
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sel) CardBg2 else CardBg)
                            .then(if (sel) Modifier.border(1.dp, Lime, RoundedCornerShape(14.dp)) else Modifier)
                            .clickable { activity = a }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(a.labelRes), color = if (sel) Lime else Color.White, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(a.descRes), color = Muted, fontSize = 12.sp)
                        }
                        Text("× ${num(a.factor)}", color = Muted, fontSize = 12.sp)
                    }
                }
            }

            FieldLabel(stringResource(R.string.goal))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Goal.entries.forEach { gl ->
                    FilterChip(goal == gl, { goal = gl }, { Text(stringResource(gl.labelRes)) }, colors = chipColors())
                }
            }

            FieldLabel(stringResource(R.string.meals_per_day))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { if (meals > 1) meals-- }, shape = CircleShape) { Text("−", color = Color.White) }
                Text(
                    meals.toString(), color = Color.White, fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                OutlinedButton(onClick = { if (meals < 8) meals++ }, shape = CircleShape) { Text("+", color = Color.White) }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.manual_targets), color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.manual_targets_desc), color = Muted, fontSize = 12.sp)
                }
                Switch(
                    checked = manual,
                    onCheckedChange = { on ->
                        manual = on
                        // Esitäytetään lasketuilla arvoilla
                        if (on && ageOk && heightOk && weightOk) {
                            val auto = targets(NutritionProfile(sex, ageV!!, heightV!!, weightV!!, activity, goal, meals))
                            if (mKcal.isBlank()) mKcal = auto.daily.kcal.roundToInt().toString()
                            if (mProtein.isBlank()) mProtein = auto.daily.protein.roundToInt().toString()
                            if (mFat.isBlank()) mFat = auto.daily.fat.roundToInt().toString()
                        }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Lime, checkedThumbColor = Color.Black)
                )
            }
            if (manual) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumField(stringResource(R.string.energy), mKcal, "kcal", Modifier.weight(1.2f)) { mKcal = it }
                    NumField(stringResource(R.string.protein), mProtein, "g", Modifier.weight(1f)) { mProtein = it }
                    NumField(stringResource(R.string.fat), mFat, "g", Modifier.weight(1f)) { mFat = it }
                }
                Text(stringResource(R.string.carbs_remainder_hint), color = Muted, fontSize = 12.sp)
            }

            if (result != null) ResultCard(result, meals)

            Text(stringResource(R.string.nutrition_disclaimer), color = Muted, fontSize = 11.sp)

            Button(
                onClick = { draft?.let(onSave) },
                enabled = draft != null,
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ResultCard(t: Targets, meals: Int) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBg)
            .border(1.dp, AccentBrush, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row {
            BigStat(stringResource(R.string.bmr_short), kcal(t.bmr), Modifier.weight(1f))
            BigStat(stringResource(R.string.tdee_short), kcal(t.tdee), Modifier.weight(1f))
            BigStat(stringResource(R.string.target_kcal), kcal(t.daily.kcal), Modifier.weight(1f))
        }
        HorizontalDivider(color = CardBg2)
        ResultLine(stringResource(R.string.protein), t.daily.protein, t.daily.protein * KCAL_PROTEIN / t.daily.kcal, ProteinColor)
        ResultLine(stringResource(R.string.carbs), t.daily.carbs, t.daily.carbs * KCAL_CARB / t.daily.kcal, CarbColor)
        ResultLine(stringResource(R.string.fat), t.daily.fat, t.daily.fat * KCAL_FAT / t.daily.kcal, FatColor)
        HorizontalDivider(color = CardBg2)
        Text(
            stringResource(R.string.per_meal_summary, meals, kcal(t.perMeal.kcal), g(t.perMeal.protein), g(t.perMeal.carbs), g(t.perMeal.fat)),
            color = Color.White, fontSize = 14.sp
        )
        if (t.belowBmr) Warning(stringResource(R.string.warn_below_bmr))
        if (t.macrosExceedKcal) Warning(stringResource(R.string.warn_macros_exceed))
    }
}

@Composable
private fun ResultLine(label: String, grams: Double, share: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Text("${g(grams)} g", color = Color.White, fontWeight = FontWeight.Bold)
        Text("  ${(share * 100).roundToInt()} %", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun Warning(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Warning, null, tint = FatColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = FatColor, fontSize = 12.sp)
    }
}
