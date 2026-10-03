package fi.ville.treenipaivakirja.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Animoitu viivakaavio gradienttitäytöllä.
 * Napauta kaaviota nähdäksesi yksittäisen pisteen arvon.
 */
@Composable
fun LineChart(
    values: List<Float>,
    labels: List<String>,
    unit: String,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val axisStyle = TextStyle(color = Muted, fontSize = 11.sp)
    val tipStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }

    var selected by remember(values) { mutableIntStateOf(values.lastIndex) }

    // Reunukset (dp): vasemmalla y-akselin luvut, ylhäällä tilaa tooltipille
    val leftDp = 46.dp
    val rightDp = 14.dp
    val topDp = 40.dp
    val bottomDp = 26.dp

    Canvas(
        modifier.pointerInput(values) {
            detectTapGestures { pos ->
                if (values.isEmpty()) return@detectTapGestures
                val left = leftDp.toPx()
                val w = size.width - left - rightDp.toPx()
                selected = if (values.size == 1) 0
                else (((pos.x - left) / w) * (values.size - 1)).roundToInt().coerceIn(0, values.lastIndex)
            }
        }
    ) {
        if (values.isEmpty()) return@Canvas

        val left = leftDp.toPx()
        val top = topDp.toPx()
        val w = size.width - left - rightDp.toPx()
        val h = size.height - top - bottomDp.toPx()
        val base = top + h

        // Y-akselin skaala pienellä marginaalilla
        var minV = values.min()
        var maxV = values.max()
        if (maxV - minV < 0.001f) {
            minV -= 1f; maxV += 1f
        } else {
            val pad = (maxV - minV) * 0.15f
            minV = (minV - pad).coerceAtLeast(0f)
            maxV += pad
        }
        val range = maxV - minV

        // Ruudukko + y-akselin luvut
        for (i in 0..3) {
            val v = minV + range * i / 3f
            val y = base - h * i / 3f
            drawLine(GridLine, Offset(left, y), Offset(left + w, y), strokeWidth = 1.dp.toPx())
            val t = measurer.measure(num(v.toDouble()), axisStyle)
            drawText(t, topLeft = Offset(left - t.size.width - 8.dp.toPx(), y - t.size.height / 2f))
        }

        // Pisteet (animoitu nousu pohjaviivasta)
        val pts = values.mapIndexed { i, v ->
            val x = if (values.size == 1) left + w / 2f else left + w * i / (values.size - 1)
            val y = base - (v - minV) / range * h
            Offset(x, base - (base - y) * progress.value)
        }

        fun Path.curveThrough(points: List<Offset>) {
            for (i in 1 until points.size) {
                val p0 = points[i - 1]
                val p1 = points[i]
                val cx = (p0.x + p1.x) / 2f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }

        val line = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            curveThrough(pts)
        }
        val fill = Path().apply {
            moveTo(pts.first().x, base)
            lineTo(pts.first().x, pts.first().y)
            curveThrough(pts)
            lineTo(pts.last().x, base)
            close()
        }

        drawPath(
            fill,
            Brush.verticalGradient(listOf(Lime.copy(alpha = 0.35f), Color.Transparent), startY = top, endY = base)
        )
        if (pts.size > 1) {
            drawPath(
                line,
                Brush.horizontalGradient(listOf(Lime, Cyan), startX = left, endX = left + w),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Valitun pisteen pystyviiva
        val sel = selected.coerceIn(0, pts.lastIndex)
        val selPt = pts[sel]
        drawLine(
            Muted.copy(alpha = 0.6f),
            Offset(selPt.x, top),
            Offset(selPt.x, base),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        )

        pts.forEachIndexed { i, p ->
            val r = if (i == sel) 6.dp.toPx() else 4.dp.toPx()
            drawCircle(Bg, r + 2.dp.toPx(), p)
            drawCircle(if (i == sel) Cyan else Lime, r, p)
        }

        // X-akselin päivämäärät: eka, keskimmäinen, viimeinen
        val labelIdx = listOf(0, labels.lastIndex / 2, labels.lastIndex).distinct()
        labelIdx.forEach { i ->
            if (i in labels.indices && i in pts.indices) {
                val t = measurer.measure(labels[i], axisStyle)
                val x = (pts[i].x - t.size.width / 2f).coerceIn(left, size.width - t.size.width)
                drawText(t, topLeft = Offset(x, base + 6.dp.toPx()))
            }
        }

        // Tooltip
        val tip = measurer.measure("${labels.getOrElse(sel) { "" }}  ·  ${num(values[sel].toDouble())} $unit", tipStyle)
        val padH = 10.dp.toPx()
        val padV = 6.dp.toPx()
        val boxW = tip.size.width + padH * 2
        val boxH = tip.size.height + padV * 2
        val bx = (selPt.x - boxW / 2f).coerceIn(0f, size.width - boxW)
        val by = (selPt.y - boxH - 12.dp.toPx()).coerceAtLeast(0f)
        drawRoundRect(CardBg2, Offset(bx, by), Size(boxW, boxH), CornerRadius(10.dp.toPx()))
        drawText(tip, topLeft = Offset(bx + padH, by + padV))
    }
}
