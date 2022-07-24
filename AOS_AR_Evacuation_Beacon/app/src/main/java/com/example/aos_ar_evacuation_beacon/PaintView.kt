package com.example.aos_ar_evacuation_beacon

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.View
import com.example.aos_ar_evacuation_beacon.constant.MapInfo
import com.example.aos_ar_evacuation_beacon.viewModel.DegreeViewModel
import kotlin.math.abs

class PaintView(context: Context) : View(context) {
   private lateinit var viewModel: DegreeViewModel

   private val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ksw_blueprint_1f)
   private val newMapDict = mutableMapOf<String, Array<Pair<Float, Float>>>()

   //   private val pathList = mutableListOf("A09", "A04", "A03", "A02", "A01", "E01")
   private val pathList = mutableListOf("A01")
   private val userLocation = "A03"

   override fun onDraw(canvas: Canvas?) {
      super.onDraw(canvas)
      calculateCoordinate()

      // 이미지 불러오기
      canvas?.drawBitmap(bitmap, null, Rect(0, 0, canvas.width, canvas.height), null)

      val paint = Paint().apply {
         isAntiAlias = true
         color = Color.GREEN
         style = Paint.Style.STROKE
         strokeWidth = 3f
      }

//      if (canvas != null) {
//         drawGrid(canvas, paint)
//      }

      if (canvas != null) {
         drawCell(pathList, canvas)
         drawUser(userLocation, canvas)
      }
   }

   private fun calculateCoordinate() {
      MapInfo.dict.forEach {
         val newArray = arrayListOf<Pair<Float, Float>>()
         for (i in 0 until it.value.size) {
            val newX = ((it.value[i].first).toString().toFloat()) - 1
            val newY = ((it.value[i].second.toString().toFloat())) - 4
            newArray.add(Pair(newX, newY))
         }
         newMapDict[it.key] = newArray.toTypedArray()
      }

      newMapDict.forEach { (s, arrayOfPairs) ->
         arrayOfPairs.forEach {
            Log.i("$$$ ssss $$$$ ", "first: ${it.first}, second: ${it.second}")
         }
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

   private fun drawUser(userLocation: String, canvas: Canvas) {
      val paint = Paint().apply {
         isAntiAlias = true
         color = Color.RED
         style = Paint.Style.FILL
         strokeWidth = 80f
      }

      val pointList = MapInfo.dict[userLocation]
      Log.i("PointList: ", pointList?.size.toString())

      if (userLocation != "H02") {
         val x0 = (pointList?.get(0)?.first).toString().toFloat()
         val x1 = (pointList?.get(1)?.first).toString().toFloat()
         Log.i("UserLocation x0: ", x0.toString())
         Log.i("UserLocation x1: ", x1.toString())
         val width = abs(x1 - x0) / 2

         val y0 = (pointList?.get(2)?.second).toString().toFloat()
         val y1 = (pointList?.get(1)?.second).toString().toFloat()
         Log.i("UserLocation y0: ", y0.toString())
         Log.i("UserLocation y1: ", y1.toString())
         val height = abs(y1 - y0) / 2
         Log.i("height: ", height.toString())
         canvas.drawCircle((x0 + width) * 30, y1 * 30, 10f, paint)
      }
   }

   private fun drawCell(pathList: MutableList<String>, canvas: Canvas) {
      pathList.forEach {
         val paint = Paint().apply {
            isAntiAlias = true
            color = Color.RED
            alpha = 50
            style = Paint.Style.FILL
            strokeWidth = 3f
         }
         val path = Path()

         val coordinateList = newMapDict[it]
         path.moveTo(coordinateList!![0].first * 30, coordinateList!![0].second * 30)
         coordinateList.forEach { value ->
            path.lineTo(value.first * 30, value.second * 30)
         }
         path.lineTo(coordinateList!![0].first * 30, coordinateList!![0].second * 30)
         canvas.drawPath(path, paint)
      }
   }
}