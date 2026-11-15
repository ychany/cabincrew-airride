package com.cabincrew.airride.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.cabincrew.airride.R

/** Parked wide layout: cabin top view on the left, content on the right (A-01, A-02, A-09). */
@Composable
fun ParkedFrame(seat: SeatState, caption: @Composable () -> Unit, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(AirColors.Bg)) {
        val compact = maxWidth < 1300.dp
        Row(
            Modifier.fillMaxSize().padding(if (compact) 0.dp else 20.dp)
                .background(AirColors.Panel, RoundedCornerShape(32.dp))
                .then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = if (compact) 56.dp else 72.dp, vertical = 72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(64.dp),
        ) {
            if (!compact) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    CabinTopView(seat)
                    caption()
                }
            }
            Box(Modifier.weight(1f)) { content() }
        }
    }
}

/** A-01 탑승 확인 */
@Composable
fun BoardingWhoScreen(onChoose: (Passenger?) -> Unit) {
    ParkedFrame(
        seat = SeatState.PENDING,
        caption = { Text("뒷좌석 왼쪽", color = AirColors.Passenger, fontSize = 24.sp) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(40.dp)) {
            Heading("뒷좌석 왼쪽에 누가 탔나요?", "함께 탄 사람에 맞춰 케어를 준비할게요.")
            val tiles: List<Pair<String, Pair<Int?, () -> Unit>>> =
                SAMPLE_PASSENGERS.map { p -> (if (p.photo == null) "다른 동승자" else p.name) to (p.photo to { onChoose(p) }) } +
                    listOf("아무도\n타지 않았어요" to (null to { onChoose(null) }))
            BoxWithConstraints {
                val perRow = if (maxWidth < 900.dp) 2 else 4
                Column(Modifier.widthIn(max = 1202.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    tiles.chunked(perRow).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            row.forEach { (label, v) ->
                                PassengerTile(label, v.first, Modifier.weight(1f).aspectRatio(1f), v.second)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PassengerTile(label: String, photo: Int?, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(15.dp)).background(AirColors.Card).clickableNoRipple(onClick),
    ) {
        if (photo != null) {
            Image(painterResource(photo), contentDescription = label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Box(
                Modifier.align(Alignment.Center).padding(bottom = 40.dp).size(100.dp)
                    .background(AirColors.ButtonBasic, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_image), contentDescription = null, modifier = Modifier.size(60.dp))
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(114.dp)
                .background(Brush.verticalGradient(listOf(Color(0x003A3C42), AirColors.Card))),
        )
        Text(
            label, color = AirColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        )
    }
}

/** A-02 동승 모드 — choose care items for this trip. */
@Composable
fun BoardingCareScreen(
    passenger: Passenger,
    care: CareOptions,
    onCare: (CareOptions) -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    ParkedFrame(
        seat = SeatState.ACTIVE,
        caption = {},
    ) {
        Column(Modifier.widthIn(max = 1132.dp), verticalArrangement = Arrangement.spacedBy(36.dp)) {
            Heading("${passenger.name}님을 위해 이렇게 케어할게요", "이번 주행에 사용할 항목을 선택해 주세요.")
            Column(Modifier.fillMaxWidth().background(AirColors.Card, RoundedCornerShape(32.dp))) {
                ToggleRow("뒷좌석 바람 조절", "시작하면 바람을 부드럽게 조절해요", care.rearFan) { onCare(care.copy(rearFan = it)) }
                Box(Modifier.fillMaxWidth().height(2.dp).background(AirColors.Divider))
                ToggleRow("커브 전 음성 안내", "커브에 진입하기 전 운전자에게 알려드려요", care.curveVoice) { onCare(care.copy(curveVoice = it)) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IviButton("이전", onBack, filled = false, modifier = Modifier.weight(1f), minHeight = 100.dp, radius = 32.dp, fontSize = 32)
                IviButton("케어 시작", onStart, filled = true, modifier = Modifier.weight(1f), minHeight = 100.dp, radius = 32.dp, fontSize = 32)
            }
        }
    }
}

@Composable
fun Heading(title: String, sub: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, color = AirColors.Text, fontSize = 56.sp, fontWeight = FontWeight.Bold)
        Text(sub, color = AirColors.TextMuted, fontSize = 30.sp)
    }
}

@Composable
fun ToggleRow(title: String, sub: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 28.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = AirColors.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = AirColors.TextMuted, fontSize = 24.sp)
        }
        Spacer(Modifier.width(24.dp))
        Switch(
            checked = checked, onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedTrackColor = AirColors.Ok, checkedThumbColor = Color.White),
        )
    }
}
