package com.bookisham.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bookisham.app.R

/**
 * The system tray: raising what the server's feed says, and remembering
 * which messages have already been raised so none is shown twice.
 */
class Notifier(private val context: Context) {
    private val prefs = context.getSharedPreferences("bookisham-notify", Context.MODE_PRIVATE)

    fun ensureChannel() {
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannel(CHANNEL, "Library updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Purchase decisions, and new versions of the app."
            },
        )
    }

    private fun seen(): Set<String> = prefs.getStringSet(KEY_SEEN, emptySet()).orEmpty()

    /** True until the first sweep has run: a fresh install should not replay the whole feed. */
    fun isFirstRun(): Boolean = !prefs.contains(KEY_SEEN)

    fun hasSeen(id: String): Boolean = id in seen()

    fun markSeen(ids: Collection<String>) {
        // Bounded: the feed comes back newest-first, and a set that grew for
        // ever would be read and written on every sweep.
        val kept = LinkedHashSet(ids).apply { addAll(seen()) }.take(200).toSet()
        prefs.edit().putStringSet(KEY_SEEN, kept).apply()
    }

    /** Raise one message. Silently does nothing if the reader has not allowed notifications. */
    fun show(id: String, message: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) ?: return
        val pending = PendingIntent.getActivity(
            context,
            id.hashCode(),
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Bookisham")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        // The permission can be revoked between the check above and here.
        runCatching { manager.notify(id.hashCode(), notification) }
    }

    private companion object {
        const val CHANNEL = "bookisham-updates"
        const val KEY_SEEN = "seen"
    }
}
