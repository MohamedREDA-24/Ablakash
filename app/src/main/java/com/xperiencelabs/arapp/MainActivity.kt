package com.xperiencelabs.arapp

import ModelAdapter
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isGone
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Config
//import com.xperiencelabs.arapp.ChatAdapter.Companion.BASE_IMAGE_URL
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation

class MainActivity : AppCompatActivity() {

    private lateinit var sceneView: ArSceneView
    private lateinit var placeButton: ExtendedFloatingActionButton
    private lateinit var modelNode: ArModelNode
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var rotateGestureDetector: GestureDetector

    // Variable to track the current rotation of the model.
    private var currentRotation = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Retrieve passed image URL(s) from the intent.
        val passedImageUrlList = intent.getStringArrayListExtra("IMAGE_URL_LIST")
        val imageUrlList2d = intent.getStringArrayListExtra("IMAGE_URL_LIST_2D")

        if (passedImageUrlList != null && passedImageUrlList.isNotEmpty()) {
            Log.d("MainActivity", "Received image URLs:")
            passedImageUrlList.forEachIndexed { index, url ->
                Log.d("MainActivity", "Image URL ${index + 1}: $url")
            }
        } else {
            Log.d("MainActivity", "No image URLs provided in the intent!")
            // Optionally, you could finish() the activity or handle this scenario differently.
        }

        // Setup the DrawerLayout.
        drawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.openDrawer(GravityCompat.START)

        // Setup the AR SceneView.
        sceneView = findViewById<ArSceneView>(R.id.sceneView).apply {
            this.lightEstimationMode = Config.LightEstimationMode.DISABLED
        }

        placeButton = findViewById(R.id.place)
        placeButton.setOnClickListener {
            placeModel()
        }

        val openSidebarButton = findViewById<Button>(R.id.open_sidebar_button)
        openSidebarButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        val BASE_IMAGE_URL = "http://13.92.86.232/static/" // Use your server URL


        // Setup the sidebar (RecyclerView).
        val modelListRecyclerView = findViewById<RecyclerView>(R.id.model_list)
        modelListRecyclerView.layoutManager = LinearLayoutManager(this)

        // Dynamically build the models list using the passed URL variables.
        // If no URL list was passed, you can optionally create a default list.
//        Log.d("ChatActivity", "---------------------------: $passedImageUrlList")

        val models = if (!passedImageUrlList.isNullOrEmpty()) {
            passedImageUrlList.mapIndexed { index, url ->
                ModelItem(
                    "Item ${index + 1}",
                     url,
                     (imageUrlList2d?.get(index) ?: "")
                )
            }
        } else {
            emptyList()
        }



        val adapter = ModelAdapter(models) { modelItem ->
            loadModel(modelItem)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        modelListRecyclerView.adapter = adapter
    }

    /**
     * Loads the selected model into the AR scene.
     */
    private fun loadModel(modelItem: ModelItem) {
        // Remove any previous model.
        if (::modelNode.isInitialized) {
            sceneView.removeChild(modelNode)
        }

        modelNode = ArModelNode(sceneView.engine, PlacementMode.INSTANT).apply {
            loadModelGlbAsync(
                glbFileLocation = modelItem.modelPath,
                scaleToUnits = 1f,
                centerOrigin = Position(-0.5f)
            ) {
                sceneView.planeRenderer.isVisible = true
            }
            onAnchorChanged = {
                placeButton.isGone = false
            }
        }

        sceneView.addChild(modelNode)
        placeButton.isGone = false

        // Setup gesture controls for scaling and rotation.
        setupGestureControls()
    }

    /**
     * Anchors the current model in the AR scene.
     */
    private fun placeModel() {
        modelNode.anchor()
        sceneView.planeRenderer.isVisible = false
    }

    /**
     * Sets up pinch-to-zoom (scaling) and drag-to-rotate gesture controls.
     */
    private fun setupGestureControls() {
        // Pinch-to-scale gesture.
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (::modelNode.isInitialized) {
                    val newScale = modelNode.scale.x * detector.scaleFactor
                    val clampedScale = newScale.coerceIn(0.5f, 2.0f)
                    modelNode.scale = Position(clampedScale, clampedScale, clampedScale)
                }
                return true
            }
        })

        // Drag-to-rotate gesture.
        rotateGestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (::modelNode.isInitialized) {
                    currentRotation -= distanceX * 0.5f  // Adjust sensitivity as needed.
                    modelNode.rotation = Rotation(y = currentRotation)
                }
                return true
            }
        })

        // Attach the gesture detectors to the SceneView.
        sceneView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            rotateGestureDetector.onTouchEvent(event)
            true
        }
    }
}