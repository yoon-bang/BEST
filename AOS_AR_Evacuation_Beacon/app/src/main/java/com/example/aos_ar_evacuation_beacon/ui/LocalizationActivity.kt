package com.example.aos_ar_evacuation_beacon.ui

import android.Manifest
import android.R
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.aos_ar_evacuation_beacon.BeaconApplication
import com.example.aos_ar_evacuation_beacon.beacon.BeaconCustomManager
import com.example.aos_ar_evacuation_beacon.beacon.BeaconInfo
import com.example.aos_ar_evacuation_beacon.constant.BeaconConstants
import com.example.aos_ar_evacuation_beacon.csv.CSVHelper
import com.example.aos_ar_evacuation_beacon.databinding.ActivityLocalizationBinding
import com.example.aos_ar_evacuation_beacon.kalman.KalmanFilter
import com.example.aos_ar_evacuation_beacon.repository.LocationRepository
import com.example.aos_ar_evacuation_beacon.viewModel.MainViewModel
import com.google.firebase.ml.modeldownloader.CustomModel
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import com.opencsv.CSVReader
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.MonitorNotifier
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Math.toDegrees
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs

class LocalizationActivity : AppCompatActivity(), SensorEventListener {
   private lateinit var binding: ActivityLocalizationBinding
   val mainViewModel: MainViewModel by viewModels()

   val locationRepository = LocationRepository.instance
   private lateinit var beaconApplication: BeaconApplication
   var neverAskAgainPermissions = ArrayList<String>()
   var alertDialog: AlertDialog? = null

   var beaconCSV = arrayListOf<Array<String>>()

   lateinit var filePath: String
   private lateinit var csvHelper: CSVHelper

   private lateinit var timer: CountDownTimer

   private lateinit var kalman: KalmanFilter

   private lateinit var wifiManager: WifiManager
   private lateinit var beaconManager: BeaconCustomManager

   // bid, rssi
   private var threshold = 10

   // Sensor
   private lateinit var sensorManager: SensorManager
   private lateinit var mAccelerometer: Sensor
   private lateinit var mMagneticField: Sensor
   private var accelerationList = FloatArray(3)
   private var magneticFieldList = FloatArray(3)

   private lateinit var conditions: CustomModelDownloadConditions
   private lateinit var interpreter: Interpreter

   private lateinit var token: String

   private lateinit var assetManager: AssetManager
   private lateinit var inputStream: InputStream
   private lateinit var reader: CSVReader

   private var currentBeaconArray = mutableListOf<Float>()
   private var previousBeaconArray = mutableListOf<Float>()

   private lateinit var locationQueue: ArrayList<String>

   private var estimatedLocationList = mutableListOf<String>()

   // 사용자 방향 저장
   private var azimuth = 0F
   private var pitch = 0F
   private var roll = 0F
   var currentDegree = 0.0f

   lateinit var mSocket: Socket

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      // UI 설정
      binding = ActivityLocalizationBinding.inflate(layoutInflater)
      val view = binding.root
      setContentView(view)

      beaconApplication = application as BeaconApplication

      val regionViewModel = BeaconManager.getInstanceForApplication(this).getRegionViewModel(beaconApplication.region)
      regionViewModel.regionState.observe(this, monitoringObserver)
      regionViewModel.rangedBeacons.observe(this, rangingObserver)

      binding.beaconCount.text = "No beacons detected"
      binding.beaconList.adapter = ArrayAdapter(this, R.layout.simple_list_item_1, arrayOf("--"))

