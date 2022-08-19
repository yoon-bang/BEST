package com.example.aos_ar_evacuation_beacon.beacon

import org.altbeacon.beacon.Beacon

class BeaconInfo(val beacon: Beacon) : Beacon() {
   var minor = 0
   var major = 0
   var _rssi = -100

   init {
      this.major = beacon.id2.toInt()
      this.minor = beacon.id3.toInt()
      this._rssi = beacon.rssi
   }

   val location: MutableMap<String, Pair<Int, Int>> = mutableMapOf("001" to Pair(30, 0), "002" to Pair(30, 30), "003" to Pair(0, 30))

   val bID: String
      get() = if (minor < 10) "${major}0${minor}" else "${major}${minor}"
}