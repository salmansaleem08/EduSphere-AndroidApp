
package com.salmansaleem.edusphere

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log

class FirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Received message: ${remoteMessage.data}, notification: ${remoteMessage.notification?.title}, ${remoteMessage.notification?.body}")

        // Try to get title and body from notification payload first
        val title = remoteMessage.notification?.title
        val body = remoteMessage.notification?.body
        val data = remoteMessage.data

        // Extract classroomId and announcementId from data payload
        val type = data["type"]
        val classroomId = data["classroomId"]
        val announcementId = data["announcementId"]

//        when (type) {
//            "announcement" -> {
//                Log.d("FCM", "Announcement notification: classroomId=$classroomId, announcementId=$announcementId, title=$title, body=$body")
//                if (classroomId != null && announcementId != null && title != null && body != null) {
//                    showAnnouncementNotification(classroomId, announcementId, title, body)
//                } else {
//                    Log.e("FCM", "Invalid announcement data: type=$type, classroomId=$classroomId, announcementId=$announcementId, title=$title, body=$body")
//                }
//            }
//            else -> {
//                Log.d("FCM", "Unknown message type: $type")
//            }
//        }


        when (type) {
            "announcement" -> {
                if (classroomId != null && announcementId != null) {
                    showNotification(
                        remoteMessage.notification?.title ?: "New Announcement",
                        remoteMessage.notification?.body ?: "Check the new announcement",
                        classroomId,
                        announcementId,
                        type
                    )
                }
            }
            "assignment" -> {
                val assignmentId = data["assignmentId"]
                if (classroomId != null && assignmentId != null) {
                    showNotification(
                        remoteMessage.notification?.title ?: "New Assignment",
                        remoteMessage.notification?.body ?: "Check the new assignment",
                        classroomId,
                        assignmentId,
                        type
                    )
                }
            }
            else -> {
                Log.d(TAG, "Unknown message type: $type")
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            com.google.firebase.database.FirebaseDatabase.getInstance().reference
                .child("Users")
                .child(userId)
                .child("fcmToken")
                .setValue(token)
        }
    }

    private fun showAnnouncementNotification(classroomId: String, announcementId: String, title: String, body: String) {
        Log.d("FCM", "Showing notification for classroom $classroomId")
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "announcement_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Announcement Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, ClassPage::class.java).apply {
            putExtra("classroom_id", classroomId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, announcementId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Ensure this exists
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(announcementId.hashCode(), notification)
    }


    private fun showNotification(title: String, body: String, classroomId: String, itemId: String, type: String) {
        val channelId = "edusphere_channel"
        val notificationId = itemId.hashCode()

        val intent = when (type) {
            "announcement" -> Intent(this, ClassPage::class.java).apply {
                putExtra("classroom_id", classroomId)
                putExtra("announcement_id", itemId)
            }
            "assignment" -> Intent(this, ClassTasks::class.java).apply {
                putExtra("classroom_id", classroomId)
                putExtra("assignment_id", itemId)
            }
            else -> Intent(this, ClassPage::class.java).apply {
                putExtra("classroom_id", classroomId)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Edusphere Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        Log.d(TAG, "Showing notification: title=$title, body=$body, classroomId=$classroomId, itemId=$itemId, type=$type")
        notificationManager.notify(notificationId, notification)
    }
}