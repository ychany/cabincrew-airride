package com.cabincrew.airride.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import com.cabincrew.airride.R
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import com.cabincrew.airride.engine.Level

/** A-09 휴게소 도착: 정차 확인 → 환기 → 쉰 뒤 상태 확인 */
@Composable
fun RestStopScreen(state: UiState, onVentilate: (Boolean) -> Unit, onDone: () -> Unit) {
    val name = state.restAreaName ?: "휴게소"
    ParkedFrame(
        seat = if (state.passenger != null) SeatState.ACTIVE else SeatState.EMPTY,
        caption = { StatusChip("$name · 정차 중", AirColors.TextMuted) },
    ) {
        Column(Modifier.widthIn(max = 1124.dp), verticalArrangement = Arrangement.spacedBy(36.dp)) {
            when (state.restStep) {
                RestStep.VENTILATE -> {
                    VentilateCard({ onVentilate(true) }, { onVentilate(false) })
                    Text("외기로 3분간 환기해요", color = AirColors.TextMuted, fontSize = 26.sp)
                }
                RestStep.RECHECK -> {
                    val c = state.condition
                    ConditionGauge(
                        "운전자 각성", when (c.alertnessLevel) { Level.GOOD -> "좋음"; Level.CAUTION -> "보통"; Level.DANGER -> "낮음" },
                        AirColors.DriverStatus, c.alertness / 100f, AirColors.Driver, "추정 CO₂ ${c.estimatedCo2Ppm}ppm",
                    )
                    IviButton("다시 출발하기", onDone, filled = true, modifier = Modifier.fillMaxWidth(), minHeight = 100.dp, radius = 32.dp, fontSize = 32)
                }
            }
        }
    }
}

/** A-09 (프로토타입): wind icon + "실내를 환기할까요?" with two buttons. */
@Composable
private fun VentilateCard(onYes: () -> Unit, onNo: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().shadow(20.dp, RoundedCornerShape(48.dp))
            .background(AirColors.Notification, RoundedCornerShape(48.dp))
            .border(1.dp, AirColors.DividerSoft, RoundedCornerShape(48.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 40.dp, end = 40.dp, top = 36.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(painterResource(R.drawable.ic_wind_4step), contentDescription = null, modifier = Modifier.size(48.dp))
            Text("실내를 환기할까요?", color = Color.White, fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold)
        }
        Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IviButton("환기하기", onYes, filled = true, modifier = Modifier.weight(1f))
            IviButton("건너뛰기", onNo, filled = false, modifier = Modifier.weight(1f))
        }
    }
}

@Suppress("unused")
@Composable
private fun Steps(step: RestStep) {
    val current = if (step == RestStep.VENTILATE) 2 else 3
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf("정차 확인", "환기", "쉰 뒤 상태 확인").forEachIndexed { i, label ->
            val n = i + 1
            if (i > 0) Box(Modifier.width(48.dp).height(3.dp).background(Color(0xFF4A4C52)))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                when {
                    n < current -> Box(Modifier.size(48.dp).background(AirColors.Ok, CircleShape), contentAlignment = Alignment.Center) {
                        Text("✓", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                    n == current -> Box(Modifier.size(48.dp).border(3.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                        Text("$n", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    else -> Box(Modifier.size(48.dp).background(AirColors.Card, CircleShape), contentAlignment = Alignment.Center) {
                        Text("$n", color = AirColors.TextMuted, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(label, color = if (n <= current) AirColors.Text else AirColors.TextMuted, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
