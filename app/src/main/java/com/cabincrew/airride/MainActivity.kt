package com.cabincrew.airride

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.cabincrew.airride.ui.AirRideApp
import com.cabincrew.airride.ui.AirRideViewModel

class MainActivity : ComponentActivity() {

    private val vm: AirRideViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Heads-up suggestions over the navigation map need notification permission (API 33+).
        // Vehicle speed (CAR_SPEED) is a runtime permission on AAOS.
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            add(CAR_SPEED)
        }.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) requestPermissions(needed.toTypedArray(), 1)
        setContent { AirRideApp(vm) }
    }

    private companion object {
        const val CAR_SPEED = "android.car.permission.CAR_SPEED"
    }

    override fun onStart() {
        super.onStart()
        vm.setVisible(true)
    }

    override fun onStop() {
        super.onStop()
        vm.setVisible(false)
    }
}
