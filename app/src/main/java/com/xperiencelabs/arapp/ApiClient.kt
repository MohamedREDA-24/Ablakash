package com.xperiencelabs.arapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://13.92.86.232/get-item"  // Replace with your backend URL
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val chatApiService: ChatApiService = retrofit.create(ChatApiService::class.java)
}
