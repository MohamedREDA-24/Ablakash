package com.xperiencelabs.arapp

data class MessageRequest(
    val message: String
)

data class MessageResponse(
    val image_path: String,
    val score: Float
)
