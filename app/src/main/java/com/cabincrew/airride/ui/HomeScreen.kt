package com.cabincrew.airride.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.border
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
    BoxWithConstraints(Modifier.fillMaxSize().background(AirColors.Bg)) {
        // Split with navigation (A-04~A-08: 840dp app panel next to the map) → app column only.
        // Full width → one panel: cabin view on the left, content on the right (same frame as A-01/A-09).
        val compact = maxWidth < 1300.dp
        Row(
            Modifier.fillMaxSize().padding(if (compact) 0.dp else 20.dp)
                .background(AirColors.Panel, RoundedCornerShape(32.dp))
                .padding(horizontal = if (compact) 56.dp else 72.dp, vertical = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(64.dp),
        ) {
            if (!compact) {
                Column(
                    Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CabinTopView(if (state.passenger != null) SeatState.ACTIVE else SeatState.EMPTY)
                    Spacer(Modifier.height(28.dp))
                    Text(
                        if (state.passenger != null) "뒷좌석 왼쪽 · ${state.passengerName}" else "운전자 단독 주행",
                        color = if (state.passenger != null) AirColors.Passenger else AirColors.TextSub, fontSize = 24.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    StatusChip(
                        if (state.signals.recirculationOn) "내기순환" else "외기 유입",
                        if (state.signals.recirculationOn) AirColors.Warn else AirColors.Ok,
                    )
                }
            }
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                if (state.curveStage != null) {
                    // A-05 has no header (Figma); long-press anywhere in the flow still opens the demo panel.
                    CurveFlow(state, onOpenDemo)
                } else {
                    Header(state, onOpenDemo)
                    Gauges(state)
                    when {
                        state.appliedMessage != null -> AppliedCard(state.appliedMessage)
                        state.suggestion is Suggestion.Rest && state.restIntro -> RestIntro(state.suggestion)
                        state.suggestion != null -> SuggestionFor(state, state.suggestion, onAccept, onDismiss)
                        !state.restricted -> ParkedExtras(state, onEndTrip, onCare)
                    }
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


/** A-08 (프로토타입) 첫 단계: Gleo orb + notice without buttons. */
@Composable
private fun RestIntro(s: Suggestion.Rest) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Image(painterResource(R.drawable.gleo_orb), contentDescription = null, modifier = Modifier.size(72.dp).blur(1.25.dp))
        Column(
            Modifier.fillMaxWidth().shadow(20.dp, RoundedCornerShape(48.dp))
                .background(AirColors.Notification, RoundedCornerShape(48.dp))
                .border(1.dp, AirColors.DividerSoft, RoundedCornerShape(48.dp))
                .padding(horizontal = 40.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("피로가 쌓인 것 같아요. 잠시 쉬어 갈까요?", color = Color.White, fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold)
            Text("약 ${restEtaMinutes(s)}분 후 경로에 ${s.restAreaName}가 있어요", color = AirColors.TextTertiary, fontSize = 26.sp, lineHeight = 34.sp)
        }
    }
}

@Composable
private fun SuggestionFor(state: UiState, s: Suggestion, onAccept: () -> Unit, onDismiss: () -> Unit) {
    val copy = suggestionCopy(s, state) ?: return
    SuggestionCard(copy.title, copy.body, copy.primary, onAccept, copy.secondary, onDismiss)
    when (s) {
        Suggestion.FreshAir -> Hint("추정 CO₂ ${state.condition.estimatedCo2Ppm}ppm · 탑승 ${state.signals.occupants}명")
        is Suggestion.Rest -> Hint(reasonText(state))
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
    val care = state.care
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().background(AirColors.Card, RoundedCornerShape(32.dp))) {
            if (state.passenger != null) {
                ToggleRow("뒷좌석 바람 조절", "커브 구간 자동 적용", care.rearFan, compact = true) { onCare(care.copy(rearFan = it)) }
                Divider()
                ToggleRow("커브 전 음성 안내", "운전자에게 미리 알려요", care.curveVoice, compact = true) { onCare(care.copy(curveVoice = it)) }
                Divider()
            }
            ToggleRow("운전석 시원하게", "외기로 바꿀 때 1.5℃ 낮춰요", care.coolDriver, compact = true) { onCare(care.copy(coolDriver = it)) }
            Divider()
            ToggleRow("미세먼지 모드", "외기를 2분만 열고 다시 닫아요", care.briefFreshAir, compact = true) { onCare(care.copy(briefFreshAir = it)) }
        }
        Spacer(Modifier.height(24.dp))
        IviButton("주행 종료 리포트", onEndTrip, filled = true, modifier = Modifier.fillMaxWidth(), minHeight = 96.dp, radius = 32.dp, fontSize = 32)
    }
}

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().height(2.dp).background(AirColors.Divider))
