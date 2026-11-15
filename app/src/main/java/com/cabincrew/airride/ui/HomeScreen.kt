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
import androidx.compose.material3.SwitchDefaults
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
    onMotionSensitive: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxSize().background(AirColors.Bg).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        // Cabin side
        Column(
            Modifier.weight(1f).fillMaxHeight().background(AirColors.Panel, RoundedCornerShape(32.dp)).padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CabinTopView()
            Spacer(Modifier.height(32.dp))
            Text("뒷좌석 왼쪽 · 동승자", color = AirColors.TextSub, fontSize = 26.sp)
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
            Gauges(state)
            when {
                state.appliedMessage != null -> AppliedCard(state.appliedMessage)
                state.suggestion != null -> SuggestionFor(state, state.suggestion, onAccept, onDismiss)
                !state.restricted -> ParkedExtras(state, onEndTrip, onMotionSensitive)
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
    ConditionGauge(
        label = "동승자 멀미",
        status = when (c.motionLevel) { Level.GOOD -> "편안함"; Level.CAUTION -> "주의"; Level.DANGER -> "높음" },
        statusColor = if (c.motionLevel == Level.GOOD) AirColors.Passenger else AirColors.Warn,
        level = c.motionSickness / 100f,
        levelColor = AirColors.Passenger,
        detail = if (state.profile.motionSensitive) "뒷좌석 왼쪽 · 멀미 민감" else "뒷좌석 왼쪽",
    )
}

@Composable
private fun SuggestionFor(state: UiState, s: Suggestion, onAccept: () -> Unit, onDismiss: () -> Unit) {
    val c = state.condition
    when (s) {
        Suggestion.FreshAir -> {
            SuggestionCard("외기로 바꿀까요?", "내기순환이 오래 유지됐어요", "바꾸기", onAccept, "지금은 유지", onDismiss)
            Hint("추정 CO₂ ${c.estimatedCo2Ppm}ppm · 탑승 ${state.signals.occupants}명")
        }
        is Suggestion.Rest -> {
            SuggestionCard(
                "${s.restAreaName}에서 쉬어 갈까요?",
                reasonText(c),
                "경유지 추가", onAccept, "나중에", onDismiss,
            )
            Hint("약 ${s.distanceKm}km 앞 · 경로상 위치")
        }
        Suggestion.CurveAhead -> {
            SuggestionCard("곧 커브예요", "뒷자리를 위해 부드럽게 돌아가요", "알겠어요", onAccept, "닫기", onDismiss)
            Hint("동승자 멀미 ${c.motionSickness} · 전방 커브 구간")
        }
    }
}

private fun reasonText(c: com.cabincrew.airride.engine.Condition): String {
    val a = if (c.alertnessLevel == Level.GOOD) "운전자 각성 좋음" else "운전자 각성 낮음"
    val m = if (c.motionLevel == Level.GOOD) "동승자 편안함" else "동승자 멀미 높음"
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

/** Parked (P) only: passenger profile (Care Row) + report entry. */
@Composable
private fun ParkedExtras(state: UiState, onEndTrip: () -> Unit, onMotionSensitive: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        CareRow(
            title = "멀미가 잦은 동승자",
            sub = "커브·가감속에 더 일찍 반응해요",
            checked = state.profile.motionSensitive,
            onChecked = onMotionSensitive,
        )
        Spacer(Modifier.height(28.dp))
        IviButton("주행 종료 리포트", onEndTrip, filled = true, modifier = Modifier.fillMaxWidth(), minHeight = 100.dp, radius = 32.dp, fontSize = 32)
    }
}

/** AirRide/Care Row */
@Composable
private fun CareRow(title: String, sub: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = AirColors.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(sub, color = AirColors.TextMuted, fontSize = 24.sp)
            }
            Switch(
                checked = checked, onCheckedChange = onChecked,
                colors = SwitchDefaults.colors(checkedTrackColor = AirColors.Ok, checkedThumbColor = Color.White),
            )
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(AirColors.Divider))
    }
}
