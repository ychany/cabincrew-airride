package com.cabincrew.airride.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cabincrew.airride.engine.Level
import com.cabincrew.airride.engine.Suggestion

/** A-03 상시 화면 + A-06/A-08/A-05 제안 카드 + A-07 적용 완료. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: UiState,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    onOpenDemo: () -> Unit,
    onEndTrip: () -> Unit,
    onMotionSensitive: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(AirColors.Bg).padding(32.dp)) {
        // Top bar: long-press app name → hidden demo panel.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "AirRide",
                color = AirColors.Text,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.combinedClickable(onClick = {}, onLongClick = onOpenDemo),
            )
            Spacer(Modifier.width(20.dp))
            StatusChip(if (state.signals.isDriving) "주행 중" else "정차")
            Spacer(Modifier.width(10.dp))
            if (state.demoMode) StatusChip("데모 모드", AirColors.Caution)
            else StatusChip("차량 연결됨", AirColors.Good)
            Spacer(Modifier.width(10.dp))
            StatusChip(if (state.signals.recirculationOn) "내기순환" else "외기", AirColors.Accent)
        }
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            // Gauges (always visible)
            Row(
                Modifier.weight(1f).fillMaxHeight()
                    .background(AirColors.Surface, RoundedCornerShape(28.dp)).padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val c = state.condition
                ConditionGauge("운전석 각성", c.alertness, c.alertnessLevel, alertText(c.alertnessLevel))
                ConditionGauge("동승석 멀미", c.motionSickness, c.motionLevel, motionText(c.motionLevel))
            }

            // Right column: suggestion / applied / parked-only extras
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                when {
                    state.appliedMessage != null -> AppliedCard(state.appliedMessage)
                    state.suggestion != null -> SuggestionFor(state.suggestion, onAccept, onDismiss)
                    state.restricted -> CalmCard()
                    else -> ParkedPanel(state, onEndTrip, onMotionSensitive)
                }
            }
        }
    }
}

@Composable
private fun SuggestionFor(s: Suggestion, onAccept: () -> Unit, onDismiss: () -> Unit) {
    val m = Modifier.fillMaxWidth()
    when (s) {
        Suggestion.FreshAir -> SuggestionCard(
            "바깥 공기로 바꿀까요?", "실내 공기가 답답해지고 있어요",
            "외기로 바꾸기", onAccept, "나중에", onDismiss, m,
        )
        is Suggestion.Rest -> SuggestionCard(
            "${s.restAreaName}에서 쉬어 갈까요?", "${s.distanceKm}km 앞이에요",
            "경유지 추가", onAccept, "나중에", onDismiss, m,
        )
        Suggestion.CurveAhead -> SuggestionCard(
            "곧 커브예요", "뒷자리를 위해 부드럽게 돌아가요",
            "알겠어요", onAccept, "닫기", onDismiss, m,
        )
    }
}

@Composable
private fun AppliedCard(msg: String) {
    Column(
        Modifier.fillMaxWidth().background(AirColors.Surface, RoundedCornerShape(28.dp)).padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("✓", color = AirColors.Good, fontSize = 64.sp)
        Spacer(Modifier.height(8.dp))
        Text(msg, color = AirColors.Text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CalmCard() {
    Column(
        Modifier.fillMaxWidth().background(AirColors.Surface, RoundedCornerShape(28.dp)).padding(40.dp),
    ) {
        Text("편안하게 가고 있어요", color = AirColors.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("필요할 때 먼저 알려 드릴게요", color = AirColors.TextSub, fontSize = 22.sp)
    }
}

/** Parked (P) only: passenger profile, end-trip report. */
@Composable
private fun ParkedPanel(state: UiState, onEndTrip: () -> Unit, onMotionSensitive: (Boolean) -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(AirColors.Surface, RoundedCornerShape(28.dp)).padding(32.dp),
    ) {
        Text("동승자 설정", color = AirColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("멀미가 잦아요", color = AirColors.Text, fontSize = 22.sp)
                Text("커브·가감속에 더 일찍 반응해요", color = AirColors.TextSub, fontSize = 18.sp)
            }
            Switch(checked = state.profile.motionSensitive, onCheckedChange = onMotionSensitive)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "추정 CO₂ ${state.condition.estimatedCo2Ppm}ppm · 연속 주행 ${state.signals.continuousDriveMinutes.toInt()}분",
            color = AirColors.TextSub, fontSize = 18.sp,
        )
        Spacer(Modifier.height(24.dp))
        BigButton("주행 종료 리포트", onEndTrip, AirColors.Accent, AirColors.Bg, Modifier.fillMaxWidth())
    }
}

private fun alertText(l: Level) = when (l) {
    Level.GOOD -> "좋음"; Level.CAUTION -> "주의"; Level.DANGER -> "위험"
}

private fun motionText(l: Level) = when (l) {
    Level.GOOD -> "편안"; Level.CAUTION -> "주의"; Level.DANGER -> "위험"
}
