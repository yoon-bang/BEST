package com.example.aos_ar_evacuation_beacon.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.aos_ar_evacuation_beacon.constant.Floor
import com.example.aos_ar_evacuation_beacon.constant.MapInfo
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
   private val _pathList = MutableLiveData(listOf("A07", "A06", "A09", "A05", "A04", "A03", "R01"))
   val pathList: LiveData<List<String>> = _pathList

   private val _userPath = MutableLiveData(listOf("A07", "A06", "A09", "A05", "A04", "A03", "R01"))
   val userPath: LiveData<List<String>> = _userPath

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

   private val _secondPath = MutableLiveData(mutableListOf<String>())
   val secondPath: LiveData<MutableList<String>> = _secondPath

   private val _basePath = MutableLiveData(mutableListOf<String>())
   val basePath: LiveData<MutableList<String>> = _basePath

   private val _currentFloor = MutableLiveData(Floor.First)
   val currentFloor: LiveData<Floor> = _currentFloor

   private val _previousFloor = MutableLiveData(Floor.First)
   val previousFloor: LiveData<Floor> = _previousFloor

   private val _currentLocation = MutableLiveData("")
   val currentLocation: LiveData<String> = _currentLocation

   private val _isStart = MutableLiveData(true)
   val isStart: LiveData<Boolean> = _isStart

   private val _startLocation = MutableLiveData("")
   val startLocation: LiveData<String> = _startLocation

   private val _evacuationQueue = MutableLiveData(mutableListOf<String>())
   val evacuationQueue: LiveData<MutableList<String>> = _evacuationQueue

   lateinit var newMapDict1f: MutableMap<String, Array<Pair<Float, Float>>>
   lateinit var newMapDict2f: MutableMap<String, Array<Pair<Float, Float>>>
   lateinit var newMapDictBase: MutableMap<String, Array<Pair<Float, Float>>>

   private fun calculateCenter(location: String): Pair<Float, Float> {
      val pointList = when (currentFloor.value) {
         Floor.First -> {
            newMapDict1f[location]
         }
         Floor.Second -> {
            newMapDict2f[location]
         }
         else -> {
            newMapDictBase[location]
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

   fun updateCurrentFloor(floor: Floor) {
      _currentFloor.value = floor
   }

   fun updatePreviousFloor(floor: Floor) {
      _previousFloor.value = floor
   }

   fun updateIsStart(isStart: Boolean) {
      _isStart.value = isStart
   }

   fun updateStartPoint(location: String) {
      _startLocation.value = location
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

   fun updateFirstPath(point: String) {
      _firstPath.value?.add(point)
   }

   fun updateSecondPath(point: String) {
      _secondPath.value?.add(point)
   }

   fun updateBasePath(point: String) {
      _basePath.value?.add(point)
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