package com.example.tvquake

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.media.ToneGenerator
import android.media.AudioManager
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.NanoHTTPD
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.FixedStatusCode
import org.nanohttpd.protocols.http.response.Status
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var timer: Timer? = null
    private var seconds = 0
    private val ui = Handler(Looper.getMainLooper())
    private val tone = ToneGenerator(AudioManager.STREAM_ALARM, 80)
    private var server: SimpleHttpServer? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground()
        startServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServer()
        hideOverlay()
        tone.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForeground() {
        val channelId = "tvquake_alert"
        val channelName = "TV Quake Alert"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }
        val n: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("TV Quake Alert")
                .setContentText("Komut bekleniyor...")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("TV Quake Alert")
                .setContentText("Komut bekleniyor...")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .build()
        }
        startForeground(1, n)
    }

    private fun startServer() {
        server = SimpleHttpServer(8080, object: SimpleHttpServer.Callback {
            override fun onAlert(delay: Int, message: String) {
                ui.postDelayed({
                    showOverlay(message)
                }, (delay * 1000).toLong())
            }
        })
        server?.start()
    }

    private fun stopServer() {
        try { server?.stop() } catch (e: Exception) { }
    }

    private fun showOverlay(message: String) {
        hideOverlay() // ensure single overlay
        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_alert, null)
        val title = overlayView!!.findViewById<TextView>(R.id.title)
        val msg = overlayView!!.findViewById<TextView>(R.id.message)
        val timerText = overlayView!!.findViewById<TextView>(R.id.timer)

        title.text = "DEPREM UYARISI"
        msg.text = message
        seconds = 0
        timerText.text = "Geçen süre: 0 sn"

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 48
        params.y = 48

        windowManager.addView(overlayView, params)

        // Start ticking and beeping every second
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                seconds += 1
                ui.post {
                    timerText.text = "Geçen süre: ${'$'}seconds sn"
                    // Beep short tone
                    tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150)
                }
            }
        }, 0, 1000)
    }

    private fun hideOverlay() {
        timer?.cancel()
        timer = null
        overlayView?.let {
            windowManager.removeView(it)
        }
        overlayView = null
    }
}

class SimpleHttpServer(port: Int, private val cb: Callback) : NanoHTTPD(port) {

    interface Callback {
        fun onAlert(delay: Int, message: String)
    }

    override fun serve(session: IHTTPSession): Response {
        return try {
            if (session.uri == "/alert" && (session.method.name == "POST" || session.method.name == "GET")) {
                val params = HashMap<String, String>()
                session.parseBody(params)
                var delay = 5
                var message = "Marmara Denizi, İstanbul yakınlarında 6.2 büyüklüğünde deprem."

                // For GET, read query params
                val qs = session.parameters
                if (qs.containsKey("delay")) {
                    delay = qs["delay"]?.firstOrNull()?.toIntOrNull() ?: delay
                }
                if (qs.containsKey("message")) {
                    message = qs["message"]?.firstOrNull() ?: message
                }

                // For POST JSON
                val body = params["postData"]
                if (!body.isNullOrEmpty()) {
                    try {
                        val json = JSONObject(body)
                        delay = json.optInt("delaySeconds", delay)
                        message = json.optString("message", message)
                    } catch (_: Exception) {}
                }

                cb.onAlert(delay, message)
                newFixedLengthResponse(Status.OK, "application/json",
                    "{\"status\":\"scheduled\",\"delay\":"+delay+",\"message\":"+JSONObject.quote(message)+"}")
            } else {
                newFixedLengthResponse(Status.OK, "text/plain",
                    "TV Quake Alert çalışıyor. /alert endpoint'ine POST veya GET gönderin.")
            }
        } catch (e: Exception) {
            newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", "Hata: ${e.message}")
        }
    }
}
