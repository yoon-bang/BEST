package com.example.aos_ar_evacuation_beacon

import android.R
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.aos_ar_evacuation_beacon.beacon.BeaconCustomManager
import com.example.aos_ar_evacuation_beacon.beacon.BeaconInfo
import com.example.aos_ar_evacuation_beacon.constant.BeaconConstants
import com.example.aos_ar_evacuation_beacon.csv.CSVHelper
import com.example.aos_ar_evacuation_beacon.databinding.ActivityMainBinding
import com.example.aos_ar_evacuation_beacon.kalman.KalmanFilter
import com.example.aos_ar_evacuation_beacon.socket.SocketApplication
import com.google.firebase.ml.modeldownloader.CustomModel
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import com.opencsv.CSVReader
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
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity(), SensorEventListener {
   private lateinit var binding: ActivityMainBinding
   private lateinit var beaconApplication: BeaconApplication
   var alertDialog: AlertDialog? = null
   lateinit var permission: Permission

   var beaconCSV = arrayListOf<Array<String>>()
   lateinit var filePath: String
   private lateinit var csvHelper: CSVHelper

   private lateinit var timer: CountDownTimer

   private lateinit var kalman: KalmanFilter

   private lateinit var wifiManager: WifiManager
   private lateinit var beaconManager: BeaconCustomManager

   // bid, rssi
   private var threshold = 20

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
   private lateinit var labelList: List<String>
   private var estimatedLocationList = mutableListOf<String>()

   // 사용자 방향 저장
   private var azimuth by Delegates.notNull<Float>()
   private var pitch by Delegates.notNull<Float>()
   private var roll by Delegates.notNull<Float>()

   lateinit var mSocket: Socket

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      // UI 설정
      binding = ActivityMainBinding.inflate(layoutInflater)
      val view = binding.root
      setContentView(view)

      beaconApplication = application as BeaconApplication

      val regionViewModel = BeaconManager.getInstanceForApplication(this).getRegionViewModel(beaconApplication.region)
      regionViewModel.regionState.observe(this, monitoringObserver)
      regionViewModel.rangedBeacons.observe(this, rangingObserver)

      binding.beaconCount.text = "No beacons detected"
      binding.beaconList.adapter = ArrayAdapter(this, R.layout.simple_list_item_1, arrayOf("--"))

      setting()
      makeColumnName()
      setupTimer()
      setSensor()
      //loadMLModel()
      setLabelList()
      //socketSetup()
   }

   override fun onPause() {
      Log.d(TAG, "onPause")
      super.onPause()
   }

   override fun onResume() {
      Log.d(TAG, "onResume")
      super.onResume()
      permission.checkPermissions(MainActivity.this, applicationContext)
   }

   private fun socketSetup() {
      mSocket = SocketApplication.get()

      mSocket.on("event", onNewEvent)
      mSocket.connect()
      Log.d("$$$ Socket ID $$$$", mSocket.id())
      mSocket.emit("AOS Socket Connection", "hello")
   }

   private var onNewEvent: Emitter.Listener = Emitter.Listener { args ->
      runOnUiThread(Runnable {
         val data = args[0] as JSONObject
         try {
            Log.w("$$$ Socket Data from Server", data.toString())
         } catch (e: Exception) {
            e.printStackTrace()
            return@Runnable
         }
      })
   }

   private fun loadMLModel() {
      conditions = CustomModelDownloadConditions.Builder().requireWifi().build()
      FirebaseModelDownloader.getInstance()
         .getModel(BeaconConstants.modelName, DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND, conditions)
         .addOnFailureListener { Log.w("$$$ Model Download $$$", "Failure") }
         .addOnCanceledListener { Log.w("$$$ Model Download $$$", "Cancel") }
         .addOnSuccessListener { model: CustomModel? ->
            val modelFile = model?.file
            interpreter = modelFile?.let { Interpreter(it) }!!
            Log.w("$$$ Model Download $$$", "Success")
         }
   }

   private fun setting() {
      filePath = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString()
      csvHelper = CSVHelper(filePath)
      kalman = KalmanFilter(R = 0.001f, Q = 2f)
      wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
      locationQueue = arrayListOf()
      permission = Permission()
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
      csvHelper.writeData("${BeaconConstants.saveCSVName}${
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
         val filteredBeaconCell = ArrayList<String>()
         filteredBeaconCell.add(LocalDateTime.now().toString())
         for (i in 0 until 22) {
            filteredBeaconCell.add("-200")
         }
         for (i in 0 until 9) {
            filteredBeaconCell.add("0")
         }

         val rawBeaconFloatCell = ArrayList<Float>()
         for (i in 0 until 20) {
            rawBeaconFloatCell.add(-200F)
         }

         beacons.forEach {
            val beacon = BeaconInfo(it)

            // beacon rssi 업데이트
            beaconManager.rawRSSIDict[beacon.bID] = it.rssi
            rawBeaconFloatCell[beacon.bID.toInt()] = it.rssi.toFloat()

            // 각 비콘마다 kalman 생성
            if (beaconManager.beaconInfo[beacon.bID] == null) {
               beaconManager.beaconInfo[beacon.bID] = KalmanFilter(R = 0.001f, Q = 2f)
            }

            var filteredRssi = beaconManager.beaconInfo[beacon.bID]?.filter(signal = beacon._rssi.toFloat())
            beaconManager.filteredRSSIDict[beacon.bID] = filteredRssi!!


            //Log.i("$$$ Detected Beacons $$$", "minor: ${beacon.bID}, RSSI: ${it.rssi}, Filtered: $filteredRssi")

            filteredBeaconCell[beacon.minor] = filteredRssi.toString()
            filteredBeaconCell[23] = azimuth.toString()
            filteredBeaconCell[24] = classifyOrientation(azimuth)


            /*
            filteredBeaconCell[23] = accelerationList[0]
            filteredBeaconCell[24] = accelerationList[1]
            filteredBeaconCell[25] = accelerationList[2]

            filteredBeaconCell[26] = BigDecimal(gyroscopeList[0]).toPlainString()
            filteredBeaconCell[27] = BigDecimal(gyroscopeList[1]).toPlainString()
            filteredBeaconCell[28] = BigDecimal(gyroscopeList[2]).toPlainString()

            filteredBeaconCell[29] = magneticFieldList[0]
            filteredBeaconCell[30] = magneticFieldList[1]
            filteredBeaconCell[31] = magneticFieldList[2]
             */
         }


         // from here
         /*
         val filteredBeaconFloatCell = mutableListOf<Float>()
         filteredBeaconCell.subList(1, 23).forEach { value -> filteredBeaconFloatCell.add(value.toFloat()) }
         currentBeaconArray = filteredBeaconFloatCell

         // Kalman 초기화
         // 상위 7개만 뽑기
         val dict1 = mutableMapOf<String, Float>()
         val dict2 = mutableMapOf<String, Float>()

         filteredBeaconFloatCell.forEachIndexed { index, value -> dict1 += ("$index" to value) }
         rawBeaconFloatCell.forEachIndexed { index, value -> dict2 += ("$index" to value) }

         val filteredList = dict1.toList().sortedByDescending { (_, value) -> value }.subList(0, 7)
         val rawList = dict2.toList().sortedByDescending { (_, value) -> value }.subList(0, 7)

         val newFilteredList = mutableListOf<Float>()
         val newRawList = mutableListOf<Float>()
         var st1 = "[ "
         var st2 = "[ "

         filteredBeaconFloatCell.forEach { newFilteredList.add(-200F) }
         filteredList.forEach { (index, value) ->
            newFilteredList[index.toInt()] = value

         }

         rawBeaconFloatCell.forEach { newRawList.add(-200F) }
         rawList.forEach { (index, value) -> newRawList[index.toInt()] = value }

         var errorNum = 0

         // 에러 >= 10 이면 칼만 초기화
         for (i in 0 until filteredBeaconFloatCell.size) {
            if (newFilteredList[i] != -200F && newRawList[i] != -200F) {
               Log.w("fff 1 : ", newFilteredList[i].toString())
               Log.w("fff 2 : ", newRawList[i].toString())

               val error = abs(abs(newFilteredList[i]) - abs(newRawList[i]))
               Log.i("RSSI Error ", error.toString())
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

         if (previousBeaconArray.size == 0) {
            previousBeaconArray = filteredBeaconFloatCell
         } else {
            val filteredBeaconList = mutableListOf<Float>()
            var previousString = "[ "
            var currentString = "[ "
            var filteredString = "[ "

            for (i in 0 until previousBeaconArray.size) {
               if (previousBeaconArray[i] < currentBeaconArray[i]) {
                  filteredBeaconList.add(currentBeaconArray[i])
               } else {
                  filteredBeaconList.add(previousBeaconArray[i])
               }
               previousString += "${previousBeaconArray[i]},"
               currentString += "${currentBeaconArray[i]}," // 둘 중에 큰 값으로 저장
               filteredString += "${filteredBeaconList[i]},"
            }

            Log.i("Previous String ", previousString)
            Log.i("Current String ", currentString)
            Log.i("$$$ Filtered String 111 $$$", filteredString)

            var filteredString2 = "[ "
            val dict = mutableMapOf<String, Float>()
            filteredBeaconList.forEachIndexed { index, value -> dict += ("$index" to value) }
            val list = dict.toList().sortedByDescending { (key, value) -> value }.subList(0, 7)

            filteredBeaconList.forEachIndexed { index, fl -> filteredBeaconList[index] = -200F }
            list.forEach { (index, value) -> filteredBeaconList[index.toInt()] = value }
            filteredBeaconList.forEachIndexed { index, fl -> filteredString2 += "${filteredBeaconList[index]}," }

            // 상위 7개만 뽑아서 저장
            Log.i("$$$ Filtered 7 Beacon $$$", "$filteredString2 ]")


            //getModelOutput(filteredBeaconList)
            //previousBeaconArray.clear()
         }
         */

         addtoCSV(filteredBeaconCell.toTypedArray())
         binding.beaconList.adapter = ArrayAdapter(this, R.layout.simple_list_item_1, beacons.map { "Major ${it.id2}          Minor: ${it.id3}\nRaw rssi: ${it.rssi}\n" }.toTypedArray())
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

   private fun addtoCSV(cell: Array<String>) {
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
            probabilityString += "${probability}, "
         }
         Log.i("$$$ Probability $$$ ", "$probabilityString ]")

         val maxProbability = probabilityArray.maxOrNull()
         val mappedLabel = labelList[probabilityArray.indexOf(maxProbability)]

         if (locationQueue.size >= 7) {
            locationQueue.removeAt(0)
         }
         locationQueue.add(mappedLabel)

         var locationString = ""
         locationQueue.forEach { locationString += "$it " }
         binding.locationQueue.text = locationString

         estimatedLocationList.add(mappedLabel)
         addtoCSV(arrayOf(mappedLabel))
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
            if (!shouldShowRequestPermissionRationale(permissions[i])) {
               permission.neverAskAgainPermissions.add(permissions[i])
            }
         }
      }
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
         "OrientationValue",
         "OrientationString",
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
      azimuth = Math.toDegrees(values[0].toDouble()).toFloat()
      // 좌우 기울기 값
      pitch = Math.toDegrees(values[1].toDouble()).toFloat()
      // 앞뒤 기울기 값
      roll = Math.toDegrees(values[2].toDouble()).toFloat()

      if (azimuth < 0) {
         azimuth += 360
      }

      Log.i("mmmmmmm azimuth: ", azimuth.toString())
      Log.i("mmmmmmm pitch: ", pitch.toString())
      Log.i("mmmmmmm roll: ", roll.toString())
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

   private fun setLabelList() {
      labelList =
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


   companion object {
      const val TAG = "MainActivity"
      val PERMISSION_REQUEST_BACKGROUND_LOCATION = 0
      val PERMISSION_REQUEST_BLUETOOTH_SCAN = 1
      val PERMISSION_REQUEST_BLUETOOTH_CONNECT = 2
      val PERMISSION_REQUEST_FINE_LOCATION = 3
      const val timerTime = 10000L
   }
}