package com.cabincrew.airride.control

import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
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
    private val main = Handler(Looper.getMainLooper())
    private var restoreRecirc: Runnable? = null

    /**
     * Switch HVAC to fresh (outside) air: recirculation OFF.
     * [brief] = 미세먼지 모드: back to recirculation after [BRIEF_FRESH_AIR_MS]
     * (한국교통안전공단 권고: 내기순환 중 1~2시간마다 1~2분 외기 전환).
     */
    fun switchToFreshAir(
        brief: Boolean = false,
        onRestored: (appliedToVehicle: Boolean) -> Unit = {},
        onResult: (appliedToVehicle: Boolean) -> Unit = {},
    ) {
        restoreRecirc?.let(main::removeCallbacks)
        restoreRecirc = null
        if (brief) {
            restoreRecirc = Runnable { setRecirculation(true, onRestored) }.also {
                main.postDelayed(it, BRIEF_FRESH_AIR_MS)
            }
        }
        setRecirculation(false, onResult)
    }

    private fun setRecirculation(on: Boolean, onResult: (appliedToVehicle: Boolean) -> Unit) {
        // UI/demo state always reflects the action.
        demo.update { it.copy(recirculationOn = on, recircMinutes = 0f) }

        val v = vehicle
        val area = hvacArea
        if (v == null || area == null) {
            Log.i(TAG, "demo mode: recirculation=$on applied to UI only")
            onResult(false)
            return
        }
        runCatching {
            v.getHVAC().setHvacRecirculation(
                area, on,
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

    /**
     * 운전석 온도를 [deltaC]만큼 낮춥니다 (외기 전환과 함께, 졸음 완화).
     * Capability → current setpoint → clamp to bounds. Returns the new setpoint, or null if not applied.
     */
    fun coolDriverSeat(deltaC: Float = DRIVER_COOL_DELTA_C, onResult: (newCelsius: Float?) -> Unit = {}) {
        val v = vehicle ?: return onResult(null)
        runCatching {
            val hvac = v.getHVAC()
            hvac.checkHvacTemperatureCapabilities(
                { bounds: Map<SeatArea, AreaBounds<Float>> ->
                    val entry = bounds.entries.firstOrNull { SeatValue.ROW_1_LEFT in it.key.values }
                        ?: bounds.entries.firstOrNull()
                    if (entry == null) {
                        onResult(null)
                    } else {
                        val apply = { current: Float? ->
                            val target = ((current ?: DEFAULT_SETPOINT_C) - deltaC).coerceIn(entry.value.minValue, entry.value.maxValue)
                            hvac.setHvacTemperature(
                                entry.key, target, TemperatureUnit.CELSIUS,
                                { _, _ -> onResult(target) },
                                { e -> Log.w(TAG, "setHvacTemperature failed", e); onResult(null) },
                            )
                        }
                        // Setpoint read can be unavailable (e.g. right after HVAC power-on) → fall back to 22℃.
                        hvac.getHvacTemperature(
                            entry.key, TemperatureUnit.CELSIUS,
                            { current: Float? -> apply(current) },
                            { e: Exception -> Log.w(TAG, "getHvacTemperature unavailable, using default", e); apply(null) },
                        )
                    }
                },
                { e: Exception -> Log.w(TAG, "temperature capability failed", e); onResult(null) },
            )
        }.onFailure {
            Log.w(TAG, "coolDriverSeat threw", it)
            onResult(null)
        }
    }

    fun release() {
        restoreRecirc?.let(main::removeCallbacks)
        runCatching { vehicle?.release() }
    }

    companion object {
        private const val TAG = "CabinController"
        const val BRIEF_FRESH_AIR_MS = 120_000L
        const val DRIVER_COOL_DELTA_C = 1.5f
        private const val DEFAULT_SETPOINT_C = 22f
    }
}
