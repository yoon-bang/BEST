package com.example.aos_ar_evacuation_beacon.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.aos_ar_evacuation_beacon.constant.Direction
import com.example.aos_ar_evacuation_beacon.repository.base.BaseRepository
import kotlin.math.atan2

class DirectionRepository private constructor() : BaseRepository() {
   companion object {
      private var outInstance: DirectionRepository? = null

      val instance: DirectionRepository
         get() {
            if (outInstance == null) {
               outInstance = DirectionRepository()
            }
            return outInstance!!
         }
   }

   private val _userPreviousHeading = MutableLiveData(0f)
   val userPreviousHeading: LiveData<Float> = _userPreviousHeading

   private val _userCurrentHeading = MutableLiveData(0f)
   val userCurrentHeading: LiveData<Float> = _userCurrentHeading

   private val _userDirection = MutableLiveData<Direction>()
   val userDegree: LiveData<Direction> = _userDirection

   private val _arrowDegree = MutableLiveData<Float>(0f)
   val arrowDegree: LiveData<Float> = _arrowDegree

   fun updateUserPreviousHeading(heading: Float) {
      _userPreviousHeading.value = heading
   }

   fun updateUserCurrentHeading(heading: Float) {
      _userCurrentHeading.value = heading
   }

   fun updateUserDirection(degree: Direction) {
      _userDirection.value = degree
   }

   fun updateArrowDegree(degree: Float) {
      Log.i("aaaaaaaaaaaaaaaaa1111", degree.toString())
      _arrowDegree.value = degree
   }

   fun classifyDirection(heading: Float): Direction {
      return if (((315 <= heading) && (heading < 360)) || ((0 <= heading)) && (heading < 45)) {
         Direction.North
      } else if (((45 <= heading) && (heading < 135))) {
         Direction.East
      } else if (((135 <= heading) && (heading < 225))) {
         Direction.South
      } else {
         Direction.West
      }
   }

   fun angleBetween2Points(previousX: Float, previousY: Float, currentX: Float, currentY: Float): Float {
      var degree: Float
      val tan = atan2((previousX - currentX), previousY - currentY) * 180 / Math.PI
      degree = if (tan < 0) {
         (-tan).toFloat() + 180f
      } else {
         180f - tan.toFloat()
      }

      updateArrowDegree(degree)
      return degree

   }

}