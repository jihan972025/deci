package com.deci.util

import android.content.Context
import android.graphics.Color

object AccentColor {

    val colorMap = mapOf(
        "lime" to "#CCFF00",
        "magenta" to "#FF00FF",
        "cyan" to "#00FFFF",
        "orange" to "#FF5F1F",
        "hotPink" to "#FF1493",
        "springGreen" to "#39FF14"
    )

    private const val PREFS_NAME = "app_settings"
    private const val KEY_COLOR = "color"
    private const val DEFAULT_COLOR = "lime"

    fun getHex(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_COLOR, DEFAULT_COLOR) ?: DEFAULT_COLOR
        return colorMap[name] ?: colorMap[DEFAULT_COLOR]!!
    }

    fun get(context: Context): Int = Color.parseColor(getHex(context))
}
