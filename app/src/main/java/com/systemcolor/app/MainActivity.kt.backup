package com.systemcolor.app

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private val bg = Color.rgb(9, 11, 23)
    private val card = Color.rgb(23, 25, 43)
    private val cyan = Color.rgb(0, 222, 240)
    private val pink = Color.rgb(246, 44, 200)
    private val prefs by lazy { getSharedPreferences("colors", MODE_PRIVATE) }
    private val presets = listOf("QUENTE" to Color.rgb(255, 155, 70), "FRIO" to Color.rgb(65, 145, 255), "ÂMBAR" to Color.rgb(255, 110, 20), "NOTURNO" to Color.rgb(255, 65, 25), "ROSA" to Color.rgb(255, 80, 160), "VERDE" to Color.rgb(50, 200, 100))
    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()
    private fun shape(color: Int, outline: Int = cyan) = GradientDrawable().apply { setColor(color); cornerRadius = dp(18).toFloat(); setStroke(dp(1), outline) }
    private fun text(value: String, size: Float, color: Int = Color.WHITE, title: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        typeface = Typeface.create(if (title) "sans-serif-condensed" else "sans-serif", if (title) Typeface.BOLD else Typeface.NORMAL)
        if (title) letterSpacing = .09f
    }
    private fun vertical() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun add(parent: LinearLayout, child: View, margin: Int = 8) { parent.addView(child, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(margin) }) }
    private fun tile(title: String, subtitle: String, color: Int, action: () -> Unit): View = vertical().apply {
        background = shape(card, color); setPadding(dp(16), dp(16), dp(16), dp(16))
        add(this, text(title, 20f, color, true), 6); add(this, text(subtitle, 13f, Color.LTGRAY), 0)
        setOnClickListener { action() }
    }
    private fun applyColor(color: Int) { prefs.edit().putInt("color", color).apply(); sendUpdate() }
    private fun sendUpdate() {
        if (Settings.canDrawOverlays(this)) startService(Intent(this, OverlayService::class.java).setAction("com.systemcolor.app.UPDATE"))
    }
    private fun usageAllowed(): Boolean {
        val ops = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg; window.navigationBarColor = bg
        val root = vertical().apply { setPadding(dp(20), dp(28), dp(20), dp(24)); setBackgroundColor(bg) }
        add(root, text("◈ SYSTEM COLOR", 33f, cyan, true), 3)
        add(root, text("FILTROS E CAMADAS GLOBAIS", 12f, Color.LTGRAY, true), 22)
        val status = text("● SOBREPOSIÇÃO DESATIVADA", 15f, pink, true)
        val toggle = Switch(this).apply { text = "FILTRO GLOBAL"; textSize = 17f; setTextColor(Color.WHITE); isChecked = prefs.getBoolean("enabled", false) }
        val switchCard = vertical().apply { background = shape(card, pink); setPadding(dp(16), dp(14), dp(16), dp(12)) }
        add(switchCard, toggle); add(switchCard, status, 0); add(root, switchCard, 20)
        toggle.setOnCheckedChangeListener { _, checked ->
            if (checked && !Settings.canDrawOverlays(this)) {
                toggle.isChecked = false
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                Toast.makeText(this, "Autorize a sobreposição e ative novamente", Toast.LENGTH_LONG).show()
            } else {
                prefs.edit().putBoolean("enabled", checked).apply()
                if (checked) {
                    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
                    startForegroundService(Intent(this, OverlayService::class.java))
                } else stopService(Intent(this, OverlayService::class.java))
                status.text = if (checked) "● SOBREPOSIÇÃO ATIVADA" else "● SOBREPOSIÇÃO DESATIVADA"
            }
        }
        add(root, text("PERFIS", 23f, Color.WHITE, true), 10)
        add(root, tile("🎮  JOGOS", "Camada fria para sessões de jogos", cyan) { applyColor(Color.rgb(65, 145, 255)) }, 8)
        add(root, tile("▶  VÍDEOS", "Tonalidade quente para filmes e YouTube", pink) { applyColor(Color.rgb(255, 155, 70)) }, 8)
        add(root, tile("☾  USO NOTURNO", "Tom âmbar para ambientes escuros", Color.rgb(170, 100, 255)) { applyColor(Color.rgb(255, 110, 20)) }, 20)
        add(root, text("▶ YOUTUBE MODE", 23f, pink, true), 10)
        val ytCard = vertical().apply { background = shape(card, pink); setPadding(dp(16), dp(14), dp(16), dp(14)) }
        val ytSwitch = Switch(this).apply {
            text = "ATIVAÇÃO AUTOMÁTICA"; setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("youtube_auto", true)
        }
        add(ytCard, ytSwitch, 8)
        ytSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("youtube_auto", checked).apply()
            sendUpdate()
        }
        add(ytCard, text("FILTRO: PERSONALIZADO", 16f, cyan, true), 4)
        add(ytCard, text("Cor padrão: magenta #FF00B7", 13f, Color.LTGRAY), 8)
        val ytLabel = text("INTENSIDADE YOUTUBE: ${prefs.getInt("youtube_intensity", 45)}%", 15f, pink, true)
        add(ytCard, ytLabel, 3)
        add(ytCard, SeekBar(this).apply {
            max = 75; progress = prefs.getInt("youtube_intensity", 45).coerceIn(0, 75)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        prefs.edit().putInt("youtube_intensity", value).apply()
                        ytLabel.text = "INTENSIDADE YOUTUBE: $value%"
                        sendUpdate()
                    }
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        }, 8)
        add(ytCard, tile("ALTERAR COR PERSONALIZADA", "Alternar entre magenta, azul e âmbar", cyan) {
            val choices = intArrayOf(Color.rgb(255, 0, 183), Color.rgb(0, 190, 255), Color.rgb(255, 155, 70))
            val current = prefs.getInt("youtube_color", choices[0])
            val next = choices[(choices.indexOf(current) + 1) % choices.size]
            prefs.edit().putInt("youtube_color", next).apply()
            sendUpdate()
            Toast.makeText(this, "Cor do YouTube atualizada", Toast.LENGTH_SHORT).show()
        }, 6)
        add(ytCard, tile("PERMITIR DETECÇÃO DO YOUTUBE", "Conceder acesso aos dados de uso dos apps", cyan) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }, 6)
        add(ytCard, text("Detecta o YouTube oficial em primeiro plano. Ao sair, restaura o filtro global anterior. Exige acesso ao uso dos apps e sobreposição. Alguns aparelhos podem limitar a detecção.", 12f, Color.LTGRAY), 0)
        add(root, ytCard, 20)
        add(root, text("FILTROS DE CORES", 23f, Color.WHITE, true), 10)
        val grid = GridLayout(this).apply { columnCount = 2 }
        presets.forEach { (name, color) ->
            val b = TextView(this).apply {
                text = "●  $name"; textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
                background = shape(card, color); setOnClickListener { applyColor(color); Toast.makeText(this@MainActivity, "$name selecionado", Toast.LENGTH_SHORT).show() }
            }
            grid.addView(b, GridLayout.LayoutParams().apply { width = (resources.displayMetrics.widthPixels - dp(64)) / 2; height = dp(60); setMargins(dp(3), dp(3), dp(3), dp(3)) })
        }
        add(root, grid, 20)
        val intensityLabel = text("INTENSIDADE  ${prefs.getInt("intensity", 20)}%", 17f, pink, true)
        add(root, intensityLabel, 4)
        val seek = SeekBar(this).apply { max = 75; progress = prefs.getInt("intensity", 20).coerceIn(0, 75) }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) { intensityLabel.text = "INTENSIDADE  $progress%"; prefs.edit().putInt("intensity", progress).apply(); sendUpdate() } }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        add(root, seek, 10)
        add(root, text("O filtro é uma camada translúcida. Não aumenta saturação, HDR, nitidez ou contraste reais. Algumas telas protegidas bloqueiam sobreposições.", 13f, Color.LTGRAY), 14)
        add(root, tile("⚙  PERMISSÃO DE SOBREPOSIÇÃO", "Abrir configurações do Android", cyan) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }, 8)
        add(root, tile("■  ENCERRAR SERVIÇO", "Remover a camada de cor", pink) { toggle.isChecked = false }, 0)
        
val scroll = ScrollView(this).apply {
    isFillViewport = true
    addView(root)
 }

        setContentView(scroll)
        status.text = if (prefs.getBoolean("enabled", false)) "● FILTRO CONFIGURADO" else "● SOBREPOSIÇÃO DESATIVADA"
    }
}
