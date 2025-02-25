package com.xperiencelabs.arapp

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ChatApiService {
    @POST("chat")  // Adjust endpoint as needed
    fun sendMessage(@Body request: MessageRequest): Call<List<MessageResponse>>
}
