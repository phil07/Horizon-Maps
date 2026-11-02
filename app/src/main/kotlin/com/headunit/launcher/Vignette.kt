package com.headunit.launcher

/**
 * Vignettes and special tolls: the countries where using the motorways needs a vignette (a sticker or a digital one bought in
 * advance) or, in Bulgaria and Romania, any main road outside towns. Which country a point lies in comes from simplified borders
 * (Natural Earth, public domain, about 1 km accurate), whether a stretch is a motorway from its road number or name, or from a
 * motorway-like speed limit.
 */
object Vignette {
    /** [cc] needs a vignette from [off] metres along the route; [special] names the stretches with their own toll (Brenner …). */
    data class Need(val cc: String, val off: Double, val special: List<String> = emptyList())

    /** The countries, each as an outline of lat, lon pairs (from vignette_borders.txt next to this class, read on first use). */
    private val BORDERS: List<Pair<String, DoubleArray>> by lazy {
        val text = try { Vignette::class.java.getResourceAsStream("vignette_borders.txt")?.bufferedReader()?.use { it.readText() } } catch (e: Exception) { null } ?: ""
        text.lines().filter { it.length > 3 && !it.startsWith("#") }.map { line ->
            val parts = line.trim().split(' ')
            val a = DoubleArray((parts.size - 1) * 2)
            for (k in 1 until parts.size) { val ll = parts[k].split(','); a[2 * (k - 1)] = ll[0].toDouble(); a[2 * (k - 1) + 1] = ll[1].toDouble() }
            parts[0] to a
        }
    }
    private val BOXES: List<DoubleArray> by lazy {
        BORDERS.map { (_, a) ->
            var s = 90.0; var n = -90.0; var w = 180.0; var e = -180.0
            var i = 0
            while (i < a.size) { s = minOf(s, a[i]); n = maxOf(n, a[i]); w = minOf(w, a[i + 1]); e = maxOf(e, a[i + 1]); i += 2 }
            doubleArrayOf(s, w, n, e)
        }
    }

    /** The borders are about half a kilometre accurate: a point only counts this far inside, so a road along the border doesn't. */
    private const val INSIDE_M = 1500.0

    val COUNTRIES = setOf("AT", "CH", "SI", "CZ", "SK", "HU", "BG", "RO")

    /** The vignette country [p] lies in, or null (anywhere else). [margin]: only when at least that many metres inside. */
    fun countryOf(p: LL, margin: Double = 0.0): String? {
        for (k in BORDERS.indices) {
            val b = BOXES[k]
            if (p.lat < b[0] || p.lat > b[2] || p.lon < b[1] || p.lon > b[3]) continue
            val a = BORDERS[k].second
            if (inside(a, p.lat, p.lon)) return if (margin <= 0.0 || edgeDist(a, p) >= margin) BORDERS[k].first else null
        }
        return null
    }

    /** Metres from [p] to the nearest point of the outline [a]. */
    private fun edgeDist(a: DoubleArray, p: LL): Double {
        val kx = 111_320.0 * Math.cos(Math.toRadians(p.lat)); val ky = 110_574.0
        var best = Double.MAX_VALUE
        val n = a.size / 2
        for (i in 0 until n) {
            val j = (i + 1) % n
            val ax = (a[2 * i + 1] - p.lon) * kx; val ay = (a[2 * i] - p.lat) * ky
            val bx = (a[2 * j + 1] - p.lon) * kx; val by = (a[2 * j] - p.lat) * ky
            val dx = bx - ax; val dy = by - ay
            val l2 = dx * dx + dy * dy
            val t = if (l2 > 0) (-(ax * dx + ay * dy) / l2).coerceIn(0.0, 1.0) else 0.0
            val d = Math.hypot(ax + dx * t, ay + dy * t)
            if (d < best) best = d
        }
        return best
    }

    private fun inside(a: DoubleArray, lat: Double, lon: Double): Boolean {
        var c = false
        val n = a.size / 2
        var j = n - 1
        for (i in 0 until n) {
            val yi = a[2 * i]; val xi = a[2 * i + 1]; val yj = a[2 * j]; val xj = a[2 * j + 1]
            if ((yi > lat) != (yj > lat) && lon < (xj - xi) * (lat - yi) / (yj - yi) + xi) c = !c
            j = i
        }
        return c
    }

    private val MOTORWAY_REF = mapOf(
        "AT" to Regex("\\b[AS]\\s?\\d{1,2}\\b"), "CH" to Regex("\\b[AN]\\s?\\d{1,2}\\b"), "SI" to Regex("\\b[AH]\\s?\\d{1,2}\\b"),
        "CZ" to Regex("\\bD\\s?\\d{1,2}\\b"), "SK" to Regex("\\b[DR]\\s?\\d{1,2}\\b"), "HU" to Regex("\\bM\\s?\\d{1,2}\\b"),
    )
    private val MOTORWAY_WORDS = listOf("autobahn", "schnellstraße", "schnellstrasse", "autoroute", "autostrada", "avtocesta", "hitra cesta",
        "dálnice", "dalnice", "diaľnica", "dialnica", "rýchlostná", "autópálya", "autopalya", "autóút")

