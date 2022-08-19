package com.example.aos_ar_evacuation_beacon.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.aos_ar_evacuation_beacon.constant.Floor
import com.example.aos_ar_evacuation_beacon.constant.MapInfo
import com.example.aos_ar_evacuation_beacon.constant.Position
import com.example.aos_ar_evacuation_beacon.repository.base.BaseRepository
import kotlin.math.abs

class LocationRepository private constructor() : BaseRepository() {
   companion object {
      private var outInstance: LocationRepository? = null

      val instance: LocationRepository
         get() {
            if (outInstance == null) {
               outInstance = LocationRepository()
            }
            return outInstance!!
         }
   }

   init {
      calculateCoordinate1f()
      calculateCoordinate2f()
      calculateCoordinateBase()
   }

   // sample path
   // "H01", "S05", "S06", "H02", "A07", "A09", "A11", "E03", "A01"
   private val _pathList = MutableLiveData(listOf<String>())
   val pathList: LiveData<List<String>> = _pathList

   private val _fireCellList = MutableLiveData(listOf<String>())
   val fireCellList: LiveData<List<String>> = _fireCellList

   private val _congestionCellList = MutableLiveData(listOf<String>())
   val congestionCellList: LiveData<List<String>> = _congestionCellList

   private val _predictedCellList = MutableLiveData(listOf<String>())
   val predictedCellList: LiveData<List<String>> = _predictedCellList

   private val _currentUserX = MutableLiveData(0f)
   val currentUserX: LiveData<Float> = _currentUserX

   private val _currentUserY = MutableLiveData(0f)
   val currentUserY: LiveData<Float> = _currentUserY

   private val _previousUserX = MutableLiveData(0f)
   val previousUserX: LiveData<Float> = _previousUserX

   private val _previousUserY = MutableLiveData(0f)
   val previousUserY: LiveData<Float> = _previousUserY

   private val _firstPath = MutableLiveData(mutableListOf<String>())
   val firstPath: LiveData<MutableList<String>> = _firstPath

   private val _firstFireCell = MutableLiveData(mutableListOf<String>())
   val firstFireCell: LiveData<MutableList<String>> = _firstFireCell

   private val _firstPredictedCell = MutableLiveData(mutableListOf<String>())
   val firstPredictedCell: LiveData<MutableList<String>> = _firstPredictedCell

   private val _firstCongestionCell = MutableLiveData(mutableListOf<String>())
   val firstCongestionCell: LiveData<MutableList<String>> = _firstCongestionCell

   private val _secondPath = MutableLiveData(mutableListOf<String>())
   val secondPath: LiveData<MutableList<String>> = _secondPath

   private val _secondFireCell = MutableLiveData(mutableListOf<String>())
   val secondFireCell: LiveData<MutableList<String>> = _secondFireCell

   private val _secondPredictedCell = MutableLiveData(mutableListOf<String>())
   val secondPredictedCell: LiveData<MutableList<String>> = _secondPredictedCell

   private val _secondCongestionCell = MutableLiveData(mutableListOf<String>())
   val secondCongestionCell: LiveData<MutableList<String>> = _secondCongestionCell

   private val _basePath = MutableLiveData(mutableListOf<String>())
   val basePath: LiveData<MutableList<String>> = _basePath

   private val _baseFireCell = MutableLiveData(mutableListOf<String>())
   val baseFireCell: LiveData<MutableList<String>> = _baseFireCell

   private val _basePredictedCell = MutableLiveData(mutableListOf<String>())
   val basePredictedCell: LiveData<MutableList<String>> = _basePredictedCell

   private val _baseCongestionCell = MutableLiveData(mutableListOf<String>())
   val baseCongestionCell: LiveData<MutableList<String>> = _baseCongestionCell


   private val _currentFloor = MutableLiveData(Floor.First)
   val currentFloor: LiveData<Floor> = _currentFloor

   private val _previousFloor = MutableLiveData(Floor.First)
   val previousFloor: LiveData<Floor> = _previousFloor

   private val _currentLocation = MutableLiveData(Position.unknown.position)
   val currentLocation: LiveData<String> = _currentLocation

   private val _previousLocation = MutableLiveData(Position.unknown.position)
   val previousLocation: LiveData<String> = _previousLocation

   private val _isStart = MutableLiveData(true)
   val isStart: LiveData<Boolean> = _isStart

   private val _startLocation = MutableLiveData("")
   val startLocation: LiveData<String> = _startLocation

   private val _evacuationQueue = MutableLiveData(mutableListOf<String>())
   val evacuationQueue: LiveData<MutableList<String>> = _evacuationQueue

   private val _pathListIndex = MutableLiveData(0)
   val pathListIndex: LiveData<Int> = _pathListIndex

   private val _isEvacuated = MutableLiveData(false)
   val isEvacuated: LiveData<Boolean> = _isEvacuated

   lateinit var newMapDict1f: MutableMap<String, Array<Pair<Float, Float>>>
   lateinit var newMapDict2f: MutableMap<String, Array<Pair<Float, Float>>>
   lateinit var newMapDictBase: MutableMap<String, Array<Pair<Float, Float>>>

   fun updatePathList(value: List<String>) {
      _pathList.postValue(value)
   }

   fun updateCongestionCellList(value: List<String>) {
      _congestionCellList.postValue(value)
   }

   fun updatePredictedCellList(value: List<String>) {
      _predictedCellList.postValue(value)
   }

