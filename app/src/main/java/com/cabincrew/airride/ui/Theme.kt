package com.cabincrew.airride.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.cabincrew.airride.engine.Level

// Placeholder palette — Figma 시안 반영 예정.
object AirColors {
    val Bg = Color(0xFF0E1116)
    val Surface = Color(0xFF1A1F27)
    val SurfaceHigh = Color(0xFF242B35)
    val Text = Color(0xFFF2F4F7)
    val TextSub = Color(0xFF9AA4B2)
    val Accent = Color(0xFF4FC3F7)
    val Good = Color(0xFF4ADE80)
    val Caution = Color(0xFFFBBF24)
    val Danger = Color(0xFFF87171)

    fun of(level: Level) = when (level) {
        Level.GOOD -> Good
        Level.CAUTION -> Caution
        Level.DANGER -> Danger
    }
}

@Composable
fun AirRideTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = AirColors.Bg,
            surface = AirColors.Surface,
            primary = AirColors.Accent,
            onSurface = AirColors.Text,
            onBackground = AirColors.Text,
        ),
        content = content,
    )
}
