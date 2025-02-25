package com.xperiencelabs.arapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)  // Ensure this layout exists

        // Delay for 3 seconds then start ChatActivity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, ChatActivity::class.java))
            finish()
        }, 3000)
    }
}
