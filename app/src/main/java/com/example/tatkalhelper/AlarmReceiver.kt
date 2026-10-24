package com.example.tatkalhelper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/** Fires at the Tatkal alarm time: arms autofill and posts a one-tap "Open IRCTC" notification. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        AutofillState.arm()

        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("tatkal", "Tatkal alarm", NotificationManager.IMPORTANCE_HIGH)
        )
        val open = PendingIntent.getActivity(
            ctx, 3, Irctc.appIntent(ctx), PendingIntent.FLAG_IMMUTABLE
        )
        nm.notify(
            1,
            NotificationCompat.Builder(ctx, "tatkal")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Tatkal window opening")
                .setContentText("Tap to open IRCTC. Autofill is armed.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(open)
                .addAction(
                    android.R.drawable.ic_menu_view, "Website",
                    PendingIntent.getActivity(ctx, 4, Irctc.websiteIntent(), PendingIntent.FLAG_IMMUTABLE)
                )
                .setAutoCancel(true)
                .build()
        )
    }
}
