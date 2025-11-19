//package com.example.hackathon.filament
//
//import android.content.Context
//import android.util.Log
//import android.view.Surface
//import com.google.android.filament.*
//import com.google.android.filament.gltfio.*
//import com.google.android.filament.utils.*
//
//class FilamentRenderer(
//    private val context: Context,
//    private val surface: Surface
//) {
//
//    private val engine: Engine = Engine.create()
//    private val renderer: Renderer = engine.createRenderer()
//    private val scene: Scene = engine.createScene()
//    private val view: View = engine.createView()
//    private val cameraEntity = EntityManager.get().create()
//    private val camera = view.camera
//    private val swapChain: SwapChain = engine.createSwapChain(surface)
//
//    // gltfio loaders
//    private val assetLoader: AssetLoader
//    private val resourceLoader: ResourceLoader
//    private val ubershaderProvider: UbershaderProvider
//
//    private var filamentAsset: FilamentAsset? = null
//
//    init {
//        view.camera = camera
//        view.scene = scene
//
//        // GLTFio setup for Filament 1.51
//        ubershaderProvider = UbershaderProvider(engine)
//        assetLoader = AssetLoader(engine, ubershaderProvider, EntityManager.get())
//        resourceLoader = ResourceLoader(engine)
//
//        // Add default light
//        addDefaultLight()
//
//        Log.d("FILAMENT", "Renderer initialized")
//    }
//
//    fun loadModelFromAssets(path: String) {
//        try {
//            val glbBytes = context.assets.open(path).readBytes()
//            filamentAsset = assetLoader.createAssetFromJson(glbBytes) // for .gltf JSON
//
//            if (filamentAsset == null) {
//                Log.e("FILAMENT", "Failed to decode GLB!")
//                return
//            }
//
//            resourceLoader.loadResources(filamentAsset!!)
//
//            // add asset's renderables to scene
//            for (entity in filamentAsset!!.entities) {
//                if (engine.renderableManager.hasComponent(entity)) {
//                    scene.addEntity(entity)
//                }
//            }
//
//            // wait until materials/textures ready
//            filamentAsset!!.releaseSourceData()
//
//        } catch (e: Exception) {
//            Log.e("FILAMENT", "ERROR loading model: ${e.message}")
//        }
//    }
//
//    fun setViewport(width: Int, height: Int) {
//        view.viewport = Viewport(0, 0, width, height)
//
//        camera.setProjection(
//            45.0,             // fov
//            aspect,           // width/height
//            0.1, 10.0,
//            Camera.Fov.VERTICAL // you must provide direction
//        )
//        camera.lookAt(
//            eyeX, eyeY, eyeZ,
//            centerX, centerY, centerZ,
//            upX, upY, upZ    // must supply 'up' vector now
//        )
//
//    }
//
//    fun setModelTransformFor2DOverlay() {
//        filamentAsset?.let { asset ->
//            val tm = engine.transformManager
//            val inst = tm.getInstance(asset.root)
//
//            tm.setTransform(inst, floatArrayOf(
//                1f, 0f, 0f, 0f,
//                0f, 1f, 0f, 0f,
//                0f, 0f, 1f, 0f,
//                0f, -0.5f, 0f, 1f
//            ))
//        }
//    }
//
//    fun requestRender() {
//        if (renderer.beginFrame(swapChain)) {
//            renderer.render(view)
//            renderer.endFrame()
//        }
//    }
//
//    fun destroy() {
//        filamentAsset?.let {
//            assetLoader.destroyAsset(it)
//        }
//        engine.destroyRenderer(renderer)
//        engine.destroyView(view)
//        engine.destroyScene(scene)
//        engine.destroyCameraComponent(cameraEntity)
//        engine.destroy()
//    }
//
//    private fun addDefaultLight() {
//        // simple sunlight
//        val sunlight = EntityManager.get().create()
//        LightManager.Builder(LightManager.Type.SUN)
//            .intensity(100_000f)
//            .direction(0f, -1f, -0.5f)
//            .color(1.0f, 1.0f, 1.0f)
//            .castShadows(true)
//            .build(engine, sunlight)
//
//        scene.addEntity(sunlight)
//    }
//}
