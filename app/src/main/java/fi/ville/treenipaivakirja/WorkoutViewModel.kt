package fi.ville.treenipaivakirja

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import fi.ville.treenipaivakirja.ui.WeightUnit
import java.util.Locale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import fi.ville.treenipaivakirja.data.AppDatabase
import fi.ville.treenipaivakirja.data.DayExercise
import fi.ville.treenipaivakirja.data.Exercise
import fi.ville.treenipaivakirja.data.Template
import fi.ville.treenipaivakirja.data.TemplateExercise
import fi.ville.treenipaivakirja.data.WorkoutSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Päivän yksi liike: kirjatut sarjat, tavoite ja edellisen kerran sarjat. */
data class ExerciseGroup(
    val exercise: Exercise,
    val sets: List<WorkoutSet>,
    val targetSets: Int,
    val targetReps: Int,
    val targetRepsMax: Int,
    val toFailure: Boolean,
    val previous: List<WorkoutSet>
)

/** Yhden liikkeen yhden päivän yhteenveto historiakäyrää varten. */
data class DayStat(
    val epochDay: Long,
    val maxWeight: Double,
    val est1rm: Double,
    val volume: Double,
    val sets: List<WorkoutSet>
)

data class TemplateItem(val exerciseName: String, val targetSets: Int, val targetReps: Int, val targetRepsMax: Int, val toFailure: Boolean)
data class TemplateWithItems(val template: Template, val items: List<TemplateItem>)

/** Ohjelmaeditorin muokattava rivi. key = vakaa tunniste listaa varten. */
data class DraftItem(val key: Long, val name: String, val sets: Int, val reps: Int, val repsMax: Int = reps, val toFailure: Boolean = false)
data class TemplateDraft(val id: Long?, val name: String, val items: List<DraftItem>)

enum class Metric(@StringRes val labelRes: Int) {
    MAX(R.string.metric_max),
    ONE_RM(R.string.metric_1rm),
    VOLUME(R.string.volume)
}

fun DayStat.value(m: Metric): Double = when (m) {
    Metric.MAX -> maxWeight
    Metric.ONE_RM -> est1rm
    Metric.VOLUME -> volume
}

/** "8–10" tai "8" (jos ei haarukkaa). */
fun repsRange(min: Int, max: Int): String = if (max > min) "$min–$max" else "$min"

/** Epleyn kaava: arvioitu yhden toiston maksimi. */
fun epley(weight: Double, reps: Int): Double =
    if (reps <= 1) weight else weight * (1 + reps / 30.0)

