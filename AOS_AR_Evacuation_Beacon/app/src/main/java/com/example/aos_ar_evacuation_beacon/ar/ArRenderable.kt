import android.content.Context
import android.util.Log
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

   lateinit var transformableNode: TransformableNode
   private var modelRenderable: ModelRenderable? = null
   var isCreated = false

   fun addNodeToScene() {
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
            renderable = modelRenderable
         }
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
            transformableNode.worldRotation = Quaternion.eulerAngles(Vector3(0f, it, -2f))
         }
      }
   }

   private fun getCurrentScene(): Scene = arFragment.arSceneView.scene
}