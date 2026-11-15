package com.cabincrew.airride.data

import kotlinx.coroutines.flow.StateFlow

/** Source of [VehicleSignals]. Implemented by the Pleos SDK and by demo mode. */
interface VehicleSignalSource {
    val signals: StateFlow<VehicleSignals>

    /** True when values are simulated (SDK unavailable or demo panel in use). */
    val isDemo: Boolean

    fun start() {}
    fun stop() {}
}
