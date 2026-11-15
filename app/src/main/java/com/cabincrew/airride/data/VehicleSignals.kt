package com.cabincrew.airride.data

/**
 * Snapshot of the vehicle/cabin signals the condition engine needs.
 * Values come from the Pleos Vehicle SDK when available, otherwise from demo mode.
 */
data class VehicleSignals(
    val isDriving: Boolean = true,
    val speedKmh: Float = 80f,
    /** Moving average of longitudinal acceleration change (m/s²). */
    val accelChange: Float = 0.6f,
    /** Lateral sway estimate from steering/yaw (m/s²). */
    val lateralSway: Float = 0.5f,
    val steeringCorrectionsPerMin: Float = 2f,
    val recirculationOn: Boolean = true,
    /** Minutes the HVAC has stayed in recirculation mode. */
    val recircMinutes: Float = 10f,
    val occupants: Int = 2,
    /** Lane departure / lane keeping warning within the last 5 minutes. */
    val laneWarningRecent: Boolean = false,
    val continuousDriveMinutes: Float = 30f,
    val curveAhead: Boolean = false,
)
