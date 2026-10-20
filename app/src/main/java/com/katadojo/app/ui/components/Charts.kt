package com.katadojo.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katadojo.app.ui.theme.*
import com.katadojo.app.utils.ChartPoint
import kotlin.math.roundToInt

private val axisLabelStyle: TextStyle
    @Composable get() =
    MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.sp)

@Composable
private fun NoChartData(height: Dp) {
    Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        Text("No data yet", style = MaterialTheme.typography.bodyMedium, color = Stone)
    }
}

/** Vertical bars with value labels. The last bar is highlighted. */
@Composable
fun BarChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = DojoRed,
    height: Dp = 170.dp,
    valueFormatter: (Float) -> String = { it.roundToInt().toString() }
) {
    if (points.isEmpty()) {
        NoChartData(height)
        return
    }
    val max = points.maxOf { it.value }.coerceAtLeast(1f)
    Row(
        modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEachIndexed { index, point ->
            val highlighted = index == points.lastIndex
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    valueFormatter(point.value),
                    style = axisLabelStyle,
                    color = if (highlighted) Rice else StoneLight,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                    val fraction = if (point.value <= 0f) 0f else (point.value / max).coerceIn(0.03f, 1f)
                    if (fraction > 0f) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.72f)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        if (highlighted) listOf(barColor, barColor.copy(alpha = 0.7f))
                                        else listOf(barColor.copy(alpha = 0.55f), barColor.copy(alpha = 0.3f))
                                    )
                                )
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(CharcoalBorder))
                Spacer(Modifier.height(6.dp))
                Text(
                    point.label,
                    style = axisLabelStyle,
                    color = Stone,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Line chart with gradient fill, drawn on a Canvas. */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = DojoRed,
    height: Dp = 150.dp,
    minValue: Float? = null,
    maxValue: Float? = null,
    valueFormatter: (Float) -> String = { String.format(java.util.Locale.US, "%.1f", it) }
) {
    if (points.isEmpty()) {
        NoChartData(height)
        return
    }
    val dataMin = points.minOf { it.value }
    val dataMax = points.maxOf { it.value }
    val low = minValue ?: (dataMin - (dataMax - dataMin) * 0.2f).coerceAtLeast(0f)
    val high = (maxValue ?: (dataMax + (dataMax - dataMin) * 0.2f)).let { if (it - low < 0.5f) low + 1f else it }

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text("High ${valueFormatter(dataMax)}", style = axisLabelStyle, color = Stone, modifier = Modifier.weight(1f))
            Text("Latest ${valueFormatter(points.last().value)}", style = axisLabelStyle, color = Rice)
        }
        Spacer(Modifier.height(8.dp))
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val n = points.size
            val step = size.width / n
            fun x(i: Int) = step * (i + 0.5f)
            fun y(v: Float) = size.height - ((v - low) / (high - low)).coerceIn(0f, 1f) * size.height

            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            for (g in 0..3) {
                val gy = size.height * g / 3f
                drawLine(CharcoalBorder, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f, pathEffect = dash)
            }

            val line = Path()
            points.forEachIndexed { i, p ->
                if (i == 0) line.moveTo(x(i), y(p.value)) else line.lineTo(x(i), y(p.value))
            }
            if (n > 1) {
                val fill = Path().apply {
                    addPath(line)
                    lineTo(x(n - 1), size.height)
                    lineTo(x(0), size.height)
                    close()
                }
                drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)))
                drawPath(line, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            points.forEachIndexed { i, p ->
                val last = i == n - 1
                drawCircle(if (last) Rice else lineColor, radius = if (last) 6.dp.toPx() else 4.dp.toPx(), center = Offset(x(i), y(p.value)))
                drawCircle(Ink, radius = if (last) 2.5.dp.toPx() else 1.5.dp.toPx(), center = Offset(x(i), y(p.value)))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            points.forEach { p ->
                Text(
                    p.label,
                    style = axisLabelStyle,
                    color = Stone,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
