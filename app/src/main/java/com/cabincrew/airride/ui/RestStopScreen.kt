package com.cabincrew.airride.ui

import androidx.compose.foundation.background
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
            Steps(state.restStep)
            when (state.restStep) {
                RestStep.VENTILATE -> {
                    SuggestionCard("실내를 환기할까요?", "${name}에 정차했어요", "환기하기", { onVentilate(true) }, "건너뛰기", { onVentilate(false) })
                    Text("외기로 전환해 실내 공기를 바꿔요", color = AirColors.TextSub, fontSize = 26.sp)
                    Text("환기 후 잠시 쉬면 두 사람의 컨디션을 다시 확인해 드릴게요", color = AirColors.TextMuted, fontSize = 26.sp)
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
