package week11.st421007.finalproject.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val _notificationPermissionGranted = MutableStateFlow(false)
    val notificationPermissionGranted: StateFlow<Boolean> = _notificationPermissionGranted.asStateFlow()

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private var currentUserId: String? = null

    init {
        checkNotificationPermission()
        getFCMToken()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            _notificationPermissionGranted.value = hasPermission
        } else {
            _notificationPermissionGranted.value = true
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _notificationPermissionGranted.value = granted
        if (granted) {
            Log.d(TAG, "Notification permission granted")
        }
    }

    private fun getFCMToken() {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                _fcmToken.value = token
                Log.d(TAG, "FCM Token: $token")

                saveFCMTokenToFirestore(token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get FCM token", e)
            }
        }
    }

    private fun saveFCMTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token saved to Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save FCM token", e)
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                }
        }
    }

    fun setupNotificationScheduler(userId: String) {
        if (_notificationPermissionGranted.value) {
            currentUserId = userId
            Log.d(TAG, "Notification permission granted for user: $userId")
            Log.d(TAG, "Revisit reminders and monthly recaps will be sent via push notifications from Cloud Functions")
        }
    }

    fun cancelNotificationScheduler() {
        currentUserId?.let { userId ->
            Log.d(TAG, "Notification scheduler cancelled for user: $userId")
        }
        currentUserId = null
    }

    companion object {
        private const val TAG = "NotificationViewModel"
    }
}
