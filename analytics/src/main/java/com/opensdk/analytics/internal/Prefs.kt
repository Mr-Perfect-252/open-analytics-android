package com.opensdk.analytics.internal

import android.content.Context
import android.content.SharedPreferences

/** Thin SharedPreferences wrapper scoped by the configured storage prefix. */
internal class Prefs(context: Context, storagePrefix: String) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(storagePrefix, Context.MODE_PRIVATE)

    fun getString(key: String): String? = sp.getString(key, null)
    fun putString(key: String, value: String) = sp.edit().putString(key, value).apply()
    fun getLong(key: String, def: Long): Long = sp.getLong(key, def)
    fun putLong(key: String, value: Long) = sp.edit().putLong(key, value).apply()
    fun remove(key: String) = sp.edit().remove(key).apply()
    fun contains(key: String): Boolean = sp.contains(key)
}
