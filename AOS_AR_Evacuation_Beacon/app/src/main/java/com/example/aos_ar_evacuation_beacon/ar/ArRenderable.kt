import android.content.Context
import android.util.Log
import com.example.aos_ar_evacuation_beacon.repository.DirectionRepository
import com.example.aos_ar_evacuation_beacon.ui.ARActivity
import com.example.aos_ar_evacuation_beacon.viewModel.MainViewModel
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
   val directionRepository = DirectionRepository.instance
   lateinit var anchorNode: AnchorNode

   lateinit var transformableNode: TransformableNode
   var modelRenderable: ModelRenderable? = null
   var isCreated = false
   var degree = 0f

   fun addNodeToScnee() {
      val session = arFragment.arSceneView.session
      val frame = arFragment.arSceneView.arFrame

      val newMarkAnchor = session?.createAnchor(frame?.camera?.pose?.compose(Pose.makeTranslation(-0.5f, 0f, -2f))?.extractTranslation())
      anchorNode = AnchorNode(newMarkAnchor)
      ModelRenderable.builder().setSource(context, resId).build().thenAccept {
         modelRenderable = it
         anchorNode.localScale = Vector3(1.0f, 1.0f, 1.0f)
         getCurrentScene().addChild(anchorNode)

         transformableNode = TransformableNode(arFragment.transformationSystem).apply {
            setParent(anchorNode)
            //localRotation = Quaternion.eulerAngles(Vector3(10f, 20f, 60f))
            renderable = modelRenderable
         }

         //transformableNode.setParent(anchorNode)
         isCreated = true

      }.exceptionally {
         Log.e("3D Model File Failed", it.toString())
         return@exceptionally null
      }
   }

   fun onUpdateFrame(frameTime: FrameTime) {
      val session = arFragment.arSceneView.session ?: return
      val frame = arFragment.arSceneView.arFrame ?: return

      if (modelRenderable != null && isCreated && (frame.camera.trackingState == TrackingState.TRACKING)) {
         val position = frame?.camera?.pose?.compose(Pose.makeTranslation(-0.5f, 0f, -2f))?.extractTranslation()
         anchorNode.localPosition = Vector3(position?.tx()!!, position.ty(), position.tz())
         directionRepository.arrowDegree.value?.let {
            transformableNode.localRotation = Quaternion.eulerAngles(Vector3(0f, it, -2f))

            Log.i("aaaaaaaaaaaaaaaaa", it.toString())
//            transformableNode.worldRotation = Quaternion.axisAngle(Vector3(0f, it, 0f), 10f)
         }


         /*
         val newMarkAnchor = session?.createAnchor(frame?.camera?.pose?.compose(Pose.makeTranslation(-0.5f, 0f, -2f))?.extractTranslation())
         val newAnchorNode = AnchorNode(newMarkAnchor)
         newAnchorNode.localScale = Vector3(1.0f, 1.0f, 1.0f)
         newAnchorNode.renderable = modelRenderable
         //newAnchorNode.localRotation = Quaternion.axisAngle(Vector3(0.0f, 0.0f, degree), 5.0f)
//         newAnchorNode.localRotation = Quaternion.multiply(q1, q2)

         val rotatio010 = Quaternion.axisAngle(Vector3(1.0f, degree, 0.0f), -10f)

         val node = TransformableNode(arFragment.transformationSystem)
         node.setParent(newAnchorNode)
         node.select()
         node.localRotation = rotatio010
         val newNode = Node()
         newNode.setParent(node)
         newNode.renderable = modelRenderable
         getCurrentScene().addChild(node)
         currentTransformableNode = node

//         newAnchorNode.setParent(getCurrentScene())
//         getCurrentScene().addChild(newAnchorNode)
         currentAnchorNode = newAnchorNode
         degree += 0.2f
         */
      }
   }

   private fun getCurrentScene(): Scene = arFragment.arSceneView.scene

}