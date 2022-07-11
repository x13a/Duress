package me.lucky.duress

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationManager(private val ctx: Context) {
    companion object {
        const val CHANNEL_DEFAULT_ID = "default"
        private const val GROUP_KEY = "test"
        private const val NOTIFICATION_ID = 1000
    }

    private val manager = NotificationManagerCompat.from(ctx)

    fun createNotificationChannels() {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(
            CHANNEL_DEFAULT_ID,
            NotificationManagerCompat.IMPORTANCE_LOW,
        ).setName(ctx.getString(R.string.notification_channel_default_name)).build())
    }

    fun send() {
        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(ctx, CHANNEL_DEFAULT_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(ctx.getString(R.string.notification_title))
                .setContentText(ctx.getString(android.R.string.ok))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .build(),
        )
    }
}