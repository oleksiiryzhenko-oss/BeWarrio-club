package com.katadojo.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katadojo.app.R
import com.katadojo.app.ui.theme.*
import com.katadojo.app.utils.Formatters

val CardShape = RoundedCornerShape(14.dp)

/** Scoreboard-style card: charcoal surface, thin border, optional red accent stripe. */
@Composable
fun DojoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accent: Color? = null,
    containerColor: Color = Charcoal,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier
        .fillMaxWidth()
        .clip(CardShape)
        .background(containerColor)
        .border(1.dp, CharcoalBorder, CardShape)
    Box(if (onClick != null) base.clickable(onClick = onClick) else base) {
        Column(Modifier.fillMaxWidth().padding(contentPadding), content = content)
        if (accent != null) {
            Box(Modifier.matchParentSize()) {
                Box(Modifier.fillMaxHeight().width(4.dp).background(accent))
            }
        }
    }
}

@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = Stone) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = color
    )
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(width = 4.dp, height = 18.dp).background(DojoRed))
        Spacer(Modifier.width(10.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.5.sp),
            color = Rice,
            modifier = Modifier.weight(1f)
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge,
                color = DojoRed,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun StatBlock(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Rice,
    large: Boolean = false
) {
    Column(modifier) {
        Text(
            text = value,
            style = if (large) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
            color = valueColor,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Overline(label)
    }
}

/** Grid of stat cells laid out two or three per row. */
@Composable
fun StatGrid(items: List<Pair<String, String>>, modifier: Modifier = Modifier, columns: Int = 2) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (value, label) ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CharcoalLight)
                            .padding(14.dp)
                    ) {
                        Text(value, style = MaterialTheme.typography.headlineSmall, color = Rice, maxLines = 1)
                        Spacer(Modifier.height(2.dp))
                        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Stone, maxLines = 2)
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun ThickProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = DojoRed,
    height: Dp = 10.dp
) {
    val clamped = fraction.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(CharcoalBorder)
    ) {
        if (clamped > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .clip(RoundedCornerShape(height / 2))
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.75f), color)))
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
    color: Color = DojoRed,
    height: Dp = 56.dp
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Rice,
            disabledContainerColor = CharcoalLight,
            disabledContentColor = StoneDark
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
    color: Color = Rice,
    height: Dp = 52.dp
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, if (enabled) StoneDark else CharcoalBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color, disabledContentColor = StoneDark)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Selectable pill used for filters and option pickers. */
@Composable
fun SelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 44.dp
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .heightIn(min = minHeight)
            .clip(shape)
            .background(if (selected) DojoRed else CharcoalLight)
            .border(1.dp, if (selected) DojoRed else CharcoalBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) Rice else StoneLight,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun TagPill(text: String, modifier: Modifier = Modifier, color: Color = Stone, filled: Boolean = false) {
    val shape = RoundedCornerShape(8.dp)
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = if (filled) Rice else color,
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(if (filled) color else color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Rice) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Stone, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor, textAlign = TextAlign.End)
    }
}

@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: Painter? = null,
    trailing: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Icon(leading, contentDescription = null, tint = StoneLight, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Rice)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Stone)
        }
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.titleSmall, color = StoneLight)
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = StoneDark)
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    DojoCard(modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(DojoRedGlow),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painterResource(R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = Rice, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = Stone, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                PrimaryButton(actionLabel, onAction)
            }
        }
    }
}

fun scoreColor(score: Double): Color = when {
    score >= 8.0 -> Beige
    score >= 6.0 -> Rice
    score >= 4.0 -> StoneLight
    else -> ErrorRed
}

/** Training Performance Score display: "7.8 / 10". */
@Composable
fun ScoreBadge(score: Double, modifier: Modifier = Modifier, large: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            Formatters.score(score),
            style = if (large) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineMedium,
            color = scoreColor(score)
        )
        Text(
            " / 10",
            style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
            color = Stone,
            modifier = Modifier.padding(bottom = if (large) 8.dp else 3.dp)
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Cancel",
    destructive: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalLight,
        titleContentColor = Rice,
        textContentColor = StoneLight,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel.uppercase(), color = if (destructive) ErrorRed else DojoRed, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel.uppercase(), color = StoneLight, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = DojoRed)
    }
}
