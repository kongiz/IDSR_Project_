package com.idsr_project.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.idsr_project.R
import com.idsr_project.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class CleanupWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getInstance(context).pendingReportDao()
                val cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                val deletedCount = dao.deleteOldSyncedReports(cutoff)
                if (deletedCount > 0) showNotification(deletedCount)
                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }

    private fun showNotification(count: Int) {
        val channelId = "idsr_cleanup"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Storage Cleanup", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Monthly local storage cleanup" }
            )
        }

        nm.notify(1001,
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(com.google.android.gms.cast.R.drawable.cast_ic_notification_0)
                .setContentTitle("Storage Cleaned")
                .setContentText("$count synced ${if (count == 1) "report" else "reports"} older than 30 days removed.")
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    "$count synced ${if (count == 1) "report" else "reports"} older than 30 days " +
                            "have been removed from your device. All data remains safely on the IDSR server."
                ))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .build()
        )
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "idsr_monthly_cleanup",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<CleanupWorker>(30, TimeUnit.DAYS).build()
            )
        }
    }
}