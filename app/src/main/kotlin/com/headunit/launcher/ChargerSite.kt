package com.headunit.launcher

/** A charge point (or site) for the map: its position, its power in kW when known, its number of chargers and how many are free. */
class ChargerSite(val pos: LL, val kw: Int?, val total: Int?, val free: Int?)