    /** Whether a road with this name or number is a vignette road in [cc] ([kmh]: its limit, -1 unknown). Bulgaria and Romania: all roads out of town. */
    fun vignetteRoad(cc: String, street: String, kmh: Int): Boolean {
        if (cc == "BG" || cc == "RO") return true
        val s = street.lowercase()
        if (MOTORWAY_WORDS.any { s.contains(it) }) return true
        if (MOTORWAY_REF[cc]?.containsMatchIn(street) == true) return true
        // no name to go by: a motorway-like limit
        return kmh >= 115
    }

    /** Austria's stretches with their own toll on top of the vignette, by road number. */
    private val SPECIAL_AT = listOf(
        Regex("\\bA\\s?13\\b") to "A 13 Brenner", Regex("\\bA\\s?10\\b") to "A 10 Tauern", Regex("\\bA\\s?9\\b") to "A 9 Pyhrn (Bosruck, Gleinalm)",
        Regex("\\bA\\s?11\\b") to "A 11 Karawanken", Regex("\\bS\\s?16\\b") to "S 16 Arlberg",
    )
    private val SPECIAL_CH = listOf(Regex("gro(ß|ss)er? st\\.? ?bernhard", RegexOption.IGNORE_CASE) to "Großer St. Bernhard")

    /**
     * The vignette countries on [r], each with where its first vignette road starts. [street] gives the road at a distance along the
     * route, [kmh] its limit (-1 unknown). Short dips under 2 km (a border crossing on a small road) don't count.
     */
    fun along(r: Route, street: (Double) -> String = { off -> streetAt(r, off) }, kmh: (Double) -> Int = { off -> limitAt(r, off) }): List<Need> {
        if (r.pts.size < 2) return emptyList()
        val first = HashMap<String, Double>()
        val run = HashMap<String, Double>()
        val special = LinkedHashMap<String, LinkedHashSet<String>>()
        var off = 0.0
        val step = 400.0
        while (off <= r.len) {
            val p = pointAt(r, off)
            val cc = countryOf(p, INSIDE_M)
            if (cc != null) {
                val st = street(off)
                if (vignetteRoad(cc, st, kmh(off))) {
                    val start = run.getOrPut(cc) { off }
                    if (off - start >= 2000.0 && cc !in first) first[cc] = start
                    val sp = if (cc == "AT") SPECIAL_AT else if (cc == "CH") SPECIAL_CH else emptyList()
                    for ((re, name) in sp) if (re.containsMatchIn(st)) special.getOrPut(cc) { LinkedHashSet() } += name
                } else run.remove(cc)
            } else run.clear()
            off += step
        }
        return first.entries.sortedBy { it.value }.map { Need(it.key, it.value, special[it.key]?.toList().orEmpty()) }
    }

    fun streetAt(r: Route, off: Double): String = r.steps.lastOrNull { it.off <= off + 1.0 }?.street.orEmpty()
    fun limitAt(r: Route, off: Double): Int = r.limits.firstOrNull { off >= it.off && off < it.end }?.let { Math.round(it.ms * 3.6).toInt() } ?: -1

    private fun pointAt(r: Route, off: Double): LL {
        var lo = 0; var hi = r.cum.size - 1
        while (lo < hi) { val m = (lo + hi) / 2; if (r.cum[m] < off) lo = m + 1 else hi = m }
        return r.pts[lo.coerceIn(0, r.pts.size - 1)]
    }

    fun name(cc: String): String = when (cc) {
        "AT" -> Lang.t("Österreich", "Austria"); "CH" -> Lang.t("Schweiz", "Switzerland"); "SI" -> Lang.t("Slowenien", "Slovenia")
        "CZ" -> Lang.t("Tschechien", "Czechia"); "SK" -> Lang.t("Slowakei", "Slovakia"); "HU" -> Lang.t("Ungarn", "Hungary")
        "BG" -> Lang.t("Bulgarien", "Bulgaria"); "RO" -> Lang.t("Rumänien", "Romania"); else -> cc
    }

    /** One line for the route overview: "Vignette nötig: Österreich, Schweiz · Streckenmaut: A 13 Brenner". */
    fun summary(needs: List<Need>): String {
        val sp = needs.flatMap { it.special }
        return Lang.t("Vignette nötig: ", "Vignette needed: ") + needs.joinToString(", ") { name(it.cc) } +
            (if (sp.isNotEmpty()) Lang.t(" · Streckenmaut: ", " · Section toll: ") + sp.joinToString(", ") else "")
    }

    /** What's said before the border. */
    fun spoken(n: Need): String {
        val c = name(n.cc)
        val base = if (n.cc == "BG" || n.cc == "RO") Lang.t("Achtung: In $c gilt auf fast allen Straßen außerorts Vignettenpflicht.", "Note: in $c a vignette is needed on almost all roads outside towns.")
            else Lang.t("Achtung: In $c gilt auf Autobahnen Vignettenpflicht.", "Note: motorways in $c need a vignette.")
        val sp = if (n.special.isNotEmpty()) Lang.t(" Auf der ${n.special.first()} kommt eine Streckenmaut dazu.", " The ${n.special.first()} has a section toll on top.") else ""
        val at = if (n.cc == "AT") Lang.t(" Eine online gekaufte digitale Vignette gilt erst nach 18 Tagen, an der Tankstelle gekauft sofort.", " A digital vignette bought online is only valid after 18 days; one bought at a petrol station is valid at once.") else ""
        return base + sp + at
    }
}
