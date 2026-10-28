package com.tracker.instareels

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackerAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlayBadge: TextView? = null
    private var isOverlayAttached = false

    private var lastPostScrollTime = 0L
    private var lastReelScrollTime = 0L
    private var sessionStartTime = 0L
    private var isAppActive = false

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isAppActive) {
                updateTodayDuration()
                updateBadgeText()
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private fun getTodayKey(): String {
        return SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: ""
        val isInstagram = pkg == "com.instagram.android"

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (isInstagram) {
                if (!isAppActive) {
                    isAppActive = true
                    sessionStartTime = SystemClock.elapsedRealtime()
                    handler.post(timerRunnable)
                    showOverlay()
                }
            } else {
                if (isAppActive) {
                    isAppActive = false
                    updateTodayDuration()
                    handler.removeCallbacks(timerRunnable)
                    hideOverlay()
                }
            }
        }

        if (isInstagram && event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            val currentTime = SystemClock.elapsedRealtime()
            val className = event.className?.toString() ?: ""

            // Reels Viewer detection (Vertical ViewPager)
            if (className.contains("ViewPager", ignoreCase = true)) {
                if (currentTime - lastReelScrollTime > 1500) {
                    lastReelScrollTime = currentTime
                    incrementCount("reels")
                }
            } 
            // Home Feed Posts detection (RecyclerView / ListView)
            else if (className.contains("RecyclerView", ignoreCase = true) || 
                       className.contains("ListView", ignoreCase = true)) {
                // Minimum 2.2 seconds gap vunte posts jump / rapid duplicate counts padavu
                if (currentTime - lastPostScrollTime > 2200) {
                    lastPostScrollTime = currentTime
                    incrementCount("posts")
                }
            }
        }
    }

    private fun incrementCount(type: String) {
        val today = getTodayKey()
        val prefs = getSharedPreferences("InstaReelsStats", Context.MODE_PRIVATE)
        val key = if (type == "reels") "${today}_reels" else "${today}_posts"
        val count = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, count).apply()
        updateBadgeText()
    }

    private fun updateTodayDuration() {
        if (sessionStartTime == 0L) return
        val elapsed = SystemClock.elapsedRealtime() - sessionStartTime
        sessionStartTime = SystemClock.elapsedRealtime()

        val today = getTodayKey()
        val prefs = getSharedPreferences("InstaReelsStats", Context.MODE_PRIVATE)
        val totalMs = prefs.getLong("${today}_time", 0L) + elapsed
        prefs.edit().putLong("${today}_time", totalMs).apply()
    }

    private fun showOverlay() {
        if (isOverlayAttached || !Settings.canDrawOverlays(this)) return

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 30
            y = 120
        }

        overlayBadge = TextView(this).apply {
            setBackgroundColor(Color.parseColor("#CC111111"))
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(20, 10, 20, 10)
        }

        try {
            windowManager?.addView(overlayBadge, params)
            isOverlayAttached = true
            updateBadgeText()
        } catch (_: Exception) {}
    }

    private fun hideOverlay() {
        if (isOverlayAttached && overlayBadge != null) {
            try {
                windowManager?.removeView(overlayBadge)
            } catch (_: Exception) {}
            isOverlayAttached = false
        }
    }

    private fun updateBadgeText() {
        if (!isOverlayAttached || overlayBadge == null) return
        val today = getTodayKey()
        val prefs = getSharedPreferences("InstaReelsStats", Context.MODE_PRIVATE)
        val reels = prefs.getInt("${today}_reels", 0)
        val posts = prefs.getInt("${today}_posts", 0)
        val timeMs = prefs.getLong("${today}_time", 0L)
        val mins = (timeMs / 1000) / 60

        overlayBadge?.text = "🎬 $reels | 📰 $posts | ⏱️ ${mins}m"
    }

    override fun onInterrupt() {
        hideOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
    }
}
