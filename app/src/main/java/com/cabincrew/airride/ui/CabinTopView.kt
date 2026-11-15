package com.cabincrew.airride.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class SeatState { EMPTY, PENDING, ACTIVE }

/** AirRide/Cabin Top View — driver seat (blue) and rear-left passenger seat (magenta; dashed while asking). */
@Composable
fun CabinTopView(passenger: SeatState, modifier: Modifier = Modifier) {
    Box(
        modifier.size(280.dp, 540.dp)
            .background(Color(0xFF16171A), RoundedCornerShape(140.dp))
            .border(3.dp, Color(0xFF34363C), RoundedCornerShape(140.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(28.dp), modifier = Modifier.padding(top = 20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Seat(border = AirColors.Driver, fill = Color(0xFF1B2640))
                Seat()
            }
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                when (passenger) {
                    SeatState.ACTIVE -> Seat(border = AirColors.Passenger, fill = Color(0xFF2E1A33))
                    SeatState.PENDING -> Seat(border = AirColors.Passenger, fill = Color(0xFF26272C), dashed = true)
                    SeatState.EMPTY -> Seat()
                }
                Seat()
            }
        }
    }
}

@Composable
private fun Seat(border: Color? = null, fill: Color = Color(0xFF2A2B30), dashed: Boolean = false) {
    val shape = RoundedCornerShape(18.dp)
    var m = Modifier.size(90.dp, 160.dp).background(fill, shape)
    if (border != null) {
        m = if (dashed) {
            m.drawBehind {
                drawRoundRect(
                    border, cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                )
            }
        } else m.border(4.dp, border, shape)
    }
    Box(m)
}
