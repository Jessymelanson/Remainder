package com.remainder.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VSpace(dp: Int) = Spacer(Modifier.height(dp.dp))

@Composable
fun SectionHeader(text: String, trailing: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** A round emoji tile, which is what stands in for an icon throughout. */
@Composable
fun IconTile(
    emoji: String,
    size: Int = 42,
    background: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(percent = 30))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = (size * 0.48f).sp)
    }
}

/**
 * The paycheck drawn as one bar, in the order the money leaves.
 *
 * Far quicker to read than four numbers. When the plan runs past the paycheck
 * the overspend is drawn in the error colour on the end, so being over budget
 * looks like what it is rather than a bar that simply reads full.
 */
@Composable
fun ShareBar(
    parts: List<Pair<Double, Color>>,
    over: Double = 0.0,
    height: Int = 14
) {
    val total = parts.sumOf { it.first } + over
    if (total <= 0.0) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height.dp)
                .clip(RoundedCornerShape(height.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        return
    }

    Row(
        Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(height.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        parts.forEach { (value, colour) ->
            // A zero weight is not allowed, and a sliver too small to see is
            // worse than nothing, so tiny slices are dropped rather than drawn.
            val fraction = (value / total).toFloat()
            if (fraction > 0.004f) {
                Box(Modifier.weight(fraction).fillMaxWidth().background(colour))
            }
        }
        val overFraction = (over / total).toFloat()
        if (overFraction > 0.004f) {
            Box(
                Modifier.weight(overFraction).fillMaxWidth()
                    .background(MaterialTheme.colorScheme.error)
            )
        }
    }
}

/**
 * How full a savings pot is.
 *
 * Separate from [ShareBar] because it answers a different question. That one
 * divides a fixed amount between competing claims; this one is a single number
 * creeping towards a target, and it fills up rather than being carved up.
 */
@Composable
fun GoalBar(fraction: Double, height: Int = 10) {
    val safe = fraction.coerceIn(0.0, 1.0).toFloat()
    Box(
        Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(height.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Nothing is drawn at zero. A rounded cap on an empty bar reads as a
        // sliver of progress that is not there.
        if (safe > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(safe)
                    .height(height.dp)
                    .clip(RoundedCornerShape(height.dp))
                    .background(if (safe >= 1f) Money.good() else MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/** Key for a [ShareBar]: a coloured dot, a label and a figure. */
@Composable
fun LegendRow(colour: Color, label: String, value: String, note: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(colour))
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (note != null) {
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
