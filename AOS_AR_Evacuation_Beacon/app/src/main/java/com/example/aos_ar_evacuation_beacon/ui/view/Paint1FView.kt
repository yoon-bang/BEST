package com.example.aos_ar_evacuation_beacon.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Log
import android.view.View
import com.example.aos_ar_evacuation_beacon.constant.MapInfo


class PaintView : View {
   val newMapDict = mutableMapOf<String, Array<Pair<Float, Float>>>()
   val pathList = mutableListOf("H01", "S04", "S03", "S02")

   constructor(context: Context) : super(context) {
      init()
   }

   private fun init() {
      calculateCoordinate()
   }

   override fun onDraw(canvas: Canvas?) {
      super.onDraw(canvas)
//      if (canvas != null) {
//         drawGrid(canvas, paint)
//      }

      if (canvas != null) {
         drawCell(pathList, canvas)
      }
   }

   private fun calculateCoordinate() {
      MapInfo.dict2.forEach {
         val newArray = arrayListOf<Pair<Float, Float>>()
         for (i in 0 until it.value.size) {
            val newX = ((it.value[i].first).toString().toFloat()) - 1
            val newY = ((it.value[i].second.toString().toFloat())) - 4
            newArray.add(Pair(newX, newY))
         }
         newMapDict[it.key] = newArray.toTypedArray()
      }

      newMapDict.forEach { (_, arrayOfPairs) ->
         arrayOfPairs.forEach {
            Log.i("$$$ ssss111 $$$$ ", "first: ${it.first}, second: ${it.second}")
         }
      }
   }

   private fun drawCell(pathList: MutableList<String>, canvas: Canvas) {
      pathList.forEach {
         val paint = Paint().apply {
            isAntiAlias = true
            color = Color.GREEN
            alpha = 50
            style = Paint.Style.FILL
            strokeWidth = 3f
         }

         val path = Path()
         val coordinateList = newMapDict[it]
         path.moveTo(coordinateList!![0].first * 30, coordinateList!![0].second * 30)
         coordinateList.forEach { value ->
            path.lineTo(value.first * 30, value.second * 30)
            Log.i("Location: ", "${(value.first * 30)}, ${(value.second * 30)}")
         }
         path.lineTo(coordinateList!![0].first * 30, coordinateList!![0].second * 30)
         canvas.drawPath(path, paint)
      }
   }

   private fun drawGrid(canvas: Canvas, paint: Paint) {
      var x = 0
      var y = 0
      val offset = 30

      // 격자 그리기
      if (canvas != null) {
         while (y < canvas.height) {
            canvas.drawLine(0F, y.toFloat(), canvas.width.toFloat(), y.toFloat(), paint)
            y += offset
         }

         while (x < canvas.width) {
            canvas.drawLine(x.toFloat(), 0F, x.toFloat(), canvas.height.toFloat(), paint)
            x += offset
         }
      }
   }
}