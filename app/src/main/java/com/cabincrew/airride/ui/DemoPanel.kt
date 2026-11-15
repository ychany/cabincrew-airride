package com.cabincrew.airride.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cabincrew.airride.data.DemoVehicleSource
import com.cabincrew.airride.data.VehicleSignals

/** Hidden demo panel (long-press "AirRide"). Reproduces scenarios on the emulator. */
@Composable
fun DemoPanel(
    signals: VehicleSignals,
    onScenario: (DemoVehicleSource.Scenario) -> Unit,
    onChange: ((VehicleSignals) -> VehicleSignals) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.width(460.dp).fillMaxHeight()
            .background(AirColors.Card, RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("데모 패널", color = AirColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("닫기", color = AirColors.DriverStatus, fontSize = 20.sp, modifier = Modifier.padding(8.dp).clickableNoRipple(onClose))
        }
        Spacer(Modifier.height(12.dp))
        Text("시나리오", color = AirColors.TextSub, fontSize = 16.sp)
        DemoVehicleSource.Scenario.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { sc -> FilterChip(selected = false, onClick = { onScenario(sc) }, label = { Text(sc.label) }) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Toggle("주행 중", signals.isDriving) { v -> onChange { it.copy(isDriving = v) } }
        Toggle("내기순환", signals.recirculationOn) { v -> onChange { it.copy(recirculationOn = v) } }
        Toggle("전방 커브", signals.curveAhead) { v -> onChange { it.copy(curveAhead = v) } }
        Toggle("차선 경고(5분 내)", signals.laneWarningRecent) { v -> onChange { it.copy(laneWarningRecent = v) } }
        SliderRow("내기순환 시간", signals.recircMinutes, 0f..60f, "분") { v -> onChange { it.copy(recircMinutes = v) } }
        SliderRow("탑승 인원", signals.occupants.toFloat(), 1f..5f, "명", steps = 3) { v -> onChange { it.copy(occupants = v.toInt()) } }
        SliderRow("조향 미세 보정", signals.steeringCorrectionsPerMin, 0f..12f, "회/분") { v -> onChange { it.copy(steeringCorrectionsPerMin = v) } }
        SliderRow("가감속 변화", signals.accelChange, 0f..5f, "m/s²") { v -> onChange { it.copy(accelChange = v) } }
        SliderRow("좌우 흔들림", signals.lateralSway, 0f..5f, "m/s²") { v -> onChange { it.copy(lateralSway = v) } }
        SliderRow("연속 주행", signals.continuousDriveMinutes, 0f..200f, "분") { v -> onChange { it.copy(continuousDriveMinutes = v) } }
    }
}

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AirColors.Text, fontSize = 18.sp, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(
    label: String, value: Float, range: ClosedFloatingPointRange<Float>, unit: String,
    steps: Int = 0, onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(top = 6.dp)) {
        Row {
            Text(label, color = AirColors.Text, fontSize = 18.sp, modifier = Modifier.weight(1f))
            Text("%.1f %s".format(value, unit), color = AirColors.TextSub, fontSize = 16.sp)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
    Spacer(Modifier.width(0.dp))
}
