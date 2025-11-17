package com.example.hackathon

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment

class AppleARActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private var appleRenderable: ModelRenderable? = null
    private var placedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_banana_ar) // uses your existing XML

        arFragment = supportFragmentManager
            .findFragmentById(R.id.arFragment) as ArFragment

        // Load apple 3D model from assets/models/apple.glb
        ModelRenderable.builder()
            .setSource(this, Uri.parse("models/apple.glb"))
            .setIsFilamentGltf(true)
            .build()
            .thenAccept { renderable ->
                appleRenderable = renderable
            }
            .exceptionally {
                it.printStackTrace()
                null
            }

        // Tap on plane to place apple once
        arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, _ ->
            if (appleRenderable == null || placedOnce) return@setOnTapArPlaneListener

            val anchor: Anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor).apply {
                renderable = appleRenderable
                setParent(arFragment.arSceneView.scene)
            }

            placedOnce = true
        }

        // Optional: hide the “move your phone” hand if you want
//        arFragment.planeDiscoveryController.hide()
//        arFragment.planeDiscoveryController.setInstructionView(null)
    }
}
