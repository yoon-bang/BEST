package com.example.aos_ar_evacuation_beacon.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aos_ar_evacuation_beacon.constant.Floor
import com.example.aos_ar_evacuation_beacon.repository.DirectionRepository
import com.example.aos_ar_evacuation_beacon.repository.LocationRepository
import com.example.aos_ar_evacuation_beacon.ui.ARActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainViewModel : ViewModel() {
   private val _timerCount = MutableLiveData<Int>()
   val timerCount: LiveData<Int> = _timerCount

   val locationRepository = LocationRepository.instance
   val directionRepository = DirectionRepository.instance
   private lateinit var job: Job

   val evacuationQueue = locationRepository.evacuationQueue
   val currentLocation = locationRepository.currentLocation
   val previousLocation = locationRepository.previousLocation
   val isEvacuated = locationRepository.isEvacuated

   val pathList = locationRepository.pathList
   val fireCellList = locationRepository.fireCellList
   val predictedCellList = locationRepository.predictedCellList
   val congestionCellList = locationRepository.congestionCellList

   val currentFloor = locationRepository.currentFloor
   val previousFloor = locationRepository.previousFloor

   var pathListIndex = locationRepository.pathListIndex
   val isStart = locationRepository.isStart
   val startPoint = locationRepository.startLocation

   init {
      _timerCount.value = locationRepository.pathList.value?.size
   }

   fun updatePathList(value: List<String>) {
      locationRepository.updatePathList(value)
   }

   fun updateCongestionCellList(value: List<String>) {
      locationRepository.updateCongestionCellList(value)
   }

   fun updatePredictedCellList(value: List<String>) {
      locationRepository.updatePredictedCellList(value)
   }

   fun updateFireCellList(value: List<String>) {
      locationRepository.updateFireCellList(value)
   }


   fun updateIsEvacuated() {
      locationRepository.updateIsEvacuated()
   }

   fun addQueue(location: String) {
      locationRepository.addQueue(location)
   }

   fun clearQueue() {
      locationRepository.clearQueue()
   }

   fun timerStart(aCustom1FView: ARActivity.FirstFloorView) {
      if (::job.isInitialized) job.cancel()

      job = viewModelScope.launch {
         while ((_timerCount.value!! - 1) > 0) {
            val index = pathListIndex.value!!
            val currentCell = locationRepository.pathList.value?.get(index)
            if (currentCell != null) {
               val pair = calculateCenter(currentCell)
               locationRepository.updateCurrentPoint(pair.first, pair.second)
               Log.i("currentCellCell: ", currentCell)
               //_currentCell.value = currentCell
               var currentX: Float
               var currentY: Float
               var nextX: Float
               var nextY: Float

               val nextCell = locationRepository.pathList.value?.get(index + 1)
               nextCell?.let { Log.i("nextCellCell: ", it) }
               //_nextCell.value = nextCell
               var currentPair = currentCell?.let { calculateCenter(it) }
               currentX = currentPair?.first!!
               currentY = currentPair?.second!!

               var nextPair = nextCell?.let { calculateCenter(it) }
               nextX = nextPair?.first!!
               nextY = nextPair?.second!!

               val degree = directionRepository.vectorBetween2Points(currentX, currentY, nextX, nextY)
               Log.i("angleDegree: ", degree.toString())
               //locationRepository.updateLocationString(currentCell)
               //aCustom1FView?.invalidate()
               locationRepository.updatePathListIndex()
               decreaseTimer()
               locationRepository.updatePreviousPoint(locationRepository.currentUserX.value!!, locationRepository.currentUserY.value!!)
               delay(3000L)
            }

         }
      }
   }

   fun update3DArrowDegree() {
      val currentCell = previousLocation.value
      if (pathList.value?.isEmpty() == true) return

      val index = pathList.value?.indexOf(currentCell)!!

      if (index < pathList.value?.size?.minus(1)!!) {
         val start = calculateCenter(pathList.value!![index])
         val end = calculateCenter(pathList.value!![index + 1])
         val vector = directionRepository.vectorBetween2Points(start.first, start.second, end.first, end.second)
         directionRepository.updateArrowDegree(vector)

      } else {
         locationRepository.updateIsEvacuated()
         Log.i("Safely Exit!!!", "Done!!! ")
      }
   }

   fun updateFloor(floor: Floor) {
      locationRepository.updateCurrentFloor(floor)
   }

   private fun decreaseTimer() {
      _timerCount.value = _timerCount.value?.minus(1)
   }

   fun is2fTo1fBackDoor(): Boolean {
      return if (previousLocation.value == "S06" && evacuationQueue.value!!.contains("S05")) {
         locationRepository.updateCurrentFloor(Floor.First)
         true
      } else {
         false
      }
   }

   fun is2fTo1f(): Boolean {
      return if ((previousLocation.value == "S03") && evacuationQueue.value!!.contains("S04")) {
         Log.i("is2fTo1f : ", "true")
         locationRepository.updateCurrentFloor(Floor.First)
         true
      } else {
         false
      }
   }

   fun is1fTo2f(): Boolean {
      return if (((previousLocation.value == "S03") || (previousLocation.value == "S04") || (previousLocation.value == "H01")) && (evacuationQueue.value!!.contains("S02") || evacuationQueue.value!!.contains(
            "E01"))
      ) {
         Log.i("is1fTo2f : ", "true")
         locationRepository.updateCurrentFloor(Floor.Second)
         true
      } else {
         false
      }
   }

   fun is1fToBase(): Boolean {
      Log.i("is1fToBase : ", "true")
      return if ((previousLocation.value == "E02" || previousLocation.value == "S08") && (evacuationQueue.value!!.contains("S07") || evacuationQueue.value!!.contains("H02"))) {
         locationRepository.updateCurrentFloor(Floor.Base)
         true
      } else {
         false
      }
   }

   fun isBaseTo1f(): Boolean {
      return if ((previousLocation.value == "E02" || previousLocation.value == "S07") && (evacuationQueue.value!!.contains("S08") || evacuationQueue.value!!.contains("S09"))) {
         Log.i("isBaseTo1f : ", "true")
         locationRepository.updateCurrentFloor(Floor.First)
         true
      } else {
         false
      }
   }

   fun is1fTo2fBackDoor(): Boolean {
      return if ((previousLocation.value == "S06") && (evacuationQueue.value!!.contains("H02") || evacuationQueue.value!!.contains("A07"))) {
         Log.i("is1fTo2fBackDoor : ", "true")
         locationRepository.updateCurrentFloor(Floor.Second)
         true
      } else {
         false
      }
   }


   fun timerStop() {
      if (::job.isInitialized) job.cancel()
   }

   fun calculateCenter(location: String): Pair<Float, Float> {
      val pointList = when (currentFloor.value) {
         Floor.First -> {
            Log.i("First5lllocation: ", location)
            locationRepository.newMapDict1f[location]
         }
         Floor.Second -> {
            Log.i("Secondlllocation: ", location)
            locationRepository.newMapDict2f[location]
         }
         else -> {
            Log.i("Baselllocation: ", location)
            locationRepository.newMapDictBase[location]
         }
      }
      val x0 = (pointList?.get(0)?.first).toString().toFloat()
      val x1 = (pointList?.get(1)?.first).toString().toFloat()

      val y0 = (pointList?.get(1)?.second).toString().toFloat()
      val y1 = (pointList?.get(2)?.second).toString().toFloat()

      val width = abs(x1 - x0)
      val height = abs(y1 - y0)

      val centerX = (x0 + (width / 2)) * 30
      val centerY = (y0 + (height / 2)) * 30
      return Pair(centerX, centerY)
   }
}