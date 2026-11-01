package com.headunit.launcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * Keeps an offline map download going while the app is in the background. It does no work itself: being a foreground
 * data-sync service keeps the process, and so the download, alive. The download runs in the app ([NavEngine.downloadPack]).
 * It shows a notification with the current step, and stops when the download is finished or has failed.
 */
class OfflineDownloadService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Android 15 and later limit a data-sync foreground service to about six hours in a day. When the limit is reached the
     * system calls this and gives the service a few seconds to stop; otherwise the app is stopped. The download itself
     * carries on in the app while it is open, so the service just ends here.
     */
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra(EXTRA_TEXT).orEmpty().ifBlank { Lang.t("Karten werden geladen…", "Downloading maps…") }
        val n = notification(this, text)
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            else startForeground(ID, n)
        } catch (e: Exception) {
            // not allowed to start in the foreground right now: the download carries on while the app is open
            stopSelf()
        }
        return START_NOT_STICKY
    }

    companion object {
        private const val ID = 8
        private const val CHANNEL = "offline_download"
        private const val EXTRA_TEXT = "text"

        private fun notification(ctx: Context, text: String): Notification {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(NotificationChannel(CHANNEL, Lang.t("Karten-Download", "Map download"), NotificationManager.IMPORTANCE_LOW).apply {
                    description = Lang.t("Zeigt den Fortschritt, solange Karten geladen werden", "Shows progress while maps are downloading")
                    setShowBadge(false)
                })
            }
            val open = PendingIntent.getActivity(
                ctx, 1,
                Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            return Notification.Builder(ctx, CHANNEL).setSmallIcon(R.drawable.ic_stat_nav)
                .setContentTitle(Lang.t("Horizon Nav: Karten laden", "Horizon Nav: downloading maps"))
                .setContentText(text)
                .setContentIntent(open)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()
        }

        /** Starts the service, or updates its text when it is already running. Safe to call often. */
        fun show(ctx: Context, text: String) {
            try {
                ctx.startForegroundService(Intent(ctx, OfflineDownloadService::class.java).putExtra(EXTRA_TEXT, text))
            } catch (e: Exception) { /* background start not allowed: the download still runs while the app is open */ }
        }

        fun stop(ctx: Context) {
            try { ctx.stopService(Intent(ctx, OfflineDownloadService::class.java)) } catch (e: Exception) { }
        }
    }
}
