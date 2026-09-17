package com.bookisham.app.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bookisham.app.BookishamApplication
import java.util.concurrent.TimeUnit

/**
 * Checks the reader's feed in the background and raises anything new in the
 * system tray.
 *
 * This is polling, not push. Real push would mean Firebase Cloud
 * Messaging, a Firebase project and a service-account key on the server;
 * without those, WorkManager's fifteen minutes is the floor, and Doze may
 * stretch it further. What arrives is a real system notification either
 * way.
 */
class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? BookishamApplication ?: return Result.success()
        if (!app.session.signedIn) return Result.success()

        val feed = try {
            app.api.notifications()
        } catch (e: UnauthorizedException) {
            return Result.success() // Signed out elsewhere; nothing to say.
        } catch (e: Exception) {
            return Result.retry()
        }

        val notifier = app.notifier
        notifier.ensureChannel()

        if (notifier.isFirstRun()) {
            // Start quiet: record the backlog rather than replaying it.
            notifier.markSeen(feed.map { it.id })
            return Result.success()
        }

        // Unread only, so anything already read in the app stays quiet, and
        // oldest first so the newest ends up on top of the tray.
        feed.filter { !it.read && !notifier.hasSeen(it.id) }
            .asReversed()
            .forEach { notifier.show(it.id, it.message) }
        notifier.markSeen(feed.map { it.id })
        return Result.success()
    }

    companion object {
        private const val NAME = "bookisham-notifications"

        /** Runs for as long as the app is installed; KEEP leaves an existing schedule alone. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
