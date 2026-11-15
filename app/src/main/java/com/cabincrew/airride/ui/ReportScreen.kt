package com.cabincrew.airride.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cabincrew.airride.report.TripReport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A-10 주행 종료 리포트 (정차 중에만 표시). */
@Composable
fun ReportScreen(report: TripReport, onClose: () -> Unit) {
    val fmt = SimpleDateFormat("HH:mm", Locale.KOREA)
    Row(
        Modifier.fillMaxSize().background(AirColors.Bg).padding(32.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("오늘의 주행", color = AirColors.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Stat("부드러움 점수", "${report.smoothnessScore}점")
            Stat("외기 전환", "${report.freshAirSwitches}회")
            Stat("차가 대신한 케어", "${report.careCount}회")
            Spacer(Modifier.weight(1f))
            BigButton("확인", onClose, AirColors.Accent, AirColors.Bg, Modifier.fillMaxWidth())
        }
        Column(
            Modifier.weight(1f).fillMaxSize()
                .background(AirColors.Surface, RoundedCornerShape(28.dp)).padding(28.dp),
        ) {
            Text("케어 기록", color = AirColors.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            if (report.events.isEmpty()) {
                Text("이번 주행에는 케어가 필요 없었어요", color = AirColors.TextSub, fontSize = 20.sp)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(report.events) { e ->
                    Row {
                        Text(fmt.format(Date(e.timeMillis)), color = AirColors.TextSub, fontSize = 20.sp)
                        Text("  ${e.type.label}", color = AirColors.Accent, fontSize = 20.sp)
                        Text("  ${e.detail}", color = AirColors.Text, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp)
            .background(AirColors.Surface, RoundedCornerShape(20.dp)).padding(24.dp),
    ) {
        Text(label, color = AirColors.TextSub, fontSize = 24.sp, modifier = Modifier.weight(1f))
        Text(value, color = AirColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}
