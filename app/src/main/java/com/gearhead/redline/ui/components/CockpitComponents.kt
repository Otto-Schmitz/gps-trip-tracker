package com.gearhead.redline.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gearhead.redline.ui.theme.MetricValueStyle
import com.gearhead.redline.ui.theme.Panel
import com.gearhead.redline.ui.theme.TextMuted
import com.gearhead.redline.ui.theme.TextPrimary
import com.gearhead.redline.ui.theme.TextSecondary

/** All-caps tracking label used above values and section headers. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = TextMuted) {
    Text(
        text = text.uppercase(),
        style = com.gearhead.redline.ui.theme.RedlineTypography.labelSmall,
        color = color,
        modifier = modifier,
    )
}

/** A boxed numeric readout: big monospace value + unit + label. */
@Composable
fun MetricTile(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SectionLabel(label)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, style = MetricValueStyle, color = valueColor)
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    style = com.gearhead.redline.ui.theme.RedlineTypography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

/** Two metric tiles sharing a row with equal weight. */
@Composable
fun MetricRow(
    left: @Composable (Modifier) -> Unit,
    right: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        left(Modifier.weight(1f))
        right(Modifier.weight(1f))
    }
}
