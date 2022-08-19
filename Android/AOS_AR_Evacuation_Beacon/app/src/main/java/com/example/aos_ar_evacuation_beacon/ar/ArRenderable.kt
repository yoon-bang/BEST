import android.content.Context
import android.util.Log
import android.widget.TextView
import com.example.aos_ar_evacuation_beacon.repository.DirectionRepository
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode


class ArRenderable(private val context: Context, private val arFragment: ArFragment, private val resId: Int) {
   private val directionRepository = DirectionRepository.instance
   lateinit var anchorNode: AnchorNode
   lateinit var currentNode: AnchorNode

   lateinit var transformableNode: TransformableNode
   private var modelRenderable: ModelRenderable? = null
   var isCreated = false
   var test = 0f

   fun addNodeToScene() {
      val session = arFragment.arSceneView.session
      val frame = arFragment.arSceneView.arFrame

      val newMarkAnchor = session?.createAnchor(frame?.camera?.pose?.compose(Pose.makeTranslation(0f, 0f, -2f)))
//      val newMarkAnchor = session?.createAnchor(frame?.camera?.displayOrientedPose)
//      val pos = floatArrayOf(0f, 0f, -4f)
//      val rotation = floatArrayOf(0f, 0f, 0f, 1f)
//      val newMarkAnchor = session?.createAnchor(Pose(pos, rotation))
      anchorNode = AnchorNode(newMarkAnchor)

      ModelRenderable.builder().setSource(context, resId).build().thenAccept {
         modelRenderable = it
         anchorNode.localScale = Vector3(1f, 1f, 1f)
         getCurrentScene().addChild(anchorNode)

         transformableNode = TransformableNode(arFragment.transformationSystem).apply {
            setParent(anchorNode)
            renderable = modelRenderable
         }
         isCreated = true
         currentNode = anchorNode
      }.exceptionally {
         Log.e("3D Model File Failed", it.toString())
         return@exceptionally null
      }
   }

   fun onUpdateFrame(textView: TextView, frameTime: FrameTime) {
      val session = arFragment.arSceneView.session ?: return
      val frame = arFragment.arSceneView.arFrame ?: return

      if (modelRenderable != null && isCreated && (frame.camera.trackingState == TrackingState.TRACKING)) {
         val position = frame.camera?.pose?.compose(Pose.makeTranslation(0f, 0f, -2f))?.extractTranslation()
         anchorNode.worldPosition = Vector3(position?.tx()!!, position.ty(), position.tz())

         directionRepository.arrowDegree.value?.let {
            Log.i("updated Arrow Degree: ", it.toString())
//            if (it == 180f) {
//               transformableNode.localRotation = Quaternion.eulerAngles(Vector3(0f, 0f, 0f))
//            } else {
//               transformableNode.localRotation = Quaternion.eulerAngles(Vector3(0f, it, 0f))
//            }

//            if (it in 170f..190f) {
//               transformableNode.localRotation = Quaternion.eulerAngles(Vector3(0f, 0f, 0f))
//            } else {
//               transformableNode.localRotation = Quaternion.eulerAngles(Vector3(0f, it, 0f))
//            }
            val pointDirection = directionRepository.classifyDirection(it)
            val headingDirection = directionRepository.classifyDirection(directionRepository.userCurrentHeading.value!!)
            when (pointDirection.index - headingDirection.index) {
               // forward
               0 -> transformableNode.localRotation = Quaternion.eulerAngles(Vector3(0f, 180f, 0f))
               // right
               -1 -> transformableNode.localRotation = Quaternion.eulerAngles(Vector3(0f, 90f, 0f))
               // left
               1 -> transformableNode.localRotation = Quaternion.eulerAngles(Vector3(0f, 270f, 0f))
               // backward
               else -> transformableNode.localRotation = Quaternion.eulerAngles(Vector3(0f, 0f, 0f))
            }
            changeBannerText(textView, it, directionRepository.userCurrentHeading.value!!)
         }
      }
   }

   private fun changeBannerText(textView: TextView, degree: Float, heading: Float) {
      Log.i("Banner degree:", degree.toString())

      val pointDirection = directionRepository.classifyDirection(degree)
      val headingDirection = directionRepository.classifyDirection(heading)

      Log.i("Banner pointing: ", pointDirection.toString())
      Log.i("Banner haedingDirection: ", headingDirection.toString())

      when (pointDirection.index - headingDirection.index) {
         0 -> {
            textView.text = "Go Forward"
         }
         -1 -> {
            textView.text = "Go Right"
         }
         1 -> {
            textView.text = "Go Left"
         }
         else -> {
            textView.text = "Go Back"
         }
      }
   }

   private fun getCurrentScene(): Scene = arFragment.arSceneView.scene
}