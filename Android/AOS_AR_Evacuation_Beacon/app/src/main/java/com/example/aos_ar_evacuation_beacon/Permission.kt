package com.example.aos_ar_evacuation_beacon

import android.Manifest
import android.R
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat.checkSelfPermission
import com.example.aos_ar_evacuation_beacon.ui.LocalizationActivity

class Permission(activity: LocalizationActivity, context: Context) {
   var neverAskAgainPermissions = ArrayList<String>()
   var activity = activity
   var context = context

   fun checkPermissions() {
      var permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
      var permissionRationale = "Fine location permission is needed."

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
         permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN)
         permissionRationale = "Fine location permission & bluetooth scan permission are needed."
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
         if ((checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionRationale = "Fine location permission is needed."
         } else {
            permissions = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            permissionRationale = "Background location permission is needed."
         }
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
         permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
         permissionRationale = "Fine location permission & background location permission are needed."
      }

      var isAllGranted = true
      for (permission in permissions) {
         if (checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) isAllGranted = false;
      }

      if (!isAllGranted) {
         if (neverAskAgainPermissions.isEmpty()) {
            val builder = AlertDialog.Builder(context)
            builder.apply {
               setTitle("Permissions Need")
               setMessage(permissionRationale)
               setPositiveButton(R.string.ok, null)
               setOnDismissListener {
                  requestPermissions(activity, permissions, LocalizationActivity.PERMISSION_REQUEST_FINE_LOCATION)
               }
            }

            if (!activity.isFinishing) {
               builder.create().show()
            }

         } else {
            val builder = AlertDialog.Builder(context)
            builder.apply {
               setTitle("Permission Need")
               setMessage("Please go to Settings -> Applications -> Permissions and grant location and device discovery permissions to this app.")
               setPositiveButton(R.string.ok, null)
               setOnDismissListener { }
            }.create().show()
         }
      } else {
         if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            if (checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
               if (shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                  val builder = AlertDialog.Builder(context)
                  builder.apply {
                     setTitle("Background location access needed.")
                     setMessage("Please grant location access.")
                     setPositiveButton(R.string.ok, null)
                     setOnDismissListener {
                        requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), LocalizationActivity.PERMISSION_REQUEST_BACKGROUND_LOCATION)
                     }
                  }.create().show()
               } else {
                  val builder = AlertDialog.Builder(context)
                  builder.apply {
                     setTitle("Permission Need")
                     setMessage("Please go to Settings -> Applications -> Permissions and grant background location access to this app.")
                     setPositiveButton(R.string.ok, null)
                     setOnDismissListener { }
                  }.create().show()
               }
            }
         } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S && (checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            if (shouldShowRequestPermissionRationale(activity, Manifest.permission.BLUETOOTH_SCAN)) {
               val builder = AlertDialog.Builder(context)
               builder.apply {
                  setTitle("Permission Need")
                  setMessage("Please grant scan permission so this app can detect beacons.")
                  setPositiveButton(R.string.ok, null)
                  setOnDismissListener {
                     requestPermissions(activity, arrayOf(Manifest.permission.BLUETOOTH_SCAN), LocalizationActivity.PERMISSION_REQUEST_BLUETOOTH_SCAN)
                  }
               }.create().show()
            } else {
               val builder = AlertDialog.Builder(context)
               builder.apply {
                  setTitle("Permission Need")
                  setMessage("Please go to Settings -> Applications -> Permissions and grant bluetooth scan permission to this app.")
                  setPositiveButton(R.string.ok, null)
                  setOnDismissListener { }
               }.create().show()
            }
         } else {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
               if (checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                  if (shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                     val builder = AlertDialog.Builder(context)
                     builder.apply {
                        setTitle("Background location access is needed.")
                        setMessage("Please grant location access so this app can detect beacons in the background.")
                        setPositiveButton(R.string.ok, null)
                        setOnDismissListener {
                           requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), LocalizationActivity.PERMISSION_REQUEST_BACKGROUND_LOCATION)
                        }
                     }.create().show()
                  } else {
                     val builder = AlertDialog.Builder(context)
                     builder.apply {
                        setTitle("Permission Need")
                        setMessage("Please go to Settings -> Applications -> Permissions and grant background location access to this app.")
                        setPositiveButton(R.string.ok, null)
                        setOnDismissListener { }
                     }.create().show()
                  }
               }
            }
         }
      }
   }
}