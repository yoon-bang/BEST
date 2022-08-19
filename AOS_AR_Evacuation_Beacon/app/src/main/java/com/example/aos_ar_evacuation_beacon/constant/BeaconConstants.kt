package com.example.aos_ar_evacuation_beacon.constant

object BeaconConstants {
   const val IP_ADDRESS = "146.148.59.28"
   const val port = 12000

   const val testMode = false
   const val beaconPeriod = 1100L
   const val beaconNum = 5
   const val modelName = "aos_beacon${beaconNum}_concat"
   const val beaconCSVName = "AOS_estimated${beaconNum}_"
   const val readCSVName = "aos_clf_data7U01.csv"

   val labelList =
      listOf("A01",
             "A10",
             "A11",
             "A02",
             "A03",
             "A04",
             "A05",
             "A06",
             "A07",
             "A08",
             "A09",
             "E01",
             "E03",
             "E02",
             "H01",
             "H02",
             "R01",
             "R02",
             "R03",
             "R04",
             "R05",
             "S01",
             "S02",
             "S03",
             "S04",
             "S05",
             "S06",
             "S07",
             "S08",
             "S09",
             "U01")
}