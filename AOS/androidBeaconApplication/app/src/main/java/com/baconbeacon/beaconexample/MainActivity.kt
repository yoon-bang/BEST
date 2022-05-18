package com.baconbeacon.beaconexample

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
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
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beaconreference.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    lateinit var beaconListView: ListView
    lateinit var beaconCountTextView: TextView
    lateinit var monitoringButton: Button
    lateinit var rangingButton: Button
    lateinit var beaconReferenceApplication: BeaconExample
    var alertDialog: AlertDialog? = null
    var neverAskAgainPermissions = ArrayList<String>()

    private val fileName = "AOS_exp3_"
    var beaconDataList = arrayListOf<Array<String>>()
    lateinit var filePath: String
    lateinit var csvHelper: CSVHelper
    private lateinit var timer: CountDownTimer

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

        beaconDataList.add(arrayOf("Date", "UUID", "Major", "Minor", "RSSI"))
//        filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//            .toString()
        filePath = Environment.getExternalStorageDirectory().absolutePath + "/Download"
        Log.w("aaaaaaaa", filePath)
        csvHelper = CSVHelper(filePath)
        permission()
        Toast.makeText(this@MainActivity.applicationContext, "Start Detect", Toast.LENGTH_LONG)
            .show()
        setupTimer()
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
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            val permission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)
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

    private val rangingObserver = Observer<Collection<Beacon>> { beacons ->
        Log.d(TAG, "Ranged: ${beacons.count()} beacons")
        if (BeaconManager.getInstanceForApplication(this).rangedRegions.isNotEmpty()) {
            beaconCountTextView.text = "Ranging enabled: ${beacons.count()} beacon(s) detected"
            beaconListView.adapter = ArrayAdapter(this,
                android.R.layout.simple_list_item_1,
                beacons.sortedBy { it.distance }
                    .map { "UUID: ${it.id1}\nmajor: ${it.id2} minor:${it.id3}\nRSSI: ${it.rssi}\n" }
                    .toTypedArray())


            if (beacons.map { it.rssi }.isNotEmpty()) {
                beaconDataList.add(arrayOf(LocalDateTime.now().toString(),
                    beacons.map { it.id1 }[0].toString(),
                    beacons.map { it.id2 }[0].toString(),
                    beacons.map { it.id3 }[0].toString(),
                    beacons.map { it.rssi }[0].toString()))
                Log.i("$$$ Detected Beacons $$$",
                    "UUID: ${beacons.map { it.id1 }} major: ${beacons.map { it.id2 }} minor:${beacons.map { it.id3 }} RSSI: ${beacons.map { it.rssi }}")
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

    companion object {
        val TAG = "MainActivity"
    }
}