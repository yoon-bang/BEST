package com.example.aos_ar_evacuation_beacon

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.RotateAnimation
import android.view.animation.TranslateAnimation
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.aos_ar_evacuation_beacon.constant.MapInfo
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.android.synthetic.main.activity_ar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.math.abs

class ARActivity : AppCompatActivity(), SensorEventListener {
   val mainViewModel: MainViewModel by viewModels()
   private val coroutineScopeMain = CoroutineScope(Dispatchers.Main.immediate)

   // Sensor
   private lateinit var sensorManager: SensorManager
   private lateinit var mAccelerometer: Sensor
   private lateinit var mMagneticField: Sensor
   private var accelerationList = FloatArray(3)
   private var magneticFieldList = FloatArray(3)

   private var azimuth = 0F
   private var pitch = 0F
   private var roll = 0F
   var currentDegree = 0.0f
   var previousDegree = 0.0f

   var currentUserX = 0f
   var currentUserY = 0f
   var previousUserX = 0f
   var previousUserY = 0f
   val newMapDict = mutableMapOf<String, Array<Pair<Float, Float>>>()

   private lateinit var paintView: PaintView
   lateinit var custom1FView: Custom1FView
   lateinit var custom2FView: Custom2FView
   lateinit var arFragment: ArFragment

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.activity_ar)
      setSensor()
      calculateCoordinate()

      arFragment = fragment as ArFragment
      custom1FView = Custom1FView(applicationContext)
      custom2FView = Custom2FView(applicationContext)
      paintView = PaintView(applicationContext)

      containerFramelayout.addView(paintView)
      containerFramelayout.addView(custom1FView)

      mainViewModel.timerStart(custom1FView!!)
      //setBottomNavigation()
   }

   private fun setSensor() {
      sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
      sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { this.mAccelerometer = it }
      sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let { this.mMagneticField = it }
   }

   override fun onResume() {
      super.onResume()
      sensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
      sensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL)
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

      var degree = Math.toDegrees(values[0].toDouble() + 360).toFloat() % 360
      currentDegree = degree

//      currentDegree = -degree
      if (abs(previousDegree - currentDegree) > 10) {
         custom1FView?.invalidate()
      }
      previousDegree = currentDegree

      if (azimuth < 0) {
         azimuth += 360
      }
   }

   override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

   }

   inner class Custom1FView : View {
      constructor(context: Context) : super(context) {}

      override fun onDraw(canvas: Canvas?) {
         super.onDraw(canvas)
         canvas?.let {
            rotateAnimation()
            Log.i("currentUserX", currentUserX.toString())
            Log.i("currentUserY", currentUserY.toString())
            drawUser(currentUserX, currentUserY, it)
         }
      }

      fun updateUserLocation(prevX: Float, prevY: Float, currX: Float, currY: Float) {
         previousUserX = prevX
         previousUserY = prevY
         currentUserX = currX
         currentUserY = currY
         //moveAnimation(previousX.value!!, previousY.value!!, currentX.value!!, currentY.value!!)
      }

      private fun rotateAnimation() {
         val anim = RotateAnimation(previousDegree, currentDegree, currentUserX, currentUserY)
         anim.duration = 1000L
         startAnimation(anim)
      }

      fun moveAnimation(previousX: Float, previousY: Float, currentX: Float, currentY: Float) {
         Log.w("previousUser222X", previousX.toString())
         Log.w("previousUser222Y", previousY.toString())
         Log.w("currentUser222X", currentX.toString())
         Log.w("currentUser222Y", currentY.toString())

         val anim = TranslateAnimation(previousX, previousY, currentX, currentY)
         anim.duration = 1000
         anim.fillAfter = true
         //startAnimation(anim)
      }
   }

   fun calculateCoordinate() {
      MapInfo.dict.forEach {
         val newArray = arrayListOf<Pair<Float, Float>>()
         for (i in 0 until it.value.size) {
            val newX = ((it.value[i].first).toString().toFloat()) - 1
            val newY = ((it.value[i].second.toString().toFloat())) - 4
            newArray.add(Pair(newX, newY))
         }
         newMapDict[it.key] = newArray.toTypedArray()
      }
   }

   fun drawUser(userX: Float, userY: Float, canvas: Canvas) {
      val paint = Paint().apply {
         isAntiAlias = true
         color = Color.RED
         style = Paint.Style.FILL
         strokeWidth = 100f
      }
      canvas.drawCircle(userX, userY, 10f, paint)
      drawTriangle(userX, userY - 6, canvas)
   }

   fun drawTriangle(x: Float, y: Float, canvas: Canvas) {
      val m_path = Path()
      val paint = Paint().apply {
         isAntiAlias = true
         color = Color.BLUE
         style = Paint.Style.FILL
         strokeWidth = 80f
      }
      var y = y
      val side = 10

      /** 삼각형 크기  */
      val height = 18
      y -= height
      m_path.reset()
      val point1 = Point(x.toInt(), y.toInt()) // 왼
      val point2 = Point(x.toInt() - side, y.toInt() + height) // 아래
      val point3 = Point(x.toInt() + side, y.toInt() + height) // 오른

      m_path.moveTo(point1.x.toFloat(), point1.y.toFloat())
      m_path.lineTo(point2.x.toFloat(), point2.y.toFloat())
      m_path.lineTo(point3.x.toFloat(), point3.y.toFloat())
      m_path.lineTo(point1.x.toFloat(), point1.y.toFloat())
      m_path.close()
      canvas.drawPath(m_path, paint)
   }

   companion object {
      const val TAG = "ARActivity"
   }
}