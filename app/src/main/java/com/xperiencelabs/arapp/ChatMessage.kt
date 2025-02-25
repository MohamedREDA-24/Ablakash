package com.xperiencelabs.arapp

// Data classes for JSON parsing
data class ApiResponse(
    val content: List<ContentItem>? = null,
    val content_scrapped: String? = null

)

data class ContentItem(
    val image_2d: String,
    val image_3d: String,
    val score: Double
)

data class ChatMessage(
    val message: CharSequence? = null, // Text message (if applicable)
    val imageUrl: String? = null, // Image path (if applicable)
    val model3dUrl: String? = null,  // For future 3D model loading
    val isSent: Boolean ,// True if the user sent the message, false for received
    val isReply: Boolean = false // True for bot replies

)
