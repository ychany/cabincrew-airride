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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** AirRide/Cabin Top View — driver seat (blue) and cared passenger seat (magenta, rear-left). */
@Composable
fun CabinTopView(modifier: Modifier = Modifier, passengerActive: Boolean = true) {
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
                if (passengerActive) Seat(border = AirColors.Passenger, fill = Color(0xFF2E1A33)) else Seat()
                Seat()
            }
        }
    }
}

@Composable
private fun Seat(border: Color? = null, fill: Color = Color(0xFF2A2B30)) {
    val shape = RoundedCornerShape(18.dp)
    val base = Modifier.size(90.dp, 160.dp).background(fill, shape)
    Box(if (border != null) base.border(4.dp, border, shape) else base)
}
