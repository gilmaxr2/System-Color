package com.systemcolor.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*

class SettingsActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("colors", MODE_PRIVATE) }
    private fun dp(x: Int) = (x * resources.displayMetrics.density + .5f).toInt()
    private fun text(s: String, size: Float = 16f, color: Int = Color.WHITE) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); setPadding(dp(4), dp(7), dp(4), dp(7))
    }
    private fun add(p: LinearLayout, v: View) { p.addView(v, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) }) }
    private fun section(p: LinearLayout, title: String, fn: (LinearLayout) -> Unit) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply { setColor(Color.rgb(20, 31, 48)); cornerRadius = dp(15).toFloat(); setStroke(dp(1), Color.rgb(35, 200, 172)) }
        }
        add(card, text(title, 21f, Color.rgb(35, 220, 180))); fn(card); add(p, card)
    }
    private fun notifyService() {
        if (Settings.canDrawOverlays(this)) startService(Intent(this, OverlayService::class.java).setAction("com.systemcolor.app.UPDATE"))
    }
    private fun toggle(p: LinearLayout, title: String, key: String, initial: Boolean) {
        add(p, Switch(this).apply {
            text = title; textSize = 15f; setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean(key, initial)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean(key, checked).apply(); notifyService() }
        })
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(18), dp(16), dp(22)); setBackgroundColor(Color.rgb(9, 11, 23)) }
        add(root, text("SYSTEM COLOR • CONFIGURAÇÕES", 23f, Color.rgb(35, 220, 180)))
        section(root, "Aparência") { card ->
            add(card, text("Tema da interface"))
            val themes = arrayOf("Blue Emerald", "Pure Darkness", "Cyberpunk", "Toxic Green", "Red Dragon", "Ice Blue", "Sakura Pink")
            val spinner = Spinner(this)
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themes)
            spinner.setSelection(themes.indexOf(prefs.getString("ui_theme", "Blue Emerald")).coerceAtLeast(0))
            spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    prefs.edit().putString("ui_theme", themes[position]).apply()
                }
            }
            add(card, spinner)
            add(card, text("O tema é salvo; a aplicação visual em todas as telas será concluída em etapa posterior.", 12f, Color.LTGRAY))
        }
        section(root, "Menu flutuante") { card ->
            toggle(card, "Exibir botão flutuante", "bubble_enabled", false)
            val label = text("Transparência: ${prefs.getInt("menu_transparency", 35)}%")
            add(card, label)
            add(card, SeekBar(this).apply {
                max = 90; progress = prefs.getInt("menu_transparency", 35).coerceIn(0, 90)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                        if (fromUser) { label.text = "Transparência: $value%"; prefs.edit().putInt("menu_transparency", value).apply(); notifyService() }
                    }
                    override fun onStartTrackingTouch(bar: SeekBar?) {}
                    override fun onStopTrackingTouch(bar: SeekBar?) {}
                })
            })
            add(card, text("Padrão 35%. Ajuste salvo automaticamente.", 12f, Color.LTGRAY))
        }
        section(root, "Preferências") { card ->
            toggle(card, "YouTube automático", "youtube_auto", true)
            toggle(card, "Netflix automático (em desenvolvimento)", "netflix_auto", true)
            toggle(card, "Restaurar filtro anterior", "restore_previous", true)
        }
        section(root, "Tutorial") { card ->
            add(card, text("1. Autorize a sobreposição nas configurações do Android."))
            add(card, text("2. Ative o filtro global na tela principal."))
            add(card, text("3. Selecione uma tonalidade e ajuste a intensidade."))
            add(card, text("4. Habilite o menu flutuante nesta tela quando desejar."))
            add(card, text("5. Apps protegidos podem bloquear a sobreposição.", 13f, Color.LTGRAY))
        }
        section(root, "Desenvolvedor") { card ->
            add(card, text("Gilmar Ribeiro", 19f))
            add(card, text("System Color • 6.0 Beta (em desenvolvimento)"))
            add(card, text("Android • Kotlin"))
            add(card, text("Os filtros de tonalidade não aumentam resolução, nitidez ou HDR reais.", 13f, Color.LTGRAY))
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
