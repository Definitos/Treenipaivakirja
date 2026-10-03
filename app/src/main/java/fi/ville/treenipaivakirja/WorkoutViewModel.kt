package fi.ville.treenipaivakirja

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fi.ville.treenipaivakirja.data.AppDatabase
import fi.ville.treenipaivakirja.data.Exercise
import fi.ville.treenipaivakirja.data.WorkoutSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

data class ExerciseGroup(val exercise: Exercise, val sets: List<WorkoutSet>)

/** Yhden liikkeen yhden päivän yhteenveto historiakäyrää varten. */
data class DayStat(
    val epochDay: Long,
    val maxWeight: Double,
    val est1rm: Double,
    val volume: Double,
    val sets: List<WorkoutSet>
)

enum class Metric(val label: String) {
    MAX("Maks. paino"),
    ONE_RM("Arvio 1RM"),
    VOLUME("Volyymi")
}

fun DayStat.value(m: Metric): Double = when (m) {
    Metric.MAX -> maxWeight
    Metric.ONE_RM -> est1rm
    Metric.VOLUME -> volume
}

/** Epleyn kaava: arvioitu yhden toiston maksimi. */
fun epley(weight: Double, reps: Int): Double =
    if (reps <= 1) weight else weight * (1 + reps / 30.0)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).dao()
    private fun <T> kotlinx.coroutines.flow.Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    // ---------- Päivänäkymä ----------
    val selectedDay = MutableStateFlow(LocalDate.now())

    val exercises: StateFlow<List<Exercise>> = dao.exercises().state(emptyList())

    val trainingDays: StateFlow<Set<Long>> =
        dao.trainingDays().map { it.toSet() }.state(emptySet())

    val dayGroups: StateFlow<List<ExerciseGroup>> =
        combine(
            selectedDay.flatMapLatest { dao.setsForDay(it.toEpochDay()) },
            dao.exercises()
        ) { sets, exList ->
            val byId = exList.associateBy { it.id }
            sets.groupBy { it.exerciseId }          // säilyttää lisäysjärjestyksen
                .mapNotNull { (id, s) -> byId[id]?.let { ExerciseGroup(it, s) } }
        }.state(emptyList())

    fun addSets(name: String, reps: Int, weight: Double, count: Int) {
        val clean = name.trim()
        if (clean.isEmpty() || reps <= 0 || count <= 0) return
        viewModelScope.launch {
            val exId = dao.findExercise(clean)?.id ?: dao.insertExercise(Exercise(name = clean))
            val day = selectedDay.value.toEpochDay()
            val now = System.currentTimeMillis()
            dao.insertSets(List(count) {
                WorkoutSet(exerciseId = exId, epochDay = day, reps = reps, weight = weight, createdAt = now + it)
            })
        }
    }

    fun deleteSet(set: WorkoutSet) = viewModelScope.launch { dao.deleteSet(set) }

    fun restoreSet(set: WorkoutSet) = viewModelScope.launch { dao.insertSets(listOf(set)) }

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