      socketSetup()
      setting()
      //makeColumnName()
      makeEstimatedLocationColumn()
      setupTimer()
      setSensor()
      loadMLModel()
      //socketSetup()
      setBottomNavigation()
   }

   override fun onPause() {
      Log.d(TAG, "onPause")
      super.onPause()
   }

   override fun onResume() {
      Log.d(TAG, "onResume")
      super.onResume()
      checkPermissions()
   }

   private fun socketSetup() {
//      mSocket = SocketApplication.get()

      val options = IO.Options()
      options.port = 12001
      options.reconnection = false

      try {
         mSocket = IO.socket("http://146.148.59.28:12000")
         mSocket.on("path", onNewEvent)
         mSocket.connect()
         //mSocket.emit("location", "A11")

         //Log.w("===socket===", mSocket.connected().toString())
      } catch (e: Exception) {
         Log.e("===socket===", e.toString())
      }

//      if (mSocket != null) {
//         Log.d("$$$ Socket ID $$$$", mSocket.isActive.toString())
//      } else {
//         Log.d("$$$ Socket ID $$$$", "empty")
//      }
//
//      mSocket.on("path", onNewEvent)
//      mSocket.emit("location", "A11")
   }

   private var onNewEvent: Emitter.Listener = Emitter.Listener { args ->
      runOnUiThread(Runnable {
         val data = args[0] as JSONObject
         try {
            Log.w("SSSSSSSSSSSS", data.toString())
         } catch (e: Exception) {
            e.printStackTrace()
            return@Runnable
         }
      })
   }

   private fun setBottomNavigation() {
      binding.bottomNavigation.selectedItemId = com.example.aos_ar_evacuation_beacon.R.id.localizationItem
      binding.bottomNavigation.setOnItemSelectedListener {
         when (it.itemId) {
            com.example.aos_ar_evacuation_beacon.R.id.navigationItem -> {
               val intent = Intent(this, ARActivity::class.java)
               intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
               startActivity(intent)
               overridePendingTransition(0, 0)
            }
         }
         true
      }
   }

   private fun loadMLModel() {
      conditions = CustomModelDownloadConditions.Builder().requireWifi().build()

      FirebaseModelDownloader.getInstance()
         .getModel(BeaconConstants.modelName, DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND, conditions)
         .addOnFailureListener { Log.w("$$$ ${BeaconConstants.modelName} Download $$$", "Failure") }
         .addOnCanceledListener { Log.w("$$$ ${BeaconConstants.modelName} Download $$$", "Cancel") }
         .addOnSuccessListener { model: CustomModel? ->
            val modelFile = model?.file
            interpreter = modelFile?.let { Interpreter(it) }!!
            Log.w("$$$ ${BeaconConstants.modelName} Download $$$", "Success")
         }
   }


   private fun setting() {
      filePath = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString()
      csvHelper = CSVHelper(filePath)
      kalman = KalmanFilter(R = 0.001f, Q = 2f)
      wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
      locationQueue = arrayListOf()
   }

   private fun setupTimer() {
      timer = object : CountDownTimer(timerTime, 1000) {
         override fun onTick(time: Long) { //Log.v("timer: ", time.toString())
         }

         override fun onFinish() {
            saveCSV()
            binding.isCSVSaved.visibility = View.VISIBLE
         }
      }.start()
   }

   fun saveCSV() {
      csvHelper.writeData("${BeaconConstants.beaconCSVName}${
         LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
      }.csv", beaconCSV)
   }

   private fun printDeviceInfo() {
      Log.w("dddd", "${getMacAddress()}")
      // UUID 는 앱을 삭제하고 다시 설치하면 값이 변경됨
      Log.w("cccc", "${UUID.randomUUID()}")
   }

   private fun getMacAddress(): String? = try {
      NetworkInterface.getNetworkInterfaces().toList().find { networkInterface ->
         networkInterface.name.equals("wlan0", ignoreCase = true)
      }?.hardwareAddress?.joinToString(separator = ":") { byte -> "%02X".format(byte) }
   } catch (exception: Exception) {
      exception.printStackTrace()
      null
   }

   private fun setSensor() {
      beaconManager = BeaconCustomManager.instance

      sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
      sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { this.mAccelerometer = it }
      sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let { this.mMagneticField = it }
      sensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
      sensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL)
   }

   val monitoringObserver = Observer<Int> { state ->
      var dialogTitle = "Beacons detected"
      var dialogMessage = "didEnterRegionEvent has fired"
      var stateString = "inside"

      if (state == MonitorNotifier.OUTSIDE) {
         dialogTitle = "No beacons detected"
         dialogMessage = "didExitRegionEvent has fired"
         stateString == "outside"
         binding.beaconCount.text = "Outside of the beacon region -- no beacons detected"
         binding.beaconList.adapter = ArrayAdapter(this, R.layout.simple_list_item_1, arrayOf("--"))
      } else {
         binding.beaconCount.text = "Inside the beacon region."
      }
      Log.d(TAG, "monitoring state changed to : $stateString")
      val builder = AlertDialog.Builder(this)
      builder.apply {
         setTitle(dialogTitle)
         setMessage(dialogMessage)
         setPositiveButton(R.string.ok, null)
      }.create().show()

      alertDialog?.dismiss()
      alertDialog = builder.create()
      alertDialog?.show()
   }

   val rangingObserver = Observer<Collection<Beacon>> { beacons ->
      Log.d(TAG, "Ranged: ${beacons.count()} beacons")
      if (BeaconManager.getInstanceForApplication(this).rangedRegions.isNotEmpty()) {
         binding.beaconCount.text = "Ranging enabled: ${beacons.count()} beacon(s) detected"

         // initialize
         val filteredBeaconList = ArrayList<String>()
         filteredBeaconList.add(LocalDateTime.now().toString())
         for (i in 0 until 22) {
            filteredBeaconList.add("-200")
         }
         for (i in 0 until 2) {
            filteredBeaconList.add("0")
         }

         val rawBeaconFloatList = mutableListOf<Float>()
         for (i in 0 until 22) {
            rawBeaconFloatList.add(-200F)
         }

         beacons.forEach {
            val beacon = BeaconInfo(it)

            // beacon rssi 업데이트
            beaconManager.rawRSSIDict[beacon.bID] = it.rssi
            rawBeaconFloatList[beacon.bID.toInt() - 1] = it.rssi.toFloat()

            // 각 비콘마다 kalman 생성
            if (beaconManager.beaconInfo[beacon.bID] == null) {
               beaconManager.beaconInfo[beacon.bID] = KalmanFilter(R = 0.001f, Q = 2f)
            }

            var filteredRssi = beaconManager.beaconInfo[beacon.bID]?.filter(signal = beacon._rssi.toFloat())
            beaconManager.filteredRSSIDict[beacon.bID] = filteredRssi!!

            //Log.i("$$$ Detected Beacons $$$", "minor: ${beacon.bID}, RSSI: ${it.rssi}, Filtered: $filteredRssi")

            filteredBeaconList[beacon.minor] = filteredRssi.toString()
//            filteredBeaconList[23] = azimuth.toString()
            val direction = classifyOrientation(azimuth)
            //filteredBeaconList[23] = directionEncoder(direction).toString()
         }

         // 1~23 자르고 float 으로 변환
         val filteredBeaconFloatList = mutableListOf<Float>()
         filteredBeaconList.subList(1, 23).forEach { value -> filteredBeaconFloatList.add(value.toFloat()) }
         filteredBeaconFloatList.forEachIndexed { index, fl ->
            //Log.i("filteredBeaconFloatList $index: ", fl.toString())
         }
         selectBeacon(rawBeaconFloatList, filteredBeaconFloatList, BeaconConstants.beaconNum)
         //addToCSV(filteredBeaconList.toTypedArray())
         binding.beaconList.adapter = ArrayAdapter(this, R.layout.simple_list_item_1, beacons.map { "Major ${it.id2}          Minor: ${it.id3}\nRaw rssi: ${it.rssi}\n" }.toTypedArray())
      }
   }

   private fun selectBeacon(rawBeacon: MutableList<Float>, filteredBeacon: MutableList<Float>, beaconNum: Int) {
      var errorNum = 0
      currentBeaconArray = filteredBeacon

      // 상위 n개만 뽑기
      val rawDict = mutableMapOf<String, Float>()
      val filteredDict = mutableMapOf<String, Float>()

      filteredBeacon.forEachIndexed { index, value -> filteredDict += ("$index" to value) }
      rawBeacon.forEachIndexed { index, value -> rawDict += ("$index" to value) }

      // 상위 n개로 정렬
      val filteredList = filteredDict.toList().sortedByDescending { (_, value) -> value }.subList(0, beaconNum)
      val rawList = rawDict.toList().sortedByDescending { (_, value) -> value }.subList(0, beaconNum)

      val newFilteredList = mutableListOf<Float>()
      val newRawList = mutableListOf<Float>()
      var st1 = "[ "
      var st2 = "[ "

      // 상위 n개 값 저장
      filteredBeacon.forEach { _ -> newFilteredList.add(-200F) }
      filteredList.forEach { (index, value) -> newFilteredList[index.toInt()] = value }

      rawBeacon.forEach { _ -> newRawList.add(-200F) }
      rawList.forEach { (index, value) -> newRawList[index.toInt()] = value }

      // 에러 >= 20 이면 칼만 초기화
      for (i in 0 until filteredBeacon.size) {
         if (newFilteredList[i] != -200F && newRawList[i] != -200F) {
            st1 += "${newFilteredList[i]}, "
            st2 += "${newRawList[i]}, "

            val error = abs(abs(newFilteredList[i]) - abs(newRawList[i]))
            //Log.i("RSSI Error ", error.toString())
            if (error > threshold) {
               errorNum += 1
            }
         }
      }

      if (errorNum >= 1) {
         beaconManager.beaconInfo.forEach { (s, _) ->
            beaconManager.beaconInfo["$s"] = KalmanFilter(R = 0.001f, Q = 2f)
         }
      }
      combineBeaconCells(filteredBeacon, beaconNum)
//      filteredBeacon.add(direction)
   }

   private fun combineBeaconCells(filteredBeacon: MutableList<Float>, beaconNum: Int) {
      if (previousBeaconArray.size == 0) {
         previousBeaconArray = filteredBeacon
      } else {
         val combinedBeaconList = mutableListOf<Float>()
         var previousString = "[ "
         var currentString = "[ "
         var combinedString = "[ "

         // 둘 중에 큰 값으로 저장
         for (i in 0 until previousBeaconArray.size) {
            if (previousBeaconArray[i] < currentBeaconArray[i]) {
               combinedBeaconList.add(currentBeaconArray[i])
            } else {
               combinedBeaconList.add(previousBeaconArray[i])
            }
            previousString += "${previousBeaconArray[i]},"
            currentString += "${currentBeaconArray[i]},"
            combinedString += "${combinedBeaconList[i]},"
         }

         Log.i("Previous String ", previousString)
         Log.i("Current String ", currentString)
         // 두 개를 하나로 합친 결과
         Log.i("$$$ Combined String $$$", combinedString)

         var sortedString = "[ "
         val combinedBeaconDict = mutableMapOf<String, Float>()
         combinedBeaconList.forEachIndexed { index, value -> combinedBeaconDict += ("$index" to value) }
         val sortedList = combinedBeaconDict.toList().sortedByDescending { (_, value) -> value }.subList(0, beaconNum)

         // -200 으로 초기화
         combinedBeaconList.forEachIndexed { index, _ -> combinedBeaconList[index] = -200F }
         // 상위 n 개만 value 저장
         sortedList.forEach { (index, value) -> combinedBeaconList[index.toInt()] = value }
         combinedBeaconList.forEachIndexed { index, _ -> sortedString += "${combinedBeaconList[index]}," }
         //combinedBeaconList.add(currentBeaconArray.last())

         combinedBeaconList.forEachIndexed { index, fl ->
            Log.w("combined $index: ", "${fl}")
         }
         // 상위 n개만 뽑아서 저장
         Log.i("$$$ Filtered $beaconNum Beacon $$$", "$sortedString ]")
         getModelOutput(combinedBeaconList)
         previousBeaconArray.clear()
      }
   }

   private fun classifyOrientation(orientationValue: Float): String {
      return if (((315 < orientationValue) && (orientationValue < 360)) || ((0 < orientationValue)) && (orientationValue < 45)) {
         "N"
      } else if (((45 < orientationValue) && (orientationValue < 135))) {
         "E"
      } else if (((135 < orientationValue) && (orientationValue < 225))) {
         "S"
      } else if (((225 < orientationValue) && (orientationValue < 315))) {
         "W"
      } else {
         "0"
      }
   }

   private fun directionEncoder(direction: String): Int {
      return when (direction) {
         "E" -> {
            0
         }
         "N" -> {
            1
         }
         "S" -> {
            2
         }
         else -> {
            3
         }
      }
   }

   private fun addToCSV(cell: Array<String>) {
      beaconCSV.add(cell)
   }

   private fun getModelOutput(rssiList: MutableList<Float>) {
      val labelNum = 31
      val input = ByteBuffer.allocateDirect(4 * rssiList.size).order(ByteOrder.nativeOrder())

      for (i in 0 until rssiList.size) {
         input.putFloat(rssiList[i])
      }
      val bufferSize = labelNum * java.lang.Float.SIZE / java.lang.Byte.SIZE
      val modelOutput = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())

      interpreter?.run(input, modelOutput)
      modelOutput.rewind()
      val probabilities = modelOutput.asFloatBuffer()

      try {
         var probabilityString = "[ "
         val probabilityArray = mutableListOf<Float>()

         for (i in 0 until probabilities.capacity()) {
            val probability = probabilities.get(i)
            probabilityArray.add(probability)
         }
         val maxProbability = probabilityArray.maxOrNull()
         val mappedLabel = BeaconConstants.labelList[probabilityArray.indexOf(maxProbability)]

         if (locationQueue.size >= 7) {
            locationQueue.removeAt(0)
         }
         locationQueue.add(mappedLabel)

         var locationString = ""
         locationQueue.forEach { locationString += "$it " }
         binding.locationQueue.text = locationString

         if (locationRepository.isStart.value == true) {
            locationRepository.updateStartPoint(mappedLabel)
            locationRepository.updateIsStart(false)
         }

         locationRepository.updateLocationString(mappedLabel)

         mainViewModel.addQueue(mappedLabel)
         mainViewModel.evacuationQueue.value?.forEachIndexed { index, s ->
            Log.i("hhhhhhhhhh $index: ", s)
         }

         addToCSV(arrayOf(mappedLabel))
      } catch (e: IOException) {
         Log.e("$$$ Output Error $$$", e.toString())
      }
   }

   private fun readCSV() {
      assetManager = this.assets
      inputStream = assetManager.open("${BeaconConstants.readCSVName}")
      reader = CSVReader(InputStreamReader(inputStream))

      val allContent = reader.readAll()
      for (i in 1 until allContent.size) {
         val rssiList = mutableListOf<Float>()
         val row = allContent[i].drop(2)
         row.forEachIndexed { index, s -> Log.i("Row to read $index: ", s) }

         var csvString = "[ "
         row.forEach {
            rssiList.add(it.toFloat())
            csvString += "$it , "
         }
         Log.i("$$$$ csv list $$$$", "$csvString] ")
         //getModelOutput(rssiList)
      }
   }

   override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
      for (i in 1 until permissions.size) {
         Log.d(TAG, "onRequestPermissionResult for " + permissions[i] + ":" + grantResults[i])
         if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
            //check if user select "never ask again" when denying any permission
            if (!shouldShowRequestPermissionRationale(permissions[i])) {
               neverAskAgainPermissions.add(permissions[i])
            }
         }
      }
   }

   private fun makeEstimatedLocationColumn() {
      beaconCSV.add(arrayOf("estimated"))
   }

   private fun makeColumnName() {
      beaconCSV.add(arrayOf(
         "Date",
         "001",
         "002",
         "003",
         "004",
         "005",
         "006",
         "007",
         "008",
         "009",
         "010",
         "011",
         "012",
         "013",
         "014",
         "015",
         "016",
         "017",
         "018",
         "019",
         "020",
         "021",
         "022",
         "CompassValue",
         "CompassString",
                           ))
   }

   override fun onSensorChanged(event: SensorEvent?) {
      when (event?.sensor?.type) {
         Sensor.TYPE_ACCELEROMETER -> {
            accelerationList[0] = event.values[0]
            accelerationList[1] = event.values[1]
            accelerationList[2] = event.values[2]

         }

         Sensor.TYPE_MAGNETIC_FIELD -> {
            magneticFieldList[0] = event.values[0]
            magneticFieldList[1] = event.values[1]
            magneticFieldList[2] = event.values[2]
         }
      }

      // 행렬 계산
      val rArray = FloatArray(9)
      val iArray = FloatArray(9)
      SensorManager.getRotationMatrix(rArray, iArray, accelerationList, magneticFieldList)

      // 방위값 환산
      val values = FloatArray(3)
      SensorManager.getOrientation(rArray, values)

      // 방위값 -> 각도 단위 변경
      azimuth = toDegrees(values[0].toDouble()).toFloat()
      // 좌우 기울기 값
      pitch = toDegrees(values[1].toDouble()).toFloat()
      // 앞뒤 기울기 값
      roll = toDegrees(values[2].toDouble()).toFloat()

      var degree = toDegrees(values[0].toDouble() + 360).toFloat() % 360
      val rotateAnimation = RotateAnimation(currentDegree, -degree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
      rotateAnimation.duration = 1000
      rotateAnimation.fillAfter = true

      currentDegree = -degree

      if (azimuth < 0) {
         azimuth += 360
      }
      //binding.azimuth.text = azimuth.toString()
   }

   override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

   }

   fun rangingButtonTapped(view: View) {
      val beaconManager = BeaconManager.getInstanceForApplication(this)
      if (beaconManager.rangedRegions.isEmpty()) {
         beaconManager.startRangingBeacons(beaconApplication.region)
         binding.rangingButton.text = "Stop Ranging"
         binding.beaconCount.text = "Ranging enabled"
      } else {
         beaconManager.stopRangingBeacons(beaconApplication.region)
         binding.rangingButton.text = "Start Ranging"
         binding.beaconCount.text = "Ranging disabled -- no beacons detected"
         binding.beaconList.adapter = ArrayAdapter(this, R.layout.simple_list_item_1, arrayOf("--"))
      }
   }

   fun monitoringButtonTapped(view: View) {
      var dialogTitle = "Beacon Dialog"
      var dialogMessage = ""
      val beaconManager = BeaconManager.getInstanceForApplication(this)

      if (beaconManager.monitoredRegions.isEmpty()) {
         beaconManager.startMonitoring(beaconApplication.region)
         dialogMessage = "Beacon monitoring started."
         binding.monitoringButton.text = "Stop Monitoring"

      } else {
         beaconManager.stopMonitoring(beaconApplication.region)
         dialogMessage = "Beacon monitoring stopped."
         binding.monitoringButton.text = "Start Monitoring"
      }

      val builder = AlertDialog.Builder(this).apply {
         this.setTitle(dialogTitle)
         this.setMessage(dialogMessage)
         this.setPositiveButton(R.string.ok, null)
      }
      alertDialog?.dismiss()
      alertDialog = builder.create()
      alertDialog?.show()
   }

   private fun checkPermissions() {
      var permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
      var permissionRationale = "This app needs fine location permission to detect beacons.  Please grant this now."
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
         permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN)
         permissionRationale = "This app needs fine location permission, and bluetooth scan permission to detect beacons.  Please grant all of these now."
      } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
         if ((checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionRationale = "This app needs fine location permission to detect beacons.  Please grant this now."
         } else {
            permissions = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            permissionRationale = "This app needs background location permission to detect beacons in the background.  Please grant this now."
         }
      } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
         permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
         permissionRationale = "This app needs both fine location permission and background location permission to detect beacons in the background.  Please grant both now."
      }
      var allGranted = true
      for (permission in permissions) {
         if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) allGranted = false;
      }
      if (!allGranted) {
         if (neverAskAgainPermissions.count() == 0) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("This app needs permissions to detect beacons")
            builder.setMessage(permissionRationale)
            builder.setPositiveButton(android.R.string.ok, null)
            builder.setOnDismissListener {
               requestPermissions(permissions, PERMISSION_REQUEST_FINE_LOCATION)
            }
            builder.show()
         } else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Functionality limited")
            builder.setMessage("Since location and device permissions have not been granted, this app will not be able to discover beacons.  Please go to Settings -> Applications -> Permissions and grant location and device discovery permissions to this app.")
            builder.setPositiveButton(android.R.string.ok, null)
            builder.setOnDismissListener { }
            builder.show()
         }
      } else {
         if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
               if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                  val builder = AlertDialog.Builder(this)
                  builder.setTitle("This app needs background location access")
                  builder.setMessage("Please grant location access so this app can detect beacons in the background.")
                  builder.setPositiveButton(android.R.string.ok, null)
                  builder.setOnDismissListener {
                     requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PERMISSION_REQUEST_BACKGROUND_LOCATION)
                  }
                  builder.show()
               } else {
                  val builder = AlertDialog.Builder(this)
                  builder.setTitle("Functionality limited")
                  builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons in the background.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.")
                  builder.setPositiveButton(android.R.string.ok, null)
                  builder.setOnDismissListener { }
                  builder.show()
               }
            }
         } else if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.S && (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN)) {
               val builder = AlertDialog.Builder(this)
               builder.setTitle("This app needs bluetooth scan permission")
               builder.setMessage("Please grant scan permission so this app can detect beacons.")
               builder.setPositiveButton(android.R.string.ok, null)
               builder.setOnDismissListener {
                  requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN), PERMISSION_REQUEST_BLUETOOTH_SCAN)
               }
               builder.show()
            } else {
               val builder = AlertDialog.Builder(this)
               builder.setTitle("Functionality limited")
               builder.setMessage("Since bluetooth scan permission has not been granted, this app will not be able to discover beacons  Please go to Settings -> Applications -> Permissions and grant bluetooth scan permission to this app.")
               builder.setPositiveButton(android.R.string.ok, null)
               builder.setOnDismissListener { }
               builder.show()
            }
         } else {
            if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
               if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                  if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                     val builder = AlertDialog.Builder(this)
                     builder.setTitle("This app needs background location access")
                     builder.setMessage("Please grant location access so this app can detect beacons in the background.")
                     builder.setPositiveButton(android.R.string.ok, null)
                     builder.setOnDismissListener {
                        requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PERMISSION_REQUEST_BACKGROUND_LOCATION)
                     }
                     builder.show()
                  } else {
                     val builder = AlertDialog.Builder(this)
                     builder.setTitle("Functionality limited")
                     builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons in the background.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.")
                     builder.setPositiveButton(android.R.string.ok, null)
                     builder.setOnDismissListener { }
                     builder.show()
                  }
               }
            }
         }
      }
   }

   companion object {
      const val TAG = "MainActivity"
      val PERMISSION_REQUEST_BACKGROUND_LOCATION = 0
      val PERMISSION_REQUEST_BLUETOOTH_SCAN = 1
      val PERMISSION_REQUEST_BLUETOOTH_CONNECT = 2
      val PERMISSION_REQUEST_FINE_LOCATION = 3
      const val timerTime = 30000L
   }
}