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
                report != null && !state.restricted -> ReportScreen(report, vm::closeReport)
                else -> HomeScreen(
                    state = state,
                    onAccept = vm::acceptSuggestion,
                    onDismiss = vm::dismissSuggestion,
                    onOpenDemo = { vm.toggleDemoPanel(true) },
                    onEndTrip = vm::endTrip,
                    onMotionSensitive = vm::setMotionSensitive,
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
