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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import com.cabincrew.airride.R
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cabincrew.airride.engine.Level
import com.cabincrew.airride.engine.Suggestion

/**
 * Driving (A-03~A-08): cabin view + 840dp app column (gauges + one suggestion, max 2 buttons).
 * Parked: cabin view + gauges + passenger settings + report entry.
 */
@Composable
fun HomeScreen(
    state: UiState,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    onOpenDemo: () -> Unit,
    onEndTrip: () -> Unit,
    onCare: (CareOptions) -> Unit,
) {
    Row(Modifier.fillMaxSize().background(AirColors.Bg).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        // Cabin side
        Column(
            Modifier.weight(1f).fillMaxHeight().background(AirColors.Panel, RoundedCornerShape(32.dp)).padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CabinTopView(if (state.passenger != null) SeatState.ACTIVE else SeatState.EMPTY)
            Spacer(Modifier.height(32.dp))
            Text(
                if (state.passenger != null) "뒷좌석 왼쪽 · ${state.passengerName}" else "운전자 단독 주행",
                color = if (state.passenger != null) AirColors.Passenger else AirColors.TextSub, fontSize = 26.sp,
            )
            Spacer(Modifier.height(16.dp))
            StatusChip(
                if (state.signals.recirculationOn) "내기순환" else "외기 유입",
                if (state.signals.recirculationOn) AirColors.Warn else AirColors.Ok,
            )
        }

        // App column (840dp per design)
        Column(
            Modifier.width(840.dp).fillMaxHeight().background(AirColors.Panel, RoundedCornerShape(32.dp)).padding(56.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Header(state, onOpenDemo)
            if (state.suggestion == Suggestion.CurveAhead) {
                CurveGuide(state)
            } else {
                Gauges(state)
                when {
                    state.appliedMessage != null -> AppliedCard(state.appliedMessage)
                    state.suggestion != null -> SuggestionFor(state, state.suggestion, onAccept, onDismiss)
                    !state.restricted -> ParkedExtras(state, onEndTrip, onCare)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Header(state: UiState, onOpenDemo: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // Long-press app name → hidden demo panel.
        Text(
            "에어라이드",
            color = AirColors.Text, fontSize = 36.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.combinedClickable(onClick = {}, onLongClick = onOpenDemo),
        )
        if (state.demoMode) {
            Spacer(Modifier.width(16.dp))
            Text("데모", color = AirColors.TextMuted, fontSize = 24.sp)
        }
        Spacer(Modifier.weight(1f))
        val (label, dot) = headerChip(state)
        StatusChip(label, dot)
    }
}

private fun headerChip(state: UiState): Pair<String, Color> = when (state.suggestion) {
    is Suggestion.Rest -> "휴식 권장" to AirColors.Warn
    Suggestion.FreshAir -> "환기 필요" to AirColors.Warn
    Suggestion.CurveAhead -> "커브 대비" to AirColors.Passenger
    null -> (if (state.signals.isDriving) "케어 중" else "정차") to AirColors.Ok
}

@Composable
private fun Gauges(state: UiState) {
    val c = state.condition
    val drive = state.signals.continuousDriveMinutes.toInt()
    ConditionGauge(
        label = "운전자 각성",
        status = when (c.alertnessLevel) { Level.GOOD -> "좋음"; Level.CAUTION -> "보통"; Level.DANGER -> "낮음" },
        statusColor = AirColors.DriverStatus,
        level = c.alertness / 100f,
        levelColor = AirColors.Driver,
        detail = "연속 주행 ${if (drive >= 60) "${drive / 60}시간 ${drive % 60}분" else "${drive}분"}",
    )
    if (state.passenger != null) PassengerGauge(state)
}

@Composable
private fun PassengerGauge(state: UiState, detail: String? = null) {
    val c = state.condition
    val good = c.motionLevel == Level.GOOD
    ConditionGauge(
        label = "${state.passengerName} 멀미",
        status = when (c.motionLevel) { Level.GOOD -> "편안함"; Level.CAUTION -> "주의"; Level.DANGER -> "높음" },
        statusColor = if (good) AirColors.Passenger else AirColors.Warn,
        level = c.motionSickness / 100f,
        levelColor = if (good) AirColors.Passenger else AirColors.Warn,
        detail = detail ?: if (state.profile.motionSensitive) "뒷좌석 왼쪽 · 멀미 민감" else "뒷좌석 왼쪽",
    )
}

/** A-05 커브 대응 — voice guide card, passenger gauge, rear airflow chip. No buttons while driving. */
@Composable
private fun CurveGuide(state: UiState) {
    Row(
        Modifier.fillMaxWidth().shadow(20.dp, RoundedCornerShape(48.dp))
            .background(AirColors.Card, RoundedCornerShape(48.dp)).padding(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_voice), contentDescription = null, modifier = Modifier.size(56.dp))
        }
        Text("곧 커브예요\n급가속과 급제동을 줄여주세요", color = AirColors.Text, fontSize = 36.sp, lineHeight = 46.sp, fontWeight = FontWeight.SemiBold)
    }
    PassengerGauge(state, detail = "뒷좌석 왼쪽 · 커브 구간")
    if (state.rearFanActive) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ic_wind_4step), contentDescription = null, modifier = Modifier.size(48.dp))
            Text("뒷좌석 바람 조절 중", color = AirColors.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SuggestionFor(state: UiState, s: Suggestion, onAccept: () -> Unit, onDismiss: () -> Unit) {
    val c = state.condition
    when (s) {
        Suggestion.FreshAir -> {
            SuggestionCard(
                "바깥 공기를 들일까요?",
                "내기순환이 ${state.signals.recircMinutes.toInt()}분째예요. 답답한 공기는 졸음을 불러요",
                "외기로 전환", onAccept, "지금은 유지", onDismiss,
            )
            Hint("추정 CO₂ ${c.estimatedCo2Ppm}ppm · 탑승 ${state.signals.occupants}명")
        }
        is Suggestion.Rest -> {
            SuggestionCard(
                "${s.restAreaName}에서 쉬어 갈까요?",
                reasonText(state),
                "경유지 추가", onAccept, "나중에", onDismiss,
            )
            Hint("약 ${s.distanceKm}km 앞 · 경로상 위치")
        }
        Suggestion.CurveAhead -> Unit
    }
}

private fun reasonText(state: UiState): String {
    val c = state.condition
    val a = if (c.alertnessLevel == Level.GOOD) "운전자 각성 좋음" else "운전자 각성 낮음"
    if (state.passenger == null) return a
    val m = if (c.motionLevel == Level.GOOD) "${state.passengerName} 편안함" else "${state.passengerName} 멀미 높음"
    return "$a · $m"
}

@Composable
private fun Hint(text: String) {
    Text(text, color = AirColors.TextMuted, fontSize = 26.sp)
}

/** A-07 적용 완료 */
@Composable
private fun AppliedCard(msg: String) {
    Row(
        Modifier.fillMaxWidth().background(AirColors.Notification, RoundedCornerShape(48.dp)).padding(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("✓", color = AirColors.Ok, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text(msg, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
    }
}

/** Parked (P) only: care options + report entry. */
@Composable
private fun ParkedExtras(state: UiState, onEndTrip: () -> Unit, onCare: (CareOptions) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        if (state.passenger != null) {
            Column(Modifier.fillMaxWidth().background(AirColors.Card, RoundedCornerShape(32.dp))) {
                ToggleRow("뒷좌석 바람 조절", "커브 구간 자동 적용", state.care.rearFan) { onCare(state.care.copy(rearFan = it)) }
                Box(Modifier.fillMaxWidth().height(2.dp).background(AirColors.Divider))
                ToggleRow("커브 전 음성 안내", "운전자에게 미리 알려요", state.care.curveVoice) { onCare(state.care.copy(curveVoice = it)) }
            }
            Spacer(Modifier.height(28.dp))
        }
        IviButton("주행 종료 리포트", onEndTrip, filled = true, modifier = Modifier.fillMaxWidth(), minHeight = 100.dp, radius = 32.dp, fontSize = 32)
    }
}
