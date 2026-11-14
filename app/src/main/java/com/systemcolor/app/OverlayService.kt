package com.systemcolor.app

import android.app.*
import android.app.usage.UsageStatsManager
import android.os.Handler
import android.os.Looper
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.*
import kotlin.math.abs

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var tint: View? = null
    private var bubble: TextView? = null
    private var panel: LinearLayout? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private val prefs by lazy { getSharedPreferences("colors", MODE_PRIVATE) }
    private val colors = listOf(
        "Quente" to Color.rgb(255, 155, 70),
        "Frio" to Color.rgb(65, 145, 255),
        "Âmbar" to Color.rgb(255, 110, 20),
        "Noturno" to Color.rgb(255, 65, 25),
        "Rosa" to Color.rgb(255, 80, 160),
        "Verde" to Color.rgb(50, 200, 100),
        "Cinza" to Color.rgb(80, 80, 80)
    )
    private var currentColor = colors[0].second
    private var intensity = 20
    private var enabled = false
    private val handler = Handler(Looper.getMainLooper())
    private var youtubeForeground = false
    private val youtubePackage = "com.google.android.youtube"
    private val monitor = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, 1500L)
        }
    }

    private fun usageAllowed(): Boolean {
        val ops = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    }

    private fun checkForegroundApp() {
        if (!usageAllowed() || !prefs.getBoolean("youtube_auto", true)) {
            youtubeForeground = false
            renderTint()
            return
        }
        val usage = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val events = usage.queryEvents(end - 12000L, end)
        val event = android.app.usage.UsageEvents.Event()
        var lastPackage: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                lastPackage = event.packageName
            }
        }
        // If no recent foreground event, query aggregate usage as a best-effort fallback.
        if (lastPackage == null) {
            lastPackage = usage.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, end - 60000L, end)
                ?.maxByOrNull { it.lastTimeUsed }?.packageName
        }
        youtubeForeground = lastPackage == youtubePackage
        renderTint()
    }

    private fun renderTint() {
        val yt = youtubeForeground && prefs.getBoolean("youtube_auto", true)
        val color = if (yt) prefs.getInt("youtube_color", Color.rgb(255, 0, 183)) else currentColor
        val level = if (yt) prefs.getInt("youtube_intensity", 45) else intensity
        tint?.setBackgroundColor(color)
        tint?.alpha = if (enabled) level.coerceIn(0, 75) / 100f else 0f
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentColor = prefs.getInt("color", currentColor)
        intensity = prefs.getInt("intensity", intensity).coerceIn(0, 75)
        enabled = prefs.getBoolean("enabled", false)
        if (intent?.action == "com.systemcolor.app.UPDATE") renderTint()
        return START_NOT_STICKY
    }


    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        currentColor = prefs.getInt("color", currentColor)
        intensity = prefs.getInt("intensity", intensity).coerceIn(0, 100)
        enabled = prefs.getBoolean("enabled", false)
        startNotification()
        try {
            createTint()
            handler.post(monitor)
            // Menu flutuante desativado por padrão; controle na tela principal.
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density + 0.5f).toInt()

    private fun rounded(color: Int, radius: Int = 18, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun params(width: Int, height: Int, flags: Int) =
        WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START }

    private fun createTint() {
        tint = View(this).apply {
            setBackgroundColor(currentColor)
            alpha = if (enabled) (intensity.coerceAtMost(75) / 100f) else 0f
        }
        wm.addView(tint, params(-1, -1,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS))
    }

    private fun createBubble() {
        val size = dp(56)
        val p = params(size, size, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE).apply {
            x = prefs.getInt("bubble_x", (resources.displayMetrics.widthPixels - size - dp(12)).coerceAtLeast(0))
            y = prefs.getInt("bubble_y", resources.displayMetrics.heightPixels / 3)
        }
        bubbleParams = p
        bubble = TextView(this).apply {
            text = "🎨"
            textSize = 26f
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(31, 35, 53), 28, Color.rgb(98, 111, 160))
            elevation = dp(9).toFloat()
            setOnTouchListener(object : View.OnTouchListener {
                private var downX = 0f
                private var downY = 0f
                private var startX = 0
                private var startY = 0
                private var dragged = false
                override fun onTouch(v: View, event: android.view.MotionEvent): Boolean {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            downX = event.rawX; downY = event.rawY
                            startX = p.x; startY = p.y; dragged = false
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.rawX - downX
                            val dy = event.rawY - downY
                            if (abs(dx) > dp(7) || abs(dy) > dp(7)) dragged = true
                            if (dragged) {
                                p.x = (startX + dx.toInt()).coerceIn(0, (resources.displayMetrics.widthPixels - size).coerceAtLeast(0))
                                p.y = (startY + dy.toInt()).coerceIn(0, (resources.displayMetrics.heightPixels - size).coerceAtLeast(0))
                                wm.updateViewLayout(v, p)
                            }
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (!dragged) { if (panel == null) showPanel() else hidePanel() }
                            else prefs.edit().putInt("bubble_x", p.x).putInt("bubble_y", p.y).apply()
                            return true
                        }
                        MotionEvent.ACTION_CANCEL -> return true
                    }
                    return false
                }
            })
        }
        wm.addView(bubble, p)
    }

    private fun showPanel() {
        if (panel != null) return
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(16))
            background = rounded(Color.rgb(23, 27, 42), 22, Color.rgb(66, 77, 110))
            elevation = dp(12).toFloat()
        }
        fun label(value: String, size: Float = 15f, color: Int = Color.WHITE): TextView =
            TextView(this).apply { text = value; textSize = size; setTextColor(color) }
        fun space(height: Int) = View(this).also { container.addView(it, LinearLayout.LayoutParams(1, dp(height))) }
        fun action(title: String, onClick: () -> Unit): Button = Button(this).apply {
            text = title
            isAllCaps = false
            setOnClickListener { onClick() }
        }
        container.addView(label("System Color", 23f))
        container.addView(label("Filtros globais para apps, jogos e vídeos", 12f, Color.rgb(175, 184, 205)))
        space(14)
        val selected = label("Filtro: " + (colors.firstOrNull { it.second == currentColor }?.first ?: "Personalizado"), 14f)
        container.addView(selected)
        space(8)
        val swatches = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        colors.forEach { (name, color) ->
            val item = TextView(this).apply {
                contentDescription = "Filtro $name"
                text = if (color == currentColor) "✓" else ""
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                textSize = 19f
                background = rounded(color, 30)
                setOnClickListener {
                    currentColor = color
                    selected.text = "Filtro: $name"
                    for (i in 0 until swatches.childCount) {
                        (swatches.getChildAt(i) as TextView).text = if (i == colors.indexOfFirst { it.second == color }) "✓" else ""
                    }
                    updateTint()
                }
            }
            val lp = LinearLayout.LayoutParams(0, dp(38), 1f).apply { marginEnd = dp(5) }
            swatches.addView(item, lp)
        }
        container.addView(swatches)
        space(17)
        val value = label("Intensidade: $intensity% (máx. visual 75%)", 14f)
        container.addView(value)
        container.addView(SeekBar(this).apply {
            max = 100; progress = intensity
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    intensity = progress
                    value.text = "Intensidade: $intensity% (máx. visual 75%)"
                    updateTint()
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        })
        space(8)
        val toggle = action(if (enabled) "Desativar filtro" else "Ativar filtro") {}
        toggle.setOnClickListener {
            enabled = !enabled
            toggle.text = if (enabled) "Desativar filtro" else "Ativar filtro"
            updateTint()
        }
        container.addView(toggle)
        container.addView(action("Fechar painel") { hidePanel() })
        container.addView(action("Encerrar filtros") { stopSelf() })
        space(4)
        container.addView(label("Filtro de tonalidade; não altera os pixels dos apps.", 11f, Color.rgb(175, 184, 205)))
        val width = dp(330).coerceAtMost(resources.displayMetrics.widthPixels - dp(24))
        val p = params(width, -2, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE).apply {
            x = ((resources.displayMetrics.widthPixels - width) / 2).coerceAtLeast(0)
            y = dp(65)
        }
        try { wm.addView(container, p); panel = container } catch (_: Exception) { panel = null }
    }

    private fun hidePanel() {
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel = null
    }

    private fun updateTint() {
        renderTint()
        prefs.edit().putInt("color", currentColor).putInt("intensity", intensity)
            .putBoolean("enabled", enabled).apply()
    }

    private fun startNotification() {
        val channelId = "system_color_overlay"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(channelId, "Filtros de tela", NotificationManager.IMPORTANCE_LOW))
        val intent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("System Color ativo")
            .setContentText("Abra o aplicativo para ajustar o filtro")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(intent)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, notification)
    }

    override fun onDestroy() {
        handler.removeCallbacks(monitor)
        if (::wm.isInitialized) {
            hidePanel()
            bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
            tint?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        }
        bubble = null; tint = null
        super.onDestroy()
    }
}
