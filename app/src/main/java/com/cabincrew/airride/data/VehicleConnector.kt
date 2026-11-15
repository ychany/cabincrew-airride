package com.cabincrew.airride.data

import android.content.Context
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Tries to connect to the Pleos Vehicle Service. Falls back to demo mode when
 * initialization throws or HVAC recirculation capability can't be confirmed.
 */
object VehicleConnector {

    private const val TAG = "VehicleConnector"
    private const val CAPABILITY_TIMEOUT_MS = 3_000L

    data class Result(val source: VehicleSignalSource, val vehicle: Vehicle?, val hvacArea: SeatArea?)

    suspend fun connect(context: Context, demo: DemoVehicleSource, scope: CoroutineScope): Result {
        val vehicle = runCatching { Vehicle(context.applicationContext).also { it.initialize() } }
            .onFailure { Log.w(TAG, "Vehicle SDK init failed → demo mode", it) }
            .getOrNull() ?: return Result(demo, null, null)

        val areas: List<SeatArea>? = withTimeoutOrNull(CAPABILITY_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                runCatching {
                    vehicle.getHVAC().checkHvacRecirculationCapabilities(
                        { list: List<SeatArea> -> if (cont.isActive) cont.resume(list) },
                        { e: Exception ->
                            Log.w(TAG, "recirculation capability failed", e)
                            if (cont.isActive) cont.resume(null)
                        },
                    )
                }.onFailure { if (cont.isActive) cont.resume(null) }
            }
        }

        if (areas.isNullOrEmpty()) {
            Log.w(TAG, "HVAC recirculation not supported → demo mode")
            runCatching { vehicle.release() }
            return Result(demo, null, null)
        }

        val area = areas.first()
        Log.i(TAG, "Pleos Vehicle connected, HVAC area=$area")
        return Result(PleosVehicleSource(vehicle, demo, scope, area), vehicle, area)
    }
}
