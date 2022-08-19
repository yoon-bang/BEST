package com.example.aos_ar_evacuation_beacon

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.aos_ar_evacuation_beacon.ui.LocalizationActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

   // 수신 메세지 처리
   override fun onMessageReceived(message: RemoteMessage) {
      super.onMessageReceived(message)
      if (message.data.isNotEmpty()) {
         sendNotification(message.data["title"].toString(), message.data["body"].toString())
      } else {
         message.notification?.let {
            sendNotification(message.notification!!.title.toString(), message.notification!!.body.toString())
         }

      }

   }

   // token 을 서버로 전송
   override fun onNewToken(token: String) {
      super.onNewToken(token)
      Log.i("$$$ FCM Token $$$", token)
   }

   private fun sendNotification(title: String, body: String) {
      val notifyId = (System.currentTimeMillis() / 7).toInt()
      val intent = Intent(this, LocalizationActivity::class.java)
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

      val pendingIntent = PendingIntent.getActivity(this, notifyId, intent, PendingIntent.FLAG_IMMUTABLE)

      val channelId = getString(R.string.default_notification_channel_id)
      val notificationBuilder =
         NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationManagerCompat.IMPORTANCE_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

      val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         val channel = NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH)
         notificationManager.createNotificationChannel(channel)
      }

      notificationManager.notify(notifyId, notificationBuilder.build())
   }
}