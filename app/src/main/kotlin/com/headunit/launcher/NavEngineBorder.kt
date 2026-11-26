package com.headunit.launcher

import com.headunit.launcher.NavEngine.Notice

/** Which route and countries the border notice has been given for (once per country and route). */
internal object BorderNotes {
    var route: Route? = null
    val said = HashSet<String>()
    var at = 0L
}

/**
 * Before the border: when the route enters another country within 20 km, the country is said and shown with its usual limits, the
 * emergency number, the currency and the languages (a guide only). Looked at every 10 seconds while driving; never throws.
 */
internal fun NavEngine.checkBorder(r: Route, progress: Double) {
    if (previewing || simMode != 0) return
    val now = System.currentTimeMillis()
    if (BorderNotes.route !== r) { BorderNotes.route = r; BorderNotes.said.clear(); BorderNotes.at = 0L }
    if (now - BorderNotes.at < 10_000L) return
    BorderNotes.at = now
    val here = CountryBorders.at(ctx, pointAt(r, progress).first)
    val end = minOf(r.len, progress + 20_000.0)
    var off = progress + 500.0
    while (off <= end) {
        val c = CountryBorders.at(ctx, pointAt(r, off).first)
        if (c != null && c != here) {
            // the first other country ahead is the one the route enters: not in the table, nothing is said
            val inf = CountryBorders.info[c] ?: return
            if (!BorderNotes.said.add(c)) return
            val speeds = "${inf.motorway} / ${inf.rural} / ${inf.urban} " + if (inf.mph) "mph" else "km/h"
            val text = Lang.t("Richtwert: Tempo $speeds · Notruf ${inf.emergency} · ${inf.currency} · ${inf.language}",
                "Guide: speed $speeds · emergency ${inf.emergency} · ${inf.currency} · ${inf.language}")
            speak(Lang.t("Gleich ${inf.de}. Richtwert Tempo $speeds, Notruf ${inf.emergency}.", "Next, ${inf.en}. Guide speed $speeds, emergency ${inf.emergency}."), true)
            showNotice(Notice("border", Lang.t("Willkommen in ${inf.de}", "Welcome to ${inf.en}"), text, 2), 15_000L)
            return
        }
        off += 500.0
    }
}
