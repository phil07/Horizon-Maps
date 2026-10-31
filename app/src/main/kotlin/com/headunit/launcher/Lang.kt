package com.headunit.launcher

import java.util.Locale

/** For texts written in code rather than in strings.xml: German like the app's default strings, English on English devices
 *  (the same rule Android uses to pick values/ or values-en/). */
object Lang {
    val de: Boolean get() = Locale.getDefault().language != "en"
    fun t(de: String, en: String): String = if (this.de) de else en
}
