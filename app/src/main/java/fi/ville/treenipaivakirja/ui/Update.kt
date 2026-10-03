package fi.ville.treenipaivakirja.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.ville.treenipaivakirja.R
import fi.ville.treenipaivakirja.UpdateChecker
import fi.ville.treenipaivakirja.UpdateInfo
import fi.ville.treenipaivakirja.UpdateState

/** Avaa latauslinkin selaimeen; puhelin lataa APK:n ja käyttäjä asentaa sen. */
fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Käynnistyksessä näytettävä ilmoitus uudesta versiosta. */
@Composable
fun UpdateDialog(info: UpdateInfo, onDownload: () -> Unit, onLater: () -> Unit) {
    val context = LocalContext.current
    val current = UpdateChecker.currentVersionName(context)
    AlertDialog(
        onDismissRequest = onLater,
        containerColor = CardBg,
        icon = { Icon(Icons.Filled.SystemUpdate, null, tint = Lime) },
        title = { Text(stringResource(R.string.update_title), fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(R.string.update_body, info.versionName, current), color = Muted) },
        confirmButton = {
            Button(
                onClick = {
                    openUrl(context, info.downloadUrl)
                    onDownload()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black)
            ) { Text(stringResource(R.string.download), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.later)) } }
    )
}

/** Asetusten päivitysosio: tarkistusnappi + tila. */
@Composable
fun UpdateSection(state: UpdateState, onCheck: () -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when (state) {
            is UpdateState.Available -> Button(
                onClick = { openUrl(context, state.info.downloadUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color.Black)
            ) {
                Icon(Icons.Filled.SystemUpdate, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.download_version, state.info.versionName), fontWeight = FontWeight.Bold)
            }
            else -> OutlinedButton(onClick = onCheck, enabled = state !is UpdateState.Checking) {
                if (state is UpdateState.Checking) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Lime)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.checking), color = Color.White)
                } else {
                    Text(stringResource(R.string.check_updates), color = Color.White)
                }
            }
        }
        when (state) {
            UpdateState.UpToDate -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✓ " + stringResource(R.string.up_to_date), color = Lime, fontSize = 13.sp)
            }
            UpdateState.Error -> Text(stringResource(R.string.update_error), color = MaterialThemeError, fontSize = 13.sp)
            else -> {}
        }
    }
}

private val MaterialThemeError = Color(0xFFFF5A6E)
