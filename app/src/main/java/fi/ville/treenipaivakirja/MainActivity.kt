package fi.ville.treenipaivakirja

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
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
import fi.ville.treenipaivakirja.ui.Lime
import fi.ville.treenipaivakirja.ui.Muted
import fi.ville.treenipaivakirja.ui.TreeniTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TreeniTheme { TreeniApp() } }
    }
}

@Composable
fun TreeniApp(vm: WorkoutViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }

    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color.Black,
        selectedTextColor = Lime,
        indicatorColor = Lime,
        unselectedIconColor = Muted,
        unselectedTextColor = Muted
    )

    Scaffold(
        containerColor = Bg,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = CardBg, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null) },
                    label = { Text("Treenit") }, colors = itemColors
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Timeline, contentDescription = null) },
                    label = { Text("Historia") }, colors = itemColors
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> DayScreen(vm, snackbar)
                else -> HistoryScreen(vm)
            }
        }
    }
}
