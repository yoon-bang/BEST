package com.example.aos_ar_evacuation_beacon.ui

import ArRenderable
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.RotateAnimation
import android.view.animation.TranslateAnimation
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.aos_ar_evacuation_beacon.BeaconApplication
import com.example.aos_ar_evacuation_beacon.R
import com.example.aos_ar_evacuation_beacon.beacon.LocalizationManager
import com.example.aos_ar_evacuation_beacon.constant.BeaconConstants
import com.example.aos_ar_evacuation_beacon.constant.Floor
import com.example.aos_ar_evacuation_beacon.constant.MapInfo
import com.example.aos_ar_evacuation_beacon.model.ServerData
import com.example.aos_ar_evacuation_beacon.repository.DirectionRepository
import com.example.aos_ar_evacuation_beacon.repository.LocationRepository
import com.example.aos_ar_evacuation_beacon.ui.view.Paint1FView
import com.example.aos_ar_evacuation_beacon.ui.view.Paint2FView
import com.example.aos_ar_evacuation_beacon.ui.view.PaintBaseView
import com.example.aos_ar_evacuation_beacon.viewModel.MainViewModel
import com.google.android.gms.tasks.OnCompleteListener
import com.google.ar.core.Config
import com.google.ar.sceneform.ux.ArFragment
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_ar.*
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import kotlin.math.atan2
import kotlin.math.sqrt


class ARActivity : AppCompatActivity(), SensorEventListener {
   val mainViewModel: MainViewModel by viewModels()

   var imageName = "ksw_base"
   val locationRepository = LocationRepository.instance
   val directionRepository = DirectionRepository.instance
   private lateinit var beaconApplication: BeaconApplication
   lateinit var serverData: ServerData

   // Sensor
   private lateinit var sensorManager: SensorManager
   private lateinit var mAccelerometer: Sensor
   private lateinit var mMagneticField: Sensor
   private lateinit var mRotationVector: Sensor
   private var accelerationList = FloatArray(3)
   private var magneticFieldList = FloatArray(3)
   private var rotationVectorList = FloatArray(4)

   var pathList: MutableList<String>? = null

   private lateinit var localizationManager: LocalizationManager

   private var azimuth = 0F

   var currentUserX = 0f
   var currentUserY = 0f
   var previousUserX = 0f
   var previousUserY = 0f
   val newMapDict = mutableMapOf<String, Array<Pair<Float, Float>>>()

   private lateinit var paint1FView: Paint1FView
   private lateinit var paint2FView: Paint2FView
   private lateinit var paintBaseView: PaintBaseView

   lateinit var firstFloorPathView: FirstFloorView
   lateinit var secondFloorPathView: SecondFloorView
   lateinit var baseFloorPathView: BaseFloorView

   lateinit var arFragment: ArFragment
   lateinit var client: Socket
   lateinit var output: PrintWriter
   lateinit var input: BufferedReader
   var isSocketConnect = false

   lateinit var arRenderable: ArRenderable
   var neverAskAgainPermissions = ArrayList<String>()

