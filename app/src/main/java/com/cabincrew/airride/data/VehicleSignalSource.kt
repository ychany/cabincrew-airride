package com.cabincrew.airride.data

import kotlinx.coroutines.flow.StateFlow

/** Source of [VehicleSignals]. Implemented by the Pleos SDK and by demo mode. */
interface VehicleSignalSource {
    val signals: StateFlow<VehicleSignals>

    /** True when values are simulated (SDK unavailable or demo panel in use). */
    val isDemo: Boolean

    /** Demo panel in use: simulated values take priority over live SDK values. */
    fun setDemoOverride(on: Boolean) {}

    fun start() {}
    fun stop() {}
}
