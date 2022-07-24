package com.example.aos_ar_evacuation_beacon

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ARActivity : AppCompatActivity(), SensorEventListener {
   private val newMapDict = mutableMapOf<String, Array<Pair<Float, Float>>>()

   // Sensor
   private lateinit var sensorManager: SensorManager
   private lateinit var mAccelerometer: Sensor
   private lateinit var mMagneticField: Sensor
   private var accelerationList = FloatArray(3)
   private var magneticFieldList = FloatArray(3)

   // 사용자 방향 저장
   private var azimuth = 0F
   private var pitch = 0F
   private var roll = 0F

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(PaintView(applicationContext))
      setSensor()
      //setBottomNavigation()
   }

   private fun setSensor() {
      sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
      sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { this.mAccelerometer = it }
      sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let { this.mMagneticField = it }
      sensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
      sensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL)
   }

   fun drawableToBitmap(drawable: Drawable): Bitmap? {
      if (drawable is BitmapDrawable) {
         return drawable.bitmap
      }
      var width = drawable.intrinsicWidth
      width = if (width > 0) width else 1
      var height = drawable.intrinsicHeight
      height = if (height > 0) height else 1
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)
      drawable.setBounds(0, 0, canvas.width, canvas.height)
      drawable.draw(canvas)
      return bitmap
   }

//   private fun setBottomNavigation() {
//      binding.bottomNavigation.selectedItemId = R.id.navigationItem
//      binding.bottomNavigation.setOnItemSelectedListener {
//         when (it.itemId) {
//            R.id.localizationItem -> {
//               val intent = Intent(this, LocalizationActivity::class.java)
//               startActivity(intent)
//               overridePendingTransition(0, 0)
//               finish()
//            }
//         }
//         true
//
//      }
//   }

   companion object {
      const val TAG = "ARActivity"
   }

   override fun onSensorChanged(event: SensorEvent?) {
      when (event?.sensor?.type) {
         Sensor.TYPE_ACCELEROMETER -> {
            accelerationList[0] = event.values[0]
            accelerationList[1] = event.values[1]
            accelerationList[2] = event.values[2]

         }

         Sensor.TYPE_MAGNETIC_FIELD -> {
            magneticFieldList[0] = event.values[0]
            magneticFieldList[1] = event.values[1]
            magneticFieldList[2] = event.values[2]
         }
      }

      // 행렬 계산
      val rArray = FloatArray(9)
      val iArray = FloatArray(9)
      SensorManager.getRotationMatrix(rArray, iArray, accelerationList, magneticFieldList)

      // 방위값 환산
      val values = FloatArray(3)
      SensorManager.getOrientation(rArray, values)

      // 방위값 -> 각도 단위 변경
      azimuth = Math.toDegrees(values[0].toDouble()).toFloat()
      // 좌우 기울기 값
      pitch = Math.toDegrees(values[1].toDouble()).toFloat()
      // 앞뒤 기울기 값
      roll = Math.toDegrees(values[2].toDouble()).toFloat()

      if (azimuth < 0) {
         azimuth += 360
      }
   }

   override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
      TODO("Not yet implemented")
   }
}