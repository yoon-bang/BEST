package com.baconbeacon.beaconexample

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.baconbeacon.beaconexample.csv.CSVHelper
import com.baconbeacon.beaconexample.kalman.KalmanFilter
import com.google.firebase.ml.modeldownloader.CustomModel
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beaconreference.databinding.ActivityMainBinding
import org.tensorflow.lite.Interpreter
import java.io.File
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var beaconReferenceApplication: BeaconExample
    var alertDialog: AlertDialog? = null
    var neverAskAgainPermissions = ArrayList<String>()

    private val fileName = "AOS_exp4_"
    var beaconDataList = arrayListOf<Array<String>>()
    var filteredRssiArr = arrayListOf<Float>()
    lateinit var filePath: String
    private lateinit var csvHelper: CSVHelper
    private lateinit var timer: CountDownTimer

    private lateinit var kalman: KalmanFilter
    var previousRssi = 0

    lateinit var wifiManager: WifiManager

    lateinit var conditions: CustomModelDownloadConditions
    lateinit var modelFile: File
    lateinit var interpreter: Interpreter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        beaconReferenceApplication = application as BeaconExample

        // Set up a Live Data observer for beacon data
        val regionViewModel = BeaconManager.getInstanceForApplication(this)
            .getRegionViewModel(beaconReferenceApplication.region)
        // observer will be called each time the monitored regionState changes (inside vs. outside region)
        regionViewModel.regionState.observe(this, monitoringObserver)
        // observer will be called each time a new list of beacons is ranged (typically ~1 second in the foreground)
        regionViewModel.rangedBeacons.observe(this, rangingObserver)

        binding.beaconCount.text = "No beacons detected"
        binding.beaconList.adapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayOf("--"))

        beaconDataList.add(arrayOf("Date",
            "UUID",
            "Major",
            "Minor",
            "RSSI",
            "Filtered RSSI",
            "Error",
            "AP RSSI"))
