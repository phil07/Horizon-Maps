package com.headunit.launcher

/** Pure decision for which Mapbox base style should be requested for the current map settings. */
internal enum class BaseMapStyle {
    DARK_STREETS,
    STREETS,
    STANDARD,
}

internal object MapStylePolicy {
    /** Full detail is the only mode that enables the heavier optional map layers. */
    fun supportsFullDetailFeatures(mapDetail: String): Boolean = mapDetail == "full"

    /** Satellite takes precedence: the classic streets style with Mapbox's photos drawn under it (MapBinding.addSatellite). Lightest mode uses the streets style, darkened at night. */
    fun choose(satellite: Boolean, mapDetail: String, nightStyle: Boolean): BaseMapStyle = when {
        satellite -> BaseMapStyle.STREETS
        mapDetail == "lightest" && nightStyle -> BaseMapStyle.DARK_STREETS
        mapDetail == "lightest" -> BaseMapStyle.STREETS
        else -> BaseMapStyle.STANDARD
    }
}
