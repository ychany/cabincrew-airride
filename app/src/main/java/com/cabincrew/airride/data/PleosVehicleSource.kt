package com.cabincrew.airride.data

import android.os.SystemClock
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.HvacRecirculationStateListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * Overlays real Pleos Vehicle SDK signals on top of the demo source.
 *
 * From the SDK: gear (driving / parked), HVAC recirculation state, steering wheel angle.
 * Not exposed by the Vehicle SDK (speed, acceleration, curve, lane warning, occupants)
 * stay on the demo values — README에 명시.
 */
class PleosVehicleSource(
    private val vehicle: Vehicle,
    private val base: DemoVehicleSource,
    private val scope: CoroutineScope,
    val hvacArea: SeatArea,
) : VehicleSignalSource {

    override val isDemo: Boolean = false

    private data class Live(
        val driving: Boolean? = null,
        val recirc: Boolean? = null,
        val recircSinceMs: Long? = null,
        val correctionsPerMin: Float? = null,
    )

    private val live = MutableStateFlow(Live())

    override val signals: StateFlow<VehicleSignals> =
        combine(base.signals, live) { demo, l ->
            demo.copy(
                isDriving = l.driving ?: demo.isDriving,
                recirculationOn = l.recirc ?: demo.recirculationOn,
                steeringCorrectionsPerMin = l.correctionsPerMin ?: demo.steeringCorrectionsPerMin,
            )
        }.stateIn(scope, SharingStarted.Eagerly, base.signals.value)

    // Steering micro-correction detection: direction reversals with small amplitude.
    private val correctionTimes = ArrayDeque<Long>()
    private var lastAngle: Float? = null
    private var lastDir = 0f

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) {
            gear ?: return
            live.value = live.value.copy(driving = gear != VehicleGear.GEAR_PARK)
        }
        override fun onFailed(e: Exception) { Log.w(TAG, "listener failed", e) }
    }

    private val recircListener = object : HvacRecirculationStateListener {
        override fun onHvacAutoRecirculationStateUpdated(on: Boolean?) { on?.let(::onRecirc) }
        override fun onFailed(e: Exception) { Log.w(TAG, "listener failed", e) }
    }

    private val steeringListener = object : SteeringWheelAngleListener {
        override fun onSteeringWheelAngleUpdated(angle: Float?) { angle?.let(::onAngle) }
        override fun onFailed(e: Exception) { Log.w(TAG, "listener failed", e) }
    }

    private var ticker: Job? = null

    private fun onRecirc(on: Boolean) {
        val prev = live.value
        live.value = prev.copy(
            recirc = on,
            recircSinceMs = if (on) prev.recircSinceMs ?: SystemClock.elapsedRealtime() else null,
        )
    }

    private fun onAngle(angle: Float) {
        val prev = lastAngle
        lastAngle = angle
        if (prev == null) return
        val delta = angle - prev
        if (abs(delta) < 0.5f) return
        val dir = sign(delta)
        // Small reversal (< 15°) counts as a micro-correction. 튜닝 예정.
        if (lastDir != 0f && dir != lastDir && abs(angle) < 15f) {
            synchronized(correctionTimes) { correctionTimes.addLast(SystemClock.elapsedRealtime()) }
        }
        lastDir = dir
    }

    override fun start() {
        runCatching { vehicle.getGear().registerGearSelection(gearListener) }
            .onFailure { Log.w(TAG, "gear listener unavailable", it) }
        runCatching { vehicle.getHVAC().registerHvacRecirculationState(hvacArea, recircListener) }
            .onFailure { Log.w(TAG, "recirculation listener unavailable", it) }
        runCatching { vehicle.getSteeringWheel().registerSteeringWheelAngle(steeringListener) }
            .onFailure { Log.w(TAG, "steering listener unavailable", it) }

        runCatching {
            vehicle.getHVAC().getHvacRecirculationState(hvacArea, { it?.let(::onRecirc) }, { Log.w(TAG, "recirc get failed", it) })
        }

        ticker = scope.launch {
            while (isActive) {
                delay(1_000)
                val now = SystemClock.elapsedRealtime()
                val perMin = synchronized(correctionTimes) {
                    while (correctionTimes.isNotEmpty() && now - correctionTimes.first() > 60_000) {
                        correctionTimes.removeFirst()
                    }
                    correctionTimes.size.toFloat()
                }
                val l = live.value
                live.value = l.copy(correctionsPerMin = if (lastAngle != null) perMin else null)
                // Recirculation duration drives the CO₂ estimate.
                l.recircSinceMs?.let { since ->
                    base.update { it.copy(recircMinutes = (now - since) / 60_000f) }
                }
            }
        }
    }

    override fun stop() {
        ticker?.cancel()
        runCatching { vehicle.getGear().unregisterGearSelection(gearListener) }
        runCatching { vehicle.getHVAC().unregisterHvacRecirculationState(hvacArea, recircListener) }
        runCatching { vehicle.getSteeringWheel().unregisterSteeringWheelAngle(steeringListener) }
    }

    companion object {
        private const val TAG = "PleosVehicleSource"
        val DEFAULT_AREA = SeatArea(SeatValue.ROW_1_LEFT)
    }
}
