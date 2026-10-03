package fi.ville.treenipaivakirja

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Uusin GitHub-julkaisu. runNumber = CI:n ajonumero (tagi "build-N"). */
data class UpdateInfo(val runNumber: Int, val versionName: String, val downloadUrl: String)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data object Error : UpdateState
}

/**
 * Päivitystarkistus GitHub Releases -rajapinnasta.
 * Lähettää vain tavallisen GET-pyynnön, ei mitään käyttäjän tietoja.
 */
object UpdateChecker {
    private const val LATEST_API =
        "https://api.github.com/repos/Definitos/Treenipaivakirja/releases/latest"
    const val RELEASES_PAGE = "https://github.com/Definitos/Treenipaivakirja/releases/latest"

    suspend fun fetchLatest(): UpdateInfo = withContext(Dispatchers.IO) {
        val conn = (URL(LATEST_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val run = json.getString("tag_name").substringAfter("build-").toInt()
            var url = json.optString("html_url", RELEASES_PAGE)
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk")) {
                        url = a.getString("browser_download_url")
                        break
                    }
                }
            }
            UpdateInfo(runNumber = run, versionName = "1.$run", downloadUrl = url)
        } finally {
            conn.disconnect()
        }
    }

    /** versionCode = ajonumero + 1 (ks. app/build.gradle.kts). */
    fun currentRun(context: Context): Int {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return (PackageInfoCompat.getLongVersionCode(info) - 1).toInt()
    }

    fun currentVersionName(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
}
