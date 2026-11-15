package com.cabincrew.airride.control

import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Watches AAOS CarUxRestrictions. While distraction optimization is required,
 * the UI shows only the two gauges + one suggestion card (max 2 buttons).
 */
class DrivingRestriction(context: Context) {

    private val _restricted = MutableStateFlow<Boolean?>(null)

    /** null = platform doesn't report it (non-car device) → UI falls back to gear/demo state. */
    val restricted: StateFlow<Boolean?> = _restricted.asStateFlow()

    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    init {
        try {
            car = Car.createCar(context.applicationContext)
            manager = car?.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
            manager?.let { m ->
                _restricted.value = m.currentCarUxRestrictions?.isRequiresDistractionOptimization
                m.registerListener { r -> _restricted.value = r.isRequiresDistractionOptimization }
            }
        } catch (t: Throwable) {
            // android.car not present (phone emulator) → ignore.
            Log.w(TAG, "CarUxRestrictions unavailable", t)
        }
    }

    fun release() {
        runCatching { manager?.unregisterListener() }
        runCatching { car?.disconnect() }
    }

    private companion object {
        const val TAG = "DrivingRestriction"
    }
}
