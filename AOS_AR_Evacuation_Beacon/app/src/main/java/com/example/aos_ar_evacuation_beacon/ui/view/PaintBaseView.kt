package com.example.aos_ar_evacuation_beacon.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Log
import android.view.View
import com.example.aos_ar_evacuation_beacon.repository.LocationRepository

class PaintBaseView : View {
   private lateinit var locationRepository: LocationRepository

   constructor(context: Context) : super(context) {
      init()
   }

   private fun init() {
      locationRepository = LocationRepository.instance
   }

   override fun onDraw(canvas: Canvas?) {
      super.onDraw(canvas)

      if (canvas != null) {
         locationRepository.basePath.value?.let { drawCell(it, canvas) }
      }
   }

   private fun drawCell(pathList: List<String>, canvas: Canvas) {
      pathList?.forEach {
         val paint = Paint().apply {
            isAntiAlias = true
            color = Color.GREEN
            alpha = 50
            style = Paint.Style.FILL
            strokeWidth = 3f
         }

         val path = Path()

         val coordinateList = locationRepository.newMapDictBase[it]
         if (coordinateList != null) {
            path.moveTo(coordinateList[0].first * 30, coordinateList[0].second * 30)
         }

         coordinateList?.forEach { value ->
            path.lineTo(value.first * 30, value.second * 30)
            Log.i("Location: ", "${(value.first * 30)}, ${(value.second * 30)}")
         }
         if (coordinateList != null) {
            path.lineTo(coordinateList[0].first * 30, coordinateList[0].second * 30)
         }
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