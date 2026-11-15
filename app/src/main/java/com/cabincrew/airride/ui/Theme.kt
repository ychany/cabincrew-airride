package com.cabincrew.airride.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Tokens from the AirRide Figma (Pleos Connect design assets). 1px = 1dp on the 160dpi IVI display. */
object AirColors {
    val Bg = Color(0xFF131417)
    val Panel = Color(0xFF1D1E21)          // AirRide App panel
    val Card = Color(0xFF3A3C42)           // gauge / stat / chip
    val Track = Color(0xFF26282D)
    val Notification = Color(0xFF44464E)   // Pleos notification surface
    val Divider = Color(0xFF3A3D43)
    val DividerSoft = Color(0x1AFFFFFF)

    val Text = Color(0xFFF2F3F5)
    val TextSub = Color(0xFFC7C9CE)
    val TextMuted = Color(0xFFA3A6AD)
    val TextTertiary = Color(0xA3FFFFFF)

    val Driver = Color(0xFF4C8DFF)
    val DriverStatus = Color(0xFF8FB6FF)
    val Passenger = Color(0xFFE962FF)
    val Warn = Color(0xFFFF9F43)
    val Ok = Color(0xFF5BD18A)

    val ButtonFilled = Color(0xFFE0E1E6)
    val ButtonFilledText = Color(0xFF131417)
    val ButtonBasic = Color(0x0DFFFFFF)
}

@Composable
fun AirRideTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = AirColors.Bg,
            surface = AirColors.Panel,
            primary = AirColors.Driver,
            onSurface = AirColors.Text,
            onBackground = AirColors.Text,
        ),
        content = content,
    )
}
