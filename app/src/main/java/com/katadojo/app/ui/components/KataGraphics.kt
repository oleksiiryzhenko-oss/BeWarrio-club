package com.katadojo.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.katadojo.app.R
import com.katadojo.app.data.model.SectionStatus
import com.katadojo.app.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** Brush-stroke circle (ensō) drawn as a ring whose width tapers towards the tail. */
@Composable
fun Enso(modifier: Modifier = Modifier, color: Color = DojoRed, thickness: Float = 0.12f, alpha: Float = 1f) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val c = center
        val base = r * thickness
        val radius = r - base
        val steps = 90
        val start = -70.0
        val sweep = 320.0
        val outer = ArrayList<Offset>(steps + 1)
        val inner = ArrayList<Offset>(steps + 1)
        for (i in 0..steps) {
            val t = i / steps.toDouble()
            val a = Math.toRadians(start + sweep * t)
            val w = base * (0.35 + 0.65 * sin(PI * min(1.0, t * 1.15)).pow(0.6)) * (1 - 0.55 * t.pow(3))
            val half = w.coerceAtLeast(base * 0.12) / 2
            outer += Offset((c.x + cos(a) * (radius + half)).toFloat(), (c.y + sin(a) * (radius + half)).toFloat())
            inner += Offset((c.x + cos(a) * (radius - half)).toFloat(), (c.y + sin(a) * (radius - half)).toFloat())
        }
        val path = Path().apply {
            moveTo(outer.first().x, outer.first().y)
            outer.drop(1).forEach { lineTo(it.x, it.y) }
            inner.asReversed().forEach { lineTo(it.x, it.y) }
            close()
        }
        drawPath(path, color.copy(alpha = color.alpha * alpha))
    }
}

/** Subtle tatami mat texture: long panels with a woven stripe pattern. */
@Composable
fun TatamiTexture(modifier: Modifier = Modifier, color: Color = Beige, alpha: Float = 0.05f) {
    Canvas(modifier) {
        val line = color.copy(alpha = alpha)
        val border = color.copy(alpha = alpha * 2.2f)
        val panel = 96.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(border, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5.dp.toPx())
            x += panel
        }
        val step = 5.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += step
        }
    }
}

/** Dark header with tatami texture, a red ensō and a thin geometric frame. */
@Composable
fun DojoHero(
    modifier: Modifier = Modifier,
    overline: String? = null,
    title: String,
    subtitle: String? = null,
    minHeight: Dp = 190.dp,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2A1714), Charcoal, Ink)))
    ) {
        TatamiTexture(Modifier.matchParentSize())
        Enso(
            Modifier
                .size(170.dp)
                .align(Alignment.TopEnd)
                .offset(x = 36.dp, y = (-22).dp),
            color = DojoRed,
            alpha = 0.75f
        )
        Canvas(Modifier.matchParentSize()) {
            val inset = 10.dp.toPx()
            drawRect(
                color = Beige.copy(alpha = 0.12f),
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(width = 1f)
            )
        }
        Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 22.dp)) {
            if (overline != null) {
                Overline(overline, color = Beige)
                Spacer(Modifier.height(6.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineLarge, color = Rice)
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = StoneLight)
            }
            content()
        }
    }
}

/** Progress bar styled as a karate belt with a knot and two tails at the filled end. */
@Composable
fun BeltProgress(fraction: Float, beltColor: Color, modifier: Modifier = Modifier, height: Dp = 14.dp) {
    val f = fraction.coerceIn(0f, 1f)
    Canvas(modifier.fillMaxWidth().height(height + 12.dp)) {
        val h = height.toPx()
        val top = 2.dp.toPx()
        val radius = h / 2
        drawRoundRect(CharcoalBorder, Offset(0f, top), Size(size.width, h), androidx.compose.ui.geometry.CornerRadius(radius))
        val w = (size.width * f).coerceAtLeast(if (f > 0f) h * 1.6f else 0f)
        if (w > 0f) {
            drawRoundRect(beltColor, Offset(0f, top), Size(w, h), androidx.compose.ui.geometry.CornerRadius(radius))
            drawRoundRect(Beige.copy(alpha = 0.35f), Offset(0f, top), Size(w, h), androidx.compose.ui.geometry.CornerRadius(radius), style = Stroke(1f))
            drawLine(Color.Black.copy(alpha = 0.18f), Offset(radius, top + h * 0.35f), Offset(w - radius, top + h * 0.35f), strokeWidth = 1f)
            drawLine(Color.Black.copy(alpha = 0.18f), Offset(radius, top + h * 0.65f), Offset(w - radius, top + h * 0.65f), strokeWidth = 1f)
            val kx = (w - h * 0.9f).coerceAtLeast(h * 0.4f)
            val knot = h * 1.05f
            drawRect(beltColor, Offset(kx - knot / 2, top - h * 0.1f), Size(knot, h * 1.2f))
            drawRect(Color.Black.copy(alpha = 0.22f), Offset(kx - knot / 2, top - h * 0.1f), Size(knot, h * 1.2f), style = Stroke(1f))
            val tail = Path().apply {
                moveTo(kx - knot * 0.35f, top + h)
                lineTo(kx - knot * 0.9f, top + h + 10.dp.toPx())
                lineTo(kx - knot * 0.35f, top + h + 10.dp.toPx())
                lineTo(kx, top + h)
                close()
                moveTo(kx, top + h)
                lineTo(kx + knot * 0.55f, top + h + 10.dp.toPx())
                lineTo(kx + knot * 1.0f, top + h + 9.dp.toPx())
                lineTo(kx + knot * 0.35f, top + h)
                close()
            }
            drawPath(tail, beltColor)
        }
    }
}

/** Circular progress ring with content in the middle. */
@Composable
fun RingProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = DojoRed,
    track: Color = CharcoalBorder,
    strokeWidth: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val s = strokeWidth.toPx()
            val d = size.minDimension - s
            val topLeft = Offset((size.width - d) / 2, (size.height - d) / 2)
            drawArc(track, 0f, 360f, false, topLeft, Size(d, d), style = Stroke(s))
            val sweep = 360f * fraction.coerceIn(0f, 1f)
            if (sweep > 0f) drawArc(color, -90f, sweep, false, topLeft, Size(d, d), style = Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
fun DojoWordmark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(28.dp).clip(CircleShape))
        Spacer(Modifier.width(8.dp))
        Text("BEЕWARRIOR", style = MaterialTheme.typography.headlineSmall, color = Rice)
        Text(" CLUB", style = MaterialTheme.typography.headlineSmall, color = DojoRed)
    }
}

fun statusColor(status: SectionStatus?): Color = when (status) {
    SectionStatus.COMPLETED -> SuccessGreen
    SectionStatus.NEEDS_PRACTICE -> Amber
    SectionStatus.DIFFICULT -> ErrorRed
    null -> Stone
}

@Composable
fun StatusPill(status: SectionStatus?) {
    TagPill(status?.label ?: "Not practiced", color = statusColor(status))
}

fun beltColor(argb: Long): Color = Color(argb)
