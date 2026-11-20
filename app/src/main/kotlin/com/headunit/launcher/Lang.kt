package com.headunit.launcher

import java.util.Locale

/** For texts written in code rather than in strings.xml: German on German devices, English on all others
 *  (the same rule Android uses to pick values-de/ or values/). */
object Lang {
    val de: Boolean get() = Locale.getDefault().language == "de"
    fun t(de: String, en: String): String = if (this.de) de else en
    /** The language tag the map and place services take for the app's language (German or English). */
    val serviceTag: String get() = if (de) "de-DE" else "en-GB"
}
