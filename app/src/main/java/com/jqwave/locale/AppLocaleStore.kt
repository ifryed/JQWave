package com.jqwave.locale

import android.content.Context
import java.util.Locale

/**
 * Persists UI language so [MainActivity.attachBaseContext] can apply it with the forced light theme.
 */
object AppLocaleStore {

    private const val PREFS_NAME = "jqwave_locale"
    private const val KEY_LANGUAGE = "language"
    const val LANG_EN = "en"
    const val LANG_IW = "iw"

    fun getTag(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, LANG_EN) ?: LANG_EN

    fun setTag(context: Context, tag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, tag)
            .apply()
    }

    fun localeForTag(tag: String): Locale = when (tag) {
        LANG_IW, "he" -> Locale.forLanguageTag("iw")
        else -> Locale.forLanguageTag("en")
    }
}
