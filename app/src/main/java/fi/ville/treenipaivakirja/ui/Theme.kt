package fi.ville.treenipaivakirja.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import fi.ville.treenipaivakirja.R
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

// ---------- Värit ----------
val Lime = Color(0xFFC6FF3D)
val Cyan = Color(0xFF3DDCFF)
val Bg = Color(0xFF0D0E12)
val CardBg = Color(0xFF171920)
val CardBg2 = Color(0xFF21242E)
val Muted = Color(0xFF8A90A2)
val GridLine = Color(0xFF2A2E3A)

val AccentBrush = Brush.linearGradient(listOf(Lime, Cyan))

private val scheme = darkColorScheme(
    primary = Lime,
    onPrimary = Color.Black,
    secondary = Cyan,
    onSecondary = Color.Black,
    background = Bg,
    onBackground = Color.White,
    surface = CardBg,
    onSurface = Color.White,
    surfaceVariant = CardBg2,
    onSurfaceVariant = Muted,
    surfaceContainer = CardBg,
    surfaceContainerHigh = CardBg2,
    surfaceContainerHighest = CardBg2,
    outline = Color(0xFF3A3F4E),
    error = Color(0xFFFF5A6E)
)

@Composable
fun TreeniTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}

// ---------- Painoyksikkö ----------
/** Tietokannassa paino on aina kiloina; yksikkö vaikuttaa vain näyttöön ja syöttöön. */
enum class WeightUnit(val label: String, private val perKg: Double) {
    KG("kg", 1.0),
    LB("lb", 2.20462262185);

    fun fromKg(kg: Double): Double = kg * perKg
    fun toKg(value: Double): Double = value / perKg
}

val LocalUnit = staticCompositionLocalOf { WeightUnit.KG }

/** Kilot -> "80 kg" / "176,4 lb" valitulla yksiköllä. */
@Composable
fun wt(kg: Double): String {
    val u = LocalUnit.current
    return "${num(u.fromKg(kg))} ${u.label}"
}

/** Kilot -> pelkkä luku valitulla yksiköllä. */
@Composable
fun wtNum(kg: Double): String = num(LocalUnit.current.fromKg(kg))

// ---------- Muotoilu ----------
/** 82.5 -> "82,5" (fi) / "82.5" (en), 12450.0 -> "12 450" / "12,450" */
fun num(d: Double): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 1 }.format(d)

/** Kenttään esitäytettävä arvo yhdellä desimaalilla, ilman tuhaterottimia. */
fun editable(d: Double): String {
    val r = (d * 10).roundToLong() / 10.0
    val sep = DecimalFormatSymbols.getInstance().decimalSeparator
    return if (r % 1.0 == 0.0) r.toLong().toString() else r.toString().replace('.', sep)
}

/** Hyväksyy sekä pilkun että pisteen desimaalierottimena. */
fun parseDecimal(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

@Composable
fun appLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
private fun LocalDate.fmt(patternRes: Int): String {
    val loc = appLocale()
    return format(DateTimeFormatter.ofPattern(stringResource(patternRes), loc))
        .replaceFirstChar { it.titlecase(loc) }
}

@Composable fun LocalDate.longLabel(): String = fmt(R.string.fmt_date_long)
@Composable fun LocalDate.shortLabel(): String = fmt(R.string.fmt_date_short)
@Composable fun LocalDate.sessionLabel(): String = fmt(R.string.fmt_date_session)
