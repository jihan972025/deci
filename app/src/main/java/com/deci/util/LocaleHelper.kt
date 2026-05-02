package com.deci.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    private const val PREFS_NAME = "app_settings"
    private const val KEY_LANGUAGE = "language"
    private const val DEFAULT_LANGUAGE = "English"

    private val nameToCode = mapOf(
        "English" to "en",
        "한국어" to "ko",
        "日本語" to "ja",
        "中文" to "zh",
        "Español" to "es",
        "Français" to "fr",
        "Deutsch" to "de",
        "Português" to "pt",
        "العربية" to "ar",
        "Русский" to "ru"
    )

    fun getLanguageName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun getCode(context: Context): String {
        return nameToCode[getLanguageName(context)] ?: "en"
    }

    fun wrap(context: Context): Context {
        val locale = Locale(getCode(context))
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
