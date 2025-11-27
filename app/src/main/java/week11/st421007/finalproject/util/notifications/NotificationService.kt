package week11.st421007.finalproject.util.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NotificationService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token saved to Firestore for user: $userId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save FCM token", e)
                }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        val title = message.notification?.title ?: message.data["title"] ?: "TasteTracker"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val type = message.data["type"] ?: "general"

        Log.d(TAG, "Notification - Title: $title, Body: $body, Type: $type")

        val notificationId = when (type) {
            "monthly_recap" -> NotificationHelper.MONTHLY_RECAP_NOTIFICATION_ID
            "revisit_reminder" -> NotificationHelper.REVISIT_NOTIFICATION_ID
            else -> 999
        }

        val notificationHelper = NotificationHelper(this)
        notificationHelper.sendNotification(
            title = title,
            message = body,
            notificationId = notificationId
        )

        Log.d(TAG, "Notification displayed successfully")
    }
}