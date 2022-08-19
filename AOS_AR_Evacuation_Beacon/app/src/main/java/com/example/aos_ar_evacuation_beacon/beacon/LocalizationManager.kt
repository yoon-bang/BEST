package com.example.aos_ar_evacuation_beacon.beacon

import android.content.Context
import android.util.Log
import androidx.lifecycle.Observer
import com.example.aos_ar_evacuation_beacon.BeaconApplication
import com.example.aos_ar_evacuation_beacon.constant.AdjacentCell
import com.example.aos_ar_evacuation_beacon.constant.BeaconConstants
import com.example.aos_ar_evacuation_beacon.constant.Position
import com.example.aos_ar_evacuation_beacon.kalman.KalmanFilter
import com.example.aos_ar_evacuation_beacon.repository.DirectionRepository
import com.example.aos_ar_evacuation_beacon.repository.LocationRepository
import com.example.aos_ar_evacuation_beacon.ui.ARActivity
import com.example.aos_ar_evacuation_beacon.ui.LocalizationActivity
import com.example.aos_ar_evacuation_beacon.viewModel.MainViewModel
import com.google.firebase.ml.modeldownloader.CustomModel
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.MonitorNotifier
import org.tensorflow.lite.Interpreter
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime
import kotlin.math.abs

class LocalizationManager constructor(private val activity: ARActivity, private val context: Context, private val application: BeaconApplication, private val viewModel: MainViewModel) {
   val locationRepository = LocationRepository.instance
   val directionRepository = DirectionRepository.instance
   private lateinit var kalman: KalmanFilter

   private lateinit var beaconManager: BeaconCustomManager

   // bid, rssi
   private var threshold = 2

   private lateinit var conditions: CustomModelDownloadConditions
   private lateinit var interpreter: Interpreter

   private var currentBeaconArray = mutableListOf<Float>()
   private var previousBeaconArray = mutableListOf<Float>()

   private var locationIndex = 0

   init {
      loadMLModel()
   }

   fun setting() {
      beaconManager = BeaconCustomManager.instance
      val regionViewModel = BeaconManager.getInstanceForApplication(context).getRegionViewModel(application.region)
      regionViewModel.regionState.observe(activity, monitoringObserver)
      regionViewModel.rangedBeacons.observe(activity, rangingObserver)
      kalman = KalmanFilter(R = 0.001f, Q = 2f)
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

   private val monitoringObserver = Observer<Int> { state ->
      var stateString = "inside"

      if (state == MonitorNotifier.OUTSIDE) {
         stateString == "outside"
         Log.d(LocalizationActivity.TAG, "Outside of the beacon region -- no beacons detected")
      } else {
         Log.d(LocalizationActivity.TAG, "Inside the beacon region.")
      }
      Log.d(LocalizationActivity.TAG, "monitoring state changed to : $stateString")
   }

   private val rangingObserver = Observer<Collection<Beacon>> { beacons ->
      Log.d(LocalizationActivity.TAG, "Ranged: ${beacons.count()} beacons")
      if (BeaconManager.getInstanceForApplication(activity).rangedRegions.isNotEmpty()) {
         val filteredBeaconList = java.util.ArrayList<String>()
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
         }

         // 1~23 자르고 float 으로 변환
         val filteredBeaconFloatList = mutableListOf<Float>()
         filteredBeaconList.subList(1, 23).forEach { value -> filteredBeaconFloatList.add(value.toFloat()) }
         filteredBeaconFloatList.forEachIndexed { index, fl ->
            //Log.i("filteredBeaconFloatList $index: ", fl.toString())
         }
         selectBeacon(rawBeaconFloatList, filteredBeaconFloatList, BeaconConstants.beaconNum)
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

//         Log.i("Previous String ", previousString)
//         Log.i("Current String ", currentString)
         // 두 개를 하나로 합친 결과
//         Log.i("$$$ Combined String $$$", combinedString)

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
//            Log.w("combined $index: ", "${fl}")
         }
         // 상위 n개만 뽑아서 저장
//         Log.i("$$$ Filtered $beaconNum Beacon $$$", "$sortedString ]")


         getModelOutput(combinedBeaconList)
         previousBeaconArray.clear()
      }
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
         val probabilityArray = mutableListOf<Float>()

         for (i in 0 until probabilities.capacity()) {
            val probability = probabilities.get(i)
            probabilityArray.add(probability)
         }

         val maxProbability = probabilityArray.maxOrNull()
         var mappedLabel: String = BeaconConstants.labelList[probabilityArray.indexOf(maxProbability)]

