package com.example.tvquake

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)

        // Ask for overlay permission if needed
        if (!Settings.canDrawOverlays(this)) {
            statusText.text = "Lütfen 'Diğer uygulamaların üzerinde çiz' iznini verin."
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            startService()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            startService()
        }
    }

    private fun startService() {
        statusText.text = "Servis çalışıyor. PC'den komut bekleniyor..."
        val i = Intent(this, OverlayService::class.java)
        startForegroundService(i)
    }
}
