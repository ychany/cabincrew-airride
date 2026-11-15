package com.cabincrew.airride.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AirRideApp(vm: AirRideViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    AirRideTheme {
        Box(Modifier.fillMaxSize().background(AirColors.Bg)) {
            val report = state.report
            when {
                state.connecting -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                // Report detail is parked-only.
                report != null && !state.restricted -> ReportScreen(report, state.passenger?.name, vm::saveSettingsForNextTrip, vm::closeReport)
                // Boarding / rest-stop flows are parked-only screens.
                state.phase == Phase.BOARDING_WHO && !state.restricted -> BoardingWhoScreen(vm::choosePassenger)
                state.phase == Phase.BOARDING_CARE && !state.restricted -> state.pendingPassenger?.let { p ->
                    BoardingCareScreen(p, state.care, vm::setCare, vm::backToWho, vm::startCare)
                }
                state.phase == Phase.REST_STOP && !state.restricted -> RestStopScreen(state, vm::ventilate, vm::finishRest)
                else -> HomeScreen(
                    state = state,
                    onAccept = vm::acceptSuggestion,
                    onDismiss = vm::dismissSuggestion,
                    onOpenDemo = { vm.toggleDemoPanel(true) },
                    onEndTrip = vm::endTrip,
                    onCare = vm::setCare,
                )
            }
            if (state.demoPanelOpen) {
                DemoPanel(
                    signals = state.signals,
                    onScenario = vm::applyScenario,
                    onChange = vm::updateDemo,
                    onClose = { vm.toggleDemoPanel(false) },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }
}
