package com.cabincrew.airride.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cabincrew.airride.report.CareType
import com.cabincrew.airride.report.TripReport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A-10 주행 종료 리포트 (parked only). */
@Composable
fun ReportScreen(report: TripReport, onClose: () -> Unit) {
    val fmt = SimpleDateFormat("HH:mm", Locale.KOREA)
    val minutes = ((report.endMillis - report.startMillis) / 60_000).coerceAtLeast(1)
    BoxWithConstraints(Modifier.fillMaxSize().background(AirColors.Bg)) {
    if (maxWidth < 1300.dp) {
        // Split with navigation: single scrolling column.
        Column(
            Modifier.fillMaxSize().background(AirColors.Panel, RoundedCornerShape(32.dp))
                .verticalScroll(rememberScrollState()).padding(56.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            ReportSummary(report, fmt, minutes)
            CareLog(report, fmt, onClose)
        }
        return@BoxWithConstraints
    }
    Row(
        Modifier.fillMaxSize().padding(20.dp)
            .background(AirColors.Panel, RoundedCornerShape(32.dp))
            .padding(horizontal = 96.dp, vertical = 72.dp),
        horizontalArrangement = Arrangement.spacedBy(64.dp),
    ) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            ReportSummary(report, fmt, minutes)
        }
        Column(Modifier.width(500.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            CareLog(report, fmt, onClose, Modifier.weight(1f))
        }
    }
    }
}

@Composable
private fun ReportSummary(report: TripReport, fmt: SimpleDateFormat, minutes: Long) {
    StatusChip("주행 종료 리포트", AirColors.Ok)
    Text(
        if (report.smoothnessScore >= 70) "동승자가 멀미 없이 도착했어요" else "오늘은 조금 흔들렸어요",
        color = AirColors.Text, fontSize = 52.sp, fontWeight = FontWeight.Bold,
    )
    Text(
        "${fmt.format(Date(report.startMillis))} → ${fmt.format(Date(report.endMillis))} · ${minutes}분",
        color = AirColors.TextMuted, fontSize = 28.sp,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatCard("부드러움 점수", "${report.smoothnessScore}", Modifier.weight(1f))
        StatCard("외기 전환", "${report.freshAirSwitches}회", Modifier.weight(1f))
        StatCard("차가 대신한 케어", "${report.careCount}회", Modifier.weight(1f))
    }
    ConditionChart(report)
}

@Composable
private fun CareLog(report: TripReport, fmt: SimpleDateFormat, onClose: () -> Unit, listModifier: Modifier = Modifier) {
    Text("차가 대신한 케어", color = AirColors.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
    Column(listModifier) {
        if (report.events.isEmpty()) {
            Text("이번 주행에는 케어가 필요 없었어요", color = AirColors.TextMuted, fontSize = 26.sp, modifier = Modifier.padding(vertical = 18.dp))
        }
        report.events.forEach { e ->
            CareLogRow(fmt.format(Date(e.timeMillis)), e.detail, careColor(e.type))
        }
    }
    IviButton("닫기", onClose, filled = true, modifier = Modifier.fillMaxWidth(), minHeight = 100.dp, radius = 32.dp, fontSize = 32)
}

private fun careColor(t: CareType) = when (t) {
    CareType.FRESH_AIR -> AirColors.Ok
    CareType.CURVE_GUIDE -> AirColors.Warn
    CareType.REST -> AirColors.Driver
    CareType.CABIN_ADJUST -> AirColors.Passenger
}

/** 구간별 컨디션 — recorded alertness (blue) / motion sickness (orange), dashed lines at care events. */
@Composable
private fun ConditionChart(report: TripReport) {
    Column(
        Modifier.fillMaxWidth().background(AirColors.Card, RoundedCornerShape(40.dp)).padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Text("구간별 컨디션", color = AirColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Medium)
            LegendKey(AirColors.Driver, "운전자 각성")
            LegendKey(AirColors.Warn, "동승자 멀미")
        }
        val samples = report.samples
        if (samples.size < 2) {
            Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                Text("주행 기록이 쌓이면 그래프가 표시돼요", color = AirColors.TextMuted, fontSize = 24.sp)
            }
            return@Column
        }
        val t0 = samples.first().timeMillis
        val span = (samples.last().timeMillis - t0).coerceAtLeast(1).toFloat()
        Canvas(Modifier.fillMaxWidth().height(260.dp)) {
            fun x(t: Long) = (t - t0) / span * size.width
            fun y(v: Int) = size.height * (1f - v / 100f)
            report.events.forEach { e ->
                if (e.timeMillis in t0..samples.last().timeMillis) {
                    drawLine(
                        Color(0x66FFFFFF), Offset(x(e.timeMillis), 0f), Offset(x(e.timeMillis), size.height),
                        strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                    )
                }
            }
            fun line(color: Color, value: (com.cabincrew.airride.report.ConditionSample) -> Int) {
                val p = Path()
                samples.forEachIndexed { i, s ->
                    if (i == 0) p.moveTo(x(s.timeMillis), y(value(s))) else p.lineTo(x(s.timeMillis), y(value(s)))
                }
                drawPath(p, color, style = Stroke(width = 4.dp.toPx()))
            }
            line(AirColors.Driver) { it.alertness }
            line(AirColors.Warn) { it.motionSickness }
        }
        Row(Modifier.fillMaxWidth()) {
            Text("출발", color = AirColors.TextMuted, fontSize = 22.sp)
            Spacer(Modifier.weight(1f))
            Text("도착", color = AirColors.TextMuted, fontSize = 22.sp)
        }
    }
}

@Composable
private fun LegendKey(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(28.dp, 6.dp).background(color, RoundedCornerShape(3.dp)))
        Text(label, color = AirColors.TextMuted, fontSize = 24.sp)
    }
}
