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

   val queueSize = 3

   val evacuationQueue = locationRepository.evacuationQueue
   val currentLocation = locationRepository.currentLocation

   val currentFloor = locationRepository.currentFloor
   val previousFloor = locationRepository.previousFloor

   private val _isEvacuated = MutableLiveData(false)
   val isEvacuated: LiveData<Boolean> = _isEvacuated

   var pathListIndex = 0
   val isStart = locationRepository.isStart

   init {
      _timerCount.value = locationRepository.pathList.value?.size
   }

   fun isExit() {
      _isEvacuated.value = currentLocation.value?.contains("E")
   }

   fun addQueue(location: String) {
      locationRepository.addQueue(location)
   }

   fun timerStart(aCustom1FView: ARActivity.FirstFloorView) {
      if (::job.isInitialized) job.cancel()

      job = viewModelScope.launch {
         while ((_timerCount.value!! - 1) > 0) {
            val currentCell = locationRepository.pathList.value?.get(pathListIndex)
            if (currentCell != null) {
               val pair = calculateCenter(currentCell)
               locationRepository.updateCurrentPoint(pair.first, pair.second)
               Log.i("currentCell: ", currentCell)
               var currentX: Float
               var currentY: Float
               var nextX: Float
               var nextY: Float

               val nextCell = locationRepository.pathList.value?.get(pathListIndex + 1)
               nextCell?.let { Log.i("nextCell: ", it) }
               var currentPair = currentCell?.let { calculateCenter(it) }
               currentX = currentPair?.first!!
               currentY = currentPair?.second!!

               var nextPair = nextCell?.let { calculateCenter(it) }
               nextX = nextPair?.first!!
               nextY = nextPair?.second!!

               directionRepository.angleBetween2Points(currentX, currentY, nextX, nextY)
               //locationRepository.updateLocationString(currentCell)
               //aCustom1FView?.invalidate()
               pathListIndex += 1
               decreaseTimer()
               locationRepository.updatePreviousPoint(locationRepository.currentUserX.value!!, locationRepository.currentUserY.value!!)
               delay(3000L)
            }

         }
      }
   }

   fun updateFloor(floor: Floor) {
      locationRepository.updateCurrentFloor(floor)
   }

   private fun decreaseTimer() {
      _timerCount.value = _timerCount.value?.minus(1)
   }

//   fun clearQueue() {
//      _evacuationQueue.value?.clear()
//   }


   fun is2fTo1fBackDoor(): Boolean {
      return if (currentLocation.value == "S06" && evacuationQueue.value!!.contains("S05")) {
         locationRepository.updateCurrentFloor(Floor.First)
         true
      } else {
         false
      }
   }

   fun is2fTo1f(): Boolean {
      Log.i("is2fTo1f curr: ", currentLocation.value.toString())
      evacuationQueue.value!!.forEachIndexed { index, s ->
         Log.i("is2fTo1f evacuationQueue $index: ", s)
      }

      return if ((currentLocation.value == "S03") && evacuationQueue.value!!.contains("S04")) {
         Log.i("is2fTo1f : ", "true")
         locationRepository.updateCurrentFloor(Floor.First)
         true
      } else {
         false
      }
   }

   fun is1fTo2f(): Boolean {
      return if (((currentLocation.value == "S03") || (currentLocation.value == "S04") || (currentLocation.value == "H01")) && (evacuationQueue.value!!.contains("S02") || evacuationQueue.value!!.contains(
            "E01"))
      ) {
         locationRepository.updateCurrentFloor(Floor.Second)
         true
      } else {
         false
      }
   }

   fun is1fToBase(): Boolean {
      return if ((currentLocation.value == "E02" || currentLocation.value == "S08") && (evacuationQueue.value!!.contains("S07") || evacuationQueue.value!!.contains("H02"))) {
         locationRepository.updateCurrentFloor(Floor.Base)
         true
      } else {
         false
      }
   }

   fun isBaseTo1f(): Boolean {
      evacuationQueue.value?.forEach {
         Log.i("isBaseTo1f", "evacuationQueue: $it")
      }
      Log.i("isBaseTo1f", "CurrentLocation: ${currentLocation.value}")
      return if ((currentLocation.value == "E02" || currentLocation.value == "S07") && (evacuationQueue.value!!.contains("S08") || evacuationQueue.value!!.contains("S09"))) {
         locationRepository.updateCurrentFloor(Floor.First)
         true
      } else {
         false
      }
   }

   fun is1fTo2fBackDoor(): Boolean {
//      evacuationQueue.value?.forEach {
//         Log.i("is1fTo2fBackDoor", "evacuationQueue: $it")
//      }
//      Log.i("is1fTo2fBackDoor", "CurrentLocation: ${currentLocation.value}")
      return if ((currentLocation.value == "S06") && (evacuationQueue.value!!.contains("H02") || evacuationQueue.value!!.contains("A07"))) {
         locationRepository.updateCurrentFloor(Floor.Second)
         true
      } else {
         false
      }
   }


   fun timerStop() {
      if (::job.isInitialized) job.cancel()
   }

   private fun calculateCenter(location: String): Pair<Float, Float> {
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