   override fun onPause() {
      super.onPause()
      client.close()
   }

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.activity_ar)

      beaconApplication = application as BeaconApplication
      localizationManager = LocalizationManager(this, applicationContext, beaconApplication, mainViewModel)
      localizationManager.setting()
      setSensor()
      calculateCoordinate()

      mainViewModel.startPoint.observe(this) {
         if (it != "") {
            Thread(Runnable {
               kotlin.run {
                  try {
                     client()
                  } catch (e: Exception) {
                     Log.e("Client Connect Error", e.toString())
                  }
               }
            }).start()

            CoroutineScope(Dispatchers.IO).launch {
               delay(3500L)
               var message: String

               launch {
                  message = input.readLine()
                  Log.i("Client message", message)

                  var data = message.split("|")
                  var pathList = data[0].split(" ")
                  var fireCell = data[1].split(" ")
                  var predictedCell = data[2].split(" ")
                  var congestionCell = data[3].split(" ")
                  serverData = ServerData(pathList, fireCell, predictedCell, congestionCell)

                  mainViewModel.updatePathList(serverData.pathList)
                  mainViewModel.updateFireCellList(serverData.fireCell)
                  mainViewModel.updatePredictedCellList(serverData.predictedCell)
                  mainViewModel.updateCongestionCellList(serverData.congestionCell)
               }.join()

               launch {
                  delay(1000L)
                  setPath()
               }.join()
            }
         }
      }


      FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
         if (!task.isSuccessful) {
            Log.w(TAG, "Fetching FCM registration token failed", task.exception)
            return@OnCompleteListener
         }

         // Get new FCM registration token
         val token = task.result
         Log.w("tokentoken", token.toString())
      })
      // AR setup
      arFragment = fragment as ArFragment
      arFragment.planeDiscoveryController.hide()
      arFragment.planeDiscoveryController.setInstructionView(null)

      arRenderable = ArRenderable(this, arFragment, R.raw.aos_arrow)
      arFragment.arSceneView.session?.configure(arFragment.arSceneView.session!!.config.apply {
         geospatialMode = Config.GeospatialMode.ENABLED
      })

      arRenderable.addNodeToScene()
      arFragment.arSceneView.scene.addOnUpdateListener {
         arRenderable.onUpdateFrame(bannerText, it)
      }

      paint1FView = Paint1FView(applicationContext)
      paint2FView = Paint2FView(applicationContext)
      paintBaseView = PaintBaseView(applicationContext)

      firstFloorPathView = FirstFloorView(applicationContext)
      secondFloorPathView = SecondFloorView(applicationContext)
      baseFloorPathView = BaseFloorView(applicationContext)

      // 소켓 통신 성공하면 경로 그리도록
      mainViewModel.pathList.observe(this) { pathList ->
         if (pathList.isNotEmpty()) {
            mainViewModel.isStart.observe(this) { isStart ->
               if (!isStart) {
                  Log.i("pathListisStart", "false")
                  setStartPathView()
               } else {
                  Log.i("pathListisStart", "true")
                  updatePathView()
               }
            }
         }
      }

      mainViewModel.previousLocation.observe(this) { location ->
         if (location.isNotEmpty()) {
            changeMapView()
            lifecycleScope.launch(Dispatchers.IO) {
               if (isSocketConnect) {
                  output.println(location)
               }
            }

            mainViewModel.currentFloor.observe(this) { floor ->
               when (floor) {
                  Floor.First -> {
                     firstFloorPathView?.invalidate()
                  }
                  Floor.Second -> {
                     secondFloorPathView?.invalidate()
                  }
                  Floor.Base -> {
                     baseFloorPathView?.invalidate()
                  }
               }
            }

            if (location.contains("E")) {
               bannerText.text = "Evacuate Safely!"
            }
         }
      }
   }

   fun updatePathView() {
      mainViewModel.currentFloor.observe(this) {
         when (it) {
            Floor.First -> firstFloorPathView?.invalidate()
            Floor.Second -> secondFloorPathView?.invalidate()
            Floor.Base -> baseFloorPathView?.invalidate()
         }
      }
   }

   fun client() {
      CoroutineScope(Dispatchers.IO).launch {
         launch {
            client = Socket(BeaconConstants.IP_ADDRESS, BeaconConstants.port)
            output = PrintWriter(client.getOutputStream(), true)
            input = BufferedReader(InputStreamReader(client.inputStream))
            output.println(locationRepository.startLocation.value)
         }.join()

         launch {
            val message = input.readLine()
            Log.i("Client message", message.toString())
            isSocketConnect = true

            // 경로 , 화재셀, predicted cell, congestion cell
            var data = message.split("|")
            var pathList = data[0].split(" ")

            var fireCell = data[1].split("")
            var predictedCell = data[2].split("")
            var congestionCell = data[3].split("")
            serverData = ServerData(pathList, fireCell, predictedCell, congestionCell)
         }.join()

         launch {
            mainViewModel.updatePathList(serverData.pathList)
            mainViewModel.updateFireCellList(serverData.fireCell)
            mainViewModel.updatePredictedCellList(serverData.predictedCell)
            mainViewModel.updateCongestionCellList(serverData.congestionCell)
         }.join()

      }
   }

   private fun setStartPathView() {
      val startPoint = locationRepository.startLocation.value
      setPath()
      Log.i("startPoint", startPoint.toString())

      if (startPoint != null) {
         if (locationRepository.is1F(startPoint)) {
            Log.i("startPoint", "1f")
            mainViewModel.updateFloor(Floor.First)
            imageName = "ksw_1f"

            val paintViewIndex = containerFramelayout.indexOfChild(paint1FView)
            val pathViewIndex = containerFramelayout.indexOfChild(firstFloorPathView)
            if (paintViewIndex > 0 && pathViewIndex > 0) {
               containerFramelayout.apply {
                  removeView(paint1FView)
                  removeView(firstFloorPathView)
               }
            }
            mapImage.setImageResource(R.drawable.ksw_1f)
            containerFramelayout.addView(paint1FView)
            containerFramelayout.addView(firstFloorPathView)
            firstFloorPathView?.invalidate()
            paint1FView?.invalidate()
         }

         if (locationRepository.is2F(startPoint)) {
            Log.i("startPoint", "2f")
            mainViewModel.updateFloor(Floor.Second)
            imageName = "ksw_2f"

            if (paint2FView.parent != null) {
               val viewGroup = paint2FView.parent as ViewGroup
               viewGroup.removeView(paint2FView)
            }

            mapImage.setImageResource(R.drawable.ksw_2f)
            containerFramelayout.addView(paint2FView)
            containerFramelayout.addView(secondFloorPathView)
            secondFloorPathView?.invalidate()
            paint2FView?.invalidate()
         }

         if (locationRepository.isBase(startPoint)) {
            Log.i("startPoint", "base")
            mainViewModel.updateFloor(Floor.Base)
            imageName = "ksw_base"

            if (paintBaseView.parent != null) {
               val viewGroup = paintBaseView.parent as ViewGroup
               viewGroup.removeView(paintBaseView)
            }

            mapImage.setImageResource(R.drawable.ksw_base)
            containerFramelayout.addView(paintBaseView)
            containerFramelayout.addView(baseFloorPathView)
            paintBaseView?.invalidate()
            baseFloorPathView?.invalidate()
         }
      }
   }

   private fun setSensor() {
      sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
      sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { this.mAccelerometer = it }
      sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let { this.mMagneticField = it }
      sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let { this.mRotationVector = it }
   }

   override fun onResume() {
      super.onResume()
      sensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
      sensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL)
      sensorManager.registerListener(this, mRotationVector, SensorManager.SENSOR_DELAY_NORMAL)
   }

   private fun setPath() {
      Log.i("setPath", "called")
      val path = locationRepository.pathList.value
      val fireCell = locationRepository.fireCellList.value
      val predictedCell = locationRepository.predictedCellList.value
      val congestionCell = locationRepository.congestionCellList.value
      locationRepository.clearFirstPath()

      path?.forEach { point ->
         if (locationRepository.is1F(point)) {
            locationRepository.updatePath(point, Floor.First)
         }

         if (locationRepository.is2F(point)) {
            locationRepository.updatePath(point, Floor.Second)
         }

         if (locationRepository.isBase(point)) {
            locationRepository.updatePath(point, Floor.Base)
         }
      }

      fireCell?.forEach { point ->
         if (locationRepository.is1F(point)) {
            locationRepository.updateFireCell(point, Floor.First)
         }

         if (locationRepository.is2F(point)) {
            locationRepository.updateFireCell(point, Floor.Second)
         }

         if (locationRepository.isBase(point)) {
            locationRepository.updateFireCell(point, Floor.Base)
         }
      }

      predictedCell?.forEach { point ->
         if (locationRepository.is1F(point)) {
            locationRepository.updatePredictedCell(point, Floor.First)
         }

         if (locationRepository.is2F(point)) {
            locationRepository.updatePredictedCell(point, Floor.Second)
         }

         if (locationRepository.isBase(point)) {
            locationRepository.updatePredictedCell(point, Floor.Base)
         }
      }

      congestionCell?.forEach { point ->
         if (locationRepository.is1F(point)) {
            locationRepository.updateCongestionCell(point, Floor.First)
         }

         if (locationRepository.is2F(point)) {
            locationRepository.updateCongestionCell(point, Floor.Second)
         }

         if (locationRepository.isBase(point)) {
            locationRepository.updateCongestionCell(point, Floor.Base)
         }
      }

      firstFloorPathView?.invalidate()
      paint1FView?.invalidate()
   }

   private fun changePaintView(floor: Floor) {
      if (paint1FView.parent != null) {
         val viewGroup = paint1FView.parent as ViewGroup
         viewGroup.removeView(paint1FView)
      }

      if (paint2FView.parent != null) {
         val viewGroup = paint2FView.parent as ViewGroup
         viewGroup.removeView(paint2FView)
      }

      if (paintBaseView.parent != null) {
         val viewGroup = paintBaseView.parent as ViewGroup
         viewGroup.removeView(paintBaseView)
      }

      when (floor) {
         Floor.First -> containerFramelayout.addView(paint1FView)
         Floor.Second -> containerFramelayout.addView(paint2FView)
         else -> containerFramelayout.addView(paintBaseView)
      }
   }

   private fun changeCustomView(floor: Floor) {
      var firstParent: ViewGroup? = null
      var secondParent: ViewGroup? = null
      var baseParent: ViewGroup? = null

      if (firstFloorPathView.parent != null) {
         firstParent = (firstFloorPathView.parent) as ViewGroup
         firstParent.removeView(firstFloorPathView)
      }

      if (secondFloorPathView.parent != null) {
         secondParent = (secondFloorPathView.parent) as ViewGroup
         secondParent.removeView(secondFloorPathView)
      }

      if (baseFloorPathView.parent != null) {
         baseParent = (baseFloorPathView.parent) as ViewGroup
         baseParent.removeView(baseFloorPathView)
      }

      when (floor) {
         Floor.First -> {
//            secondParent?.removeView(secondFloorPathView)
//            baseParent?.removeView(baseFloorPathView)
//            firstParent?.removeView(firstFloorPathView)
            firstParent?.addView(firstFloorPathView)
         }

         Floor.Second -> {
//            firstParent?.removeView(firstFloorPathView)
//            secondParent?.removeView(secondFloorPathView)
//            baseParent?.removeView(baseFloorPathView)
            secondParent?.addView(secondFloorPathView)
         }

         else -> {
//            firstParent?.removeView(firstFloorPathView)
//            secondParent?.removeView(secondFloorPathView)
//            baseParent?.removeView(baseFloorPathView)
            baseParent?.addView(baseFloorPathView)
         }
      }
   }

   private fun changeMapView() {
      locationRepository.previousLocation.value?.let { Log.i("changeMapView() current Location: ", it) }

      if (mainViewModel.isBaseTo1f()) {
         Log.i("changeMapView() ", "here1111111")
         imageName = "ksw_1f"
         mapImage.setImageResource(R.drawable.ksw_1f)
         changeCustomView(Floor.First)
         changePaintView(Floor.First)

      } else if (mainViewModel.is1fToBase()) {
         Log.i("changeMapView() ", "here222222")
         imageName = "ksw_base"
         mapImage.setImageResource(R.drawable.ksw_base)
         changeCustomView(Floor.Second)
         changePaintView(Floor.Second)

      } else if (mainViewModel.is1fTo2f()) {
         Log.i("changeMapView() ", "here3333333")
         imageName = "ksw_2f"
         mapImage.setImageResource(R.drawable.ksw_2f)
         changeCustomView(Floor.Second)
         changePaintView(Floor.Second)

      } else if (mainViewModel.is2fTo1f()) {
         Log.i("changeMapView() ", "here4444444")
         imageName = "ksw_1f"
         mapImage.setImageResource(R.drawable.ksw_1f)
         changeCustomView(Floor.First)
         changePaintView(Floor.First)

      } else if (mainViewModel.is1fTo2fBackDoor()) {
         Log.i("changeMapView() ", "here6666666")
         imageName = "ksw_2f"
         mapImage.setImageResource(R.drawable.ksw_2f)
         changeCustomView(Floor.Second)
         changePaintView(Floor.Second)

      } else if (mainViewModel.is2fTo1fBackDoor()) {
         Log.i("changeMapView() ", "here777777")
         imageName = "ksw_1f"
         mapImage.setImageResource(R.drawable.ksw_1f)
         changeCustomView(Floor.First)
         changePaintView(Floor.First)

      }
      mainViewModel.clearQueue()
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

         Sensor.TYPE_ROTATION_VECTOR -> {
            rotationVectorList[0] = event.values[0]
            rotationVectorList[1] = event.values[1]
            rotationVectorList[2] = event.values[2]
            rotationVectorList[3] = event.values[3]
         }
      }

      val norm =
         sqrt(rotationVectorList[0] * rotationVectorList[0] + rotationVectorList[1] * rotationVectorList[1] + rotationVectorList[2] * rotationVectorList[2] + rotationVectorList[3] * rotationVectorList[3])
      rotationVectorList[0] /= norm
      rotationVectorList[1] /= norm
      rotationVectorList[2] /= norm
      rotationVectorList[3] /= norm

      val x = rotationVectorList[0]
      val y = rotationVectorList[1]
      val z = rotationVectorList[2]
      val w = rotationVectorList[3]

      val sinA = 2.0 * (w * z + x * y)
      val cosA = 1.0 - 2.0 * (y * y + z * z)

      azimuth = (atan2(sinA, cosA) * (180 / Math.PI)).toFloat()
      if (azimuth < 0) {
         azimuth += 360
      }

      if (45 < azimuth && azimuth < 135) {
         azimuth += 180
      } else if (225 < azimuth && azimuth < 315) {
         azimuth -= 180
      }

      val direction = directionRepository.classifyDirection(azimuth)
      directionRepository.updateUserCurrentHeading(azimuth)
      directionRepository.updateUserDirection(direction)

      // map 에서 유저 회전시키기위해 previousHeading 저장
      directionRepository.updateUserPreviousHeading(directionRepository.userCurrentHeading.value!!)


      /*
      if (mMagneticField != null && mAccelerometer != null) {
         val rArray = FloatArray(9)
         val iArray = FloatArray(9)
         val isSucess = SensorManager.getRotationMatrix(rArray, iArray, accelerationList, magneticFieldList)

         if (isSucess) {
            // 방위값 환산
            val values = FloatArray(3)
            SensorManager.getOrientation(rArray, values)
            // 방위값 -> 각도 단위 변경
//            azimuth = Math.toDegrees(values[0].toDouble()).toFloat()
            azimuth = (values[0] * 180 / Math.PI).toFloat()
            // -180 ~ 180 라서 양수로 변환
            if (azimuth < 0) {
               azimuth += 360
            }

//      val currentDegree = (Math.PI / 180 * (170 + degree)).toFloat()
            //var currentDegree = degree + 170

            // heading : 실제 value
            // direction : 동 서 남 북
            val direction = directionRepository.classifyDirection(azimuth)
            Log.i("=== azimuth === ", azimuth.toString())
            Log.i("=== sensor direction === ", direction.toString())
            directionRepository.updateUserCurrentHeading(azimuth)
            directionRepository.updateUserDirection(direction)

//            when (mainViewModel.currentFloor.value) {
//               Floor.First -> firstFloorPathView?.invalidate()
//               Floor.Second -> secondFloorPathView?.invalidate()
//               else -> baseFloorPathView?.invalidate()
//            }

            // map 에서 유저 회전시키기위해 previousHeading 저장
            directionRepository.updateUserPreviousHeading(directionRepository.userCurrentHeading.value!!)
         }
      }
      */
   }

   override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

   }

   inner class FirstFloorView : View {
      constructor(context: Context) : super(context) {}

      override fun onDraw(canvas: Canvas?) {
         super.onDraw(canvas)
         canvas?.let {
            rotateAnimation()
            drawUser(locationRepository.previousUserX.value!!, locationRepository.previousUserY.value!!, it)
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
         val anim =
            RotateAnimation(directionRepository.userPreviousHeading.value!! + 180,
                            directionRepository.userCurrentHeading.value!! + 180,
                            locationRepository.previousUserX.value!!,
                            locationRepository.previousUserY.value!!)
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

   inner class SecondFloorView : View {
      constructor(context: Context) : super(context) {}

      override fun onDraw(canvas: Canvas?) {
         super.onDraw(canvas)
         canvas?.let {
            rotateAnimation()
            drawUser(locationRepository.previousUserX.value!!, locationRepository.previousUserY.value!!, it)
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
         val anim =
            RotateAnimation(directionRepository.userPreviousHeading.value!!,
                            directionRepository.userCurrentHeading.value!!,
                            locationRepository.previousUserX.value!!,
                            locationRepository.previousUserY.value!!)
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

   inner class BaseFloorView : View {
      constructor(context: Context) : super(context) {}

      override fun onDraw(canvas: Canvas?) {
         super.onDraw(canvas)
         canvas?.let {
            rotateAnimation()
            Log.i("BasecurrentUserX", locationRepository.currentUserX.value.toString())
            Log.i("BasecurrentUserY", locationRepository.currentUserY.value.toString())
            drawUser(locationRepository.previousUserX.value!!, locationRepository.previousUserY.value!!, it)
         }
      }

      private fun rotateAnimation() {
         val anim =
            RotateAnimation(directionRepository.userPreviousHeading.value!!,
                            directionRepository.userCurrentHeading.value!!,
                            locationRepository.currentUserX.value!!,
                            locationRepository.currentUserY.value!!)
         Log.i("BasepreviousDegree: ", directionRepository.userPreviousHeading.value!!.toString())
         Log.i("BasecurrentDegree: ", directionRepository.userCurrentHeading.value!!.toString())
         anim.duration = 1000L
         startAnimation(anim)
      }
   }

   fun calculateCoordinate() {
      MapInfo.dict1f.forEach {
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
         color = Color.BLUE
         style = Paint.Style.FILL
         strokeWidth = 60f
      }

      drawTriangle(userX, userY - 10, canvas)
      canvas.drawCircle(userX, userY, 15f, paint)

   }

   fun drawTriangle(x: Float, y: Float, canvas: Canvas) {
      val m_path = Path()
      val paint = Paint().apply {
         isAntiAlias = true
         color = Color.BLUE
         style = Paint.Style.FILL
         strokeWidth = 60f
      }
      var y = y
      val side = 13

      /** 삼각형 크기  */
      val height = 25
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

   private fun checkPermissions() {
      var permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
      var permissionRationale = "This app needs fine location permission to detect beacons.  Please grant this now."
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
         permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN)
         permissionRationale = "This app needs fine location permission, and bluetooth scan permission to detect beacons.  Please grant all of these now."
      } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
         if ((checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionRationale = "This app needs fine location permission to detect beacons.  Please grant this now."
         } else {
            permissions = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            permissionRationale = "This app needs background location permission to detect beacons in the background.  Please grant this now."
         }
      } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
         permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
         permissionRationale = "This app needs both fine location permission and background location permission to detect beacons in the background.  Please grant both now."
      }
      var allGranted = true
      for (permission in permissions) {
         if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) allGranted = false;
      }
      if (!allGranted) {
         if (neverAskAgainPermissions.isEmpty()) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("This app needs permissions to detect beacons")
            builder.setMessage(permissionRationale)
            builder.setPositiveButton(android.R.string.ok, null)
            builder.setOnDismissListener {
               requestPermissions(permissions, PERMISSION_REQUEST_FINE_LOCATION)
            }
            builder.show()
         } else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Functionality limited")
            builder.setMessage("Since location and device permissions have not been granted, this app will not be able to discover beacons.  Please go to Settings -> Applications -> Permissions and grant location and device discovery permissions to this app.")
            builder.setPositiveButton(android.R.string.ok, null)
            builder.setOnDismissListener { }
            builder.show()
         }
      } else {
         if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
               if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                  val builder = AlertDialog.Builder(this)
                  builder.setTitle("This app needs background location access")
                  builder.setMessage("Please grant location access so this app can detect beacons in the background.")
                  builder.setPositiveButton(android.R.string.ok, null)
                  builder.setOnDismissListener {
                     requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PERMISSION_REQUEST_BACKGROUND_LOCATION)
                  }
                  builder.show()
               } else {
                  val builder = AlertDialog.Builder(this)
                  builder.setTitle("Functionality limited")
                  builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons in the background.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.")
                  builder.setPositiveButton(android.R.string.ok, null)
                  builder.setOnDismissListener { }
                  builder.show()
               }
            }
         } else if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.S && (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN)) {
               val builder = AlertDialog.Builder(this)
               builder.setTitle("This app needs bluetooth scan permission")
               builder.setMessage("Please grant scan permission so this app can detect beacons.")
               builder.setPositiveButton(android.R.string.ok, null)
               builder.setOnDismissListener {
                  requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN), PERMISSION_REQUEST_BLUETOOTH_SCAN)
               }
               builder.show()
            } else {
               val builder = AlertDialog.Builder(this)
               builder.setTitle("Functionality limited")
               builder.setMessage("Since bluetooth scan permission has not been granted, this app will not be able to discover beacons  Please go to Settings -> Applications -> Permissions and grant bluetooth scan permission to this app.")
               builder.setPositiveButton(android.R.string.ok, null)
               builder.setOnDismissListener { }
               builder.show()
            }
         } else {
            if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
               if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                  if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                     val builder = AlertDialog.Builder(this)
                     builder.setTitle("This app needs background location access")
                     builder.setMessage("Please grant location access so this app can detect beacons in the background.")
                     builder.setPositiveButton(android.R.string.ok, null)
                     builder.setOnDismissListener {
                        requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PERMISSION_REQUEST_BACKGROUND_LOCATION)
                     }
                     builder.show()
                  } else {
                     val builder = AlertDialog.Builder(this)
                     builder.setTitle("Functionality limited")
                     builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons in the background.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.")
                     builder.setPositiveButton(android.R.string.ok, null)
                     builder.setOnDismissListener { }
                     builder.show()
                  }
               }
            }
         }
      }
   }

   override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
      for (i in 1 until permissions.size) {
         Log.d(LocalizationActivity.TAG, "onRequestPermissionResult for " + permissions[i] + ":" + grantResults[i])
         if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
            //check if user select "never ask again" when denying any permission
            if (!shouldShowRequestPermissionRationale(permissions[i])) {
               neverAskAgainPermissions.add(permissions[i])
            }
         }
      }
   }

   companion object {
      const val TAG = "ARActivity"
      val PERMISSION_REQUEST_BACKGROUND_LOCATION = 0
      val PERMISSION_REQUEST_BLUETOOTH_SCAN = 1
      val PERMISSION_REQUEST_BLUETOOTH_CONNECT = 2
      val PERMISSION_REQUEST_FINE_LOCATION = 3
   }
}