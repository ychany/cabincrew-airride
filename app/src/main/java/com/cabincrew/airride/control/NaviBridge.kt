package com.cabincrew.airride.control

import android.content.Context
import android.util.Log
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.WaypointArrivedInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener

/**
 * A-08 "경유지 추가" → Pleos NaviHelper SDK.
 * With an active route the rest area is added as the first waypoint; otherwise a route to it is requested.
 */
class NaviBridge(context: Context, private val onArrived: () -> Unit = {}) {

    data class Place(val name: String, val latitude: Double, val longitude: Double, val address: String)

    private var helper: NaviHelper? = null
    private var routeActive = false

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStateInfo(info: RouteStateInfo) {
            Log.i(TAG, "route state: $info")
            routeActive = info.isRouting
        }

        override fun onRouteEnded() { routeActive = false }

        // Arrived at the rest area (as destination or waypoint) → A-09 rest-stop flow.
        override fun onDestinationArrived(info: DestinationArrivedInfo) = onArrived()
        override fun onWayPointArrived(info: WaypointArrivedInfo) = onArrived()
        override fun onRouteCancelled() { routeActive = false }
    }

    init {
        runCatching {
            NaviHelper(context.applicationContext).also { h ->
                h.initialize(
                    {
                        Log.i(TAG, "NaviHelper ready")
                        runCatching { h.addListener(listener); h.getRouteStateInfo() }
                    },
                    { e -> Log.w(TAG, "NaviHelper init failed", e) },
                )
                helper = h
            }
        }.onFailure { Log.w(TAG, "NaviHelper unavailable", it) }
    }

    /** Returns true if the request was handed to the navigation app. */
    fun addRestStop(place: Place): Boolean {
        val h = helper ?: return false
        return runCatching {
            if (routeActive) {
                h.addWaypoint(
                    RequestWaypointInfo(
                        latitude = place.latitude, longitude = place.longitude,
                        waypointIndex = WaypointIndex.FIRST,
                        poiId = "", poiName = place.name, poiSubId = "0", address = place.address,
                    )
                )
            } else {
                h.requestRoute(
                    RouteInfo(
                        latitude = place.latitude, longitude = place.longitude,
                        poiId = "", poiName = place.name, poiSubId = "0", address = place.address,
                    )
                )
            }
            Log.i(TAG, "requested ${if (routeActive) "waypoint" else "route"} → ${place.name}")
            true
        }.getOrElse {
            Log.w(TAG, "navi request failed", it)
            false
        }
    }

    fun release() {
        runCatching { helper?.removeListener(listener); helper?.release() }
    }

    private companion object {
        const val TAG = "NaviBridge"
    }
}
