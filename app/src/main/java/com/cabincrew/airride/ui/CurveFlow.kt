package com.cabincrew.airride.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cabincrew.airride.R
import com.cabincrew.airride.engine.Level

/** A-05 대응 (Figma 프로토타입): Gleo orb + message, passenger gauge, rear-seat airflow view. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.CurveFlow(state: UiState, onOpenDemo: () -> Unit) {
    val name = state.passengerName
    val stage = state.curveStage ?: return
    Column(
        Modifier.fillMaxWidth().weight(1f).combinedClickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
            onClick = {}, onLongClick = onOpenDemo,
        ),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) { CurveFlowContent(state, name, stage) }
}

@Composable
private fun ColumnScope.CurveFlowContent(state: UiState, name: String, stage: CurveStage) {
    when (stage) {
        CurveStage.ADJUSTING -> {
            PassengerChipGauge(state)
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { RearSeatAirflow() }
            WindChip()
        }
        else -> {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
                Image(painterResource(R.drawable.gleo_orb), contentDescription = null, modifier = Modifier.size(115.dp))
                Text(
                    when (stage) {
                        CurveStage.WARN -> "곧 커브예요 급가속과 급제동을 줄여주세요"
                        CurveStage.CONCERN -> "커브가 이어져 ${possessive(name)} 멀미가 우려돼요"
                        CurveStage.ADJUSTING_NOTICE -> "뒷좌석을 조금 더 시원하게, 바깥 공기로 바꿀게요"
                        else -> "상태가 안정되어 바람 조절을 마쳤어요"
                    },
                    color = AirColors.Text, fontSize = 36.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center, lineHeight = 46.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp),
                )
            }
            PassengerChipGauge(state, forceCalm = stage == CurveStage.STABLE)
        }
    }
}

/** Condition gauge with the passenger's avatar (Figma: Profile 70 + name, warning + status, track). */
@Composable
private fun PassengerChipGauge(state: UiState, forceCalm: Boolean = false) {
    val c = state.condition
    val calm = forceCalm || c.motionLevel == Level.GOOD
    val color = if (calm) AirColors.Passenger else AirColors.Warn
    val level = if (forceCalm) minOf(c.motionSickness, 30) / 100f else c.motionSickness / 100f
    Column(
        Modifier.fillMaxWidth().background(AirColors.Card, RoundedCornerShape(40.dp)).padding(horizontal = 36.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val photo = state.passenger?.photo
            Box(Modifier.size(70.dp).clip(CircleShape).background(Color(0xFFDCDCDC))) {
                if (photo != null) Image(painterResource(photo), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.size(20.dp))
            Text(state.passengerName, color = AirColors.Text, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (!calm) {
                Image(painterResource(R.drawable.ic_warning), contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(Modifier.size(8.dp))
            }
            Text(
                if (calm) "편안함" else if (c.motionLevel == Level.DANGER) "높음" else "주의",
                color = color, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
            )
        }
        Box(Modifier.fillMaxWidth().height(16.dp).background(AirColors.Track, RoundedCornerShape(8.dp))) {
            Box(Modifier.fillMaxWidth(level.coerceIn(0.02f, 1f)).fillMaxHeight().background(color, RoundedCornerShape(8.dp)))
        }
    }
}

/** Rear seat with an airflow beam over a soft circle (Figma "대응+"). */
@Composable
private fun RearSeatAirflow() {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, 680.dp)
        Box(Modifier.size(side), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension * 0.49f
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(Color(0x73272727), r, center)
                drawCircle(
                    Brush.linearGradient(listOf(Color(0x33D9D9D9), Color(0x331D1E21)), Offset(size.width, size.height * 0.3f), Offset(size.width * 0.45f, size.height)),
                    r, center, style = Stroke(1.dp.toPx()),
                )
                // Airflow beam (Figma Vector 1, rotated −18.7°), from the left toward the seat back.
                val w = size.width * 0.96f
                val h = w * 339.227f / 641.41f
                val left = -size.width * 0.14f
                val top = size.height * 0.38f
                fun px(x: Float) = left + x / 641.41f * w
                fun py(y: Float) = top + y / 339.227f * h
                val path = Path().apply {
                    moveTo(px(307.4f), py(326.6f)); lineTo(px(0f), py(116.3f)); lineTo(px(17.7f), py(0f))
                    lineTo(px(641.4f), py(123.4f)); lineTo(px(541.9f), py(339.2f)); close()
                }
                rotate(-18.71f, Offset(px(320f), py(170f))) {
                    drawPath(path, Brush.linearGradient(listOf(Color(0x47D9D9D9), Color(0x00D9D9D9)), Offset(px(207f), py(-136f)), Offset(px(495f), py(303f))))
                }
            }
            Image(
                painterResource(R.drawable.rear_seat), contentDescription = null,
                modifier = Modifier.size(side * 0.92f).offset(x = side * 0.03f), contentScale = ContentScale.Fit,
            )
            // Left fade into the panel background.
            Box(
                Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(0.55f)
                    .background(Brush.horizontalGradient(listOf(AirColors.Panel, Color(0x001D1E21)))),
            )
        }
    }
}

@Composable
private fun WindChip() {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_wind_4step), contentDescription = null, modifier = Modifier.size(48.dp))
        Text("뒷좌석 바람 조절 중", color = AirColors.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}
