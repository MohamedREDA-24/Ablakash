package com.xperiencelabs.arapp

data class ModelItem(
    val name: String,
    val modelPath: String,      // Path to your .glb file (e.g., "models/sofa.glb")
    val thumbnailRes: String       // Drawable resource for thumbnail
)