   fun updateFireCellList(value: List<String>) {
      _fireCellList.postValue(value)
   }

   fun updateIsEvacuated() {
      _isEvacuated.value = previousLocation.value?.contains("E")
   }

   fun updatePathListIndex() {
      _pathListIndex.value = _pathListIndex.value?.plus(1)
   }

   fun calculateCenter(location: String): Pair<Float, Float> {
      var pointList: Array<Pair<Float, Float>> = if (is1F(location)) {
         newMapDict1f[location]!!
      } else if (is2F(location)) {
         newMapDict2f[location]!!
      } else {
         newMapDictBase[location]!!
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

   fun updateCurrentFloor(floor: Floor) {
      _currentFloor.value = floor
   }

   fun updatePreviousFloor(floor: Floor) {
      _previousFloor.value = floor
   }

   fun updateIsStart(isStart: Boolean) {
      _isStart.value = isStart
   }

   fun updateStartLocation(location: String) {
      _startLocation.value = location
      if (is1F(location)) {
         updateCurrentFloor(Floor.First)
      } else if (is2F(location)) {
         updateCurrentFloor(Floor.Second)
      } else {
         updateCurrentFloor(Floor.Base)
      }
   }

   fun updateLocationString(location: String) {
      _currentLocation.value = location
      if (is1F(location)) {
         updateCurrentFloor(Floor.First)
      } else if (is2F(location)) {
         updateCurrentFloor(Floor.Second)
      } else {
         updateCurrentFloor(Floor.Base)
      }

      val pair = calculateCenter(location)
      updateCurrentPoint(pair.first, pair.second)
      currentFloor.value?.let { updatePreviousFloor(it) }
   }

   fun updatePreviousString(location: String) {
      _previousLocation.value = location
      val pair = calculateCenter(location)
      updatePreviousPoint(pair.first, pair.second)
   }

   fun clearFirstPath() {
      _firstPath.value?.clear()
   }

   fun updatePath(point: String, floor: Floor) {
      when (floor) {
         Floor.First -> {
            _firstPath.value?.add(point)
         }
         Floor.Second -> _secondPath.value?.add(point)
         else -> _basePath.value?.add(point)
      }
   }

   fun updateFireCell(point: String, floor: Floor) {
      when (floor) {
         Floor.First -> _firstFireCell.value?.add(point)
         Floor.Second -> _secondFireCell.value?.add(point)
         else -> _baseFireCell.value?.add(point)
      }
   }

   fun updatePredictedCell(point: String, floor: Floor) {
      when (floor) {
         Floor.First -> _firstPredictedCell.value?.add(point)
         Floor.Second -> _secondPredictedCell.value?.add(point)
         else -> _basePredictedCell.value?.add(point)
      }
   }

   fun updateCongestionCell(point: String, floor: Floor) {
      when (floor) {
         Floor.First -> _firstCongestionCell.value?.add(point)
         Floor.Second -> _secondCongestionCell.value?.add(point)
         else -> _baseCongestionCell.value?.add(point)
      }
   }

   fun updateCurrentPoint(x: Float, y: Float) {
      _currentUserX.value = x
      _currentUserY.value = y
   }

   fun updatePreviousPoint(x: Float, y: Float) {
      _previousUserX.value = x
      _previousUserY.value = y
   }

   fun addQueue(location: String) {
      _evacuationQueue.value?.add(location)
      if (evacuationQueue.value?.size!! > 3) {
         _evacuationQueue.value?.removeFirst()
      }
   }

   fun clearQueue() {
      _evacuationQueue.value?.clear()
   }

   private fun calculateCoordinate1f() {
      newMapDict1f = mutableMapOf()
      MapInfo.dict1f.forEach {
         val newArray = arrayListOf<Pair<Float, Float>>()
         for (i in 0 until it.value.size) {
            val newX = ((it.value[i].first).toString().toFloat()) - 1
            val newY = ((it.value[i].second.toString().toFloat())) - 4
            newArray.add(Pair(newX, newY))
         }
         newMapDict1f[it.key] = newArray.toTypedArray()
      }
   }

   private fun calculateCoordinate2f() {
      newMapDict2f = mutableMapOf()
      MapInfo.dict2f.forEach {
         val newArray = arrayListOf<Pair<Float, Float>>()
         for (i in 0 until it.value.size) {
            val newX = ((it.value[i].first).toString().toFloat()) - 1
            val newY = ((it.value[i].second.toString().toFloat())) - 4
            newArray.add(Pair(newX, newY))
         }
         newMapDict2f[it.key] = newArray.toTypedArray()
      }
   }

   private fun calculateCoordinateBase() {
      newMapDictBase = mutableMapOf()
      MapInfo.dictb1.forEach {
         val newArray = arrayListOf<Pair<Float, Float>>()
         for (i in 0 until it.value.size) {
            val newX = ((it.value[i].first).toString().toFloat()) - 1
            val newY = ((it.value[i].second.toString().toFloat())) - 4
            newArray.add(Pair(newX, newY))
         }
         newMapDictBase[it.key] = newArray.toTypedArray()
      }
   }

   fun is1F(point: String): Boolean {
      return MapInfo.dict1f.keys.contains(point)
   }

   fun is2F(point: String): Boolean {
      return MapInfo.dict2f.keys.contains(point)
   }


   fun isBase(point: String): Boolean {
      return MapInfo.dictb1.keys.contains(point)
   }
}