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
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.baconbeacon.beaconexample.csv.CSVHelper
import com.baconbeacon.beaconexample.kalman.KalmanFilter
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beaconreference.R
import java.net.NetworkInterface
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    lateinit var beaconListView: ListView
    lateinit var beaconCountTextView: TextView
    lateinit var monitoringButton: Button
    lateinit var rangingButton: Button
    private lateinit var beaconReferenceApplication: BeaconExample
    var alertDialog: AlertDialog? = null
    var neverAskAgainPermissions = ArrayList<String>()

    private val fileName = "AOS_exp3_"
    var beaconDataList = arrayListOf<Array<String>>()
    var filteredRssiArr = arrayListOf<Float>()
    lateinit var filePath: String
    lateinit var csvHelper: CSVHelper
    private lateinit var timer: CountDownTimer

    private lateinit var kalman: KalmanFilter
    var previousRssi = 0

    lateinit var wifiManager: WifiManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        beaconReferenceApplication = application as BeaconExample

        // Set up a Live Data observer for beacon data
        val regionViewModel = BeaconManager.getInstanceForApplication(this)
            .getRegionViewModel(beaconReferenceApplication.region)
        // observer will be called each time the monitored regionState changes (inside vs. outside region)
        regionViewModel.regionState.observe(this, monitoringObserver)
        // observer will be called each time a new list of beacons is ranged (typically ~1 second in the foreground)
        regionViewModel.rangedBeacons.observe(this, rangingObserver)
        rangingButton = findViewById(R.id.rangingButton)
        monitoringButton = findViewById(R.id.monitoringButton)
        beaconListView = findViewById(R.id.beaconList)
        beaconCountTextView = findViewById(R.id.beaconCount)
        beaconCountTextView.text = "No beacons detected"
        beaconListView.adapter =
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
        setupTimer()
        kalman = KalmanFilter(R = 0.001f, Q = 2f)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        Log.w("dddd", "${getMacAddress()}")
    }

    private fun setupTimer() {
        timer = object : CountDownTimer(300000, 1000) {
            override fun onTick(p0: Long) {
                Log.v("timer: ", p0.toString())
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
            beaconCountTextView.text = "Outside of the beacon region -- no beacons detected"
            beaconListView.adapter =
                ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayOf("--"))
        } else {
            beaconCountTextView.text = "Inside the beacon region."
        }
        Log.d(TAG, "monitoring state changed to : $stateString")
        val builder = AlertDialog.Builder(this)
        builder.setTitle(dialogTitle)
        builder.setMessage(dialogMessage)
        builder.setPositiveButton(android.R.string.ok, null)
        alertDialog?.dismiss()
        alertDialog = builder.create()
        // alertDialog?.show()
    }

    @SuppressLint("MissingPermission")
    private val rangingObserver = Observer<Collection<Beacon>> { beacons ->
        Log.d(TAG, "Ranged: ${beacons.count()} beacons")
        if (BeaconManager.getInstanceForApplication(this).rangedRegions.isNotEmpty()) {
            beaconCountTextView.text = "Ranging enabled: ${beacons.count()} beacon(s) detected"
            beaconListView.adapter = ArrayAdapter(this,
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

    fun rangingButtonTapped(view: View) {
        val beaconManager = BeaconManager.getInstanceForApplication(this)
        if (beaconManager.rangedRegions.isEmpty()) {
            beaconManager.startRangingBeacons(beaconReferenceApplication.region)
            rangingButton.text = "Stop Ranging"
            beaconCountTextView.text = "Ranging enabled -- awaiting first callback"
        } else {
            beaconManager.stopRangingBeacons(beaconReferenceApplication.region)
            rangingButton.text = "Start Ranging"
            beaconCountTextView.text = "Ranging disabled -- no beacons detected"
            beaconListView.adapter =
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
            monitoringButton.text = "Stop Monitoring"

        } else {
            beaconManager.stopMonitoring(beaconReferenceApplication.region)
            dialogTitle = "Beacon monitoring stopped."
            dialogMessage = "You will no longer see dialogs when becaons start/stop being detected."
            monitoringButton.text = "Start Monitoring"
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

    private fun getMacAddress(): String? = try {
        NetworkInterface.getNetworkInterfaces().toList().find { networkInterface ->
            networkInterface.name.equals("wlan0", ignoreCase = true)
        }?.hardwareAddress?.joinToString(separator = ":") { byte -> "%02X".format(byte) }
    } catch (ex: Exception) {
        ex.printStackTrace()
        null
    }

    companion object {
        val TAG = "MainActivity"
    }
}