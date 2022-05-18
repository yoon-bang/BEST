package com.baconbeacon.beaconexample.csv

import android.util.Log
import com.opencsv.CSVWriter
import org.altbeacon.beacon.BuildConfig
import java.io.File
import java.io.FileWriter
import java.io.IOException

class CSVHelper(private val filePath: String) {
    fun writeData(fileName: String, dataList: ArrayList<Array<String>>) {
        try {
            FileWriter(File("$filePath/$fileName")).use { fw ->
                Log.d("filePath: ", "$filePath/$fileName")
                CSVWriter(fw).use {
                    for (data in dataList) {
                        it.writeNext(data)
                    }
                }
            }
        } catch (e: IOException) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
        }
    }
}