private var keyCounter = 0L
fun newKey(): Long = System.nanoTime() + (keyCounter++)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val dao = db.dao()
    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    // ---------- Asetukset ----------
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Oletus: paunat USA:ssa, Liberiassa ja Myanmarissa, muuten kilot. */
    val unit = MutableStateFlow(
        WeightUnit.entries.firstOrNull { it.name == prefs.getString("unit", null) }
            ?: if (Locale.getDefault().country in setOf("US", "LR", "MM")) WeightUnit.LB else WeightUnit.KG
    )

    fun setUnit(u: WeightUnit) {
        unit.value = u
        prefs.edit().putString("unit", u.name).apply()
    }

    // ---------- Päivitykset ----------
    private val currentRun = runCatching { UpdateChecker.currentRun(app) }.getOrDefault(Int.MAX_VALUE)
    val updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    /** Käynnistyksen ilmoitus suljettu tämän istunnon ajaksi. */
    val updatePromptDismissed = MutableStateFlow(false)

    fun checkUpdates(manual: Boolean = false) {
        if (updateState.value is UpdateState.Checking) return
        if (manual) updatePromptDismissed.value = false
        updateState.value = UpdateState.Checking
        viewModelScope.launch {
            updateState.value = try {
                val info = UpdateChecker.fetchLatest()
                if (info.runNumber > currentRun) UpdateState.Available(info) else UpdateState.UpToDate
            } catch (e: Exception) {
                UpdateState.Error
            }
        }
    }

    init {
        checkUpdates()
    }

    // ---------- Päivänäkymä ----------
    val selectedDay = MutableStateFlow(LocalDate.now())

    val exercises: StateFlow<List<Exercise>> = dao.exercises().state(emptyList())

    val trainingDays: StateFlow<Set<Long>> =
        dao.trainingDays().map { it.toSet() }.state(emptySet())

    val dayGroups: StateFlow<List<ExerciseGroup>> =
        selectedDay.flatMapLatest { date ->
            val day = date.toEpochDay()
            combine(
                dao.dayExercises(day),
                dao.setsForDay(day),
                dao.previousSessions(day),
                dao.exercises()
            ) { planned, sets, prev, exList ->
                val byId = exList.associateBy { it.id }
                val setsBy = sets.groupBy { it.exerciseId }
                val prevBy = prev.groupBy { it.exerciseId }
                val plannedBy = planned.associateBy { it.exerciseId }
                val order = planned.map { it.exerciseId } +
                    setsBy.keys.filter { it !in plannedBy }
                order.mapNotNull { id ->
                    byId[id]?.let {
                        ExerciseGroup(
                            exercise = it,
                            sets = setsBy[id].orEmpty(),
                            targetSets = plannedBy[id]?.targetSets ?: 0,
                            targetReps = plannedBy[id]?.targetReps ?: 0,
                            targetRepsMax = plannedBy[id]?.targetRepsMax ?: 0,
                            toFailure = plannedBy[id]?.toFailure ?: false,
                            previous = prevBy[id].orEmpty()
                        )
                    }
                }
            }
        }.state(emptyList())

    private suspend fun exerciseId(name: String): Long {
        val clean = name.trim()
        return dao.findExercise(clean)?.id ?: dao.insertExercise(Exercise(name = clean))
    }

    fun addSets(name: String, reps: Int, weight: Double, count: Int) {
        if (name.isBlank() || reps <= 0 || count <= 0) return
        viewModelScope.launch {
            db.withTransaction {
                val exId = exerciseId(name)
                val day = selectedDay.value.toEpochDay()
                // Varmista, että liike on päivän listalla (IGNORE jos jo on)
                dao.insertDayExercises(
                    listOf(DayExercise(epochDay = day, exerciseId = exId, position = dao.maxDayPosition(day) + 1, targetSets = 0, targetReps = 0))
                )
                val now = System.currentTimeMillis()
                dao.insertSets(List(count) {
                    WorkoutSet(exerciseId = exId, epochDay = day, reps = reps, weight = weight, createdAt = now + it)
                })
            }
        }
    }

    /** Yksittäisen sarjan kirjaus suoraan liikekortista (paino kiloina, reps 0 = uupumukseen, ei laskettu). */
    fun logSet(exerciseId: Long, reps: Int, weightKg: Double) {
        if (reps < 0 || weightKg < 0) return
        viewModelScope.launch {
            dao.insertSets(listOf(
                WorkoutSet(exerciseId = exerciseId, epochDay = selectedDay.value.toEpochDay(), reps = reps, weight = weightKg)
            ))
        }
    }

    fun updateSet(set: WorkoutSet) = viewModelScope.launch { dao.updateSet(set) }

    fun setNote(exerciseId: Long, note: String) = viewModelScope.launch {
        dao.setNote(exerciseId, note.trim().ifEmpty { null })
    }

    fun deleteSet(set: WorkoutSet) = viewModelScope.launch { dao.deleteSet(set) }

    fun restoreSet(set: WorkoutSet) = viewModelScope.launch { dao.insertSets(listOf(set)) }

    fun removeExerciseFromDay(exerciseId: Long) = viewModelScope.launch {
        val day = selectedDay.value.toEpochDay()
        db.withTransaction {
            dao.deleteSetsFor(day, exerciseId)
            dao.deleteDayExercise(day, exerciseId)
        }
    }

    /** Lisää ohjelman liikkeet valitulle päivälle (jo olemassa olevat ohitetaan). */
    fun applyTemplate(templateId: Long) = viewModelScope.launch {
        val day = selectedDay.value.toEpochDay()
        db.withTransaction {
            var pos = dao.maxDayPosition(day) + 1
            val items = dao.templateExercisesOf(templateId).map {
                DayExercise(epochDay = day, exerciseId = it.exerciseId, position = pos++, targetSets = it.targetSets, targetReps = it.targetReps, targetRepsMax = it.targetRepsMax, toFailure = it.toFailure)
            }
            dao.insertDayExercises(items)
        }
    }

    /** Tallentaa valitun päivän liikkeet uudeksi ohjelmaksi. */
    fun saveDayAsTemplate(name: String) {
        val groups = dayGroups.value
        if (name.isBlank() || groups.isEmpty()) return
        val items = groups.map { g ->
            val reps = when {
                g.targetReps > 0 -> g.targetReps
                g.sets.isNotEmpty() -> g.sets.groupingBy { it.reps }.eachCount().maxBy { it.value }.key
                else -> 10
            }
            DraftItem(newKey(), g.exercise.name, maxOf(g.sets.size, g.targetSets, 1), reps, maxOf(reps, g.targetRepsMax), g.toFailure)
        }
        saveTemplate(TemplateDraft(null, name, items))
    }

    // ---------- Ohjelmat ----------
    val templates: StateFlow<List<TemplateWithItems>> =
        combine(dao.templates(), dao.templateExercises(), dao.exercises()) { ts, tes, exList ->
            val names = exList.associate { it.id to it.name }
            val byTemplate = tes.groupBy { it.templateId }
            ts.map { t ->
                TemplateWithItems(
                    t,
                    byTemplate[t.id].orEmpty().map {
                        TemplateItem(names[it.exerciseId] ?: "?", it.targetSets, it.targetReps, it.targetRepsMax, it.toFailure)
                    }
                )
            }
        }.state(emptyList())

    fun saveTemplate(draft: TemplateDraft) {
        val items = draft.items.filter { it.name.isNotBlank() }
        if (draft.name.isBlank() || items.isEmpty()) return
        viewModelScope.launch {
            db.withTransaction {
                val id = if (draft.id == null) {
                    dao.insertTemplate(Template(name = draft.name.trim()))
                } else {
                    dao.updateTemplate(Template(id = draft.id, name = draft.name.trim()))
                    dao.clearTemplate(draft.id)
                    draft.id
                }
                dao.insertTemplateExercises(items.mapIndexed { i, it ->
                    TemplateExercise(
                        templateId = id,
                        exerciseId = exerciseId(it.name),
                        position = i,
                        targetSets = it.sets,
                        targetReps = it.reps,
                        targetRepsMax = if (it.repsMax > it.reps) it.repsMax else 0,
                        toFailure = it.toFailure
                    )
                })
            }
        }
    }

    fun deleteTemplate(id: Long) = viewModelScope.launch { dao.deleteTemplate(id) }

    // ---------- Historia ----------
    val historyExerciseId = MutableStateFlow<Long?>(null)

    val history: StateFlow<List<DayStat>> =
        historyExerciseId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else dao.setsForExercise(id).map(::toDayStats)
        }.state(emptyList())

    private fun toDayStats(sets: List<WorkoutSet>): List<DayStat> =
        sets.groupBy { it.epochDay }
            .map { (day, s) ->
                DayStat(
                    epochDay = day,
                    maxWeight = s.maxOf { it.weight },
                    est1rm = s.maxOf { epley(it.weight, it.reps) },
                    volume = s.sumOf { it.weight * it.reps },
                    sets = s
                )
            }
            .sortedBy { it.epochDay }
}
