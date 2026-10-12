package com.divso.hub

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object ScriptLoader {

    private const val BASE =
        "https://raw.githubusercontent.com/divso123/script/main/games/"

    // loader.lua PlaceId map — add entries as the repo grows
    private val GAMES = mapOf(
        112274915308120L to Game("Test",        "test.lua"),
        2753915549L      to Game("Blox Fruits", "bloxfruits.lua"),
        4924922222L      to Game("Brookhaven",  "brookhaven.lua"),
    )

    data class Game(val name: String, val file: String)

    fun allGames(): List<Pair<Long, Game>> = GAMES.entries.map { it.key to it.value }

    /**
     * Fetch script source from raw GitHub.
     * Returns the Lua source string or throws on network error.
     */
    suspend fun fetchScript(file: String): String = withContext(Dispatchers.IO) {
        URL("$BASE$file").readText()
    }

    /**
     * Fetch the loader itself — lets users run the full hub loader in Delta.
     */
    suspend fun fetchLoader(): String = withContext(Dispatchers.IO) {
        URL("https://raw.githubusercontent.com/divso123/script/main/loader.lua").readText()
    }

    /**
     * Push script to Delta executor:
     *  1. Copy script to clipboard
     *  2. Fire Delta's inject intent
     *
     * Delta on Android reads clipboard when launched via intent.
     * Package: com.roblox.delta (common sideload name)
     */
    fun sendToDelta(context: Context, script: String) {
        // 1 — clipboard
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("script", script))

        // 2 — try Delta intent (auto-injects if Delta is open in Roblox)
        val intent = Intent().apply {
            setClassName("com.roblox.delta", "com.roblox.delta.MainActivity")
            action = "com.roblox.delta.INJECT"
            putExtra("script", script)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
        // if Delta isn't installed or intent fails, script is still on clipboard
        // user can paste into Delta manually — fallback always works
    }
}
