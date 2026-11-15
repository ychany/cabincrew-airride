package com.cabincrew.airride.control

import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import com.cabincrew.airride.data.DemoVehicleSource

/**
 * Applies cabin actions. Uses the Pleos Vehicle SDK when connected;
 * otherwise (or on failure) logs and reflects the change in demo state only.
 */
class CabinController(
    private val vehicle: Vehicle?,
    private val hvacArea: SeatArea?,
    private val demo: DemoVehicleSource,
) {
    /** Switch HVAC to fresh (outside) air: recirculation OFF. */
    fun switchToFreshAir(onResult: (appliedToVehicle: Boolean) -> Unit = {}) {
        // UI/demo state always reflects the action.
        demo.update { it.copy(recirculationOn = false, recircMinutes = 0f) }

        val v = vehicle
        val area = hvacArea
        if (v == null || area == null) {
            Log.i(TAG, "demo mode: fresh air applied to UI only")
            onResult(false)
            return
        }
        runCatching {
            v.getHVAC().setHvacRecirculation(
                area, false,
                { _, _ -> onResult(true) },
                { e ->
                    Log.w(TAG, "setHvacRecirculation failed", e)
                    onResult(false)
                },
            )
        }.onFailure {
            Log.w(TAG, "setHvacRecirculation threw", it)
            onResult(false)
        }
    }

    fun release() {
        runCatching { vehicle?.release() }
    }

    private companion object {
        const val TAG = "CabinController"
    }
}