         if (BeaconConstants.testMode) {
            // 처음
            if (locationRepository.isStart.value == true) {
               val curPosition = locationRepository.pathList.value?.get(0)!!
               locationRepository.updateStartLocation(curPosition)
               locationRepository.updateIsStart(false)
               locationRepository.updatePreviousString(curPosition)
               viewModel.addQueue(curPosition)
               locationRepository.updatePathListIndex()
            } else {
               val index = locationRepository.pathListIndex.value!!
               val position = locationRepository.pathList.value?.get(index)
               val nextPosition = locationRepository.pathList.value?.get(index + 1)

               position?.let {
                  locationRepository.updatePreviousString(it)
                  viewModel.addQueue(it)
               }

               if (index == locationRepository.pathList.value?.size!! - 1) {
                  locationRepository.updateIsEvacuated()
                  return
               }

               var currentPoint = locationRepository.calculateCenter(position!!)
               var nextPoint = locationRepository.calculateCenter(nextPosition!!)
               val degree = directionRepository.vectorBetween2Points(currentPoint.first, currentPoint.second, nextPoint.first, nextPoint.second)
               directionRepository.updateArrowDegree(degree)
               locationRepository.updatePathListIndex()
            }
         } else {
            // 처음
            if (locationRepository.isStart.value == true) {
               locationRepository.updateStartLocation("R01")
               locationRepository.updateIsStart(false)
               locationRepository.updatePreviousString("R01")
               viewModel.addQueue(mappedLabel)
            } else {
               // 처음 아닐때
               val previousLocation = locationRepository.previousLocation.value
               Log.i("filteredLocationnn:", previousLocation.toString())
               val location = filterErrorWithHeading(Position.valueOf(previousLocation!!), Position.valueOf(mappedLabel))
               locationRepository.updatePreviousString(location.position)
               Log.i("mappedLocationnn:", location.position)

               // AR 화살표 돌리기
               val index = locationRepository.pathList.value?.indexOf(location.position)
               if (index!! < 0) return

               if (index < locationRepository.pathList.value!!.size - 1) {
                  val start = locationRepository.pathList.value?.get(index)
                  val end = locationRepository.pathList.value?.get(index + 1)
                  Log.w("Arrow start: ", start.toString())
                  Log.w("Arrow end: ", end.toString())
                  val startPoint = start?.let { locationRepository.calculateCenter(it) }
                  val endPoint = end?.let { locationRepository.calculateCenter(it) }
                  val degree = directionRepository.vectorBetween2Points(startPoint?.first!!, startPoint.second, endPoint?.first!!, endPoint.second)
                  Log.w("Arrow Degree: ", degree.toString())
                  directionRepository.updateArrowDegree(degree)
               }
               if (location.position.contains("E")) locationRepository.updateIsEvacuated()
               Log.i("filteredLabelLocation111: ", location.toString())
               Log.i("mappedLabelLocation111: ", mappedLabel)
               viewModel.addQueue(location.position)
            }
         }
         Log.i("Output Label: ", mappedLabel)
      } catch (e: IOException) {
         Log.e("$$$ Output Error $$$", e.toString())
      }
   }

   private fun filterErrorWithHeading(previousLocation: Position, currentLocation: Position = Position.unknown): Position {
      if (previousLocation == Position.unknown || currentLocation == Position.unknown) return Position.unknown

      Log.i("***previousLocation: ", previousLocation.position)
      Log.i("***currentLocation: ", currentLocation.position)

      val adjacentCellList = mutableListOf<String>()
      val adjacentCells = AdjacentCell.valueOf(previousLocation.position).cell.toList()
      for (cell in adjacentCells) {
         if (cell is List<*>) {
            val list = cell.filterIsInstance<Position>()
            list.forEachIndexed { _, value ->
               adjacentCellList.add(value.position)
            }
         } else {
            adjacentCellList.add(cell.toString())
         }
      }

      adjacentCellList.forEachIndexed { index, s ->
         //Log.i("***adjacentCellList $index: ", s)
      }

      val userDirection = directionRepository.userDirection.value
      Log.i("=== updateddirection ===", userDirection.toString())

      // currentLocation 과 previousLocation 이 인접하면
      if (adjacentCellList.contains(currentLocation.position)) {
         var candidateCells = adjacentCells[userDirection?.index!!]
         val newCandidateCells = mutableListOf<Position>()

         if (candidateCells is List<*>) {
            val list = candidateCells.filterIsInstance<Position>()
            list.forEach { value ->
               newCandidateCells.add(Position.valueOf(value.position))
            }
         } else {
            newCandidateCells.add(Position.valueOf(candidateCells.toString()))
         }
         newCandidateCells.removeAll { it == Position.unknown }

         return if (newCandidateCells.contains(currentLocation)) {
            Log.i("***filterErrorWithHeading ", "인접셀, 방향, 모델")
            currentLocation
         } else {
            Log.i("***filterErrorWithHeading ", "인접셀, 방향X, 그대로있기")
            previousLocation
         }
      } else { // 인접셀 아닐 때
         val currentPair = locationRepository.calculateCenter(currentLocation.position)
         val currentX = currentPair.first
         val currentY = currentPair.second

         val previousPair = locationRepository.calculateCenter(previousLocation.position)
         val previousX = previousPair.first
         val previousY = previousPair.second
         val direction = directionRepository.classifyDirection(directionRepository.vectorBetween2Points(previousX!!, previousY!!, currentX!!, currentY!!))

         return if (direction == userDirection) {
            Log.i("***filterErrorWithHeading ", "인접셀X, 방향 같음, 변경")
            var candidateCells = adjacentCells[direction?.index!!]

            var newCells = mutableListOf<Position>()
            if (candidateCells is List<*>) {
               val list = candidateCells.filterIsInstance<Position>()
               list.forEach { value ->
                  newCells.add(Position.valueOf(value.position))
               }
            } else {
               newCells.add(Position.valueOf(candidateCells.toString()))
            }
            newCells.removeAll { it == Position.unknown }

            if (newCells.isEmpty()) {
               previousLocation
            } else {
               newCells.first()
            }
         } else {
            Log.i("***filterErrorWithHeading ", "인접셀X, 방향 다름, 변경X")
            previousLocation
         }
      }
   }

}