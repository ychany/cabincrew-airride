package com.cabincrew.airride.data

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Vehicle speed from the AAOS vehicle HAL (PERF_VEHICLE_SPEED, android.car.permission.CAR_SPEED).
 * The Pleos Vehicle SDK has no speed API, so speed and the derived acceleration change come from here.
 * Drive it on the emulator with the Pleos Connect Virtual Console.
 */
class CarSpeedMonitor(context: Context) {

    data class Motion(val speedKmh: Float, val accelChange: Float)

    private val _motion = MutableStateFlow<Motion?>(null)

    /** null until the first speed event (no permission / unsupported → stays null → demo values). */
    val motion: StateFlow<Motion?> = _motion.asStateFlow()

    private var car: Car? = null
    private var manager: CarPropertyManager? = null
    private var lastSpeed = Float.NaN
    private var lastTime = 0L
    private var ema = 0f

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            val mps = (value.value as? Float) ?: return
            val now = SystemClock.elapsedRealtime()
            if (!lastSpeed.isNaN() && now > lastTime) {
                val accel = abs(mps - lastSpeed) / ((now - lastTime) / 1000f)
                // Moving average of |Δv/Δt| — 튜닝 예정.
                ema = ema * 0.8f + accel.coerceAtMost(10f) * 0.2f
            }
            lastSpeed = mps
            lastTime = now
            _motion.value = Motion(speedKmh = mps * 3.6f, accelChange = ema)
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.w(TAG, "speed error $propertyId")
        }
    }

    fun start(): Boolean = try {
        car = Car.createCar(appContext)
        manager = car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
        manager?.registerCallback(callback, VehiclePropertyIds.PERF_VEHICLE_SPEED, 10f) == true
    } catch (t: Throwable) {
        Log.w(TAG, "vehicle speed unavailable", t)
        false
    }

    fun stop() {
        runCatching { manager?.unregisterCallback(callback) }
        runCatching { car?.disconnect() }
    }

    private val appContext = context.applicationContext

    private companion object {
        const val TAG = "CarSpeedMonitor"
    }
}
