package com.headunit.launcher.ui

import android.content.Context
import android.provider.Settings
import com.headunit.launcher.PreferenceKeys
import com.headunit.launcher.Prefs

/**
 * Headlights on Teyes and FYT head units: these units pass the lights on in one of their own system settings instead of switching
 * Android's night mode. The setting's name isn't known in advance, so the app learns it: a system setting whose name suggests lights
 * and whose value flips between two states is taken as the lights signal (the first one seen is kept). "Lights signal is reversed"
 * flips the reading for units that have it the other way round.
 */
object VendorLight {
    private val words = listOf("light", "illum", "lamp")
    private val onValues = setOf("1", "true", "on")
    private val offValues = setOf("0", "false", "off")
    private val previous = HashMap<String, String>()

    /** Name → value of the light-like settings in the system and global tables, read in one pass. */
    private fun snapshot(ctx: Context): Map<String, String> {
        val out = HashMap<String, String>()
        for (uri in listOf(Settings.System.CONTENT_URI, Settings.Global.CONTENT_URI)) {
            try {
                ctx.contentResolver.query(uri, arrayOf("name", "value"), null, null, null)?.use { c ->
                    val ni = c.getColumnIndex("name"); val vi = c.getColumnIndex("value")
                    if (ni < 0 || vi < 0) return@use
                    while (c.moveToNext()) {
                        val name = c.getString(ni) ?: continue
                        val value = c.getString(vi) ?: continue
                        val low = name.lowercase()
                        if (words.any { low.contains(it) }) out[name] = value.trim().lowercase()
                    }
                }
            } catch (_: Throwable) { }
        }
        return out
    }

    /**
     * Reads the settings, learns the lights setting if it's still unknown, and returns whether the lights are on (or null while no
     * lights setting has been found). Called every few seconds from the app and cheap enough for that.
     */
    fun poll(ctx: Context, prefs: Prefs): Boolean? {
        val now = snapshot(ctx)
        if (prefs.s(PreferenceKeys.VENDORLIGHT, "").isEmpty() && previous.isNotEmpty()) {
            for ((name, value) in now) {
                val old = previous[name] ?: continue
                if (old != value && (old in onValues || old in offValues) && (value in onValues || value in offValues)) {
                    prefs.put(PreferenceKeys.VENDORLIGHT, name)
                    break
                }
            }
        }
        previous.clear(); previous.putAll(now)
        return reading(prefs, now)
    }

    /** Whether the lights are on right now, from the learned setting; null when no setting has been learned yet. */
    fun current(ctx: Context, prefs: Prefs): Boolean? {
        if (prefs.s(PreferenceKeys.VENDORLIGHT, "").isEmpty()) return null
        return reading(prefs, snapshot(ctx))
    }

    private fun reading(prefs: Prefs, values: Map<String, String>): Boolean? {
        val key = prefs.s(PreferenceKeys.VENDORLIGHT, "")
        if (key.isEmpty()) return null
        val v = values[key] ?: return null
        val on = when (v) { in onValues -> true; in offValues -> false; else -> return null }
        return on != prefs.b(PreferenceKeys.LIGHTSREVERSED, false)
    }

    /** The learned setting's name, for the settings screen. */
    fun learned(prefs: Prefs): String = prefs.s(PreferenceKeys.VENDORLIGHT, "")
}
