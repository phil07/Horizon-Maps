package com.headunit.launcher

/** Where the satellite photos come from. */
internal enum class SatProvider { MAPBOX, ESRI, MAPTILER, EOX }

/** One German state's free aerial photos (Digitale Orthophotos, about 20 cm per pixel) as a WMS, with the rough area it covers. */
internal data class StateWms(
    val code: String,
    val url: String,
    val layers: String,
    val west: Double, val south: Double, val east: Double, val north: Double,
    val credit: String,
)

/**
 * The satellite photo sources and the order they're tried in: Mapbox first; when Mapbox refuses (429 or a bad token) the
 * state surveying offices' photos inside Germany, with Esri, then MapTiler (each only with a key in Settings), then EOX
 * Sentinel-2 underneath for everywhere else.
 */
internal object SatelliteSources {

    /** The automatic order of the worldwide photo sources; Esri and MapTiler only with a key. */
    fun chain(esriKey: String, maptilerKey: String): List<SatProvider> = chain(AUTO, esriKey, maptilerKey)

    /**
     * The sources to try for a choice in Settings ("auto", "mapbox", "landesamt", "esri", "maptiler", "eox"). A fixed choice
     * still ends with EOX, so a refused key or a missing one never leaves the map without photos.
     */
    fun chain(choice: String, esriKey: String, maptilerKey: String): List<SatProvider> {
        val esri = esriKey.isNotBlank(); val mt = maptilerKey.isNotBlank()
        return when (choice) {
            "mapbox" -> listOf(SatProvider.MAPBOX, SatProvider.EOX)
            "landesamt" -> buildList { if (esri) add(SatProvider.ESRI); if (mt) add(SatProvider.MAPTILER); add(SatProvider.EOX) }
            "esri" -> if (esri) listOf(SatProvider.ESRI, SatProvider.EOX) else listOf(SatProvider.EOX)
            "maptiler" -> if (mt) listOf(SatProvider.MAPTILER, SatProvider.EOX) else listOf(SatProvider.EOX)
            "eox" -> listOf(SatProvider.EOX)
            else -> buildList { add(SatProvider.MAPBOX); if (esri) add(SatProvider.ESRI); if (mt) add(SatProvider.MAPTILER); add(SatProvider.EOX) }
        }
    }
    const val AUTO = "auto"

    /** The state photos are laid over the worldwide ones as soon as Mapbox isn't the source any more. */
    fun withStates(p: SatProvider): Boolean = withStates(AUTO, p)

    /** Whether the state offices' photos go over the worldwide ones: automatic once Mapbox isn't the source, or chosen. */
    fun withStates(choice: String, p: SatProvider): Boolean = when (choice) {
        AUTO -> p != SatProvider.MAPBOX
        "landesamt" -> true
        else -> false
    }

    /** How long a source Mapbox had to give way to is remembered (so Mapbox isn't asked again on every start). */
    const val REMEMBER_MS = 24L * 60 * 60 * 1000

    /** Where to start in [chain]: in automatic mode at the source remembered from the last 24 hours, otherwise at the start. */
    fun startIndex(choice: String, chain: List<SatProvider>, remembered: String, rememberedAt: Long, now: Long): Int {
        if (choice != AUTO || remembered.isEmpty() || now - rememberedAt !in 0 until REMEMBER_MS) return 0
        return chain.indexOfFirst { it.name == remembered }.coerceAtLeast(0)
    }

    /** Everything the satellite settings say, in one text (read inside Compose, so a change in Settings is noticed at once). */
    fun signature(p: Prefs): String = listOf(PreferenceKeys.SAT_SOURCE, PreferenceKeys.ESRI_KEY, PreferenceKeys.MAPTILER_KEY, PreferenceKeys.SAT_HYBRID)
        .joinToString("|") { p.s(it, "") }

    /** "subtle", "normal" or "strong": how much the roads and labels stand out over the photos. */
    fun lineOpacity(hybrid: String): Double? = when (hybrid) { "subtle" -> 0.45; "strong" -> 1.0; else -> null }
    fun labelOpacity(hybrid: String): Double? = if (hybrid == "subtle") 0.75 else null
    /** Strong darkens the photos a little, so the roads stand out. */
    fun photoBrightness(hybrid: String): Double = if (hybrid == "strong") 0.7 else 1.0

    /** How much one refused tile counts towards moving on: a refused key after 3 tiles, too many requests (429) after 10. */
    fun refusals(message: String?): Int {
        val m = message ?: return 0
        return when {
            m.contains("401") || m.contains("403") -> 4
            m.contains("429") -> 1
            else -> 0
        }
    }
    const val REFUSALS_TO_SWITCH = 10

