package com.example.tcpexample

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.net.InetSocketAddress
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
   lateinit var socket: Socket

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.activity_main)

      val btnConnect = findViewById<Button>(R.id.connectButton)
      val btnDisconnect = findViewById<Button>(R.id.disconnectButton)
      btnConnect.setOnClickListener { GlobalScope.launch { client() } }
      btnDisconnect.setOnClickListener { GlobalScope.launch { socket.close() } }
   }

   private suspend fun client() {
      socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(InetSocketAddress("18.190.54.57", 12000))

      val input = socket.openReadChannel()
      val output = socket.openWriteChannel(autoFlush = true)

      Timber.w("Start to Connect, localAddress", "${socket.localAddress}")
      Timber.w("Start to Connect, remoteAddress", "${socket.remoteAddress}")
      Timber.w("Server localAddress : ${socket.localAddress}")
      Timber.w("Server remoteAddress : ${socket.remoteAddress}")
      Timber.w("input : ${input.readUTF8Line()}")
   }

   // coroutine 으로 구현
   fun clientCoroutine() {
      runBlocking {
         val selectorManager = ActorSelectorManager(Dispatchers.IO)
         val socket = aSocket(selectorManager).tcp().connect("18.190.54.57", 12000)

         val receiveChannel = socket.openReadChannel()
         val sendChannel = socket.openWriteChannel(autoFlush = true)

         launch(Dispatchers.IO) {
            while (true) {
               val greeting = receiveChannel.readUTF8Line()
               if (greeting != null) {
                  println(greeting)
               } else {
                  println("Server closed a connection")
                  socket.close()
                  selectorManager.close()
                  exitProcess(0)
               }
            }
         }

         while (true) {
            val myMessage = readln()
            sendChannel.writeStringUtf8("$myMessage\n")
         }
      }
   }
}