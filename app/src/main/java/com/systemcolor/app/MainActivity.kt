package com.systemcolor.app

import android.Manifest
import android.app.Activity
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
import android.widget.*
import android.app.AlertDialog

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("colors", MODE_PRIVATE) }
    private val background = Color.rgb(5, 6, 9)
    private val surface = Color.rgb(28, 30, 36)
    private val cyan = Color.rgb(23, 206, 229)
    private val muted = Color.rgb(171, 175, 185)
    private val filters = listOf(
        "Cinematic" to 0xFF577D9CL.toInt(), "Teal & Orange" to 0xFF26A7A1L.toInt(),
        "Hollywood" to 0xFFBA824EL.toInt(), "Film Look" to 0xFF86777BL.toInt(),
        "Vivid" to 0xFFB842A6L.toInt(), "Tropical" to 0xFF1ABF8EL.toInt(),
        "Neon" to 0xFF9337E6L.toInt(), "Pop Color" to 0xFFF0538BL.toInt(),
        "Golden Hour" to 0xFFFFB44BL.toInt(), "Sunset" to 0xFFF27343L.toInt(),
        "Amber" to 0xFFDC8B30L.toInt(), "Warm Glow" to 0xFFF4A46BL.toInt(),
        "Ice Blue" to 0xFF7CBEEBL.toInt(), "Arctic" to 0xFF9ACFDCL.toInt(),
        "Ocean" to 0xFF168EBBL.toInt(), "Midnight" to 0xFF424F98L.toInt(),
        "Vintage" to 0xFFB19A73L.toInt(), "Retro Film" to 0xFFAD8A65L.toInt(),
        "Sepia" to 0xFF9D7549L.toInt(), "Nostalgia" to 0xFFB68B90L.toInt(),
        "Noir" to 0xFF666666L.toInt(), "Rose" to 0xFFEA7AA7L.toInt(),
        "Emerald" to 0xFF32B88BL.toInt(), "Purple Dream" to 0xFF8663D8L.toInt()
    )
    private fun dp(n: Int) = (n * resources.displayMetrics.density + .5f).toInt()
    private fun shape(color: Int, radius: Int = 22, border: Int? = null) =
        GradientDrawable().apply {
            setColor(color); cornerRadius = dp(radius).toFloat()
            if (border != null) setStroke(dp(1), border)
        }
    private fun gradient(a: Int, b: Int, radius: Int = 22) =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(a, b)).apply {
            cornerRadius = dp(radius).toFloat()
        }
    private fun label(s: String, size: Float = 16f, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply {
            text = s; textSize = size; setTextColor(color)
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun add(parent: LinearLayout, view: View, bottom: Int = 0) {
        parent.addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(bottom) })
    }
    private fun updateOverlay() {
        if (Settings.canDrawOverlays(this) && (prefs.getBoolean("enabled", false) || prefs.getBoolean("bubble_enabled", false))) {
            val intent = Intent(this, OverlayService::class.java).setAction("com.systemcolor.app.UPDATE")
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        }
    }
    private fun setFilter(index: Int) {
        val (name, color) = filters[index]
        prefs.edit().putString("selected_filter", name).putInt("color", color).apply()
        updateOverlay()
        recreate()
    }
    private fun allFilters() {
        val names = filters.map { it.first }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Escolha um filtro")
            .setSingleChoiceItems(names, filters.indexOfFirst { it.first == prefs.getString("selected_filter", "Cinematic") }.coerceAtLeast(0)) { dialog, which ->
                dialog.dismiss(); setFilter(which)
            }.setNegativeButton("Cancelar", null).show()
    }
    private fun enable(checked: Boolean) {
        if (checked && !Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Autorize a sobreposição e ative novamente", Toast.LENGTH_LONG).show()
            return
        }
        prefs.edit().putBoolean("enabled", checked).apply()
        if (checked) {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
            updateOverlay()
        } else if (prefs.getBoolean("bubble_enabled", false)) updateOverlay()
        else stopService(Intent(this, OverlayService::class.java))
        recreate()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = background
        window.navigationBarColor = background
        val root = column().apply {
            setBackgroundColor(background)
            setPadding(dp(16), dp(16), dp(16), dp(28))
        }
        val header = row()
        val settings = label("⚙", 29f).apply {
            gravity = Gravity.CENTER; background = shape(surface, 16)
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
            contentDescription = "Configurações"
        }
        header.addView(settings, LinearLayout.LayoutParams(dp(48), dp(48)))
        val brand = column().apply { setPadding(dp(13), 0, 0, 0) }
        add(brand, label("System Color", 27f, Color.WHITE, true))
        add(brand, label("Filtros, Temas e Efeitos Pro", 13f, muted))
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        val search = label("⌕", 31f).apply {
            gravity = Gravity.CENTER; contentDescription = "Ver todos os filtros"
            setOnClickListener { allFilters() }
        }
        header.addView(search, LinearLayout.LayoutParams(dp(44), dp(48)))
        add(root, header, 24)

        val chosen = prefs.getString("selected_filter", "Cinematic") ?: "Cinematic"
        val selectedColor = filters.firstOrNull { it.first == chosen }?.second ?: filters[0].second
        val hero = column().apply {
            background = gradient(0xFF1B2845L.toInt(), selectedColor)
            setPadding(dp(20), dp(22), dp(20), dp(20))
        }
        val heroTop = row()
        val heroName = column()
        add(heroName, label("FILTRO ATIVO", 13f, Color.WHITE, true), 8)
        add(heroName, label(chosen, 31f, Color.WHITE, true), 7)
        add(heroName, label("Sobreposição de tonalidade", 14f, Color.WHITE))
        heroTop.addView(heroName, LinearLayout.LayoutParams(0, -2, 1f))
        heroTop.addView(Switch(this).apply {
            isChecked = prefs.getBoolean("enabled", false)
            setOnCheckedChangeListener { _, value -> enable(value) }
            contentDescription = "Ativar filtro global"
        })
        add(hero, heroTop)
        add(root, hero, 20)

        val shortcuts = row().apply { gravity = Gravity.CENTER }
        val entries = listOf(
            Triple("Filtros", 0xFF813CE6L.toInt(), "◉"),
            Triple("Temas", 0xFF168DEBL.toInt(), "▱"),
            Triple("Efeitos Pro", 0xFFEE3F77L.toInt(), "✦"),
            Triple("Modos", 0xFF1AC45AL.toInt(), "▶")
        )
        entries.forEachIndexed { i, entry ->
            val tile = column().apply {
                gravity = Gravity.CENTER
                background = shape(surface, 19)
                setPadding(dp(4), dp(14), dp(4), dp(14))
                setOnClickListener {
                    when (i) {
                        0, 2 -> allFilters()
                        else -> startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    }
                }
            }
            val icon = label(entry.third, 25f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER; background = shape(entry.second, 14)
            }
            tile.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)))
            val name = label(entry.first, 12f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER; setPadding(0, dp(9), 0, 0)
            }
            add(tile, name)
            shortcuts.addView(tile, LinearLayout.LayoutParams(0, dp(114), 1f).apply {
                if (i != 3) rightMargin = dp(7)
            })
        }
        add(root, shortcuts, 20)

        val filterCard = column().apply {
            background = shape(surface)
            setPadding(dp(14), dp(16), dp(14), dp(14))
        }
        val filterHeading = row()
        filterHeading.addView(label("Filtros em Destaque", 19f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        filterHeading.addView(label("Ver todos  ›", 13f, cyan).apply { setOnClickListener { allFilters() } })
        add(filterCard, filterHeading, 15)
        val scrollFilters = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val choices = row()
        filters.take(8).forEachIndexed { index, pair ->
            val option = column().apply {
                gravity = Gravity.CENTER
                background = shape(if (pair.first == chosen) 0xFF164C5BL.toInt() else 0xFF343740L.toInt(),
                    15, if (pair.first == chosen) cyan else null)
                setPadding(dp(5), dp(5), dp(5), dp(7))
                setOnClickListener { setFilter(index) }
            }
            val preview = View(this).apply { background = gradient(0xFF243B59L.toInt(), pair.second, 12) }
            option.addView(preview, LinearLayout.LayoutParams(-1, dp(74)))
            val name = label(pair.first, 12f, Color.WHITE, pair.first == chosen).apply {
                gravity = Gravity.CENTER; maxLines = 1
                setPadding(0, dp(7), 0, 0)
            }
            add(option, name)
            choices.addView(option, LinearLayout.LayoutParams(dp(108), -2).apply { rightMargin = dp(8) })
        }
        scrollFilters.addView(choices)
        add(filterCard, scrollFilters)
        add(root, filterCard, 18)

        val intensityCard = column().apply {
            background = shape(surface); setPadding(dp(18), dp(15), dp(18), dp(12))
        }
        val intensityRow = row()
        intensityRow.addView(label("Intensidade do Filtro", 17f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        val intensityValue = label("${prefs.getInt("intensity", 45)}%", 20f, cyan, true)
        intensityRow.addView(intensityValue)
        add(intensityCard, intensityRow, 7)
        add(intensityCard, SeekBar(this).apply {
            max = 100; progress = prefs.getInt("intensity", 45).coerceIn(0, 100)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        intensityValue.text = "$value%"
                        prefs.edit().putInt("intensity", value).apply()
                        updateOverlay()
                    }
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        })
        add(root, intensityCard, 14)
        val transparency = row().apply {
            background = shape(surface); setPadding(dp(18), dp(17), dp(18), dp(17))
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        }
        val transparencyLabels = column()
        add(transparencyLabels, label("Transparência do Menu", 17f, Color.WHITE, true), 3)
        add(transparencyLabels, label("Ajustar nas configurações", 13f, muted))
        transparency.addView(transparencyLabels, LinearLayout.LayoutParams(0, -2, 1f))
        transparency.addView(label("${prefs.getInt("menu_transparency", 35)}%  ›", 18f, cyan, true))
        add(root, transparency)
        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        })
    }
}
