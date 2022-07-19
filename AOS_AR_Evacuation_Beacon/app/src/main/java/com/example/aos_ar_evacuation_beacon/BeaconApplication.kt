package com.example.aos_ar_evacuation_beacon

import android.app.*
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import org.altbeacon.beacon.*

class BeaconApplication : Application() {
   lateinit var region: Region
   private var beaconID = "020012AC-4202-649D-EC11-B6CBC8814AD7"

   override fun onCreate() {
      super.onCreate()

      val beaconManager = BeaconManager.getInstanceForApplication(this)
      BeaconManager.setDebug(true)
      beaconManager.beaconParsers.clear()

      // The example shows how to find iBeacon.
      beaconManager.beaconParsers.add(
         BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
                                     )

      //setupForegroundService()
      //beaconManager.setEnableScheduledScanJobs(false);
      //beaconManager.setBackgroundBetweenScanPeriod(0);
      beaconManager.backgroundScanPeriod = 2000;
      beaconManager.foregroundScanPeriod = 2000;


      // UUID 설정
      region = Region(
         "all-beacons", Identifier.parse("$beaconID"), null, null
                     )
      beaconManager.startMonitoring(region)
      beaconManager.startRangingBeacons(region)
      val regionViewModel = BeaconManager.getInstanceForApplication(this).getRegionViewModel(region)
      regionViewModel.regionState.observeForever(centralMonitoringObserver)
      regionViewModel.rangedBeacons.observeForever(centralRangingObserver)
   }

   fun setupForegroundService() {
      val builder = Notification.Builder(this, "AOS_AR_Evacuation_App")
      builder.setSmallIcon(R.drawable.ic_launcher_background)
      builder.setContentTitle("Scanning for Beacons")
      val intent = Intent(this, LocalizationActivity::class.java)
      val pendingIntent = PendingIntent.getActivity(
         this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_IMMUTABLE
                                                   )
      builder.setContentIntent(pendingIntent);
      val channel = NotificationChannel(
         "beacon-ref-notification-id", "My Notification Name", NotificationManager.IMPORTANCE_DEFAULT
                                       )
      channel.description = "My Notification Channel Description"
      val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel);
      builder.setChannelId(channel.id);
      BeaconManager.getInstanceForApplication(this).enableForegroundServiceScanning(builder.build(), 456);
   }

   private val centralMonitoringObserver = Observer<Int> { state ->
      if (state == MonitorNotifier.OUTSIDE) {
         Log.d(TAG, "outside beacon region: $region")
      } else {
         Log.d(TAG, "inside beacon region: $region")
         sendNotification()
      }
   }

   private val centralRangingObserver = Observer<Collection<Beacon>> { beacons ->
      Log.d(LocalizationActivity.TAG, "Ranged: ${beacons.count()} beacons")
      for (beacon: Beacon in beacons) {
         Log.d(TAG, "$beacon RSSI: ${beacon.rssi} ")
      }
   }

   private fun sendNotification() {
      val builder =
         NotificationCompat.Builder(this, "beacon-ref-notification-id")
            .setContentTitle("AR Evacuation Application")
            .setContentText("A beacon is nearby.")
            .setSmallIcon(R.drawable.ic_launcher_background)
      val stackBuilder = TaskStackBuilder.create(this)
      stackBuilder.addNextIntent(Intent(this, LocalizationActivity::class.java))
      val resultPendingIntent = stackBuilder.getPendingIntent(
         0, PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_IMMUTABLE
                                                             )
      builder.setContentIntent(resultPendingIntent)
      val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.notify(1, builder.build())
   }

   companion object {
      val TAG = "AR Evacuation"
   }
}