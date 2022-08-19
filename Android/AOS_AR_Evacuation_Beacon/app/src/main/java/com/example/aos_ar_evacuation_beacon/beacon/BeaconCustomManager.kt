package com.example.aos_ar_evacuation_beacon.beacon

import com.example.aos_ar_evacuation_beacon.kalman.KalmanFilter

class BeaconCustomManager {
   companion object {
      private var outInstance: BeaconCustomManager? = null

      val instance: BeaconCustomManager
         get() {
            if (outInstance == null) {
               outInstance = BeaconCustomManager()
            }
            return outInstance!!
         }
   }

   // 기존 비콘 칼만필터
   var beaconInfo = mutableMapOf<String, KalmanFilter>()

   // <비콘 bid, RSSI>
   var rawRSSIDict = mutableMapOf<String, Int>()
   var filteredRSSIDict = mutableMapOf<String, Float>()
}