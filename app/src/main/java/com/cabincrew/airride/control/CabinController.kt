package com.cabincrew.airride.control

import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
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

    /**
     * A-05 "뒷좌석 바람 조절": set the rear-left HVAC zone to a gentle fan speed (min + 1).
     * Capability is checked first; unsupported → demo (UI only).
     */
    fun softenRearFan(onResult: (appliedToVehicle: Boolean) -> Unit = {}) {
        val v = vehicle ?: return onResult(false)
        runCatching {
            val hvac = v.getHVAC()
            hvac.checkHvacFanSpeedCapabilities(
                { bounds: Map<SeatArea, ai.pleos.playground.vehicle.api.model.capability.AreaBounds<Int>> ->
                    val entry = bounds.entries.firstOrNull { SeatValue.ROW_2_LEFT in it.key.values }
                        ?: bounds.entries.firstOrNull()
                    if (entry == null) {
                        onResult(false)
                    } else {
                        val target = (entry.value.minValue + 1).coerceAtMost(entry.value.maxValue)
                        hvac.setHvacFanSpeed(
                            entry.key, target,
                            { _, _ -> onResult(true) },
                            { e -> Log.w(TAG, "setHvacFanSpeed failed", e); onResult(false) },
                        )
                    }
                },
                { e: Exception -> Log.w(TAG, "fan capability failed", e); onResult(false) },
            )
        }.onFailure {
            Log.w(TAG, "softenRearFan threw", it)
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
