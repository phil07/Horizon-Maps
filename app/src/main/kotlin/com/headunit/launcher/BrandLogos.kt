package com.headunit.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Logos for shops and brands in search results and on the map. The website comes from the search result, or from a built-in list
 * of common chains; the site's own icon is loaded the first time it is needed and kept on the unit, so after that it shows with no
 * signal. Without one (or offline before the first time) the usual pin is shown.
 */
object BrandLogos {
    /** Common chains and their websites (the logo itself is the site's icon, loaded once). Keys are [key] of the name. */
    private val known = mapOf(
        "lidl" to "lidl.de", "aldi" to "aldi-sued.de", "aldi süd" to "aldi-sued.de", "aldi sud" to "aldi-sued.de", "aldi nord" to "aldi-nord.de",
        "rewe" to "rewe.de", "edeka" to "edeka.de", "netto" to "netto-online.de", "penny" to "penny.de", "kaufland" to "kaufland.de",
        "norma" to "norma-online.de", "globus" to "globus.de", "real" to "real.de", "tegut" to "tegut.com", "nahkauf" to "nahkauf.de",
        "dm" to "dm.de", "rossmann" to "rossmann.de", "müller" to "mueller.de", "budni" to "budni.de",
        "aral" to "aral.de", "shell" to "shell.de", "esso" to "esso.de", "total" to "totalenergies.de", "totalenergies" to "totalenergies.de",
        "jet" to "jet-tankstellen.de", "avia" to "avia.de", "agip" to "eni.com", "eni" to "eni.com", "omv" to "omv.com", "star" to "star.de",
        "hem" to "hem-tankstelle.de", "orlen" to "orlen.de", "bp" to "bp.com",
        "mcdonald's" to "mcdonalds.com", "mcdonalds" to "mcdonalds.com", "burger king" to "burgerking.de", "kfc" to "kfc.de", "subway" to "subway.com",
        "starbucks" to "starbucks.com", "costa" to "costa.co.uk", "costa coffee" to "costa.co.uk", "tim hortons" to "timhortons.com",
        "nordsee" to "nordsee.com", "vapiano" to "vapiano.com", "pizza hut" to "pizzahut.com", "domino's" to "dominos.de", "dominos" to "dominos.de",
        "ikea" to "ikea.com", "obi" to "obi.de", "bauhaus" to "bauhaus.info", "hornbach" to "hornbach.de", "toom" to "toom.de", "hagebau" to "hagebau.de",
        "media markt" to "mediamarkt.de", "mediamarkt" to "mediamarkt.de", "saturn" to "saturn.de", "decathlon" to "decathlon.de", "h&m" to "hm.com",
        "deichmann" to "deichmann.com", "tedi" to "tedi.com", "action" to "action.com", "fressnapf" to "fressnapf.de", "apollo" to "apollo.de",
        "tesla supercharger" to "tesla.com", "tesla" to "tesla.com", "ionity" to "ionity.eu", "enbw" to "enbw.com", "fastned" to "fastned.nl", "allego" to "allego.eu",
        "sainsbury's" to "sainsburys.co.uk", "sainsburys" to "sainsburys.co.uk", "tesco" to "tesco.com", "asda" to "asda.com", "morrisons" to "morrisons.com",
        "waitrose" to "waitrose.com", "co-op" to "coop.co.uk", "greggs" to "greggs.co.uk", "pret a manger" to "pret.co.uk", "boots" to "boots.com",
        "spar" to "spar.com", "carrefour" to "carrefour.com", "intermarché" to "intermarche.com", "leclerc" to "e.leclerc", "auchan" to "auchan.fr",
        "albert heijn" to "ah.nl", "jumbo" to "jumbo.com", "migros" to "migros.ch", "coop" to "coop.ch", "billa" to "billa.at", "hofer" to "hofer.at",
    )

    /** A chain's name in a form to compare by: "LIDL", "Lidl - Hauptstraße" and "Lidl (Filiale)" all give "lidl". */
    fun key(name: String): String = name.lowercase().split(" - ", " – ", ",", " (", "|").first().trim()
        .replace(Regex("\\s+(filiale|markt|supermarkt|tankstelle|restaurant|store|express|city)$"), "").trim()

    /** Whether a result called [name] (of brand [brand], if the search said) is a branch of the chain with the key [k]. */
    fun sameChain(name: String, brand: String?, k: String): Boolean {
        if (k.length < 2) return false
        val n = key(name)
        return key(brand ?: name) == k || n == k || n.startsWith("$k ")
    }

    /** The website for a place's logo: the result's own, else the chain's from the list, else null. */
    fun domainFor(p: Place): String? {
        p.web?.let { w -> host(w)?.let { return it } }
        known[key(p.brand ?: p.name)]?.let { return it }
        return known[key(p.name)]
    }

    private fun host(url: String): String? = try {
        val u = if (url.contains("://")) url else "https://$url"
        URL(u).host.removePrefix("www.").lowercase().takeIf { it.contains('.') }
    } catch (_: Exception) { null }

    private val mem = HashMap<String, Bitmap?>()
    private val failed = HashSet<String>()

    private fun file(ctx: Context, domain: String) = File(File(ctx.filesDir, "logos").apply { mkdirs() }, domain.replace(Regex("[^a-z0-9.-]"), "_") + ".png")

    /** The logo if it is already on the unit (no network). */
    fun cached(ctx: Context, domain: String): Bitmap? = synchronized(mem) {
        if (mem.containsKey(domain)) return mem[domain]
        val f = file(ctx, domain)
        val b = if (f.exists() && f.length() > 0) try { BitmapFactory.decodeFile(f.path) } catch (_: Exception) { null } else null
        if (b != null) mem[domain] = b
        b
    }

    /** The logo, loaded from the site's icon the first time and kept. Null without one. Not on the main thread. */
    fun load(ctx: Context, domain: String): Bitmap? {
        cached(ctx, domain)?.let { return it }
        synchronized(mem) { if (domain in failed) return null }
        // the site's own large icon first (sharp on a car screen), then the small icons from Google's and DuckDuckGo's services
        val b = fetch("https://www.$domain/apple-touch-icon.png")?.takeIf { it.width >= 96 }
            ?: fetch("https://$domain/apple-touch-icon.png")?.takeIf { it.width >= 96 }
            ?: fetch("https://www.google.com/s2/favicons?domain=$domain&sz=256")?.takeIf { it.width >= 32 }
            ?: fetch("https://icons.duckduckgo.com/ip3/$domain.ico")
        if (b == null) { synchronized(mem) { failed += domain }; return null }
        synchronized(mem) { mem[domain] = b }
        try { file(ctx, domain).outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } } catch (_: Exception) { }
        return b
    }

    private fun fetch(url: String): Bitmap? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 6000; c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "HorizonNav/1.0")
        try {
            if (c.responseCode !in 200..299) null
            else c.inputStream.use { s -> val bytes = s.readBytes(); if (bytes.size > 400_000) null else BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
        } finally { c.disconnect() }
    } catch (_: Exception) { null }
}
