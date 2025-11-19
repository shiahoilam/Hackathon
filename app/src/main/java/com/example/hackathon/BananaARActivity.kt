//package com.example.hackathon
//
//import android.os.Bundle
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.fragment.app.Fragment
//import com.google.ar.sceneform.Sceneform
//import com.google.ar.sceneform.rendering.RenderableInstance
//import com.google.ar.sceneform.ux.ArFragment
//import android.util.Log
//
//class BananaARActivity : AppCompatActivity() {
//
//    private var arFragment: ArFragment? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // 1) Check AR support on this device
//        if (!Sceneform.isSupported(this)) {
//            Toast.makeText(
//                this,
//                "AR is not supported on this device.",
//                Toast.LENGTH_LONG
//            ).show()
//            finish()
//            return
//        }
//
//        setContentView(R.layout.activity_banana_ar)
//
//        // 2) Listen for the ArFragment being attached
//        supportFragmentManager.addFragmentOnAttachListener { _: androidx.fragment.app.FragmentManager, fragment: Fragment ->
//            if (fragment.id == R.id.arFragment && fragment is ArFragment) {
//                arFragment = fragment
//                setupAr()
//            }
//        }
//
//        // 3) Add the ArFragment into the container if first launch
//        if (savedInstanceState == null) {
//            supportFragmentManager.beginTransaction()
//                .add(R.id.arFragment, ArFragment::class.java, null)
//                .commit()
//        }
//    }
//
//    private fun setupAr() {
//        val fragment = arFragment ?: return
//
//        // 4) Load banana.glb from assets on tap
//        fragment.setOnTapPlaneGlbModel(
//            "apple_model.glb",
//            object : ArFragment.OnTapModelListener {
//                override fun onModelAdded(renderableInstance: RenderableInstance) {
//                    // Model successfully placed – you can customise scale/rotation here if you want
//                    Log.d("BananaAR", "Banana model added")
//                }
//
//                override fun onModelError(exception: Throwable) {
//                    Log.e("BananaAR", "Error loading banana model", exception)
//                    Toast.makeText(
//                        this@BananaARActivity,
//                        "Error loading banana model: ${exception.message}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//        )
//    }
//}
//
