package com.cabincrew.airride.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cabincrew.airride.MainActivity
import com.cabincrew.airride.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Driving suggestions shown over the navigation map (A-06 / A-08) when AirRide is not on screen.
 * Posted as a heads-up notification so the Pleos system renders it as its notification card,
 * with at most two action buttons.
 */
class SuggestionNotifier(private val context: Context) {

    init {
        val channel = NotificationChannel(CHANNEL_ID, "주행 케어 제안", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "운전자 각성·동승자 멀미에 따른 실내 환경 제안"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(title: String, body: String, primary: String, secondary: String) {
        if (!canPost()) {
            Log.w(TAG, "notification permission not granted")
            return
        }
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wind_4step)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(openApp())
            .addAction(0, primary, action(true))
            .addAction(0, secondary, action(false))
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
    }

    fun cancel() = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

    private fun canPost(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun action(accept: Boolean): PendingIntent = PendingIntent.getBroadcast(
        context, if (accept) 1 else 2,
        Intent(context, SuggestionActionReceiver::class.java).putExtra(EXTRA_ACCEPT, accept),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val TAG = "SuggestionNotifier"
        private const val CHANNEL_ID = "airride_suggestion"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_ACCEPT = "accept"

        /** Button taps from the notification → view model. */
        val actions = MutableSharedFlow<Boolean>(extraBufferCapacity = 4)
        val actionFlow: SharedFlow<Boolean> get() = actions
    }
}

class SuggestionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SuggestionNotifier.actions.tryEmit(intent.getBooleanExtra(SuggestionNotifier.EXTRA_ACCEPT, false))
        NotificationManagerCompat.from(context).cancel(1001)
    }
}
