package com.headunit.launcher

import androidx.compose.ui.graphics.Color

/**
 * The colour of the road signs in the country you're in, for the turn card's "Match road signs" colour: the motorway colour on a
 * motorway, the main-road colour elsewhere. These are the usual sign colours, from general knowledge: a guide, not a standard list.
 */
internal object RoadSigns {
    private val blue = Color(0xFF0E4D92)
    private val green = Color(0xFF00703C)
    private val blueUk = Color(0xFF1D4F91)

    // country -> (motorway colour, main-road colour)
    private val table: Map<String, Pair<Color, Color>> = buildMap {
        put("GB", blueUk to green)
        for (cc in listOf("SE", "NO", "DK", "FI", "IS")) put(cc, green to blue)
        for (cc in listOf("IT", "AT", "CH", "CZ", "SK", "SI", "HU", "HR", "RO", "BG")) put(cc, green to blue)
        for (cc in listOf("DE", "FR", "ES", "NL", "BE", "PT", "IE", "PL", "GR", "LU")) put(cc, blue to green)
    }

    private var memoPos: LL? = null
    private var memoCc: String? = null

    /** The country at [p]; looked up again only after moving about 2 km, as the outlines take a moment to search. */
    @Synchronized
    private fun countryNear(ctx: android.content.Context, p: LL): String? {
        val m = memoPos
        if (m != null && Geo.dist(m, p) < 2_000.0) return memoCc
        memoPos = p
        memoCc = CountryBorders.at(ctx, p)
        return memoCc
    }

    /** The sign colour for the road you're on, as the navigation sees it. The default is a blue motorway and a green main road. */
    fun colour(engine: NavEngine): Color {
        val p = engine.fix?.pos ?: return blue
        val cc = countryNear(engine.ctx, p)
        val pair = table[cc]
        return if (engine.onMotorway) pair?.first ?: blue else pair?.second ?: green
    }

    /** A darker shade of [c] for the card's bars (the same rule as the built-in colours). */
    fun darker(c: Color): Color = Color(c.red * 0.7f, c.green * 0.7f, c.blue * 0.7f, c.alpha)
}
