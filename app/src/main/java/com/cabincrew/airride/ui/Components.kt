package com.cabincrew.airride.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** AirRide/Condition Gauge — label, status word, level bar, detail. */
@Composable
fun ConditionGauge(
    label: String,
    status: String,
    statusColor: Color,
    level: Float,
    levelColor: Color,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth()
            .background(AirColors.Card, RoundedCornerShape(40.dp))
            .padding(horizontal = 36.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, color = AirColors.Text, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(status, color = statusColor, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        }
        Box(Modifier.fillMaxWidth().height(16.dp).background(AirColors.Track, RoundedCornerShape(8.dp))) {
            Box(
                Modifier.fillMaxWidth(level.coerceIn(0.02f, 1f)).fillMaxHeight()
                    .background(levelColor, RoundedCornerShape(8.dp)),
            )
        }
        Text(detail, color = AirColors.TextSub, fontSize = 26.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** AirRide/Status Chip — dot + label. */
@Composable
fun StatusChip(label: String, dot: Color = AirColors.Ok, modifier: Modifier = Modifier) {
    Row(
        modifier.background(AirColors.Card, RoundedCornerShape(999.dp)).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(16.dp).background(dot, CircleShape))
        Text(label, color = AirColors.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

/** Suggestion (Pleos Notification) — title, reason, max 2 buttons. */
@Composable
fun SuggestionCard(
    title: String,
    body: String,
    primary: String,
    onPrimary: () -> Unit,
    secondary: String,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth()
            .shadow(20.dp, RoundedCornerShape(48.dp))
            .background(AirColors.Notification, RoundedCornerShape(48.dp))
            .border(1.dp, AirColors.DividerSoft, RoundedCornerShape(48.dp)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 40.dp, end = 40.dp, top = 36.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, color = Color.White, fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(body, color = AirColors.TextTertiary, fontSize = 26.sp, lineHeight = 34.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IviButton(primary, onPrimary, filled = true, modifier = Modifier.weight(1f))
            IviButton(secondary, onSecondary, filled = false, modifier = Modifier.weight(1f))
        }
    }
}

/** ivi_basic_button (filled / basic). */
@Composable
fun IviButton(
    text: String,
    onClick: () -> Unit,
    filled: Boolean,
    modifier: Modifier = Modifier,
    minHeight: Dp = 80.dp,
    radius: Dp = 24.dp,
    fontSize: Int = 30,
) {
    Box(
        modifier.heightIn(min = minHeight)
            .clip(RoundedCornerShape(radius))
            .background(if (filled) AirColors.ButtonFilled else AirColors.ButtonBasic)
            .clickableNoRipple(onClick)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (filled) AirColors.ButtonFilledText else Color.White,
            fontSize = fontSize.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Care log row — colored dot, time, text, bottom divider. */
@Composable
fun CareLogRow(time: String, text: String, dot: Color, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(Modifier.size(14.dp).background(dot, CircleShape))
            Text(time, color = AirColors.TextMuted, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Text(text, color = AirColors.Text, fontSize = 26.sp, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.fillMaxWidth().height(2.dp).background(AirColors.Divider))
    }
}

/** Report stat card. */
@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(AirColors.Card, RoundedCornerShape(40.dp)).padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, color = AirColors.TextMuted, fontSize = 26.sp)
        Text(value, color = AirColors.Text, fontSize = 48.sp, fontWeight = FontWeight.Bold)
    }
}
