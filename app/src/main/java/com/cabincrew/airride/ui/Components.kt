package com.cabincrew.airride.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cabincrew.airride.engine.Level

/** Condition Gauge — 270° arc with score and level. */
@Composable
fun ConditionGauge(title: String, score: Int, level: Level, levelText: String, modifier: Modifier = Modifier) {
    val color = AirColors.of(level)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = AirColors.TextSub, fontSize = 22.sp)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(220.dp)) {
                val stroke = 22.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(AirColors.SurfaceHigh, 135f, 270f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(color, 135f, 270f * score / 100f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$score", color = AirColors.Text, fontSize = 64.sp, fontWeight = FontWeight.Bold)
                Text(levelText, color = color, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Status Chip */
@Composable
fun StatusChip(text: String, color: Color = AirColors.TextSub, modifier: Modifier = Modifier) {
    Text(
        text,
        color = color,
        fontSize = 18.sp,
        modifier = modifier
            .background(AirColors.SurfaceHigh, RoundedCornerShape(50))
            .padding(horizontal = 18.dp, vertical = 8.dp),
    )
}

/** Suggestion Card — title, short body, max 2 buttons (driving-safe). */
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
        modifier
            .background(AirColors.Surface, RoundedCornerShape(28.dp))
            .padding(32.dp),
    ) {
        Text(title, color = AirColors.Text, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(body, color = AirColors.TextSub, fontSize = 22.sp)
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BigButton(primary, onPrimary, AirColors.Accent, Color(0xFF00131D), Modifier.weight(1f))
            BigButton(secondary, onSecondary, AirColors.SurfaceHigh, AirColors.Text, Modifier.weight(1f))
        }
    }
}

@Composable
fun BigButton(text: String, onClick: () -> Unit, container: Color, content: Color, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) {
        Text(text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    }
}
