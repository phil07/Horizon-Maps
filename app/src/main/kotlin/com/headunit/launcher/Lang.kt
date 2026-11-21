package com.headunit.launcher

import java.util.Locale

/** For texts written in code rather than in strings.xml: German on German devices, English on all others
 *  (the same rule Android uses to pick values-de/ or values/). */
object Lang {
    val de: Boolean get() = Locale.getDefault().language == "de"
    fun t(de: String, en: String): String = if (this.de) de else en
    /** A text with placeholders (%1$s, %2$d…): the German or English text, filled in with [a]. */
    fun f(de: String, en: String, vararg a: Any): String = String.format(if (this.de) de else en, *a)
    /** The language tag the map and place services take for the app's language (German or English). */
    val serviceTag: String get() = if (de) "de-DE" else "en-GB"
}

/** A text kept in both languages: shown in the app's language ([s]), found by search in either ([de] and [en]). */
class Bilingual(val de: String, val en: String) {
    val s: String get() = Lang.t(de, en)
}
