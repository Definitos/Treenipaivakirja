package fi.ville.treenipaivakirja.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import fi.ville.treenipaivakirja.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.ville.treenipaivakirja.DayStat
import fi.ville.treenipaivakirja.Metric
import fi.ville.treenipaivakirja.WorkoutViewModel
import fi.ville.treenipaivakirja.value
import java.time.LocalDate

@Composable
fun HistoryScreen(vm: WorkoutViewModel) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val selectedId by vm.historyExerciseId.collectAsStateWithLifecycle()
    val stats by vm.history.collectAsStateWithLifecycle()
    var metric by rememberSaveable { mutableStateOf(Metric.MAX) }

    // Valitse automaattisesti ensimmäinen liike
    LaunchedEffect(exercises) {
        val cur = vm.historyExerciseId.value
        if (cur == null || exercises.none { it.id == cur }) {
            vm.historyExerciseId.value = exercises.firstOrNull()?.id
        }
    }

    val unit = LocalUnit.current
    val values = stats.map { unit.fromKg(it.value(metric)).toFloat() }
    val labels = stats.map { LocalDate.ofEpochDay(it.epochDay).shortLabel() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(stringResource(R.string.progress), color = Lime, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(R.string.tab_history),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        if (exercises.isEmpty()) {
            item { EmptyHistory() }
            return@LazyColumn
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(exercises, key = { it.id }) { e ->
                    FilterChip(
                        selected = e.id == selectedId,
                        onClick = { vm.historyExerciseId.value = e.id },
                        label = { Text(e.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Lime,
                            selectedLabelColor = Color.Black,
                            labelColor = Color.White
                        )
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric.entries.forEach { m ->
                    FilterChip(
                        selected = m == metric,
                        onClick = { metric = m },
                        label = { Text(stringResource(m.labelRes), fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Cyan,
                            selectedLabelColor = Color.Black,
                            labelColor = Muted
                        )
                    )
                }
            }
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(CardBg)
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            ) {
                if (values.isEmpty()) {
                    Text(stringResource(R.string.no_entries), color = Muted, modifier = Modifier.padding(16.dp))
                } else {
                    LineChart(
                        values = values,
                        labels = labels,
                        unit = unit.label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    )
                }
            }
        }

        if (stats.isNotEmpty()) {
            item { StatsRow(stats, metric) }
            item {
                Text(
                    stringResource(R.string.sessions),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(stats.asReversed(), key = { it.epochDay }) { s -> SessionRow(s, metric) }
        }
    }
}

@Composable
private fun StatsRow(stats: List<DayStat>, metric: Metric) {
    val vals = stats.map { it.value(metric) }
    val record = vals.max()
    val latest = vals.last()
    val first = vals.first()
    val change = if (stats.size > 1 && first > 0) (latest - first) / first * 100 else null

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStat(stringResource(R.string.record), wt(record), Modifier.weight(1f), trophy = true)
        MiniStat(stringResource(R.string.latest), wt(latest), Modifier.weight(1f))
        MiniStat(
            stringResource(R.string.change),
            change?.let { String.format(appLocale(), "%+.0f %%", it) } ?: "–",
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier, trophy: Boolean = false) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (trophy) {
            Icon(Icons.Filled.EmojiEvents, null, tint = Lime, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(2.dp))
        }
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(brush = AccentBrush),
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Text(label, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun SessionRow(s: DayStat, metric: Metric) {
    val unit = LocalUnit.current
    val failShort = stringResource(R.string.failure_short)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                LocalDate.ofEpochDay(s.epochDay).sessionLabel(),
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                s.sets.joinToString("  ·  ") { "${num(unit.fromKg(it.weight))}×${if (it.reps == 0) failShort else it.reps}" },
                color = Muted,
                fontSize = 13.sp
            )
        }
        Text(
            wt(s.value(metric)),
            color = Lime,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardBg)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Timeline, null, tint = Cyan, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.empty_history_title), fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            stringResource(R.string.empty_history_body),
            color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center
        )
    }
}
