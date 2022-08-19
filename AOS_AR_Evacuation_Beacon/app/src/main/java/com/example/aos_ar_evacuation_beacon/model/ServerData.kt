package com.example.aos_ar_evacuation_beacon.model

data class ServerData(
   val pathList: List<String>,
   val fireCell: List<String>,
   val predictedCell: List<String>,
   val congestionCell: List<String>,
                     ) : java.io.Serializable
