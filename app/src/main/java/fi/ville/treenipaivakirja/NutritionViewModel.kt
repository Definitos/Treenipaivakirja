package fi.ville.treenipaivakirja

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fi.ville.treenipaivakirja.data.AppDatabase
import fi.ville.treenipaivakirja.data.CustomFood
import fi.ville.treenipaivakirja.data.FoodEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

/** Haettava ruoka (Fineli, oma tai viimeksi syöty). Arvot per 100 g. */
data class FoodItem(
    val name: String,
    val kcal100: Double,
    val protein100: Double,
    val carbs100: Double,
    val fat100: Double,
    val fineliId: Int? = null,
    val custom: CustomFood? = null,
    /** Finelin ruokamitat: yksikkökoodi (esim. "KPL_M", "DL") -> grammat. */
    val units: List<Pair<String, Double>> = emptyList()
)

/** Finelin ruoka asset-tiedostosta: molemmat nimet säilytetään, näytettävä valitaan kielen mukaan. */
private data class FineliRow(
    val id: Int, val fi: String, val en: String,
    val kcal: Double, val p: Double, val c: Double, val f: Double,
    val units: List<Pair<String, Double>>
) {
    fun item(finnish: Boolean) = FoodItem(if (finnish || en.isBlank()) fi else en, kcal, p, c, f, fineliId = id, units = units)
}

private fun parseUnits(s: String): List<Pair<String, Double>> =
    s.split(',').mapNotNull { part ->
        val (code, grams) = part.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
        grams.toDoubleOrNull()?.takeIf { it > 0 }?.let { code to it }
    }

data class MealGroup(val index: Int, val entries: List<FoodEntry>, val total: Macros)

fun FoodEntry.macros(): Macros = macrosFor(grams, kcal100, protein100, carbs100, fat100)

