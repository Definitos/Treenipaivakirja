package fi.ville.treenipaivakirja.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

// ---------- Muotoilu ----------
val FI: Locale = Locale.forLanguageTag("fi-FI")

private val numberFormat: NumberFormat =
    NumberFormat.getNumberInstance(FI).apply { maximumFractionDigits = 1 }

/** 82.5 -> "82,5", 100.0 -> "100", 12450.0 -> "12 450" */
fun num(d: Double): String = numberFormat.format(d)

/** Kenttään esitäytettävä arvo: 82.5 -> "82,5", 100.0 -> "100" */
fun editable(d: Double): String =
    if (d % 1.0 == 0.0) d.toLong().toString() else d.toString().replace('.', ',')

private val longFmt = DateTimeFormatter.ofPattern("EEEE d.M.yyyy", FI)
private val shortFmt = DateTimeFormatter.ofPattern("d.M.", FI)
private val sessionFmt = DateTimeFormatter.ofPattern("EEE d.M.yyyy", FI)

fun LocalDate.fiLong(): String = format(longFmt).replaceFirstChar { it.titlecase(FI) }
fun LocalDate.fiShort(): String = format(shortFmt)
fun LocalDate.fiSession(): String = format(sessionFmt).replaceFirstChar { it.titlecase(FI) }
