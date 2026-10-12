package com.divso.hub

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var gameList: LinearLayout
    private lateinit var loaderBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText  = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        gameList    = findViewById(R.id.gameList)
        loaderBtn   = findViewById(R.id.loaderBtn)

        buildGameButtons()

        // full loader.lua — runs all games via PlaceId detection
        loaderBtn.setOnClickListener {
            inject("loader") { ScriptLoader.fetchLoader() }
        }
    }

    private fun buildGameButtons() {
        ScriptLoader.allGames().forEach { (placeId, game) ->
            val btn = Button(this).apply {
                text = game.name
                setBackgroundResource(R.drawable.btn_bg)
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 15f
                val dp12 = (12 * resources.displayMetrics.density).toInt()
                val dp8  = (8  * resources.displayMetrics.density).toInt()
                setPadding(dp12, dp8, dp12, dp8)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                lp.bottomMargin = dp8
                layoutParams = lp

                setOnClickListener {
                    inject(game.name) { ScriptLoader.fetchScript(game.file) }
                }
            }
            gameList.addView(btn)
        }
    }

    private fun inject(label: String, fetch: suspend () -> String) {
        lifecycleScope.launch {
            setLoading(true, "Fetching $label...")
            runCatching { fetch() }
                .onSuccess { script ->
                    ScriptLoader.sendToDelta(this@MainActivity, script)
                    setLoading(false, "✓ $label sent to Delta")
                }
                .onFailure { err ->
                    setLoading(false, "✗ Failed: ${err.message}")
                }
        }
    }

    private fun setLoading(loading: Boolean, status: String) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        statusText.text = status
    }
}