/** Hakua varten: pienet kirjaimet, yhtenäinen Unicode-muoto (ä/ö säilyvät omina kirjaiminaan). */
private fun norm(s: String): String =
    Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFC).trim()

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).dao()
    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    // ---------- Profiili ----------
    private val prefs = app.getSharedPreferences("nutrition", Context.MODE_PRIVATE)

    val profile = MutableStateFlow(loadProfile())

    private fun loadProfile(): NutritionProfile? {
        if (!prefs.contains("age")) return null
        fun intOrNull(key: String) = if (prefs.contains(key)) prefs.getInt(key, 0) else null
        return runCatching {
            NutritionProfile(
                sex = Sex.valueOf(prefs.getString("sex", Sex.MALE.name)!!),
                age = prefs.getInt("age", 30),
                heightCm = prefs.getFloat("heightCm", 175f).toDouble(),
                weightKg = prefs.getFloat("weightKg", 75f).toDouble(),
                activity = Activity.valueOf(prefs.getString("activity", Activity.MODERATE.name)!!),
                goal = Goal.valueOf(prefs.getString("goal", Goal.MAINTAIN.name)!!),
                meals = prefs.getInt("meals", 4),
                manualKcal = intOrNull("manualKcal"),
                manualProtein = intOrNull("manualProtein"),
                manualFat = intOrNull("manualFat")
            )
        }.getOrNull()
    }

    fun saveProfile(p: NutritionProfile) {
        profile.value = p
        prefs.edit().apply {
            putString("sex", p.sex.name)
            putInt("age", p.age)
            putFloat("heightCm", p.heightCm.toFloat())
            putFloat("weightKg", p.weightKg.toFloat())
            putString("activity", p.activity.name)
            putString("goal", p.goal.name)
            putInt("meals", p.meals)
            fun opt(key: String, v: Int?) { if (v == null) remove(key) else putInt(key, v) }
            opt("manualKcal", p.manualKcal)
            opt("manualProtein", p.manualProtein)
            opt("manualFat", p.manualFat)
        }.apply()
    }

    val targets: StateFlow<Targets?> = profile.map { it?.let(::targets) }.state(profile.value?.let(::targets))

    // ---------- Päivä ----------
    val selectedDay = MutableStateFlow(LocalDate.now())

    val entries: StateFlow<List<FoodEntry>> =
        selectedDay.flatMapLatest { dao.foodEntries(it.toEpochDay()) }.state(emptyList())

    val foodDays: StateFlow<Set<Long>> = dao.foodDays().map { it.toSet() }.state(emptySet())

    /** Ateriat: vähintään profiilin ateriamäärä, lisäksi kaikki ateriat joille on kirjauksia. */
    val meals: StateFlow<List<MealGroup>> =
        combine(entries, profile) { list, p ->
            val byMeal = list.groupBy { it.meal }
            val count = maxOf(p?.meals ?: 1, (byMeal.keys.maxOrNull() ?: -1) + 1)
            (0 until count).map { i ->
                val e = byMeal[i].orEmpty()
                MealGroup(i, e, e.fold(Macros.ZERO) { acc, it -> acc + it.macros() })
            }
        }.state(emptyList())

    val dayTotal: StateFlow<Macros> =
        entries.map { l -> l.fold(Macros.ZERO) { acc, it -> acc + it.macros() } }.state(Macros.ZERO)

    // ---------- Ruokahaku ----------
    private val fineli = MutableStateFlow<List<FineliRow>>(emptyList())
    val fineliInfo = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { loadFineli() }
    }

    private suspend fun loadFineli() = withContext(Dispatchers.IO) {
        val ctx = getApplication<Application>()
        val rows = runCatching {
            ctx.assets.open("fineli.csv").bufferedReader().useLines { lines ->
                lines.mapNotNull { line ->
                    val c = line.split(';')
                    if (c.size < 7) null
                    else FineliRow(
                        c[0].toIntOrNull() ?: return@mapNotNull null, c[1], c[2],
                        c[3].toDoubleOrNull() ?: 0.0, c[4].toDoubleOrNull() ?: 0.0,
                        c[5].toDoubleOrNull() ?: 0.0, c[6].toDoubleOrNull() ?: 0.0,
                        parseUnits(c.getOrElse(7) { "" })
                    )
                }.toList()
            }
        }.getOrDefault(emptyList())
        fineli.value = rows
        fineliInfo.value = runCatching {
            ctx.assets.open("fineli_info.txt").bufferedReader().readLines().firstOrNull()
        }.getOrNull()
    }

    val hasFineli: StateFlow<Boolean> = fineli.map { it.isNotEmpty() }.state(false)

    /** Ateriaehdotukset tavoitteelle (raskas laskenta, kutsutaan taustasäikeessä). */
    suspend fun suggestions(target: Macros, kind: MealKind?): List<Suggestion> = withContext(Dispatchers.Default) {
        val map = fineli.value.associate { it.id to Macros(it.kcal, it.p, it.c, it.f) }
        if (map.isEmpty()) emptyList() else suggestMeals(target, map, kind)
    }

    /** Lisää ehdotuksen kaikki osat ateriaan omina kirjauksinaan. */
    fun addSuggestion(s: Suggestion, meal: Int) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            s.parts.forEachIndexed { i, p ->
                dao.insertFoodEntry(
                    FoodEntry(
                        epochDay = selectedDay.value.toEpochDay(), meal = meal,
                        name = app.getString(p.part.labelRes), grams = p.grams,
                        kcal100 = p.per100.kcal, protein100 = p.per100.protein,
                        carbs100 = p.per100.carbs, fat100 = p.per100.fat,
                        fineliId = p.part.fineliId, createdAt = now + i
                    )
                )
            }
        }
    }

    val customFoods: StateFlow<List<CustomFood>> = dao.customFoods().state(emptyList())

    val recentFoods: StateFlow<List<FoodItem>> = combine(dao.recentFoods(), fineli) { list, rows ->
        val byId = rows.associateBy { it.id }
        list.map {
            FoodItem(it.name, it.kcal100, it.protein100, it.carbs100, it.fat100, it.fineliId,
                units = it.fineliId?.let { id -> byId[id]?.units }.orEmpty())
        }
    }.state(emptyList())

    /** Kirjauksen ruokamitat muokkausdialogia varten. */
    fun unitsFor(fineliId: Int?): List<Pair<String, Double>> =
        fineliId?.let { id -> fineli.value.firstOrNull { it.id == id }?.units }.orEmpty()

    /**
     * Haku: kaikkien hakusanojen pitää löytyä nimestä. Järjestys: omat ruoat, sitten nimet jotka alkavat
     * hakusanalla, sitten lyhyemmät nimet (yleensä perusraaka-aineet ennen valmisruokia).
     */
    fun search(query: String, finnish: Boolean): List<FoodItem> {
        val words = norm(query).split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        val first = words.first()
        fun matches(name: String) = norm(name).let { n -> words.all { it in n } }
        fun rank(name: String) = if (norm(name).startsWith(first)) 0 else 1

        val custom = customFoods.value.filter { matches(it.name) }
            .map { FoodItem(it.name, it.kcal100, it.protein100, it.carbs100, it.fat100, custom = it) }
        val fromFineli = fineli.value.asSequence()
            .filter { matches(it.fi) || matches(it.en) }
            .map { it.item(finnish) }
            .sortedWith(compareBy({ rank(it.name) }, { it.name.length }))
            .take(60)
            .toList()
        return custom + fromFineli
    }

    // ---------- Kirjaus ----------
    fun addEntry(item: FoodItem, grams: Double, meal: Int) {
        if (grams <= 0) return
        viewModelScope.launch {
            dao.insertFoodEntry(
                FoodEntry(
                    epochDay = selectedDay.value.toEpochDay(), meal = meal, name = item.name.trim(), grams = grams,
                    kcal100 = item.kcal100, protein100 = item.protein100, carbs100 = item.carbs100, fat100 = item.fat100,
                    fineliId = item.fineliId
                )
            )
        }
    }

    fun updateEntry(entry: FoodEntry) = viewModelScope.launch { dao.updateFoodEntry(entry) }
    fun deleteEntry(entry: FoodEntry) = viewModelScope.launch { dao.deleteFoodEntry(entry) }
    fun restoreEntry(entry: FoodEntry) = viewModelScope.launch { dao.insertFoodEntry(entry) }

    /** Kopioi edellisen päivän kirjaukset valitulle päivälle. Palauttaa kopioitujen määrän. */
    fun copyPreviousDay(onDone: (Int) -> Unit) = viewModelScope.launch {
        val day = selectedDay.value.toEpochDay()
        val prev = withContext(Dispatchers.IO) { dao.foodEntriesOnce(day - 1) }
        val now = System.currentTimeMillis()
        prev.forEachIndexed { i, e ->
            dao.insertFoodEntry(e.copy(id = 0, epochDay = day, createdAt = now + i))
        }
        onDone(prev.size)
    }

    fun saveCustomFood(food: CustomFood) = viewModelScope.launch {
        if (food.name.isNotBlank()) dao.upsertCustomFood(food.copy(name = food.name.trim()))
    }

    fun deleteCustomFood(food: CustomFood) = viewModelScope.launch { dao.deleteCustomFood(food) }
}
