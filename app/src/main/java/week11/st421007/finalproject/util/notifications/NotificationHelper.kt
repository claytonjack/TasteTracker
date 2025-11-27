package week11.st421007.finalproject.util.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import week11.st421007.finalproject.MainActivity
import week11.st421007.finalproject.R

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Restaurant Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to revisit your favorite restaurants"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendNotification(title: String, message: String, notificationId: Int = REVISIT_NOTIFICATION_ID) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    companion object {
        private const val TAG = "NotificationHelper"
        private const val CHANNEL_ID = "taste_tracker_reminders"
        const val REVISIT_NOTIFICATION_ID = 1
        const val MONTHLY_RECAP_NOTIFICATION_ID = 2

        fun triggerTestNotifications(context: Context, userId: String) {
            Log.d(TAG, "Triggering TEST notifications for user: $userId")
            
            val helper = NotificationHelper(context)
            
            helper.sendNotification(
                title = "Miss your favorites? 🍽️",
                message = "You haven't visited The Cozy Corner in over 3 months! Time to go back?",
                notificationId = REVISIT_NOTIFICATION_ID
            )
            Log.d(TAG, "Test revisit notification sent")
            
            helper.sendNotification(
                title = "November Dining Recap 📊",
                message = "Your Stats:\n🍽️ 5 restaurants visited\n⭐ 4.2 stars average\n💰 $$ average price\n\n\nCommunity Stats:\n🍽️ 15 visits by 3 users\n⭐ 4.0 stars average\n💰 $$ average price",
                notificationId = MONTHLY_RECAP_NOTIFICATION_ID
            )
            Log.d(TAG, "Test monthly recap notification sent")
        }
    }
}