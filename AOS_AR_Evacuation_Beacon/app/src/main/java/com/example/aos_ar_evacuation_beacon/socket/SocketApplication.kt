//package com.example.aos_ar_evacuation_beacon.socket
//
//import io.socket.client.IO
//import io.socket.client.Socket
//import java.net.URISyntaxException
//
//class SocketApplication {
//   companion object {
//      private lateinit var socket: Socket
//      fun get(): Socket {
//         try {
//            val options = IO.Options()
//            options.port = 12000
//            socket = IO.socket("146.148.59.28", options)
//         } catch (e: URISyntaxException) {
//            e.printStackTrace()
//         }
//         return socket
//      }
//   }
//}