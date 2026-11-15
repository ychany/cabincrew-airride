package com.cabincrew.airride.ui

import com.cabincrew.airride.engine.Suggestion

/** Shared copy for the in-app card and the heads-up notification (A-06 / A-08). */
data class SuggestionCopy(val title: String, val body: String, val primary: String, val secondary: String)

fun suggestionCopy(s: Suggestion, state: UiState): SuggestionCopy? = when (s) {
    Suggestion.FreshAir -> SuggestionCopy(
        "바깥 공기를 들일까요?",
        "내기순환이 ${state.signals.recircMinutes.toInt()}분째 유지되고 있어요",
        "외기로 전환", "지금은 유지",
    )
    is Suggestion.Rest -> SuggestionCopy(
        "${s.restAreaName}에서 잠시 쉬어 갈까요?",
        "약 ${restEtaMinutes(s)}분 후 현재 경로에 있어요",
        "경유지 추가", "나중에",
    )
    Suggestion.CurveAhead -> null
}

/** Demo assumption: ~90 km/h on the highway. */
fun restEtaMinutes(s: Suggestion.Rest) = (s.distanceKm * 60 / 90).coerceAtLeast(1)

/** Korean particle 와/과 for "○○와 함께한" — 민준 → 민준이와, 서아 → 서아와. */
fun withParticle(name: String): String {
    val c = name.lastOrNull() ?: return name
    val hasFinal = c in '가'..'힣' && (c - '가') % 28 != 0
    return if (hasFinal) "${name}이와" else "${name}와"
}
