package fi.ville.treenipaivakirja

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.ville.treenipaivakirja.ui.LocalUnit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.ville.treenipaivakirja.ui.Bg
import fi.ville.treenipaivakirja.ui.CardBg
import fi.ville.treenipaivakirja.ui.DayScreen
import fi.ville.treenipaivakirja.ui.HistoryScreen
import fi.ville.treenipaivakirja.ui.NutritionScreen
import fi.ville.treenipaivakirja.ui.Lime
import fi.ville.treenipaivakirja.ui.Muted
import fi.ville.treenipaivakirja.ui.TemplatesScreen
import fi.ville.treenipaivakirja.ui.TreeniTheme
import fi.ville.treenipaivakirja.ui.UpdateDialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TreeniTheme { TreeniApp() } }
    }
}

@Composable
fun TreeniApp(vm: WorkoutViewModel = viewModel(), nvm: NutritionViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val unit by vm.unit.collectAsStateWithLifecycle()
    val day by vm.selectedDay.collectAsStateWithLifecycle()
    val update by vm.updateState.collectAsStateWithLifecycle()
    val updateDismissed by vm.updatePromptDismissed.collectAsStateWithLifecycle()

    val available = update as? UpdateState.Available
    if (available != null && !updateDismissed) {
        UpdateDialog(
            info = available.info,
            onDownload = { vm.updatePromptDismissed.value = true },
            onLater = { vm.updatePromptDismissed.value = true }
        )
    }

    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color.Black,
        selectedTextColor = Lime,
        indicatorColor = Lime,
        unselectedIconColor = Muted,
        unselectedTextColor = Muted
    )

    CompositionLocalProvider(LocalUnit provides unit) {
    Scaffold(
        containerColor = Bg,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = CardBg, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_workouts)) }, colors = itemColors
                )
                NavigationBarItem(
                    selected = tab == 3, onClick = { tab = 3 },
                    icon = { Icon(Icons.Filled.Restaurant, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_nutrition)) }, colors = itemColors
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Bookmarks, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_programs)) }, colors = itemColors
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Timeline, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_history)) }, colors = itemColors
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> DayScreen(vm, snackbar)
                1 -> TemplatesScreen(vm)
                3 -> NutritionScreen(nvm, day, { vm.selectedDay.value = it }, snackbar)
                else -> HistoryScreen(vm)
            }
        }
    }
    }
}