    fun esriTiles(key: String) = "https://ibasemaps-api.arcgis.com/arcgis/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}?token=${key.trim()}"
    fun maptilerTiles(key: String) = "https://api.maptiler.com/tiles/satellite-v2/{z}/{x}/{y}.jpg?key=${key.trim()}"
    const val EOX_TILES = "https://tiles.maps.eox.at/wmts/1.0.0/s2cloudless-2023_3857/default/GoogleMapsCompatible/{z}/{y}/{x}.jpg"
    const val ESRI_CREDIT = "Luftbilder: Esri, Maxar, Earthstar Geographics"
    const val MAPTILER_CREDIT = "Luftbilder: © MapTiler"
    const val EOX_CREDIT = "Sentinel-2 cloudless by EOX IT Services GmbH (contains modified Copernicus Sentinel data)"

    /** A state's photos as Mapbox raster tiles: the WMS asked for each tile's box, transparent outside the state. */
    fun wmsTiles(s: StateWms) = "${s.url}?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&LAYERS=${s.layers}&STYLES=&CRS=EPSG:3857" +
        "&BBOX={bbox-epsg-3857}&WIDTH=256&HEIGHT=256&FORMAT=image/png&TRANSPARENT=TRUE"

    /** All 16 states' free orthophoto services (each checked to answer in EPSG:3857 and to be transparent outside its state). */
    val states = listOf(
        StateWms("bw", "https://owsproxy.lgl-bw.de/owsproxy/ows/WMS_LGL-BW_ATKIS_DOP_20_C", "IMAGES_DOP_20_RGB", 7.48, 47.50, 10.52, 49.80, "© LGL Baden-Württemberg (dl-de/by-2-0)"),
        StateWms("by", "https://geoservices.bayern.de/od/wms/dop/v1/dop40", "by_dop40c", 8.95, 47.25, 13.86, 50.58, "© Bayerische Vermessungsverwaltung (CC BY 4.0)"),
        StateWms("be", "https://gdi.berlin.de/services/wms/truedop_2024", "truedop_2024", 13.07, 52.33, 13.78, 52.69, "© Geoportal Berlin (dl-de/zero-2-0)"),
        StateWms("bb", "https://isk.geobasis-bb.de/mapproxy/dop20c/service/wms", "bebb_dop20c", 11.25, 51.35, 14.78, 53.57, "© GeoBasis-DE/LGB (dl-de/by-2-0)"),
        StateWms("hb", "https://geodienste.bremen.de/wms_dop20_2023", "DOP20_2023_HB,DOP20_2023_BHV", 8.47, 53.00, 9.00, 53.62, "© GeoInformation Bremen (CC BY 4.0)"),
        StateWms("hh", "https://geodienste.hamburg.de/wms_dop_zeitreihe_belaubt", "dop_zeitreihe_belaubt", 8.40, 53.39, 10.34, 53.97, "© Freie und Hansestadt Hamburg, LGV (dl-de/by-2-0)"),
        StateWms("he", "https://gds-srv.hessen.de/cgi-bin/lika-services/ogc-free-images.ows", "he_dop20_rgb", 7.76, 49.39, 10.25, 51.66, "© HVBG Hessen"),
        StateWms("mv", "https://www.geodaten-mv.de/dienste/adv_dop", "mv_dop", 10.58, 53.10, 14.42, 54.69, "© GeoBasis-DE/M-V"),
        StateWms("ni", "https://opendata.lgln.niedersachsen.de/doorman/noauth/dop_wms", "ni_dop20", 6.65, 51.29, 11.60, 53.90, "© LGLN Niedersachsen (CC BY 4.0)"),
        StateWms("nw", "https://www.wms.nrw.de/geobasis/wms_nw_dop", "nw_dop_rgb", 5.86, 50.32, 9.46, 52.53, "© Geobasis NRW (dl-de/zero-2-0)"),
        StateWms("rp", "https://geo4.service24.rlp.de/wms/rp_dop20.fcgi", "rp_dop20", 6.11, 48.96, 8.51, 50.94, "© GeoBasis-DE/LVermGeoRP (dl-de/by-2-0)"),
        StateWms("sl", "https://geoportal.saarland.de/freewms/dop2023", "sl_dop20_rgb", 6.35, 49.11, 7.41, 49.64, "© GeoBasis DE/LVGL-SL (dl-de/by-2-0)"),
        StateWms("sn", "https://geodienste.sachsen.de/wms_geosn_dop-rgb/guest", "sn_dop_020", 11.87, 50.17, 15.04, 51.69, "© GeoSN (dl-de/by-2-0)"),
        StateWms("st", "https://www.geodatenportal.sachsen-anhalt.de/wss/service/ST_LVermGeo_DOP_WMS_OpenData/guest", "lsa_lvermgeo_dop20_2", 10.56, 50.94, 13.19, 53.05, "© GeoBasis-DE/LVermGeo ST (dl-de/by-2-0)"),
        StateWms("sh", "https://service.gdi-sh.de/WMS_SH_DOP20col_OpenGBD", "sh_dop20_rgb", 7.86, 53.36, 11.32, 55.06, "© GeoBasis-DE/LVermGeo SH (CC BY 4.0)"),
        StateWms("th", "https://www.geoproxy.geoportal-th.de/geoproxy/services/DOP20", "th_dop", 9.87, 50.20, 12.66, 51.65, "© GeoBasis-DE/TLBG (dl-de/by-2-0)"),
    )
}
