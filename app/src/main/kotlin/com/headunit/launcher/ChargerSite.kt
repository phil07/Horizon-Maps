package com.headunit.launcher

/**
 * A charge point (or site) for the map: its position, its power in kW when known, its number of chargers and how many are free.
 * [kwLabel] is the text shown for the power when it isn't just one number (e.g. "2 × 22 kW"), [op] the operator (for the route name).
 */
class ChargerSite(val pos: LL, val kw: Int?, val total: Int?, val free: Int?, val op: String? = null, val kwLabel: String? = null)