//        filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//            .toString()
        filePath = Environment.getExternalStorageDirectory().absolutePath + "/Download"
        csvHelper = CSVHelper(filePath)
        permission()
        Toast.makeText(this@MainActivity.applicationContext, "Start Detect", Toast.LENGTH_LONG)
            .show()
        kalman = KalmanFilter(R = 0.001f, Q = 2f)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        setupTimer()
        printDeviceInfo()
        loadMLmodel()
    }

    private fun printDeviceInfo() {
        Log.w("dddd", "${getMacAddress()}")
        // UUID 는 앱을 삭제하고 다시 설치하면 값이 변경됨
        Log.w("cccc", "${UUID.randomUUID()}")
    }

    private fun loadMLmodel() {
        // Firebase 에서 ML 모델 불러오기
        conditions = CustomModelDownloadConditions.Builder().requireWifi().build()

        FirebaseModelDownloader.getInstance()
            .getModel("AOS_rssi_Model", DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND, conditions)
            .addOnSuccessListener { model: CustomModel? ->
                modelFile = model?.file!!
                if (modelFile != null) {
                    interpreter = Interpreter(modelFile)
                }
            }
    }

    private fun setupTimer() {
        timer = object : CountDownTimer(300000, 1000) {
            override fun onTick(time: Long) {
                Log.v("timer: ", time.toString())
            }

            override fun onFinish() {
                save()
                Toast.makeText(this@MainActivity.applicationContext, "CSV 저장 완료", Toast.LENGTH_LONG)
                    .show()
            }
        }.start()
    }

    private fun permission() {
        if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED

        ) {
            val permission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION)
            ActivityCompat.requestPermissions(this, permission, 100)
        }
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
    }

    private val monitoringObserver = Observer<Int> { state ->
        var dialogTitle = "Beacons detected"
        var dialogMessage = "didEnterRegionEvent has fired"
        var stateString = "inside"

        if (state == MonitorNotifier.OUTSIDE) {
            dialogTitle = "No beacons detected"
            dialogMessage = "didExitRegionEvent has fired"
            stateString == "outside"
            binding.beaconCount.text = "Outside of the beacon region -- no beacons detected"
            binding.beaconList.adapter =
                ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayOf("--"))
        } else {
            binding.beaconCount.text = "Inside the beacon region."
        }
        Log.d(TAG, "monitoring state changed to : $stateString")

        val builder = AlertDialog.Builder(this)
        builder.setTitle(dialogTitle).setMessage(dialogMessage)
            .setPositiveButton(android.R.string.ok, null)

        alertDialog?.dismiss()
        alertDialog = builder.create()
        // alertDialog?.show()
    }

    @SuppressLint("MissingPermission")
    private val rangingObserver = Observer<Collection<Beacon>> { beacons ->
        Log.d(TAG, "Ranged: ${beacons.count()} beacons")
        if (BeaconManager.getInstanceForApplication(this).rangedRegions.isNotEmpty()) {
            binding.beaconCount.text = "Ranging enabled: ${beacons.count()} beacon(s) detected"
            binding.beaconList.adapter = ArrayAdapter(this,
                android.R.layout.simple_list_item_1,
                beacons.sortedBy { it.distance }.map {
                    "UUID: ${it.id1}\nmajor: ${it.id2} minor:${it.id3}\nRSSI: ${it.rssi}\n Filtered RSSI: ${
                        kalman.filter(it.rssi.toFloat())
                    }"
                }.toTypedArray())

            if (beacons.map { it.rssi }.isNotEmpty()) {
                var uuid = beacons.map { it.id1 }[0].toString()
                var major = beacons.map { it.id2 }[0].toString()
                var minor = beacons.map { it.id3 }[0].toString()
                var rssi = beacons.map { it.rssi }[0].toString()
                var filteredRssi = kalman.filter(rssi.toFloat()).toString()
                var error = (abs(rssi.toDouble()) - abs(filteredRssi.toDouble())).pow(2).toString()
                val wifi = wifiManager.connectionInfo

                // bssid : access point 의 주소
                Log.w("$$$ WIFI INFO $$$",
                    "mac: ${wifi.macAddress}, RSSI: ${wifi.rssi}, BSSID:${wifi.bssid}, SSID:${wifi.ssid}")

                // threshold 넘어가면 reset
//                if (abs(abs(previousRssi)) - abs(rssi.toInt()) > 15) {
//                    kalman = KalmanFilter(R = 0.001f, Q = 2f)
//                    Log.w("$$$ Detected Beacons $$$", "Filter Reset")
//                }

                beaconDataList.add(arrayOf(LocalDateTime.now().toString(),
                    uuid,
                    major,
                    minor,
                    rssi,
                    filteredRssi,
                    error,
                    wifi.rssi.toString()))
                filteredRssiArr.add(filteredRssi.toFloat())
                doInference(filteredRssi)

//                Log.i("$$$ Detected Beacons $$$",
//                    "UUID: $uuid major: $major minor:$minor RSSI: $rssi Filtered RSSI: $filteredRssi")
                Log.i("$$$ Detected Beacons $$$",
                    "RSSI: $rssi Filtered RSSI: $filteredRssi, Error: $error")
                previousRssi = rssi.toInt()
            } else {
                Log.w("$$$ Detected Empty Beacons $$$", beacons.map { it.rssi }.toString())
            }
        }
    }

    fun save() {
        csvHelper.writeData("$fileName${
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
        }.csv", beaconDataList)
    }

    fun rangingButtonTapped() {
        val beaconManager = BeaconManager.getInstanceForApplication(this)
        if (beaconManager.rangedRegions.isEmpty()) {
            beaconManager.startRangingBeacons(beaconReferenceApplication.region)
            binding.rangingButton.text = "Stop Ranging"
            binding.beaconCount.text = "Ranging enabled -- awaiting first callback"
        } else {
            beaconManager.stopRangingBeacons(beaconReferenceApplication.region)
            binding.rangingButton.text = "Start Ranging"
            binding.beaconCount.text = "Ranging disabled -- no beacons detected"
            binding.beaconList.adapter =
                ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayOf("--"))
        }
    }

    fun monitoringButtonTapped(view: View) {
        var dialogTitle = ""
        var dialogMessage = ""
        val beaconManager = BeaconManager.getInstanceForApplication(this)
        if (beaconManager.monitoredRegions.isEmpty()) {
            beaconManager.startMonitoring(beaconReferenceApplication.region)
            dialogTitle = "Beacon monitoring started."
            dialogMessage =
                "You will see a dialog if a beacon is detected, and another if beacons then stop being detected."
            binding.monitoringButton.text = "Stop Monitoring"

        } else {
            beaconManager.stopMonitoring(beaconReferenceApplication.region)
            dialogTitle = "Beacon monitoring stopped."
            dialogMessage = "You will no longer see dialogs when becaons start/stop being detected."
            binding.monitoringButton.text = "Start Monitoring"
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle(dialogTitle)
        builder.setMessage(dialogMessage)
        builder.setPositiveButton(android.R.string.ok, null)
        alertDialog?.dismiss()
        alertDialog = builder.create()
        alertDialog?.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
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

    // 랜덤 mac 주소 : 0A:7B:45:A0:E2:6C
    // 휴대전화 mac 주소 : D4:11:A3:7C:89:0B
    // Android 8.0 부터 Android 기기는 네트워크와 현재 연결되지 않은 상태에서 새 네트워크를 탐색할 때 무작위 MAC 주소 사용.
    // Android 10 에서는 기본적으로 클라이언트 모드, SoftAP, Wi-Fi Direct 에서 MAC 주소 무작위 순서 지정이 사용 설정됨.
    // Why? MAC 주소로 Wi-Fi 패킷 추적을 통해 단말기 위치를 파악하는 걸 방지하기 위함

    // 삭제 후 재설치 or 재부팅해도 값이 변경되지 않음!

    // wifi 설정에서 랜덤 MAC / 휴대전화 MAC 중에서 랜덤 MAC 이 기본으로 설정돼있어서
    // 코드상에서 가져오는 값이랑 휴대전화 정보의 Wi-Fi Mac 주소가 다른 이슈가 있음
    // WiFi 연결 후 설정에서 휴대전화 MAC 로 변경하면 다바이스 wifi mac 주소 가져올 수 있음!
    // 그럼 네트워크에 연결될 때마다 설정을 바꿔줘야하는데, 어떻게 함??
    private fun getMacAddress(): String? = try {
        NetworkInterface.getNetworkInterfaces().toList().find { networkInterface ->
            networkInterface.name.equals("wlan0", ignoreCase = true)
        }?.hardwareAddress?.joinToString(separator = ":") { byte -> "%02X".format(byte) }
    } catch (exception: Exception) {
        exception.printStackTrace()
        null
    }

    // ANDROID_ID : 디바이스가 최초 Boot 될 때 생성 되는 64-bit 값
    // 디바이스를 공장초기화 하지 않는 이상 바뀌지 않는 고유 값
    // 앱 서명 키별로 ANDROID_ID 의 범위가 지정된다.
    // => 즉 릴리즈 버전과 디버깅 버전 APK 의 Android ID 가 다를 수 있다!
    // 보통 릴리즈 버전을 다운받기 때문에 큰 문제는 없을 듯!
    // 그러나, 공장 초기화와 인증키가 변경 될 때는 ANDROID_ID 가 변경된다.

    // 단말기 고유값만 필요한 경우라면 이걸 사용해도 괜찮을 것 같다!
    // 현재값: 100e2e222cfe79e1
    // 재설치 or 재부팅해도 값이 변경되지 않음
    private fun deviceID(): String {
        return Settings.Secure.getString(applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID)
    }

    private fun doInference(rssi: String) {
        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        input.putFloat(rssi.toFloat())

        val bufferSize = 1000 * java.lang.Float.SIZE / java.lang.Byte.SIZE
        val modelOutput = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())
        interpreter?.run(input, modelOutput)

        modelOutput.rewind()
        val probabilities = modelOutput.float

        Log.w("tttt ", probabilities.toString())
    }

    companion object {
        val TAG = "MainActivity"
    }
}