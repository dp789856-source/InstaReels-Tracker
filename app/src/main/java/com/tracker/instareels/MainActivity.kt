package com.tracker.instareels

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var statsTextView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            refreshStats()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(40, 80, 40, 40)
            setBackgroundColor(Color.parseColor("#121212"))
        }

        val title = TextView(this).apply {
            text = "InstaReels Tracker"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        statsTextView = TextView(this).apply {
            textSize = 19f
            setTextColor(Color.parseColor("#00E676"))
            gravity = Gravity.CENTER
            setPadding(30, 40, 30, 40)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
        }

        val overlayButton = Button(this).apply {
            text = "1. Enable Overlay Permission (Popup)"
            setBackgroundColor(Color.parseColor("#2979FF"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                }
            }
        }

        val accessibilityButton = Button(this).apply {
            text = "2. Enable Accessibility Service"
            setBackgroundColor(Color.parseColor("#6200EA"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val spacer1 = LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(1, 30) }
        val spacer2 = LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(1, 20) }

        rootLayout.addView(title)
        rootLayout.addView(statsTextView)
        rootLayout.addView(spacer1)
        rootLayout.addView(overlayButton)
        rootLayout.addView(spacer2)
        rootLayout.addView(accessibilityButton)

        setContentView(rootLayout)
    }

    private fun refreshStats() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val prefs = getSharedPreferences("InstaReelsStats", Context.MODE_PRIVATE)
        val reels = prefs.getInt("${today}_reels", 0)
        val posts = prefs.getInt("${today}_posts", 0)
        val timeMs = prefs.getLong("${today}_time", 0L)
        val mins = (timeMs / 1000) / 60
        val secs = (timeMs / 1000) % 60

        statsTextView.text = "📅 Today's Instagram Usage:\n\n🎬 Reels Watched: $reels\n📰 Posts Scrolled: $posts\n⏱️ Time Spent: ${mins}m ${secs}s"
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }
}
