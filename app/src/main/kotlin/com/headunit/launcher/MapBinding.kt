package com.headunit.launcher

import com.headunit.launcher.R
import androidx.compose.ui.res.stringResource

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.mapbox.bindgen.Value
import com.mapbox.common.Cancelable
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.MultiLineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapLoadingErrorType
import com.mapbox.maps.MapOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.applyDefaultParams
import com.mapbox.maps.toCameraOptions
import com.mapbox.maps.toMapboxImage
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.generated.FillExtrusionLayer
import com.mapbox.maps.extension.style.layers.generated.FillLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.ModelLayer
import com.mapbox.maps.interactions.standard.generated.standardBuildings
import com.mapbox.maps.interactions.standard.generated.standardPoi
import com.mapbox.maps.extension.style.layers.properties.generated.ModelType
import com.mapbox.maps.extension.style.layers.properties.generated.ModelScaleMode
import com.mapbox.maps.extension.style.layers.generated.RasterLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.CirclePitchAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.CirclePitchScale
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.IconRotationAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.RasterSource
import com.mapbox.maps.extension.style.sources.generated.VectorSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.OnRotateListener
import com.mapbox.maps.plugin.gestures.OnScaleListener
import com.mapbox.maps.plugin.gestures.OnShoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.RotateGestureDetector
import com.mapbox.android.gestures.ShoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector

/**
 * Owns the single map view (Mapbox Maps SDK, Standard style) so it can move between the Home preview and the full Navigation page.
 * Everything the app draws on the map goes through here; the rest of the app never talks to Mapbox directly.
 *
 * Mapbox edition notes:
 * - The base map is Mapbox Standard (or Standard Satellite). Its look is set with Standard's own settings (light preset for
 *   dawn / day / dusk / night, 3D objects, labels), never by recolouring its layers.
 * - The app's own layers sit in Standard's "slots": route, lanes and lamps in "middle" (above roads, below 3D buildings), icons
 *   on top. They are lit as "emissive", so Standard's night lighting doesn't darken the route or the icons.
 * - Asking the map what is on screen is asynchronous in Mapbox and never blocks the app; each question also has a time limit,
 *   so a map that isn't drawing (hidden, in the background) can't leave anything waiting.
 */
class MapBinding(private val ctx: Context, fast: Boolean = false, res: Float = 1f, ratio: Float = 0f) : DefaultLifecycleObserver {
    val mapView: MapView
    private val mbMap: MapboxMap get() = mapView.mapboxMap
    private val prefs = Prefs(ctx)
    private var style: Style? = null
    /** True once the map style has loaded (the map shows). */
    val styleReady: Boolean get() = style != null
    private var wantedStyle: String? = null
    private var loadedStyle: String? = null
    private val main0 = android.os.Handler(android.os.Looper.getMainLooper())

    private var main: List<LL> = emptyList()
    private var alts: List<List<LL>> = emptyList()
    private var dest: LL? = null
    /** Stops on the way, in order, each with whether it's a charging stop (drawn with a lightning bolt). */
    private var via: List<Pair<LL, Boolean>> = emptyList()
    private var viaLabels: List<String?> = emptyList()
    /** The names of the stops, for the numbered pins. */
    private var viaNames: List<String?> = emptyList()
    private val viaNumIds = HashSet<String>()
    /** Charge points in view, drawn as pins with their power and free count. */
    private var chargers: List<ChargerSite> = emptyList()
    /** Charger pictures already registered in the current style (cleared when the style is built again). */
    private val chgImgIds = HashSet<String>()
    private var me: LL? = null
    private var meHeading = 0.0

    var onLongPress: ((LL) -> Unit)? = null
    var onUserGesture: (() -> Unit)? = null
    /** The map has settled: the view as [minLat, minLon, maxLat, maxLon, zoom] (for the charger markers). */
    var onMapIdle: ((DoubleArray) -> Unit)? = null
    var onStyleLoaded: (() -> Unit)? = null

    /** True once a map style is showing (so a failed switch can keep it on screen). */
    fun hasStyle(): Boolean = style != null
    /** Tapped one of the grey alternative routes (or its time bubble) while driving. */
    var onAltTap: ((Int) -> Unit)? = null
    /** A charge point on the map was tapped. */
    var onChargerTap: ((ChargerSite) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    /** Mapbox's own map couldn't get a piece of map (with no signal: an area it hasn't stored). */
    var onTileMissing: (() -> Unit)? = null
    /** Mapbox turned down every satellite source (it says why in the feedback report) and the 3D model could not be loaded. */
    var onSatelliteRefused: (() -> Unit)? = null
    var onModelBroken: (() -> Unit)? = null
    private var satellite = false
    /** What is really drawn: the map style and what draws the arrow (for the feedback report). */
    fun drawingSummary(): String =
        "map style: ${loadedStyle ?: "none"} · satellite: ${if (satellite) "on" else "off"} · arrow: ${if (modelOn) "3D model ($modelShape)" else locatorStyle}"

    /** The last error the map gave (for the feedback report); blank until one comes. */
    @Volatile var lastMapError: String = ""
    // index into satChain(): moves on when the current photo source refuses its tiles (429 / 401 / 403); -1 = not chosen yet
    private var satStep = -1
    private fun satChoice() = prefs.s(PreferenceKeys.SAT_SOURCE, SatelliteSources.AUTO)
    /** The satellite sources to try (Settings > Satellitenbilder; Esri and MapTiler only once their key is in Settings). */
    private fun satChain(): List<SatProvider> =
        SatelliteSources.chain(satChoice(), prefs.s(PreferenceKeys.ESRI_KEY, ""), prefs.s(PreferenceKeys.MAPTILER_KEY, ""), tomtomKey())
    /** The TomTom key (the one in Settings, or the built-in one): TomTom's photos are a source once Mapbox refuses. */
    private fun tomtomKey(): String = prefs.s(PreferenceKeys.API_KEY, "").ifBlank { BuildConfig.TOMTOM_API_KEY }.trim()
    private var satSig: String? = null
    private var satFails = 0
    private var buildings3d = true
    private var lite = false
    private var hideTraffic = false
    private var trafficOn = true
    private var routing = false
    var tiltNow = 0.0
        private set
    private val subs = ArrayList<Cancelable>()
    // (declared before init: the logo is placed while the map is being set up, before later properties exist)
    private var brandInset: android.graphics.Rect? = null
    /** Where Mapbox's logo goes: "tl", "tr", "bl" or "br" (Settings > Map). */
    private var logoCorner = "tr"
    /** The scale the map is drawn at (screen pixels per map pixel). */
    private val pixelRatioUsed: Float

    /** Standard's import id when the style is loaded as a Mapbox style address. */
    private val BASE = "basemap"

    init {
        // a placeholder until a real token is entered: the Mapbox map refuses to be created with no token at all
        if (MapboxOptions.accessToken.isBlank()) MapboxOptions.accessToken = NO_TOKEN
        val dens = ctx.resources.displayMetrics.density
        // a lower render resolution is the biggest single saving on a slow GPU (the car screen asks for a set scale: see CarSurface)
        val pr = when {
            ratio > 0f -> ratio
            res < 1f -> max(0.6f, dens * res)
            else -> dens
        }
        pixelRatioUsed = pr
        val opts = MapInitOptions(
            context = ctx,
            mapOptions = MapOptions.Builder().applyDefaultParams(ctx).pixelRatio(pr).build(),
            cameraOptions = CameraOptions.Builder().center(Point.fromLngLat(13.4050, 52.5200)).zoom(14.0).pitch(55.0).build(),
            // the phone moves the map between pages (texture view, can be clipped to round corners); the car draws straight to a surface
            textureView = !fast,
            // no style yet: it's chosen by the engine (and needs a token first)
            styleUri = null,
        )
        mapView = MapView(ctx, opts)
        try { mapView.scalebar.enabled = false } catch (e: Throwable) { softFail(e) }
        try { mapView.compass.enabled = false } catch (e: Throwable) { softFail(e) }
        // Mapbox's logo and credits must stay visible: top right, clear of the app's own cards
        placeBranding(null)
        mapView.post { styleAttribution() }
        try { mbMap.setBounds(CameraBoundsOptions.Builder().maxPitch(75.0).build()) } catch (e: Throwable) { softFail(e) }
        try {
            mapView.gestures.rotateEnabled = true
            mapView.gestures.pitchEnabled = true
        } catch (e: Throwable) { softFail(e) }
        // counted all the time: the automatic frame rate compares frames drawn with camera moves asked for
        subs += mbMap.subscribeRenderFrameStarted { PerfStats.renders++ }
        subs += mbMap.subscribeMapLoadingError { e ->
            lastMapError = "${e.type} ${e.sourceId ?: ""}: ${e.message ?: ""}".trim().take(200)
            // the satellite photo source refuses its tiles (429, or a key it doesn't accept): move on to the next source
            if (satellite && e.type == MapLoadingErrorType.TILE && e.sourceId == "dl-sat") {
                satFails += SatelliteSources.refusals(e.message)
                if (satFails >= SatelliteSources.REFUSALS_TO_SWITCH && satStep < satChain().size - 1) {
                    satFails = 0
                    main0.post {
                        val chain = satChain()
                        if (satStep >= chain.size - 1) return@post
                        satStep++
                        android.util.Log.w("MapBinding", "satellite: source refused, next=${chain.getOrNull(satStep)}")
                        // remembered for 24 hours, so the next start doesn't ask the refusing source again
                        if (satChoice() == SatelliteSources.AUTO) chain.getOrNull(satStep)?.let {
                            prefs.put(PreferenceKeys.SAT_REMEMBERED, it.name); prefs.put(PreferenceKeys.SAT_REMEMBERED_AT, System.currentTimeMillis().toString())
                        }
                        style?.let { dropSatellite(it) }; reinstall()
                    }
                } else if (satFails >= SatelliteSources.REFUSALS_TO_SWITCH) {
                    // no source is left to try: say so, once
                    satFails = 0
                    main0.post { onSatelliteRefused?.invoke() }
                }
            }
            // only the style itself failing is reported (a missing tile now and then is normal)
            if (e.type == MapLoadingErrorType.STYLE) onError?.invoke(e.message)
            // the 3D model couldn't be loaded: the picture of the same shape instead (until the app is restarted)
            else if (modelOn && (e.sourceId ?: "").startsWith("dl-me-m")) main0.post { modelBroken = true; modelWanted = false; reinstall(); onModelBroken?.invoke() }
            // a piece of Mapbox's map itself (not one of the app's extras, like traffic) couldn't be loaded
            else if ((e.type == MapLoadingErrorType.TILE || e.type == MapLoadingErrorType.SOURCE) && !(e.sourceId ?: "").startsWith("dl-")) onTileMissing?.invoke()
        }
        mapView.gestures.addOnMapLongClickListener { p -> onLongPress?.invoke(LL(p.latitude(), p.longitude())); true }
        mapView.gestures.addOnMapClickListener { p ->
            // (whatever goes wrong in a tap is logged, never allowed to close the app)
            try {
                main0.postDelayed({ tapClears() }, 150L)
                val used = try { val s = mbMap.pixelForCoordinate(p); tapAt(android.graphics.PointF(s.x.toFloat(), s.y.toFloat())) } catch (e: Throwable) { false }
                // nothing of the app's under the tap, and (a moment later) no place of the map's either: a tap on a free spot
                if (!used) { val at = android.os.SystemClock.uptimeMillis(); main0.postDelayed({ if (poiTapAt < at) try { freeTapOrPlace(p, at) } catch (e: Throwable) { android.util.Log.w("MapBinding", "free tap: ${e.message}") } }, 300L) }
                used
            } catch (e: Throwable) { android.util.Log.w("MapBinding", "map tap: ${e.message}"); false }
        }
        // any touch that moves the map (drag, pinch, twist, two-finger tilt) stops the follow camera
        mapView.gestures.addOnMoveListener(object : OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) { onUserGesture?.invoke() }
            override fun onMove(detector: MoveGestureDetector): Boolean = false
            override fun onMoveEnd(detector: MoveGestureDetector) { try { onViewMoved?.invoke(LL(mbMap.cameraState.center.latitude(), mbMap.cameraState.center.longitude())) } catch (e: Throwable) { softFail(e) } }
        })
        mapView.gestures.addOnScaleListener(object : OnScaleListener {
            override fun onScaleBegin(detector: StandardScaleGestureDetector) { onUserGesture?.invoke() }
            override fun onScale(detector: StandardScaleGestureDetector) { }
            override fun onScaleEnd(detector: StandardScaleGestureDetector) { }
        })
        mapView.gestures.addOnRotateListener(object : OnRotateListener {
            override fun onRotateBegin(detector: RotateGestureDetector) { onUserGesture?.invoke() }
            override fun onRotate(detector: RotateGestureDetector) { }
            override fun onRotateEnd(detector: RotateGestureDetector) { }
        })
        mapView.gestures.addOnShoveListener(object : OnShoveListener {
            override fun onShoveBegin(detector: ShoveGestureDetector) { onUserGesture?.invoke() }
            override fun onShove(detector: ShoveGestureDetector) { }
            override fun onShoveEnd(detector: ShoveGestureDetector) { }
        })
        subs += mbMap.subscribeCameraChanged {
            // the way the map is turned: the compass's north mark follows it
            try { onBearing?.invoke(mbMap.cameraState.bearing) } catch (e: Throwable) { softFail(e) }
            // zooming across the level where the turn arrow appears or goes: put the route back together (or cut it for the arrow)
            if (turnCut != null && cutZoomOk() != cutActive) try { setLine(main) } catch (e: Exception) { softFail(e) }
            placeAltCards(false)
            refreshLamps(); lights.refresh(); sizeFurniture()
            // (while you drive the map never stops moving, so the roads on screen are read on the move too, every few seconds)
            refreshRoads()
            // the 3D locator is sized from the zoom: rebuild it when you zoom, even if you are standing still
            if (locatorStyle != "flat") pushModel(false) else placeLocator()
        }
        subs += mbMap.subscribeMapIdle { placeAltCards(true); refreshLamps(); lights.refresh(); refreshRoads(); viewBox()?.let { onMapIdle?.invoke(it) } }
        installTapSelect()
    }

    /** Puts Mapbox's logo and the (i) credits button top right, inside the visible part of the screen ([inset], null = whole view). */
    /** True on the car screen: the Mapbox logo and credits go right into the corner. */
    var flushBranding = false

    fun placeBranding(inset: android.graphics.Rect?) {
        brandInset = inset
        val d = ctx.resources.displayMetrics.density
        val top = logoCorner.startsWith("t")
        val left = logoCorner.endsWith("l")
        val v = if (top) android.view.Gravity.TOP else android.view.Gravity.BOTTOM
        val h = if (left) android.view.Gravity.START else android.view.Gravity.END
        // a little in from the edge (further down at the top, clear of the app's top bar), inside the visible part of the screen
        // (on the car screen the logo sits right in the corner of the visible map: there's no app top bar there)
        val vEdge = (if (top) (inset?.top ?: 0) else (inset?.bottom ?: 0)) + (if (flushBranding) 2f else if (top) 64f else 12f) * d
        val hEdge = (if (left) (inset?.left ?: 0) else (inset?.right ?: 0)) + (if (flushBranding) 2f else 10f) * d
        try {
            mapView.logo.position = v or h
            mapView.logo.marginTop = if (top) vEdge else 0f; mapView.logo.marginBottom = if (top) 0f else vEdge
            // the logo sits beside the (i) button, on the side away from the edge
            mapView.logo.marginLeft = if (left) hEdge + 30f * d else 0f; mapView.logo.marginRight = if (left) 0f else hEdge + 30f * d
        } catch (e: Throwable) { softFail(e) }
        try {
            mapView.attribution.position = v or h
            mapView.attribution.marginTop = if (top) vEdge else 0f; mapView.attribution.marginBottom = if (top) 0f else vEdge
            mapView.attribution.marginLeft = if (left) hEdge else 0f; mapView.attribution.marginRight = if (left) 0f else hEdge
        } catch (e: Throwable) { softFail(e) }
    }


    fun setLogoCorner(c: String) {
        if (c == logoCorner || c !in setOf("tl", "tr", "bl", "br")) return
        logoCorner = c
        placeBranding(brandInset)
    }

    // ---- lifecycle ----
    /**
     * Whether the map can be asked which features are on screen right now. In Mapbox the answer arrives later (it waits for a drawn
     * frame) and never blocks the app, but there's no point asking while the map isn't drawing (the app in the background, the
     * map hidden behind a settings page), so it's only asked while the map is started and actually on screen.
     */
    @Volatile private var started = true
    @Volatile private var destroyed = false
    private fun canQuery(): Boolean = started && mapView.isAttachedToWindow && mapView.isShown && mapView.windowVisibility == android.view.View.VISIBLE &&
        mapView.width > 50 && mapView.height > 50

    // (the Mapbox map also follows the screen it's on by itself; these calls are safe to repeat)
    override fun onStart(owner: LifecycleOwner) { started = true; try { mapView.onStart() } catch (e: Throwable) { softFail(e) } }
    override fun onResume(owner: LifecycleOwner) { started = true; try { flowAnim?.resume() } catch (e: Exception) { softFail(e) } }
    override fun onPause(owner: LifecycleOwner) { try { flowAnim?.pause() } catch (e: Exception) { softFail(e) } }
    override fun onStop(owner: LifecycleOwner) { started = false; try { mapView.onStop() } catch (e: Throwable) { softFail(e) } }

    /** The car screen went away for a while (another app full screen) or came back: stop or restart drawing. */
    fun carPause() { if (!started) return; started = false; try { flowAnim?.pause() } catch (e: Exception) { softFail(e) }; try { mapView.onStop() } catch (e: Throwable) { softFail(e) } }
    fun carResume() { if (started) return; try { mapView.onStart() } catch (e: Throwable) { softFail(e) }; started = true; try { flowAnim?.resume() } catch (e: Exception) { softFail(e) } }
    override fun onDestroy(owner: LifecycleOwner) = destroy()

    /** Stops everything and frees the map (the car screen calls this when it goes away). */
    @Synchronized
    fun destroy() {
        if (destroyed) return
        destroyed = true
        started = false
        try { lampExec.shutdownNow() } catch (e: Throwable) { softFail(e) }
        try { lights.close() } catch (e: Throwable) { softFail(e) }
        for (c in subs) try { c.cancel() } catch (e: Throwable) { softFail(e) }
        subs.clear()
        for (c in pendingQueries) try { c.cancel() } catch (e: Throwable) { softFail(e) }
        pendingQueries.clear()
        try { meAnim?.cancel(); bubbleAnim?.cancel(); lanePulse?.cancel(); pulseAnim?.cancel(); flowAnim?.cancel() } catch (e: Throwable) { softFail(e) }
        try { mapView.onStop(); mapView.onDestroy() } catch (e: Throwable) { softFail(e) }
    }

    // ---- token and style ----
    /** The Mapbox public token (pk.…) from Settings. A new one is used from the next style load. */
    fun setToken(token: String) {
        val t = token.trim()
        val want = if (t.isEmpty()) NO_TOKEN else t
        if (MapboxOptions.accessToken == want) return
        MapboxOptions.accessToken = want
        // the token changed: load the style again with it
        if (t.isNotEmpty()) { loadedStyle = null; wantedStyle?.let { loadStyle(it) } }
    }

    /** Asks for a map style (a Mapbox style address such as [Style.STANDARD]). Loading the same one again does nothing. */
    fun setStyleUrl(url: String) {
        wantedStyle = url
        if (url != loadedStyle) loadStyle(url)
        // the same style asked for again after it failed to load: load it again
        else if (style == null) loadStyle(url)
    }

    /** "classic" or "immersive": the app always uses "immersive" now (kept so the screens can ask). */
    var theme = "immersive"
        private set

    fun setTheme(t: String) { theme = t }

    /** "full" (Standard in 3D), "lite" (Standard, flat, fewer labels) or "lightest" (Mapbox's classic 2D map). Settings > Map. */
    var mapDetail = "full"
        private set
    /** "mapbox", "horizon" (the app's own soft colours), "faded" or "grey". Settings > Map. */
    private var colours = "mapbox"
    /** Whether the style has Standard's slots (classic styles don't: the app's layers then simply go on top). */
    private var slotsOn = true

    /** Puts a layer in one of Standard's slots (only when the style has them). */
    private fun <T : Layer> T.inSlot(s: String): T { if (slotsOn) slot(s); return this }

    /** HD roads (lane markings, turn arrows, 3D overpasses) on Mapbox's map: set by Settings > Map style, on by itself only in the UK. */
    @Volatile var hdRoads = false
    fun applyHdRoads(on: Boolean) {
        if (on == hdRoads) return
        hdRoads = on
        main0.post { try { setBase("showHdRoads", Value.valueOf(on)) } catch (e: Throwable) { softFail(e) } }
    }

    fun setMapDetail(d: String) {
        val v = if (d in setOf("full", "lite", "lightest")) d else "full"
        if (v == mapDetail) return
        val wasLightest = mapDetail == "lightest"
        mapDetail = v
        lite = v != "full"
        try { glowLayer?.visibility(vis(!lite)) } catch (e: Exception) { softFail(e) }
        // to or from the classic map: a different style; between full and lite: just Standard's settings
        if (wasLightest || v == "lightest") { if (isMapboxStyle(wantedStyle)) setStyleUrl(baseStyle()) }
        else {
            setBase("show3dObjects", Value.valueOf(b3dStd() && v == "full"))
            setBase("showPointOfInterestLabels", Value.valueOf(!lite && !poiMoving))
            setBase("showTransitLabels", Value.valueOf(!lite))
            setBase("showLandmarkIcons", Value.valueOf(!lite))
            setBase("showHdRoads", Value.valueOf(hdRoads))
            // the real 3D arrow only shows on the full map
            if (style != null) reinstall()
        }
    }

    /** The neon look is on: Standard's own 3D buildings, facades and landmarks give way to the app's red blocks. */
    private val neonMap: Boolean get() = colours == "neon"
    private fun b3dStd() = buildings3d && !neonMap

    fun setColours(c: String) {
        val v = if (c in setOf("mapbox", "horizon", "faded", "grey", "neon")) c else "mapbox"
        if (v == colours) return
        colours = v
        // the colours are part of how the map loads (and Horizon is a different base map), so it's loaded again
        if (isMapboxStyle(wantedStyle)) { loadedStyle = null; setStyleUrl(baseStyle()) }
        else if (wantedStyle != null && !slotsOn) { loadedStyle = null; setStyleUrl(wantedStyle!!) }
    }

    /** Asked to load the map style again (satellite switched): the engine knows which map source is chosen. */
    var onRestyle: (() -> Unit)? = null
    val isSatellite: Boolean get() = satellite

    private fun isMapboxStyle(u: String?) = u == Style.STANDARD || u == Style.STANDARD_SATELLITE || u == Style.MAPBOX_STREETS || u == Style.DARK || u == NAV_DAY || u == NAV_NIGHT

    /** Standard's light: "day", "dawn", "dusk" or "night". Changing it fades smoothly, without reloading the map. */
    private var lightPreset = "night"

    fun setLightPreset(p: String) {
        if (p == lightPreset) return
        lightPreset = p
        setBase("lightPreset", Value.valueOf(satLight(p)))
    }

    /** Satellite at "night" darkens the photos until there's next to nothing to see (it looked as if satellite wasn't working),
     *  so satellite goes no darker than dusk. */
    private fun satLight(p: String): String = if (satellite) "day" else p

    /** One of Standard's settings (only once a style is showing; they're also written into the style when it loads). */
    private fun setBase(key: String, v: Value) {
        val st = style ?: return
        try { st.setStyleImportConfigProperty(BASE, key, v) } catch (e: Throwable) { softFail(e) }
    }

    /** Standard's settings as they should be now, written into the style before it loads (so night never flashes up as day). */
    private fun baseConfig(): org.json.JSONObject {
        val sat = (wantedStyle ?: "") == Style.STANDARD_SATELLITE
        val c = org.json.JSONObject()
        c.put("lightPreset", if (sat && lightPreset == "night") "dusk" else lightPreset)
        // place and road names in the app's language where the map has them (not the phone's, when the app is set to another)
        c.put("language", java.util.Locale.getDefault().language.ifBlank { "en" })
        // shop and place labels rest on the light map and (on the full map) while you're on the move: placing them is the costly part
        c.put("showPointOfInterestLabels", !lite && !poiMoving)
        c.put("showTransitLabels", !lite)
        if (!sat) c.put("show3dObjects", b3dStd() && MapStylePolicy.supportsFullDetailFeatures(mapDetail))
        // (colour themes are only for the drawn map: Standard Satellite has none, so nothing is sent for it)
        if (!sat) when (colours) {
            "faded" -> c.put("theme", "faded")
            "grey" -> c.put("theme", "monochrome")
            // Mapbox's own colours with a gentle purple-blue hint (mostly at night), from a colour look-up table
            "horizon" -> if (!sat) { c.put("theme", "custom"); c.put("theme-data", HorizonLut.BASE64) }
            // the neon look: a dark blue city with cyan glowing roads (the buildings are the app's own red blocks, see installNeonCity)
            "neon" -> { c.put("theme", "custom"); c.put("theme-data", NeonLut.BASE64) }
        }
        return c
    }

    /**
     * The style as JSON: Mapbox Standard (or Standard Satellite) imported as "basemap" with its settings, and nothing else; the
     * app's own sources and layers are added once it has loaded.
     */
    private fun wrapperJson(url: String): String {
        val imp = org.json.JSONObject().put("id", BASE).put("url", url).put("config", baseConfig())
        return org.json.JSONObject()
            .put("version", 8)
            .put("imports", org.json.JSONArray().put(imp))
            .put("sources", org.json.JSONObject())
            .put("layers", org.json.JSONArray())
            .toString()
    }

    private var styleToken = 0

    private fun loadStyle(url: String) {
        // no real token yet: nothing to load (the engine shows a note asking for one)
        if (MapboxOptions.accessToken == NO_TOKEN) return
        loadedStyle = url
        style = null
        val token = ++styleToken
        // Standard (and Standard Satellite) come wrapped with their settings; Mapbox's classic 2D styles load as they are
        val standard = url == Style.STANDARD || url == Style.STANDARD_SATELLITE
        slotsOn = standard
        try {
            mbMap.loadStyle(if (standard) wrapperJson(url) else url) { st ->
                if (token != styleToken) return@loadStyle
                style = st
                try { install(st) } catch (e: Throwable) { android.util.Log.w("MapBinding", "install failed", e) }
                onStyleLoaded?.invoke()
            }
        } catch (e: Throwable) { onError?.invoke(e.message ?: Lang.t("Karte konnte nicht geladen werden", "Map failed to load")) }
    }

    /** Adds a picture to the map at the size MapLibre gave it (its pixels at the bitmap's own density). */
    private fun addImg(st: Style, id: String, b: Bitmap) {
        try {
            val scale = (if (b.density > 0) b.density else android.util.DisplayMetrics.DENSITY_DEFAULT) / android.util.DisplayMetrics.DENSITY_DEFAULT.toFloat()
            st.addStyleImage(id, scale, b.toMapboxImage(), false, emptyList(), emptyList(), null)
        } catch (e: Throwable) { softFail(e) }
    }

    /** Adds a layer; one that fails is skipped (and logged) instead of stopping the rest of the map's layers being added. */
    private fun addL(st: Style, l: Layer, below: String? = null) {
        try { if (below != null && st.styleLayerExists(below)) st.addLayerBelow(l, below) else st.addLayer(l) }
        catch (e: Throwable) { android.util.Log.w("MapBinding", "layer ${l.layerId}: ${e.message}") }
    }
    /**
     * Satellite photos under the streets: Mapbox's own satellite tiles, drawn beneath the roads and labels (the streets style
     * carries the rest). Replaces Mapbox Standard Satellite, which left the photos blank on the car's screen.
     */
    private fun addSatellite(st: Style) {
        try {
            val chain = satChain()
            val choice = satChoice()
            if (satStep < 0) satStep = SatelliteSources.startIndex(choice, chain, prefs.s(PreferenceKeys.SAT_REMEMBERED, ""),
                prefs.s(PreferenceKeys.SAT_REMEMBERED_AT, "0").toLongOrNull() ?: 0L, System.currentTimeMillis())
            val prov = chain[satStep.coerceIn(0, chain.size - 1)]
            val states = SatelliteSources.withStates(choice, prov)
            val hybrid = prefs.s(PreferenceKeys.SAT_HYBRID, "normal")
            val bright = SatelliteSources.photoBrightness(hybrid)
            android.util.Log.w("MapBinding", "satellite: install, source already there=${st.styleSourceExists("dl-sat")}, choice=$choice, source=$prov, states=$states, hybrid=$hybrid, layers=${st.styleLayers.size}")
            if (st.styleSourceExists("dl-sat")) return
            val src = when (prov) {
                SatProvider.MAPBOX -> RasterSource.Builder("dl-sat").url("mapbox://mapbox.satellite").tileSize(256).maxzoom(17L).build()
                SatProvider.ESRI -> RasterSource.Builder("dl-sat").tiles(listOf(SatelliteSources.esriTiles(prefs.s(PreferenceKeys.ESRI_KEY, ""))))
                    .tileSize(256).maxzoom(19L).attribution(SatelliteSources.ESRI_CREDIT).build()
                SatProvider.MAPTILER -> RasterSource.Builder("dl-sat").tiles(listOf(SatelliteSources.maptilerTiles(prefs.s(PreferenceKeys.MAPTILER_KEY, ""))))
                    .tileSize(512).maxzoom(20L).attribution(SatelliteSources.MAPTILER_CREDIT).build()
                // EOX's Sentinel-2 photos stop at zoom 13 (free for non-commercial use, attribution required)
                // (through the photo store on the phone: kept for later, and for routes fetched ahead)
                SatProvider.TOMTOM -> RasterSource.Builder("dl-sat").tiles(listOf(SatelliteSources.tomtomTiles(tomtomKey()))).tileSize(256).maxzoom(19L)
                    .attribution(SatelliteSources.TOMTOM_CREDIT).build()
                SatProvider.EOX -> RasterSource.Builder("dl-sat").tiles(listOf(SatTileCache.tileUrl(ctx, "eox") ?: SatelliteSources.EOX_TILES)).tileSize(256).maxzoom(13L)
                    .attribution(SatelliteSources.EOX_CREDIT).build()
            }
            addS(st, src)
            // The streets style's opaque ground is a "background"-type layer named "land": the photos go right above it
            // (below the first layer that isn't a background), and the background itself is hidden.
            val below = st.styleLayers.firstOrNull { it.type != "background" }?.id
            android.util.Log.w("MapBinding", "satellite: photo layer below=$below")
            fun put(l: RasterLayer) { if (below != null) st.addLayerBelow(l, below) else st.addLayer(l) }
            put(RasterLayer("dl-sat-photo", "dl-sat").rasterFadeDuration(0.0).rasterBrightnessMax(bright))
            // Once Mapbox isn't the source: the state surveying offices' 20 cm photos inside Germany, over the worldwide ones
            // (each state's service is transparent outside its state, and only asked within its rough area, from zoom 12)
            if (states) for (w in SatelliteSources.states) {
                try {
                    val id = "dl-sat-de-${w.code}"
                    addS(st, RasterSource.Builder(id).tiles(listOf(SatTileCache.tileUrl(ctx, "de-${w.code}") ?: SatelliteSources.wmsTiles(w))).tileSize(256)
                        .bounds(listOf(w.west, w.south, w.east, w.north)).minzoom(12L).maxzoom(19L).attribution("Luftbilder " + w.credit).build())
                    put(RasterLayer("$id-photo", id).rasterFadeDuration(0.0).rasterBrightnessMax(bright).minZoom(12.0))
                } catch (e: Throwable) { android.util.Log.w("MapBinding", "satellite ${w.code}: ${e.message}") }
            }
            // The streets style's opaque land, water and building fills would cover the photos: hide them in satellite mode.
            // Roads, labels and symbols stay on top of the photos.
            // How much the streets style's own roads and labels stand out over the photos (Settings > Satellitenbilder)
            val lineOp = SatelliteSources.lineOpacity(hybrid)
            val labelOp = SatelliteSources.labelOpacity(hybrid)
            for (info in st.styleLayers) {
                if (info.id.startsWith("dl-")) continue
                try {
                    when (info.type) {
                        "fill", "fill-extrusion", "background" -> st.setStyleLayerProperty(info.id, "visibility", Value.valueOf("none"))
                        "line" -> if (lineOp != null) st.setStyleLayerProperty(info.id, "line-opacity", Value.valueOf(lineOp))
                        "symbol" -> if (labelOp != null) {
                            st.setStyleLayerProperty(info.id, "text-opacity", Value.valueOf(labelOp))
                            st.setStyleLayerProperty(info.id, "icon-opacity", Value.valueOf(labelOp))
                        }
                    }
                } catch (e: Throwable) { softFail(e) }
            }
        } catch (e: Throwable) { android.util.Log.w("MapBinding", "satellite: ${e.message}") }
    }
    /** Takes the satellite photos out of the style again, so the other photo source can be added in their place. */
    private fun dropSatellite(st: Style) {
        for (l in st.styleLayers.map { it.id }.filter { it.startsWith("dl-sat") }) try { st.removeStyleLayer(l) } catch (e: Throwable) { softFail(e) }
        for (src in st.styleSources.map { it.id }.filter { it.startsWith("dl-sat") }) try { st.removeStyleSource(src) } catch (e: Throwable) { softFail(e) }
    }
    private fun addS(st: Style, s: com.mapbox.maps.extension.style.StyleContract.StyleSourceExtension) {
        try { st.addSource(s) } catch (e: Throwable) { android.util.Log.w("MapBinding", "source: ${e.message}") }
    }
    private fun vis(on: Boolean) = if (on) Visibility.VISIBLE else Visibility.NONE
    private fun layer(id: String): Layer? = try { style?.getLayer(id) } catch (e: Throwable) { null }

    // ---- asking the map what's on screen (asynchronous, with a time limit) ----
    private val pendingQueries = ArrayList<Cancelable>()

    /**
     * Asks which features of [layers] are drawn inside [box] (screen pixels). [done] runs on the main thread with the features, or
     * with null when the map didn't answer within [timeoutMs] (it's then cancelled), so callers can always clear their busy flags.
     */
    private fun query(box: RectF, layers: List<String>, timeoutMs: Long = 3000L, done: (List<Feature>?) -> Unit) {
        var finished = false
        fun finish(r: List<Feature>?) { if (finished) return; finished = true; try { done(r) } catch (e: Throwable) { softFail(e) } }
        val c = try {
            mbMap.queryRenderedFeatures(
                RenderedQueryGeometry(ScreenBox(ScreenCoordinate(box.left.toDouble(), box.top.toDouble()), ScreenCoordinate(box.right.toDouble(), box.bottom.toDouble()))),
                RenderedQueryOptions(layers, null),
            ) { res ->
                val list = try { res.value?.map { it.queriedFeature.feature } } catch (e: Throwable) { null }
                main0.post { finish(list ?: emptyList()) }
            }
        } catch (e: Throwable) { finish(null); return }
        pendingQueries += c
        main0.postDelayed({
            pendingQueries.remove(c)
            if (!finished) { try { c.cancel() } catch (e: Throwable) { softFail(e) }; finish(null) }
        }, timeoutMs)
    }

    /**
     * A tap on a spot with nothing of the app's under it: a named place of the map there opens as a place (the same as Mapbox's own
     * places, so TomTom's and OpenStreetMap's styles act alike); anything else is the free spot.
     */
    private fun freeTapOrPlace(p: Point, at: Long) {
        val spot = LL(p.latitude(), p.longitude())
        val layers = try { mbMap.getStyle()?.styleLayers?.map { it.id }?.filter { it.contains("poi") }.orEmpty() } catch (e: Throwable) { emptyList() }
        if (layers.isEmpty()) { onFreeTap?.invoke(spot); return }
        val s = mbMap.pixelForCoordinate(p)
        val x = s.x.toFloat(); val y = s.y.toFloat()
        query(RectF(x - 24f, y - 24f, x + 24f, y + 24f), layers) { feats ->
            if (poiTapAt >= at) return@query
            val f = feats?.firstOrNull { !it.getStringProperty("name").isNullOrBlank() }
            if (f == null) { onFreeTap?.invoke(spot); return@query }
            val at2 = (f.geometry() as? Point)?.let { LL(it.latitude(), it.longitude()) } ?: spot
            poiTapAt = android.os.SystemClock.uptimeMillis()
            onPoiTap?.invoke(f.getStringProperty("name").orEmpty(), at2)
        }
    }

    /** Google-style destination pin: red teardrop with a white centre. */
    /** Where you parked, shown on the map (Lang.t("Dein Auto", "Your car")), or null for none. */
    @Volatile private var parkedAt: LL? = null

    /** The way on foot (to your parked car), drawn as a dotted line; empty for none. */
    @Volatile private var walkPts: List<LL> = emptyList()

    fun setWalk(p: List<LL>) {
        walkPts = p
        main0.post {
            try { src("dl-walk")?.featureCollection(if (p.size >= 2) FeatureCollection.fromFeatures(listOf(line(p))) else empty()) } catch (e: Throwable) { softFail(e) }
        }
    }

    // ---- arrival: the destination's entrance, and the dotted walk to it from the suggested parking ----
    private var entranceAt: LL? = null
    private var arrWalk: List<LL> = emptyList()

    fun setEntrance(p: LL?) {
        entranceAt = p
        main0.post { try { src("dl-entrance")?.featureCollection(FeatureCollection.fromFeatures(p?.let { listOf(Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat))) } ?: emptyList())) } catch (e: Throwable) { softFail(e) } }
    }

    fun setArrivalWalk(p: List<LL>) {
        arrWalk = p
        main0.post { try { src("dl-arrwalk")?.featureCollection(if (p.size >= 2) FeatureCollection.fromFeatures(listOf(line(p))) else empty()) } catch (e: Throwable) { softFail(e) } }
    }

    fun setParked(p: LL?) {
        if (p == parkedAt || (p != null && parkedAt != null && Geo.dist(p, parkedAt!!) < 1.0)) return
        parkedAt = p
        main0.post {
            try { src("dl-parked")?.featureCollection(FeatureCollection.fromFeatures(p?.let { listOf(Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat))) } ?: emptyList())) } catch (e: Throwable) { softFail(e) }
        }
    }

    // ---- the lane sign: an overhead sign standing at the junction, like a motorway gantry, with the lanes to use lit up ----
    @Volatile private var signAt: LL? = null
    @Volatile private var signBmp: Bitmap? = null
    private var signKey = ""

    /**
     * Shows the lane sign standing on the road at [at] (null hides it). [lanes] is each lane, left to right: its arrows (each
     * with whether it's the way to go) and whether it's a lane to use. It stays upright and faces you as the map tilts.
     */
    fun setLaneSign(at: LL?, lanes: List<Pair<List<Pair<String, Boolean>>, Boolean>>) {
        val key = if (at == null) "" else "${at.lat},${at.lon}|" + lanes.joinToString(";") { (d, on) -> d.joinToString(",") { "${it.first}${if (it.second) 1 else 0}" } + (if (on) "*" else "") }
        if (key == signKey) return
        signKey = key
        main0.post {
            val st = style ?: return@post
            try {
                if (at != null && lanes.isNotEmpty()) {
                    val b = laneSignBitmap(lanes)
                    signBmp = b; signAt = at
                    addImg(st, "dl-lanesign", b)
                    src("dl-lanesign")?.featureCollection(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(Point.fromLngLat(at.lon, at.lat)))))
                } else {
                    signAt = null
                    src("dl-lanesign")?.featureCollection(FeatureCollection.fromFeatures(emptyList()))
                }
            } catch (e: Throwable) { softFail(e) }
        }
    }

    // ---- the lane bubble: a small strip of the lanes riding on the road just ahead of your arrow (when the lanes are painted on the road) ----
    @Volatile private var bubbleAt: LL? = null
    @Volatile private var bubbleBmp: Bitmap? = null
    private var bubbleLanesKey = ""
    private var bubbleKey = ""
    private var bubbleCur: LL? = null
    private var bubbleAnim: android.animation.ValueAnimator? = null
    private fun pushBubble(p: LL) {
        try { src("dl-lanebubble")?.featureCollection(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat))))) } catch (e: Throwable) { softFail(e) }
    }

    /** Shows the lane bubble at [at] (null hides it). Same lane description as [setLaneSign]. */
    fun setLaneBubble(at: LL?, lanes: List<Pair<List<Pair<String, Boolean>>, Boolean>>) {
        val lk = lanes.joinToString(";") { (d, on) -> d.joinToString(",") { "${it.first}${if (it.second) 1 else 0}" } + (if (on) "*" else "") }
        val key = if (at == null) "" else String.format(java.util.Locale.US, "%.5f,%.5f|%s", at.lat, at.lon, lk)
        if (key == bubbleKey) return
        bubbleKey = key
        main0.post {
            val st = style ?: return@post
            try {
                if (at != null && lanes.isNotEmpty()) {
                    if (lk != bubbleLanesKey || bubbleBmp == null) { bubbleLanesKey = lk; bubbleBmp = laneSignBitmap(lanes, false); addImg(st, "dl-lanebubble", bubbleBmp!!) }
                    // it glides to the new spot over a second, like the arrow it rides ahead of, instead of jumping once a second
                    val from = bubbleCur
                    bubbleAnim?.cancel()
                    if (from == null || Geo.dist(from, at) > 120.0) { bubbleCur = at; bubbleAt = at; pushBubble(at) }
                    else bubbleAnim = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 1000
                        interpolator = android.view.animation.LinearInterpolator()
                        var lastPush = 0L
                        addUpdateListener { a ->
                            val t = a.animatedFraction.toDouble()
                            val q = LL(from.lat + (at.lat - from.lat) * t, from.lon + (at.lon - from.lon) * t)
                            bubbleCur = q; bubbleAt = q
                            val now = android.os.SystemClock.uptimeMillis()
                            if (now - lastPush >= 50 || t >= 1.0) { lastPush = now; pushBubble(q) }
                        }
                        start()
                    }
                } else {
                    bubbleAnim?.cancel()
                    bubbleCur = null; bubbleAt = null
                    src("dl-lanebubble")?.featureCollection(FeatureCollection.fromFeatures(emptyList()))
                }
            } catch (e: Throwable) { softFail(e) }
        }
    }

    /** A blue round badge with a white car, for where you parked. */

    /** A hexagon path of radius [r] round ([cx], [cy]), pointing up (the neon look's marker shape). */

    /** The neon look's marker: a hexagon in [fill] with a dark [inner] mark, and a point below it when [tip] (the destination). */

    /** The neon look's selection frame round the destination marker: four cyan corner brackets (same canvas as the pin). */
    private fun neonFrameBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(96, 128, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF19F0FF.toInt(); style = Paint.Style.STROKE; strokeWidth = 5f; strokeCap = Paint.Cap.SQUARE }
        val l = 5f; val t = 0f; val rr = 91f; val b = 88f; val n = 22f
        c.drawLine(l, t + n, l, t, p); c.drawLine(l, t, l + n, t, p)
        c.drawLine(rr - n, t, rr, t, p); c.drawLine(rr, t, rr, t + n, p)
        c.drawLine(l, b - n, l, b, p); c.drawLine(l, b, l + n, b, p)
        c.drawLine(rr - n, b, rr, b, p); c.drawLine(rr, b, rr, b - n, p)
        return bmp
    }

    private fun pinBitmap(): Bitmap {
        if (neonMap) return neonHex(96, 128, 48f, 44f, 36f, 0xFFFCEE0A.toInt(), false, true)
        val bmp = Bitmap.createBitmap(96, 128, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val path = Path().apply {
            addCircle(48f, 44f, 34f, Path.Direction.CW)
            moveTo(22f, 66f); lineTo(48f, 120f); lineTo(74f, 66f); close()
        }
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF000000.toInt(); setShadowLayer(8f, 0f, 4f, 0x66000000) })
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEA4335.toInt(); style = Paint.Style.FILL })
        c.drawCircle(48f, 44f, 13f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8C1D18.toInt() })
        c.drawCircle(48f, 44f, 11f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
        return bmp
    }

    /** A charging stop on the way: a teal disc with a white lightning bolt (ring and shadow like the other markers). */
    private fun viaEvBitmap(): Bitmap {
        if (neonMap) return neonHex(96, 96, 48f, 48f, 42f, 0xFF19F0FF.toInt(), true, false)
        val bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawCircle(48f, 50f, 42f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55000000 })
        c.drawCircle(48f, 48f, 42f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
        c.drawCircle(48f, 48f, 35f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00A884.toInt() })
        val bolt = Path().apply {
            moveTo(55f, 16f); lineTo(29f, 53f); lineTo(45f, 53f); lineTo(39f, 80f)
            lineTo(67f, 41f); lineTo(51f, 41f); lineTo(58f, 16f); close()
        }
        c.drawPath(bolt, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL })
        return bmp
    }

    /** Any other stop on the way: a smaller neutral grey disc with a white centre, so it doesn't compete with the destination pin. */
    private fun viaStopBitmap(): Bitmap {
        if (neonMap) return neonHex(64, 64, 32f, 32f, 28f, 0xFFEAF4FF.toInt(), false, false)
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawCircle(32f, 34f, 27f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55000000 })
        c.drawCircle(32f, 32f, 27f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
        c.drawCircle(32f, 32f, 21f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF5F6368.toInt() })
        c.drawCircle(32f, 32f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
        return bmp
    }

    private var locatorCache: Pair<String, Bitmap>? = null
    /** The neon look's arrow colour ("#RRGGBB") while it replaces the flat arrow, else null. */
    private var neonArrowHex: String? = null
    /** The neon arrow is drawn larger than the Modern flat one so it appears as big as the normal 3D arrow. */
    private fun flatIconK(): Double = if (neonArrowHex != null) 1.55 else 1.0


    /** The same locator picture the map uses (for the current map colours), for drawing it small elsewhere. Points up. */
    fun locatorBitmap(): Bitmap {
        val key = theme + (neonArrowHex ?: "")
        locatorCache?.let { if (it.first == key) return it.second }
        val b = neonArrowHex?.let { neonArrowBitmap(it) } ?: if (theme == "immersive") arrowBitmapImmersive() else arrowBitmap()
        locatorCache = key to b
        return b
    }








    /**
     * Set by the engine: true when the night look is showing. Standard's light changes by itself (see setLightPreset); the app's
     * own layers have day and night colours (and lamps only at night), so they're put back with the new colours.
     */
    var nightStyle = true
        set(v) {
            if (field == v) return
            field = v
            main0.post { reinstall(); styleAttribution() }
        }

    /**
     * Mapbox's (i) credits button in the map's light: a light button with a dark "i" on a light map, a dark one with a light "i" on a
     * dark map (in the car it showed as a plain black circle on the dark map).
     */
    fun styleAttribution() {
        val dark = nightStyle || neonMap
        val bg = if (dark) 0xE61C2030.toInt() else 0xF2FFFFFF.toInt()
        val fg = if (dark) 0xFFF1F3FA.toInt() else 0xFF1C2130.toInt()
        val d = ctx.resources.displayMetrics.density
        fun find(v: android.view.View): android.widget.ImageView? {
            if (v is android.widget.ImageView && v.javaClass.name.contains("ttribution")) return v
            if (v is android.view.ViewGroup) for (i in 0 until v.childCount) find(v.getChildAt(i))?.let { return it }
            return null
        }
        try {
            val iv = find(mapView) ?: return
            iv.clearColorFilter()
            iv.imageTintList = null
            iv.setImageDrawable(object : android.graphics.drawable.Drawable() {
                private val circle = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = bg }
                private val edge = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = fg; alpha = 70; style = android.graphics.Paint.Style.STROKE; strokeWidth = 1f * d }
                private val text = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = fg; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD_ITALIC)
                }
                override fun draw(c: android.graphics.Canvas) {
                    val b = bounds; val r = minOf(b.width(), b.height()) / 2f - 1f * d
                    c.drawCircle(b.exactCenterX(), b.exactCenterY(), r, circle)
                    c.drawCircle(b.exactCenterX(), b.exactCenterY(), r, edge)
                    text.textSize = r * 1.35f
                    c.drawText("i", b.exactCenterX(), b.exactCenterY() - (text.ascent() + text.descent()) / 2f, text)
                }
                override fun getIntrinsicWidth() = (20 * d).toInt()
                override fun getIntrinsicHeight() = (20 * d).toInt()
                override fun setAlpha(a: Int) {}
                override fun setColorFilter(cf: android.graphics.ColorFilter?) {}
                @Deprecated("Deprecated in Java") override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
            })
        } catch (e: Throwable) { softFail(e) }
    }


    // ---- extra 3D features (Settings > Map), see MapExtras.kt ----
    private var facadesOn = true
    private var landmarksOn = true
    private var pathsOn = true
    private var terrainOn = false
    private var tapSelectOn = true
    private var furnitureOn = true
    private var clearOn = true
    private var weatherCode: Int? = null
    /** The map is drawing the rain or snow itself (so the screen overlay only adds fog). */
    @Volatile var drawsWeather = false
    /** Told the traffic lights on the route each time they change (the engine zooms in as you come up to one). */
    var onRouteSignals: ((List<LL>) -> Unit)? = null
    /** A place on Mapbox's map you tapped (its name and spot): holding on it then routes there by name. */
    var onPoiTap: ((String, LL) -> Unit)? = null

    fun setExtras(facades: Boolean, landmarks: Boolean, paths: Boolean, terrain: Boolean, tapSelect: Boolean, furniture: Boolean, clear: Boolean) {
        val rebuild = terrain != terrainOn || furniture != furnitureOn || clear != clearOn
        facadesOn = facades; landmarksOn = landmarks; pathsOn = paths; terrainOn = terrain; furnitureOn = furniture; clearOn = clear
        if (tapSelect != tapSelectOn) { tapSelectOn = tapSelect; if (!tapSelect) clearSelection() }
        main0.post {
            setBase("show3dFacades", Value.valueOf(facadesOn && b3dStd() && MapStylePolicy.supportsFullDetailFeatures(mapDetail)))
            setBase("show3dLandmarks", Value.valueOf(landmarksOn && !neonMap && MapStylePolicy.supportsFullDetailFeatures(mapDetail)))
            setBase("showPedestrianRoads", Value.valueOf(pathsOn))
            if (rebuild && style != null) reinstall()
        }
    }

    private var skyCode: Int? = null
    /** The weather for the haze towards the horizon (Settings > Fog and sky); null leaves Mapbox's own sky. */
    fun setSky(code: Int?) {
        if (code == skyCode && style != null) return
        skyCode = code
        main0.post { style?.let { Sky.atmosphere(it, skyCode, nightStyle) } }
    }

    /** The weather where you are now (Open-Meteo code), for rain or snow on the map; null for none. */
    fun setWeather(code: Int?) {
        if (code == weatherCode && style != null) return
        weatherCode = code
        main0.post { style?.let { drawsWeather = Sky.weather(it, weatherCode) } }
    }

    /** Puts the extras on a freshly loaded (or rebuilt) map. */
    private fun installExtras(st: Style) {
        // hills and valleys (not on the classic 2D map)
        Sky.terrain(st, terrainOn && mapDetail != "lightest")
        drawsWeather = Sky.weather(st, weatherCode)
        Sky.atmosphere(st, skyCode, nightStyle)
        if (furnitureOn && MapStylePolicy.supportsFullDetailFeatures(mapDetail)) {
            Furniture.addModels(st)
            addS(st, GeoJsonSource.Builder("dl-signals-m").featureCollection(empty()).build())
            // sized by the zoom so they stay easy to see (see Furniture.SIGNAL_SIZE)
            addL(st, Furniture.layer("dl-sig-model", "dl-signals-m", Furniture.SIGNAL, 0.35, 14.0, sigSize()).inSlot(TOP))
            addL(st, Furniture.layer("dl-sig-lens", "dl-signals-m", Furniture.SIGNAL_LENS, 1.0, 14.0, sigSize()).inSlot(TOP))
            // the traffic lights round you that aren't on a route (or when there's no route): from the offline pack's list
            addS(st, GeoJsonSource.Builder("dl-signals-near").featureCollection(empty()).build())
            addL(st, Furniture.layer("dl-sign-model", "dl-signals-near", Furniture.SIGNAL, 0.35, 14.0, sigSize()).inSlot(TOP))
            addL(st, Furniture.layer("dl-sign-lens", "dl-signals-near", Furniture.SIGNAL_LENS, 1.0, 14.0, sigSize()).inSlot(TOP))
            main0.postDelayed({ refreshLamps(true) }, 400L)
            // (the flat traffic light badges on your route stay at every zoom)
            furnZoom = -1.0
            applyNearLook()
            addS(st, GeoJsonSource.Builder("dl-cams-m").featureCollection(empty()).build())
            addL(st, Furniture.layer("dl-cam-model", "dl-cams-m", Furniture.CAMERA, 0.5, 14.0, sigSize()).inSlot(TOP))
            // the average speed check cameras at each end of a zone on your route
            addS(st, GeoJsonSource.Builder("dl-avg-cams").featureCollection(FeatureCollection.fromFeatures(avgCamFeats())).build())
            addL(st, Furniture.layer("dl-avgcam-model", "dl-avg-cams", Furniture.CAMERA, 0.5, 13.0, sigSize()).inSlot(TOP))
            // (the flat traffic light and camera badges stay on at every zoom: the 3D models are extra detail when you're close in)
        }
        // clear view: Mapbox's 3D trees and landmarks kept out of the way round your arrow and just ahead (Full 3D only)
        if (clearOn && slotsOn && MapStylePolicy.supportsFullDetailFeatures(mapDetail) && !satellite) {
            addS(st, ClearView.source())
            addL(st, ClearView.layer())
            clearAt = null; updateClear(true)
        }
    }

    private fun sigSize() = Furniture.sizeAt(Furniture.SIGNAL_SIZE, try { mbMap.cameraState.zoom } catch (e: Throwable) { 16.0 })
    private fun lampSize() = Furniture.sizeAt(Furniture.LAMP_SIZE, try { mbMap.cameraState.zoom } catch (e: Throwable) { 16.0 })
    private var furnZoom = -1.0

    /** Resizes the 3D street furniture to the zoom, smoothly (only when the zoom has changed a little, so not on every frame). */
    private fun sizeFurniture() {
        if (style == null) return
        val z = try { mbMap.cameraState.zoom } catch (e: Throwable) { return }
        if (Math.abs(z - furnZoom) < 0.03) return
        furnZoom = z
        // (written straight to the style by id: looking layers up first costs far more, and this runs as you zoom)
        val st = style ?: return
        val furnSigIds = arrayOf("dl-sig-model", "dl-sig-lens", "dl-sign-model", "dl-sign-lens", "dl-cam-model", "dl-avgcam-model")
        val furnLampIds = arrayOf("dl-lamp-post", "dl-lamp-light")
        val s = Furniture.sizeAt(Furniture.SIGNAL_SIZE, z); val sv = Value(listOf(Value(s), Value(s), Value(s)))
        val l = Furniture.sizeAt(Furniture.LAMP_SIZE, z); val lv = Value(listOf(Value(l), Value(l), Value(l)))
        try {
            for (id in furnSigIds) if (st.styleLayerExists(id)) st.setStyleLayerProperty(id, "model-scale", sv)
            for (id in furnLampIds) if (st.styleLayerExists(id)) st.setStyleLayerProperty(id, "model-scale", lv)
        } catch (e: Throwable) { softFail(e) }
    }

    /** On a route, the traffic lights that aren't on it are greyed out (faded and dimmed), so the ones that matter stand out. */
    private fun applyNearLook() {
        try {
            // on a route every 3D traffic light is greyed out (the flat badges on the route are what to look at); lit when there's no route
            for ((b, l) in listOf("dl-sign-model" to "dl-sign-lens", "dl-sig-model" to "dl-sig-lens")) {
                val body = layer(b) as? ModelLayer
                val lens = layer(l) as? ModelLayer
                if (routing) {
                    body?.modelOpacity(0.45); body?.modelColor("#9A9EA6"); body?.modelColorMixIntensity(0.7)
                    lens?.modelOpacity(0.45); lens?.modelColor("#7C8088"); lens?.modelColorMixIntensity(1.0); lens?.modelEmissiveStrength(0.0)
                } else {
                    body?.modelOpacity(1.0); body?.modelColorMixIntensity(0.0)
                    lens?.modelOpacity(1.0); lens?.modelColorMixIntensity(0.0); lens?.modelEmissiveStrength(1.0)
                }
            }
        } catch (e: Throwable) { softFail(e) }
    }

    // ---- zebra crossings on the route ----
    /** Each crossing: where, the road's direction there, and whether it's a zebra (with Belisha beacons). */
    private var crossings: List<Triple<LL, Double, Boolean>> = emptyList()

    private fun zebraFeats(): List<Feature> = crossings.map { (p, b, _) ->
        Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat)).also { it.addNumberProperty("r", b) }
    }

    private fun beaconFeats(): List<Feature> = crossings.filter { it.third }.flatMap { (p, b, _) ->
        listOf(-1.0, 1.0).map { side -> val at = Furniture.offset(p, b + 90.0 * side, 5.0); Feature.fromGeometry(Point.fromLngLat(at.lon, at.lat)) }
    }

    /** Stop signs on the route (from OpenStreetMap), drawn as small red octagon badges standing at the junction. */
    private var stopSigns: List<LL> = emptyList()
    private fun stopFeats(): List<Feature> = stopSigns.map { Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat)) }
    fun setStopSigns(list: List<LL>) {
        main0.post {
            stopSigns = list
            try { src("dl-stops")?.featureCollection(FeatureCollection.fromFeatures(stopFeats())) } catch (e: Throwable) { softFail(e) }
        }
    }

    /** A stop sign: a red octagon with a white rim and STOP across it (drawn at 2x, shown at half). */

    fun setCrossings(list: List<Triple<LL, Double, Boolean>>) {
        main0.post {
            crossings = list
            try {
                src("dl-zebras")?.featureCollection(FeatureCollection.fromFeatures(zebraFeats()))
                src("dl-beacons")?.featureCollection(FeatureCollection.fromFeatures(beaconFeats()))
            } catch (e: Throwable) { softFail(e) }
        }
    }

    /** White crossing stripes across the road (the picture's top is along the road; drawn wide, shown to scale by the zoom). */

    /** A Belisha beacon: a black and white pole with an amber globe on top, its foot at the bottom of the picture. */

    // ---- route ribbon and tunnels ----
    /** Settings > Route ribbon: the route's soft shadow and the see-through copy over 3D buildings. */
    private var ribbonOn = true
    fun setRibbon(on: Boolean) { if (on == ribbonOn) return; ribbonOn = on; main0.post { if (style != null) reinstall() } }
    private var tunnelLines: List<List<LL>> = emptyList()

    /** The parts of the route that go through tunnels. */
    fun setTunnels(lines: List<List<LL>>) {
        main0.post {
            tunnelLines = lines
            try { src("dl-tunnels")?.featureCollection(FeatureCollection.fromFeatures(lines.filter { it.size >= 2 }.map { line(it) })) } catch (e: Throwable) { softFail(e) }
        }
    }

    // ---- headlight beam: a soft cone of light on the road ahead of your arrow, after dark, while you move ----
    // One picture (a cone, its tip at the arrow) on one point: the point's place and heading are updated, and only when the arrow has
    // really moved (or turned, or the speed changed the length), at most about 10 times a second. It stays through a short dip in
    // speed instead of being taken off the map and put back.
    private var beamOn = false
    private var beamSpeed = 0.0
    private var beamHold = 0L
    private var beamShown = false
    private var beamAt: LL? = null
    private var beamH = 0.0
    private var beamK = 0.0
    private var beamPushedAt = 0L

    /** Cheap (no work on the map): the engine calls this with every position. */
    fun setBeam(on: Boolean, speedMs: Double, stay: Boolean = false) {
        beamOn = on; beamSpeed = speedMs; beamStay = stay
        // (taken off the map only once it has really stopped or been switched off)
        if (beamShown && (!on || android.os.SystemClock.uptimeMillis() >= beamHold)) main0.post { pushBeam() }
    }
    /** [beamStay]: on a route the beam stays on from the first move to the end, also while stopped at lights. */
    private var beamStay = false

    private fun pushBeam() {
        val s = src("dl-beam") ?: return
        val p = me
        val now = android.os.SystemClock.uptimeMillis()
        if (beamOn && (beamStay || beamSpeed >= 2.0)) beamHold = now + 1500L
        if (!beamOn || p == null || now >= beamHold) {
            if (beamShown) { beamShown = false; beamAt = null; s.featureCollection(empty()) }
            return
        }
        // the length in screen metres: about 30 m at a crawl, 110 m at motorway speed
        val len = (25.0 + beamSpeed * 2.6).coerceIn(30.0, 110.0)
        val k = len * Math.cos(Math.toRadians(p.lat))
        val prev = beamAt
        if (beamShown && prev != null) {
            val dh = Math.abs(((meHeading - beamH + 540.0) % 360.0) - 180.0)
            if (Geo.dist(prev, p) < 0.4 && dh < 0.7 && Math.abs(k - beamK) < 2.0) return
            if (now - beamPushedAt < 100L) return
        }
        // the cone starts at the arrow's own spot and goes out ahead of it along your heading (it used to start 12 m ahead, and at a tilt it
        // could come out behind the car's picture)
        // the built-in car: the beam starts just behind its middle; your own model: at its front (the model is sized to the car's length)
        val ahead = if (modelOn && modelShape == "own") OwnModel.CAR_LENGTH_M * ownSize / 100.0 / 2 - 0.5 else -3.0
        val hr = Math.toRadians(meHeading)
        val q = LL(p.lat + ahead * Math.cos(hr) / 111_320.0, p.lon + ahead * Math.sin(hr) / (111_320.0 * Math.cos(Math.toRadians(p.lat)).coerceAtLeast(0.01)))
        val f = Feature.fromGeometry(Point.fromLngLat(q.lon, q.lat))
        f.addNumberProperty("h", meHeading)
        f.addNumberProperty("k", k)
        s.feature(f)
        beamAt = p; beamH = meHeading; beamK = k; beamShown = true; beamPushedAt = now
    }

    /** The cone, pointing up with its tip at the bottom middle, fading out towards its far end (256 px long). */

    // ---- an imported GPX track, drawn as a line on the map (Settings › Places & trips › GPX tracks) ----
    private var gpxLine: List<LL> = emptyList()

    fun setTrack(pts: List<LL>) {
        main0.post {
            gpxLine = pts
            try { src("dl-gpx")?.featureCollection(FeatureCollection.fromFeatures(if (pts.size >= 2) listOf(line(pts)) else emptyList())) } catch (e: Throwable) { softFail(e) }
        }
    }

    // ---- sharp bends on the route: striped yellow with black chevrons ----
    private var sharpLines: List<List<LL>> = emptyList()

    fun setSharpBends(lines: List<List<LL>>) {
        main0.post {
            sharpLines = lines
            try { src("dl-sharp")?.featureCollection(FeatureCollection.fromFeatures(lines.filter { it.size >= 2 }.map { line(it) })) } catch (e: Throwable) { softFail(e) }
        }
    }

    private fun sharpChevBitmap(): Bitmap {
        val b = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF000000.toInt(); style = Paint.Style.STROKE; strokeWidth = 11f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        c.drawPath(Path().apply { moveTo(18f, 10f); lineTo(42f, 32f); lineTo(18f, 54f) }, p)
        return b
    }

    // ---- average speed zones on the route ----
    private var avgZoneLines: List<List<LL>> = emptyList()
    private var avgCams: List<Pair<LL, Double>> = emptyList()

    private fun avgCamFeats(): List<Feature> = avgCams.map { (p, b) -> val at = Furniture.offset(p, b - 90.0, 5.0); Furniture.point(at.lat, at.lon, b) }

    /** The average speed zones on the route: each as the route's line between its cameras, and the cameras (spot and road direction). */
    fun setAvgZones(lines: List<List<LL>>, cams: List<Pair<LL, Double>>) {
        main0.post {
            avgZoneLines = lines; avgCams = cams
            try {
                src("dl-avg")?.featureCollection(FeatureCollection.fromFeatures(lines.filter { it.size >= 2 }.map { line(it) }))
                src("dl-avg-cams")?.featureCollection(FeatureCollection.fromFeatures(avgCamFeats()))
            } catch (e: Throwable) { softFail(e) }
        }
    }

    // ---- your destination's building, lit up as you get close ----
    private var destLit: com.mapbox.maps.interactions.standard.generated.StandardBuildingsFeature? = null
    private var destLitFor: LL? = null
    private var destTryAt = 0L

    /**
     * Lights up the building at [p] (your destination, from about 400 m away) in Mapbox's building highlight colour; null puts it
     * back to normal. It keeps trying every few seconds until the building is on screen.
     */
    fun highlightDestination(p: LL?) {
        if (p == null) {
            val f = destLit
            destLit = null; destLitFor = null
            if (f != null) try { mbMap.setFeatureState(f, com.mapbox.maps.interactions.standard.generated.StandardBuildingsState { highlight(false) }) } catch (e: Throwable) { softFail(e) }
            return
        }
        if (!slotsOn || satellite) return
        val prev = destLitFor
        if (prev != null && Geo.dist(prev, p) < 5.0 && destLit != null) return
        if (prev == null || Geo.dist(prev, p) >= 5.0) { highlightDestination(null); destLitFor = p }
        val now = android.os.SystemClock.uptimeMillis()
        if (now - destTryAt < 3000L || !canQuery()) return
        destTryAt = now
        try {
            val sc = mbMap.pixelForCoordinate(Point.fromLngLat(p.lon, p.lat))
            if (sc.x < 0 || sc.y < 0 || sc.x > mapView.width || sc.y > mapView.height) return
            val r = 14.0 * mapView.resources.displayMetrics.density
            val box = RenderedQueryGeometry(ScreenBox(ScreenCoordinate(sc.x - r, sc.y - r), ScreenCoordinate(sc.x + r, sc.y + r)))
            mbMap.queryRenderedFeatures(com.mapbox.maps.interactions.standard.generated.StandardBuildings(), box) { list ->
                val f = list.firstOrNull() ?: return@queryRenderedFeatures
                main0.post {
                    if (destLitFor == null || Geo.dist(destLitFor!!, p) >= 5.0) return@post
                    destLit = f
                    try { mbMap.setFeatureState(f, com.mapbox.maps.interactions.standard.generated.StandardBuildingsState { highlight(true) }) } catch (e: Throwable) { softFail(e) }
                }
            }
        } catch (e: Throwable) { softFail(e) }
    }

    private var clearAt: LL? = null
    private var clearTime = 0L

    /** Moves the clear-view area along with you (at most about twice a second, and only once you've moved a few metres). */
    private fun updateClear(force: Boolean) {
        if (!clearOn) return
        val p = me ?: return
        val now = android.os.SystemClock.uptimeMillis()
        val last = clearAt
        if (!force && (now - clearTime < 500L || (last != null && Geo.dist(last, p) < 4.0))) return
        val s = src("dl-clip") ?: return
        clearAt = p; clearTime = now
        try { s.featureCollection(ClearView.shapes(p, meHeading, main)) } catch (e: Throwable) { softFail(e) }
    }

    // ---- tapping buildings and places on Mapbox's map: they light up (Settings > Map > Tap to highlight) ----
    private var selectAt = 0L

    /** A tap that didn't land on a building: whatever was lit up goes back to normal. */
    private fun tapClears() { if (tapSelectOn && android.os.SystemClock.uptimeMillis() - selectAt > 400L) clearSelection() }

    private fun clearSelection() {
        try {
            mbMap.resetFeatureStates(com.mapbox.maps.interactions.standard.generated.StandardBuildings())
            mbMap.resetFeatureStates(com.mapbox.maps.interactions.standard.generated.StandardPoi())
        } catch (e: Throwable) { softFail(e) }
    }

    private fun installTapSelect() {
        try {
            mbMap.addInteraction(com.mapbox.maps.ClickInteraction.standardBuildings { f, _ ->
                if (!tapSelectOn) return@standardBuildings false
                clearSelection()
                selectAt = android.os.SystemClock.uptimeMillis()
                mbMap.setFeatureState(f, com.mapbox.maps.interactions.standard.generated.StandardBuildingsState { select(true) })
                // (not "handled": the app's own tap handling, like the route cards, still gets the tap)
                false
            })
            mbMap.addInteraction(com.mapbox.maps.ClickInteraction.standardPoi { f, _ ->
                if (!tapSelectOn) return@standardPoi false
                // a place without a name or a position in the map's data is ignored (it used to crash the app)
                try {
                    val n = f.name
                    val g = f.geometry
                    if (!n.isNullOrBlank() && g != null) { poiTapAt = android.os.SystemClock.uptimeMillis(); onPoiTap?.invoke(n, LL(g.latitude(), g.longitude())) }
                } catch (e: Throwable) { android.util.Log.w("MapBinding", "place tap: ${e.message}") }
                false
            })
        } catch (e: Throwable) { android.util.Log.w("MapBinding", "tap highlight: ${e.message}") }
    }


    /**
     * The neon look's city, drawn from Mapbox Streets' data (Standard's own buildings are off then): built-up ground in a deep red,
     * the roads as wide cyan bands with a dark middle and a glow, and the buildings as one connected mass of red blocks with a bright
     * edge. Lit as "emissive", so night doesn't darken them. Flat footprints when 3D buildings are off.
     */
    private fun installNeonCity(st: Style) {
        addS(st, VectorSource.Builder("dl-nsrc").url("mapbox://mapbox.mapbox-streets-v8").build())
        val notUnder = Expression.fromRaw("[\"!=\",[\"get\",\"underground\"],\"true\"]")
        // built-up ground, a deep red under everything
        addL(st, FillLayer("dl-nland", "dl-nsrc").sourceLayer("landuse").minZoom(8.0).inSlot("bottom")
            .filter(Expression.fromRaw("[\"match\",[\"get\",\"class\"],[\"residential\",\"commercial_area\",\"industrial\",\"retail\",\"agriculture\"],true,false]"))
            .fillColor("#2A0610").fillOpacity(0.85).fillEmissiveStrength(1.0))
        // roads: a soft glow, the cyan edge and the dark middle (the edge is wider than the middle, so each band shows a cyan outline,
        // and where roads meet the outlines merge)
        val roadFilter = Expression.fromRaw("[\"all\",[\"match\",[\"get\",\"class\"],[\"motorway\",\"motorway_link\",\"trunk\",\"trunk_link\",\"primary\",\"primary_link\",\"secondary\",\"secondary_link\",\"tertiary\",\"tertiary_link\",\"street\",\"street_limited\",\"service\",\"pedestrian\"],true,false],[\"!=\",[\"get\",\"structure\"],\"tunnel\"]]")
        val cls = "[\"match\",[\"get\",\"class\"],[\"motorway\",\"trunk\"],1.0,[\"motorway_link\",\"trunk_link\",\"primary\"],0.8,[\"primary_link\",\"secondary\",\"secondary_link\"],0.65,[\"tertiary\",\"tertiary_link\"],0.55,[\"street\",\"street_limited\"],0.45,0.3]"
        // (the zoom has to be the outermost expression, so the road class goes inside each of its steps)
        fun band(k: Double) = Expression.fromRaw("[\"interpolate\",[\"exponential\",1.5],[\"zoom\"]," +
            listOf(8 to 0.6, 12 to 3.0, 14 to 7.0, 17 to 20.0, 20 to 52.0).joinToString(",") { (z, w) -> "$z,[\"*\",${w * k},$cls]" } + "]")
        addL(st, LineLayer("dl-nroad-glow", "dl-nsrc").sourceLayer("road").minZoom(9.0).inSlot("bottom").filter(roadFilter)
            .lineColor("#19F0FF").lineOpacity(0.16).lineBlur(5.0).lineEmissiveStrength(1.0).lineCap(LineCap.ROUND).lineJoin(LineJoin.ROUND).lineWidth(band(1.9)))
        addL(st, LineLayer("dl-nroad-edge", "dl-nsrc").sourceLayer("road").minZoom(9.0).inSlot("bottom").filter(roadFilter)
            .lineColor("#19F0FF").lineOpacity(0.95).lineBlur(0.6).lineEmissiveStrength(1.0).lineCap(LineCap.ROUND).lineJoin(LineJoin.ROUND).lineWidth(band(1.0)))
        addL(st, LineLayer("dl-nroad-mid", "dl-nsrc").sourceLayer("road").minZoom(9.0).inSlot("bottom").filter(roadFilter)
            .lineColor("#06182B").lineOpacity(1.0).lineEmissiveStrength(1.0).lineCap(LineCap.ROUND).lineJoin(LineJoin.ROUND).lineWidth(band(0.74)))
        // buildings: one connected red mass. A fat outline in the same red closes the gaps between blocks.
        val colour = Expression.fromRaw("[\"interpolate\",[\"linear\"],[\"to-number\",[\"get\",\"height\"],8],0,\"#6E0F1F\",40,\"#85122A\",160,\"#A01734\"]")
        addL(st, LineLayer("dl-nbld-fat", "dl-nsrc").sourceLayer("building").minZoom(13.0).inSlot("middle").filter(notUnder)
            .lineColor("#6E0F1F").lineOpacity(0.95).lineEmissiveStrength(1.0).lineJoin(LineJoin.ROUND)
            .lineWidth(Expression.fromRaw("[\"interpolate\",[\"linear\"],[\"zoom\"],13,1.5,16,5,19,16]")))
        if (buildings3d) {
            addL(st, FillExtrusionLayer("dl-nbld", "dl-nsrc").sourceLayer("building").minZoom(13.0).inSlot("middle")
                .filter(Expression.fromRaw("[\"all\",[\"!=\",[\"get\",\"extrude\"],\"false\"],[\"!=\",[\"get\",\"underground\"],\"true\"]]"))
                .fillExtrusionColor(colour)
                .fillExtrusionHeight(Expression.fromRaw("[\"interpolate\",[\"linear\"],[\"zoom\"],13,0,13.8,[\"to-number\",[\"get\",\"height\"],8]]"))
                .fillExtrusionBase(Expression.fromRaw("[\"interpolate\",[\"linear\"],[\"zoom\"],13,0,13.8,[\"to-number\",[\"get\",\"min_height\"],0]]"))
                .fillExtrusionOpacity(0.97).fillExtrusionVerticalGradient(true).fillExtrusionEmissiveStrength(0.9))
        } else {
            addL(st, FillLayer("dl-nbld", "dl-nsrc").sourceLayer("building").minZoom(13.0).inSlot("middle").filter(notUnder)
                .fillColor(colour).fillOpacity(0.95).fillEmissiveStrength(0.9))
        }
        addL(st, LineLayer("dl-nbld-edge", "dl-nsrc").sourceLayer("building").minZoom(13.5).inSlot("middle").filter(notUnder)
            .lineColor("#FF4A5E").lineOpacity(0.85).lineEmissiveStrength(1.0)
            .lineWidth(Expression.fromRaw("[\"interpolate\",[\"linear\"],[\"zoom\"],13.5,0.4,16,0.9,19,1.8]")))
    }

    /** Takes the app's own layers off the map and puts them back (after a day / night change), leaving Standard as it is. */
    private fun reinstall() {
        val st = style ?: return
        try {
            Sky.clearTerrain(st)
            for (l in st.styleLayers.map { it.id }.filter { it.startsWith("dl-") }.reversed()) try { st.removeStyleLayer(l) } catch (e: Throwable) { softFail(e) }
            for (src in st.styleSources.map { it.id }.filter { it.startsWith("dl-") }) try { st.removeStyleSource(src) } catch (e: Throwable) { softFail(e) }
            install(st)
        } catch (e: Throwable) { android.util.Log.w("MapBinding", "reinstall failed", e) }
    }

    /** Kept so the engine's settings still apply (the Mapbox edition always uses Mapbox's own map look). */
    @Volatile var keepBaseLook = false
    @Volatile var recolourBase = false
    /** (Standard Satellite has its own imagery, so this is no longer used.) */
    @Volatile var satTiles: String? = null


    /**
     * The roads on screen, for snapping your position to the road while off-route (any time) and placing guessed street lamps
     * (at night): Mapbox Streets' road lines, close in only. They're drawn almost invisibly (1% opacity): a fully transparent layer
     * isn't drawn at all, so the map couldn't be asked which roads are on screen.
     */
    private fun installRoadData(st: Style) {
        addS(st, VectorSource.Builder("dl-streets").url("mapbox://mapbox.mapbox-streets-v8").build())
        addL(st, LineLayer("dl-roadq", "dl-streets").sourceLayer("road").inSlot("bottom")
            .filter(Expression.fromRaw("[\"match\",[\"get\",\"class\"],[\"motorway\",\"motorway_link\",\"trunk\",\"trunk_link\",\"primary\",\"secondary\",\"tertiary\",\"street\",\"street_limited\",\"primary_link\",\"secondary_link\",\"tertiary_link\",\"service\"],true,false]"))
            .lineColor("#808080").lineOpacity(0.01).lineWidth(1.0).lineEmissiveStrength(1.0).minZoom(15.0))
        // Mapbox's HD roads (the lanes it paints on motorways, where it has them): the middle line of every lane, for lining the lane
        // guidance up with the painted lanes. Drawn almost invisibly, close in only, so the map can be asked where they are
        try {
            addS(st, VectorSource.Builder("dl-hd").url("mapbox://mapbox.hd-road-v1").build())
            addL(st, LineLayer("dl-hdq", "dl-hd").sourceLayer("hd_road_centerlines").inSlot("bottom")
                .lineColor("#808080").lineOpacity(0.01).lineWidth(1.0).lineEmissiveStrength(1.0).minZoom(15.5))
        } catch (e: Throwable) { softFail(e) }
    }

    /**
     * The real lanes across the road at [p] (going the way [heading] points), from Mapbox's HD roads: each lane's middle, as metres
     * sideways from [p] (+ to the right), left to right. [done] gets null when it isn't on screen, there's no HD road there, or the
     * map didn't answer. On the main thread.
     */
    /**
     * Where the real lanes are (Mapbox's HD lane lines) across the road at each of [samples] (a place and the road's heading
     * there), in metres from the route line, left to right. One map read covers them all. [done] gets one list per sample (empty
     * where nothing was found or that place is off screen) and how many lane lines were read, or null when none of the places is
     * on screen or the map can't be read yet.
     */
    fun laneCentresAlong(samples: List<Pair<LL, Double>>, done: (List<List<Double>>?, Int) -> Unit) {
        main0.post {
            val st = style
            if (st == null || samples.isEmpty() || !canQuery() || !st.styleLayerExists("dl-hdq")) { done(null, 0); return@post }
            val w = mapView.width.toFloat(); val h = mapView.height.toFloat()
            val onScreen = samples.map { (p, _) ->
                val sc = try { mbMap.pixelForCoordinate(Point.fromLngLat(p.lon, p.lat)) } catch (e: Throwable) { null }
                sc != null && sc.x >= 0 && sc.y >= 0 && sc.x <= w && sc.y <= h && !sc.x.isNaN()
            }
            val pts = samples.indices.filter { onScreen[it] }.mapNotNull { i ->
                try { mbMap.pixelForCoordinate(Point.fromLngLat(samples[i].first.lon, samples[i].first.lat)) } catch (e: Throwable) { null }
            }
            if (pts.isEmpty()) { done(null, 0); return@post }
            val m = 90f * mapView.resources.displayMetrics.density
            val box = RectF((pts.minOf { it.x }.toFloat() - m).coerceAtLeast(0f), (pts.minOf { it.y }.toFloat() - m).coerceAtLeast(0f),
                (pts.maxOf { it.x }.toFloat() + m).coerceAtMost(w), (pts.maxOf { it.y }.toFloat() + m).coerceAtMost(h))
            query(box, listOf("dl-hdq"), 3000L) { feats ->
                if (feats == null) { done(null, 0); return@query }
                val lines = ArrayList<List<LL>>()
                for (f in feats) when (val g = f.geometry()) {
                    is LineString -> lines += g.coordinates().map { LL(it.latitude(), it.longitude()) }
                    is MultiLineString -> for (c in g.coordinates()) lines += c.map { LL(it.latitude(), it.longitude()) }
                    else -> { }
                }
                done(samples.mapIndexed { i, (p, hd) -> if (onScreen[i]) MapRouteGeometry.laneOffsets(lines, p, hd) else emptyList() }, lines.size)
            }
        }
    }

    private var roadsAt = 0L
    private var roadsQuerying = false

    /** Reads the roads on screen (every few seconds at most, in the background), for snapping your position to the road. */
    private fun refreshRoads() {
        val st = style ?: return
        if (roadsQuerying || !canQuery() || !st.styleLayerExists("dl-roadq")) return
        val now = android.os.SystemClock.uptimeMillis()
        if (now - roadsAt < 4000L) return
        if ((try { mbMap.cameraState.zoom } catch (e: Throwable) { 0.0 }) < 15.0) return
        roadsAt = now; roadsQuerying = true
        query(RectF(0f, 0f, mapView.width.toFloat(), mapView.height.toFloat()), listOf("dl-roadq")) { feats ->
            roadsQuerying = false
            if (feats != null) rememberRoads(feats)
        }
    }

    // ---- street lamps: real 3D lamp posts (the lamp model in assets/models), their lamps lit at night ----
    //
    // Where they stand: the lamps surveyed in OpenStreetMap (the offline pack's lamps.tsv). Where nobody has mapped any (most of
    // the UK), lamps are placed along the streets on screen instead, a set distance apart at the kerb, arm over the road. Shown by
    // day and night (only lit at night), from zoom 15. Settings > On the map > Street lamps shows what they're doing.

    /** Settings > On the map > Street lamps at night. */
    var lampsOn = true
        private set
    /** How far apart lamps are placed along streets with no mapped lamps, in metres (Settings > Street lamp spacing). */
    var lampGap = 40.0
        private set
    /** What the lamps are doing now, for Settings. */
    @Volatile var lampStage = Lang.t("warte auf die Karte", "waiting for the map")
    fun lampInfo(): String = if (!lampsOn) "off" else lampStage

    /** Streets lamps stand along (not motorways or tunnels), from the last look at the roads on screen. */
    @Volatile private var lampRoads: List<List<Point>> = emptyList()
    @Volatile private var lampRoadsNew = false
    private var lampsCentre: LL? = null
    private var lampsAt = 0L
    @Volatile private var lampsBusy = false
    private val lampExec = java.util.concurrent.Executors.newSingleThreadExecutor { r -> Thread(r, "lamps").apply { isDaemon = true; priority = Thread.MIN_PRIORITY } }

    /** "Always night" by day: the map is dark, but the lamps are only lit once it is really dark outside (NavEngine.darkOutside). */
    var outsideDark = true
        private set
    fun setOutsideDark(on: Boolean) {
        if (on == outsideDark) return
        outsideDark = on; lampsCentre = null
        main0.post { if (style != null && lampsOn) reinstall() }
    }

    fun lampsEnabled(on: Boolean) {
        if (on == lampsOn) return
        lampsOn = on; lampsCentre = null
        main0.post { if (style != null) reinstall() }
    }

    fun applyLampGap(m: Double) {
        if (m == lampGap) return
        lampGap = m.coerceIn(10.0, 200.0); lampsCentre = null
        main0.post { refreshLamps(true) }
    }

    private fun installLamps(st: Style) {
        lampsCentre = null
        if (!lampsOn) { lampStage = "off"; return }
        Furniture.addModels(st)
        val night = nightStyle && outsideDark
        addS(st, GeoJsonSource.Builder("dl-lamps").featureCollection(empty()).build())
        addS(st, GeoJsonSource.Builder("dl-lamp-pools").featureCollection(empty()).build())
        // at night, a soft warm pool of light on the road under each lamp (about 9 m across, growing with the map as you zoom)
        if (night) addL(st, CircleLayer("dl-lamp-pool", "dl-lamp-pools").inSlot(MID)
            .circleRadius(zoomed(2.0, 15 to 5.5, 22 to 5.5 * 128)).circleBlur(1.0).circleColor("#FFD9A0").circleOpacity(0.38)
            .circlePitchAlignment(CirclePitchAlignment.MAP).circlePitchScale(CirclePitchScale.MAP).circleEmissiveStrength(1.0).minZoom(15.0))
        // the lamp post itself, and its lamp (lit up at night)
        addL(st, Furniture.layer("dl-lamp-post", "dl-lamps", Furniture.LAMP, if (night) 0.3 else 0.0, 15.0, lampSize()).inSlot(MID))
        addL(st, Furniture.layer("dl-lamp-light", "dl-lamps", Furniture.LAMP_LENS, if (night) 1.0 else 0.15, 15.0, lampSize()).inSlot(MID))
        lampStage = Lang.t("warte auf die Karte", "waiting for the map")
        main0.postDelayed({ refreshLamps(true) }, 300L)
    }

    /** Looks for the lamps round the middle of the screen again once you've moved on a bit (the work is done in the background). */
    private fun refreshLamps(force: Boolean = false) {
        if (lampsBusy) return
        val st = style ?: return
        val wantLamps = prefs.b(PreferenceKeys.LAMPS, false) && st.styleSourceExists("dl-lamps")
        val wantLights = lights.on && st.styleSourceExists("dl-signals-near")
        if (!wantLamps && !wantLights) return
        val cam = try { mbMap.cameraState } catch (e: Throwable) { return }
        if (cam.zoom < 14.0) { if (wantLamps) lampStage = Lang.t("näher heranzoomen, um sie zu sehen", "zoom in closer to see them"); return }
        val c = LL(cam.center.latitude(), cam.center.longitude())
        val now = android.os.SystemClock.uptimeMillis()
        val last = lampsCentre
        val again = force || lampRoadsNew || last == null || Geo.dist(last, c) > 120.0
        if (!again || now - lampsAt < 1200L) return
        lampsBusy = true; lampsAt = now; lampsCentre = c; lampRoadsNew = false
        val roads = lampRoads; val gap = lampGap; val night = nightStyle && outsideDark
        try {
            lampExec.execute {
                try {
                    val (posts, pools, note) = if (wantLamps) LampPlacement.place(ctx, c, roads, gap, prefs.b(PreferenceKeys.LAMPS_ONLINE, true)) else Triple(emptyList(), emptyList(), "off")
                    if (wantLamps) lampStage = note
                    val lightDots = if (wantLights) LampPlacement.nearLights(ctx, c, lights.kept, roadInfo) else emptyList()
                    main0.post {
                        lampsBusy = false
                        if (destroyed) return@post
                        if (wantLamps) {
                            src("dl-lamps")?.featureCollection(FeatureCollection.fromFeatures(posts))
                            if (night) src("dl-lamp-pools")?.featureCollection(FeatureCollection.fromFeatures(pools))
                        }
                        if (wantLights) src("dl-signals-near")?.featureCollection(FeatureCollection.fromFeatures(lightDots))
                    }
                } catch (e: Throwable) {
                    lampStage = "error: ${e.javaClass.simpleName} ${e.message ?: ""}".take(120)
                    main0.post { lampsBusy = false }
                }
            }
        } catch (e: Throwable) { lampsBusy = false }
    }







    // ---- traffic lights (OpenStreetMap or the offline pack, along the whole route; the search is in TrafficLights.kt) ----
    private val lights = TrafficLights(ctx, main0, { main }, { destroyed }, { fc, models, kept ->
        src("dl-signals")?.featureCollection(fc)
        src("dl-signals-m")?.featureCollection(models)
        try { onRouteSignals?.invoke(kept) } catch (e: Throwable) { softFail(e) }
    }, { cams -> try { onRouteCameras?.invoke(cams) } catch (e: Throwable) { softFail(e) } })
    /** Gets every speed camera found along routes so far, each time more arrive. */
    var onRouteCameras: ((List<LL>) -> Unit)? = null
    /** The map was dragged to a new place: where its middle is now (so the speed cameras there can be looked up). */
    var onViewMoved: ((LL) -> Unit)? = null
    /** The map's bearing in degrees (0 = north up), whenever the camera moves. */
    var onBearing: ((Double) -> Unit)? = null

    /** One line for the settings page: how many lights are known and whether the last fetch worked. */
    fun signalInfo(): String {
        if (!lights.on) return "off"
        if (main.size < 2) return Lang.t("keine Route (Ampeln erscheinen nur auf deiner Route)", "no route (lights only show on your route)")
        return "${lights.shown} on the drawn route, ${lights.found} found along it" + (if (lights.error.isNotEmpty()) ", last error: ${lights.error}" else "") + ", status: ${lights.stage}"
    }

    /** A short version for the performance meter. */
    fun signalShort(): String {
        if (!lights.on) return "off"
        if (main.size < 2) return Lang.t("keine Route", "no route")
        return "${lights.shown} shown/${lights.found} found" + (if (lights.error.isNotEmpty()) " (${lights.error})" else "") + " · ${lights.stage}"
    }

    fun signalsEnabled(on: Boolean) {
        lights.on = on
        // (the search along the route carries on either way: it also finds the speed cameras)
        lights.push()
        if (!on) try { src("dl-signals-near")?.featureCollection(empty()) } catch (e: Throwable) { softFail(e) }
        else main0.post { lampsCentre = null; refreshLamps(true) }
    }









    private fun empty() = FeatureCollection.fromFeatures(ArrayList<Feature>())

    // ---- the app's own layers ----
    /** Middle of Standard: above roads, below 3D buildings and labels. Top: above shop labels, below place names. */
    private val MID = "middle"
    private val BOT = "bottom"
    private val TOP = "top"

    // layers changed often (every frame or on a timer) are kept, so they don't have to be looked up each time
    private var flowLayer: LineLayer? = null
    private var glowLayer: LineLayer? = null
    private var lanesOnLayer: SymbolLayer? = null

    private fun install(st: Style) {
        if (satellite) addSatellite(st)
        val im = theme == "immersive"
        val dayLook = !nightStyle
        bubImgs.clear(); laneImgs.clear()
        // the classic map in the Horizon colours
        if (!slotsOn && (colours == "horizon" || recolourBase)) try { recolourClassic(st) } catch (e: Throwable) { softFail(e) }
        // Standard's newer settings are set once it's loaded (so a style that doesn't know one can't stop the map loading)
        if (!satellite) {
            setBase("show3dTrees", Value.valueOf(treesOn)); setBase("showLandmarkIcons", Value.valueOf(!lite))
            // lane markings, turn arrows, stop lines and 3D overpasses where Mapbox has them (Full 3D only)
            setBase("showHdRoads", Value.valueOf(hdRoads))
            // detailed building fronts, landmark buildings, footpaths (Settings > Map)
            setBase("show3dFacades", Value.valueOf(facadesOn && b3dStd() && MapStylePolicy.supportsFullDetailFeatures(mapDetail)))
            setBase("show3dLandmarks", Value.valueOf(landmarksOn && !neonMap && MapStylePolicy.supportsFullDetailFeatures(mapDetail)))
            setBase("showPedestrianRoads", Value.valueOf(pathsOn))
            // your destination's building is lit up in the app's blue as you get close
            setBase("colorBuildingHighlight", Value.valueOf("#7FB3FF"))
        }
        // live traffic on the other roads (Mapbox Traffic): slow, heavy and stopped traffic coloured, and free-flowing roads in
        // green too unless that's switched off (Settings > Map), under the route
        addS(st, VectorSource.Builder("dl-traffic-src").url("mapbox://mapbox.mapbox-traffic-v1").build())
        addL(st, LineLayer("dl-traffic", "dl-traffic-src").sourceLayer("traffic").inSlot(MID)
            .filter(trafficFilter())
            .lineColor(Expression.fromRaw("[\"match\",[\"get\",\"congestion\"],\"low\",\"#1EA362\",\"moderate\",\"#F9AB00\",\"heavy\",\"#E8453C\",\"severe\",\"#A51D1D\",\"#F9AB00\"]"))
            .lineWidth(zoomed(1.5, 8 to 1.0, 13 to 2.5, 16 to 4.5, 19 to 9.0))
            .lineOffset(zoomed(1.5, 10 to 0.5, 16 to 2.5, 19 to 5.0))
            .lineOpacity(0.85).lineEmissiveStrength(1.0)
            .lineCap(LineCap.ROUND).lineJoin(LineJoin.ROUND)
            .minZoom(8.0)
            .visibility(vis(trafficOn && !(routing && hideTraffic))))
        addS(st, GeoJsonSource.Builder("dl-alts").featureCollection(empty()).build())
        addS(st, GeoJsonSource.Builder("dl-pick").featureCollection(empty()).build())
        addS(st, GeoJsonSource.Builder("dl-live-alts").featureCollection(empty()).build())
        addS(st, GeoJsonSource.Builder("dl-live-bub").featureCollection(empty()).build())
        // the route line keeps every bend: Mapbox's default simplifies a long line at low zoom, which cut the corners
        addS(st, GeoJsonSource.Builder("dl-route").featureCollection(empty()).tolerance(0.1).build())
        addS(st, GeoJsonSource.Builder("dl-flow").featureCollection(empty()).lineMetrics(true).build())
        addS(st, GeoJsonSource.Builder("dl-done").featureCollection(empty()).build())
        addS(st, GeoJsonSource.Builder("dl-dest").featureCollection(empty()).build())
        addS(st, GeoJsonSource.Builder("dl-via").featureCollection(empty()).build())
        addS(st, GeoJsonSource.Builder("dl-hazards").featureCollection(empty()).build())
        addS(st, GeoJsonSource.Builder("dl-me").featureCollection(empty()).build())
        // the arrow at the next turn, drawn on the route; no simplification, so small shapes keep their corners
        addS(st, GeoJsonSource.Builder("dl-turn").featureCollection(empty()).tolerance(0.1).build())
        addS(st, GeoJsonSource.Builder("dl-turn-head").featureCollection(empty()).build())
        addS(st, GeoJsonSource.Builder("dl-lanes").featureCollection(empty()).build())
        // every lane arrow added now in one go (they're small), so nothing new is added to the map as you come up to a junction
        try {
            for (d in listOf("STRAIGHT", "SLIGHT_LEFT", "LEFT", "SHARP_LEFT", "LEFT_U_TURN", "SLIGHT_RIGHT", "RIGHT", "SHARP_RIGHT", "RIGHT_U_TURN"))
                for (on in listOf(true, false)) { val n = "ln-$d-${if (on) 1 else 0}"; addImg(st, n, laneBitmap(d, on)); laneImgs.add(n) }
        } catch (e: Throwable) { laneImgs.clear() }
        // lane bands: each lane drawn as a strip along the road before a junction, the lane(s) you need lit, the others dimmed, with dashed dividers
        addS(st, GeoJsonSource.Builder("dl-lane-bands").featureCollection(empty()).build())
        addS(st, GeoJsonSource.Builder("dl-lane-divs").featureCollection(empty()).build())
        fun ln(id: String, src: String) = LineLayer(id, src).inSlot(MID).lineEmissiveStrength(1.0).lineCap(LineCap.ROUND).lineJoin(LineJoin.ROUND)
        // average speed zones on the route: the stretch between the cameras shaded yellow, with dashed yellow edges (under the route)
        addS(st, GeoJsonSource.Builder("dl-avg").featureCollection(FeatureCollection.fromFeatures(avgZoneLines.filter { it.size >= 2 }.map { line(it) })).build())
        val bandW = zoomed(2.0, 12 to 8.0, 16 to 30.0, 20 to 480.0)
        addL(st, LineLayer("dl-avg-band", "dl-avg").inSlot(MID).lineEmissiveStrength(1.0).lineCap(LineCap.BUTT).lineJoin(LineJoin.ROUND)
            .lineColor("#FFCC00").lineOpacity(if (nightStyle) 0.30 else 0.38).lineWidth(bandW))
        for ((id, side) in listOf("dl-avg-edge-l" to -1.0, "dl-avg-edge-r" to 1.0)) addL(st, LineLayer(id, "dl-avg").inSlot(MID).lineEmissiveStrength(1.0)
            .lineCap(LineCap.BUTT).lineJoin(LineJoin.ROUND).lineColor("#FFCC00").lineOpacity(0.95)
            .lineWidth(zoomed(2.0, 12 to 1.2, 16 to 2.6, 20 to 8.0)).lineDasharray(listOf(3.0, 2.2))
            .lineOffset(zoomed(2.0, 12 to 4.0 * side, 16 to 15.0 * side, 20 to 150.0 * side)))
        // zebra crossings on the route: white stripes painted across the road, with Belisha beacons at each side (close in only)
        addImg(st, "dl-zebra", zebraBitmap()); addImg(st, "dl-beacon", beaconBitmap())
        addS(st, GeoJsonSource.Builder("dl-zebras").featureCollection(FeatureCollection.fromFeatures(zebraFeats())).build())
        addS(st, GeoJsonSource.Builder("dl-beacons").featureCollection(FeatureCollection.fromFeatures(beaconFeats())).build())
        addL(st, SymbolLayer("dl-zebra", "dl-zebras").inSlot(MID).iconImage("dl-zebra").iconAllowOverlap(true).iconIgnorePlacement(true)
            .iconPitchAlignment(IconPitchAlignment.MAP).iconRotationAlignment(IconRotationAlignment.MAP).iconRotate(Expression.get("r"))
            // (sized to about 9 m across the road: the picture is 200 px wide, shown at the screen's density)
            .iconSize(run { val d = mapView.resources.displayMetrics.density.toDouble(); zoomed(2.0, 16 to 0.06 * d, 20 to 0.96 * d) })
            .iconOpacity(0.92).iconEmissiveStrength(0.8).minZoom(16.0))
        // stop signs on the route: a small badge that stands up at the junction (from zoom 14)
        addImg(st, "dl-stop", stopBitmap())
        // 3D flags at the destination and the stops (see flagFeats)
        addS(st, GeoJsonSource.Builder("dl-flags").featureCollection(FeatureCollection.fromFeatures(flagFeats())).build())
        addL(st, FillExtrusionLayer("dl-flag", "dl-flags").inSlot("middle").minZoom(12.0)
            .fillExtrusionColor(Expression.fromRaw("[\"get\",\"c\"]")).fillExtrusionBase(Expression.fromRaw("[\"get\",\"b\"]"))
            .fillExtrusionHeight(Expression.fromRaw("[\"get\",\"h\"]")).fillExtrusionOpacity(0.95).fillExtrusionEmissiveStrength(1.0))
        addS(st, GeoJsonSource.Builder("dl-stops").featureCollection(FeatureCollection.fromFeatures(stopFeats())).build())
        addL(st, SymbolLayer("dl-stop", "dl-stops").inSlot(TOP).iconImage("dl-stop").iconAllowOverlap(true).iconIgnorePlacement(true)
            .iconAnchor(IconAnchor.BOTTOM).iconPitchAlignment(IconPitchAlignment.VIEWPORT)
            .iconSize(zoomed(1.5, 14 to 0.35, 17 to 0.55, 20 to 0.8)).iconEmissiveStrength(1.0).minZoom(14.0))
        addL(st, SymbolLayer("dl-beacon", "dl-beacons").inSlot(TOP).iconImage("dl-beacon").iconAllowOverlap(true).iconIgnorePlacement(true)
            .iconAnchor(IconAnchor.BOTTOM).iconPitchAlignment(IconPitchAlignment.VIEWPORT)
            .iconSize(zoomed(1.6, 16 to 0.32, 20 to 0.9)).iconEmissiveStrength(1.0).minZoom(16.0))
        addL(st, ln("dl-alts-line", "dl-alts").lineColor(prefs.s(PreferenceKeys.ALT_ROUTE_COLOUR, "#C4D9E6")).lineWidth(if (im) 8.0 else 7.0).lineOpacity(0.92))
        // a route picked once on the map while choosing (yellow): a second tap on it takes it
        addL(st, ln("dl-pick-line", "dl-pick").lineColor("#FFCC00").lineWidth(if (im) 10.0 else 9.0).lineOpacity(0.95))
        // other ways to go while driving: grey, under the route
        addL(st, ln("dl-live-alts-line", "dl-live-alts").lineColor(prefs.s(PreferenceKeys.ALT_ROUTE_COLOUR, "#C4D9E6")).lineWidth(routeW(0.75f)).lineOpacity(0.92))
        // route ribbon: a soft shadow just below the line, so it looks lifted off the road (Full 3D, Settings > Route ribbon)
        if (ribbonOn && !lite) addL(st, ln("dl-route-shadow", "dl-route").lineColor("#000000").lineOpacity(if (dayLook) 0.22 else 0.35)
            .lineWidth(routeW(if (im) 1.2f else 1.1f)).lineBlur(5.0).lineTranslate(listOf(0.0, 5.0))
            .lineTranslateAnchor(com.mapbox.maps.extension.style.layers.properties.generated.LineTranslateAnchor.VIEWPORT))
        // the wide blurred glow under the route is one of the most expensive things to draw; the light map leaves it out
        val glow = ln("dl-route-glow", "dl-route").lineColor(sevColor(im, dayLook)).lineWidth(routeW(if (im) 1.45f else 1.3f)).lineBlur(4.0)
            .lineOpacity(if (im) 0.62 else 0.4).visibility(vis(!lite))
        addL(st, glow); glowLayer = glow
        // by day a light blue edge, but deep enough to show against the light map
        addL(st, ln("dl-route-casing", "dl-route").lineColor(if (dayLook) "#8AB6F0" else if (im) "#D4F4FF" else "#eaf2ff")
            .lineWidth(routeW(if (im) 1.5f else 1.25f)).lineOpacity(if (dayLook) 1.0 else if (im) 0.95 else 0.92))
        addL(st, ln("dl-route-line", "dl-route").lineColor(sevColor(im, dayLook)).lineWidth(routeW(if (im) 1.0f else 0.85f)))
        // ...and the route showing faintly through 3D buildings that stand in front of it: a see-through copy drawn above them
        if (ribbonOn && slotsOn && MapStylePolicy.supportsFullDetailFeatures(mapDetail)) addL(st, LineLayer("dl-route-xray", "dl-route").inSlot(TOP).lineEmissiveStrength(1.0)
            .lineCap(LineCap.ROUND).lineJoin(LineJoin.ROUND).lineColor(sevColor(im, dayLook)).lineWidth(routeW(0.7f)).lineOpacity(0.28))
        // tunnels on the route: the line turns dashed and see-through there (pale dashes over it)
        addS(st, GeoJsonSource.Builder("dl-tunnels").featureCollection(FeatureCollection.fromFeatures(tunnelLines.filter { it.size >= 2 }.map { line(it) })).build())
        addL(st, ln("dl-tunnel-dash", "dl-tunnels").lineCap(LineCap.BUTT).lineColor(if (dayLook) "#EEF2F7" else "#2B3150")
            .lineWidth(routeW(if (im) 1.55f else 1.3f)).lineOpacity(0.6).lineDasharray(listOf(0.9, 1.1)))
        // the headlight beam: one picture of a cone on one point (see pushBeam)
        addImg(st, "dl-beam-img", beamBitmap())
        addS(st, GeoJsonSource.Builder("dl-beam").featureCollection(empty()).build())
        addL(st, SymbolLayer("dl-beam-l", "dl-beam").inSlot(BOT)
            .iconImage("dl-beam-img").iconAnchor(IconAnchor.BOTTOM).iconRotate(Expression.get("h"))
            .iconRotationAlignment(IconRotationAlignment.MAP).iconPitchAlignment(IconPitchAlignment.MAP)
            .iconAllowOverlap(true).iconIgnorePlacement(true).iconEmissiveStrength(1.0)
            // (its 256 px are the beam's length: screen metres times the map's pixels per metre at this zoom, over 256)
            .iconSize(Expression.product(zoomed(2.0, 10 to 2.555e-5, 20 to 2.6165e-2), Expression.get("k"))))
        beamShown = false; beamAt = null
        // an imported GPX track: a line in pink under the route, so the route stays on top of it
        addS(st, GeoJsonSource.Builder("dl-gpx").featureCollection(FeatureCollection.fromFeatures(if (gpxLine.size >= 2) listOf(line(gpxLine)) else emptyList())).build())
        addL(st, ln("dl-gpx-l", "dl-gpx").lineColor("#E91E63").lineWidth(routeW(if (im) 1.2f else 1.0f)).lineOpacity(0.85))
        // sharp bends: the line striped yellow and black, with black chevrons along it
        addS(st, GeoJsonSource.Builder("dl-sharp").featureCollection(FeatureCollection.fromFeatures(sharpLines.filter { it.size >= 2 }.map { line(it) })).build())
        addL(st, ln("dl-sharp-yellow", "dl-sharp").lineCap(LineCap.BUTT).lineColor("#FFCC00").lineWidth(routeW(if (im) 1.0f else 0.85f)))
        addL(st, ln("dl-sharp-black", "dl-sharp").lineCap(LineCap.BUTT).lineColor("#111111").lineWidth(routeW(if (im) 1.0f else 0.85f)).lineDasharray(listOf(1.0, 1.0)))
        addImg(st, "dl-sharp-chev", sharpChevBitmap())
        addL(st, SymbolLayer("dl-sharp-chevs", "dl-sharp").inSlot(MID)
            .symbolPlacement(SymbolPlacement.LINE).iconImage("dl-sharp-chev").symbolSpacing(60.0)
            .iconAllowOverlap(true).iconIgnorePlacement(true)
            .iconRotationAlignment(IconRotationAlignment.MAP).iconPitchAlignment(IconPitchAlignment.MAP)
            .iconEmissiveStrength(1.0).iconSize(zoomed(1.0, 13 to 0.4, 16 to 0.8, 19 to 1.2)).minZoom(12.0))
        // a band of light that runs along the route towards the destination
        val flow = ln("dl-flow-line", "dl-flow").lineWidth(routeW(if (im) 0.6f else 0.5f)).lineBlur(3.0)
            .lineGradient(flowGradient(0.5f)).visibility(vis(flowOn))
        addL(st, flow); flowLayer = flow
        // small direction chevrons along the route line (only close in, where they help)
        addImg(st, "dl-chev", chevronBitmap())
        addL(st, SymbolLayer("dl-route-chev", "dl-route").inSlot(MID)
            .symbolPlacement(SymbolPlacement.LINE).iconImage("dl-chev").symbolSpacing(110.0)
            .iconAllowOverlap(true).iconIgnorePlacement(true)
            .iconRotationAlignment(IconRotationAlignment.MAP).iconPitchAlignment(IconPitchAlignment.MAP)
            .iconOpacity(0.75).iconEmissiveStrength(1.0)
            .iconSize(zoomed(1.0, 15 to 0.28, 18 to 0.5, 20 to 0.7))
            .minZoom(15.0).visibility(vis(chevOn)))
        // the turn arrow: (by day) a soft dark halo so it stands out on the light map, a dark edge, a white shaft and a white head
        if (dayLook) addL(st, ln("dl-turn-soft", "dl-turn").lineColor("#16467A").lineWidth(routeW(1.62f)).lineBlur(routeW(0.3f)).lineOpacity(0.35).minZoom(13.0))
        addL(st, ln("dl-turn-case", "dl-turn").lineColor("#16467A").lineWidth(routeW(1.28f)).minZoom(13.0))
        addL(st, ln("dl-turn-line", "dl-turn").lineColor("#FFFFFF").lineWidth(routeW(1.08f)).minZoom(13.0))
        addImg(st, "dl-turn-head-img", turnHeadBitmap(dayLook))
        addL(st, SymbolLayer("dl-turn-head", "dl-turn-head").inSlot(MID)
            .iconImage("dl-turn-head-img").iconRotate(Expression.get("b"))
            .iconRotationAlignment(IconRotationAlignment.MAP).iconPitchAlignment(IconPitchAlignment.MAP)
            .iconAllowOverlap(true).iconIgnorePlacement(true).iconEmissiveStrength(1.0)
            // the head's base sits exactly on the end of the shaft, and grows with the zoom the same way the line does
            .iconAnchor(IconAnchor.BOTTOM)
            .iconSize(zoomed(1.7, 10 to headSize(3f), 13 to headSize(6.5f), 15 to headSize(10f), 17 to headSize(16f), 19 to headSize(24f)))
            .minZoom(13.0))
        // the part of the route you have already driven turns grey, drawn over the blue
        addL(st, ln("dl-done-casing", "dl-done").lineColor(if (im) "#B4B9CE" else "#C3C8D8").lineWidth(routeW(if (im) 1.5f else 1.25f)).lineOpacity(0.9).lineCap(LineCap.BUTT))
        addL(st, ln("dl-done-line", "dl-done").lineColor(if (im) "#7E849F" else "#8C92AC").lineWidth(routeW(if (im) 1.0f else 0.85f)).lineCap(LineCap.BUTT))
        // lane ribbons: the lane you need is a solid teal ribbon that fades away into the distance, the others are quiet road-coloured strips
        val night = nightStyle
        val onCol = if (night) "#2FC9B8" else "#1FB5A8"
        val offCol = if (night) "#4A5062" else "#FBFAF7"
        val isOn = Expression.eq(Expression.get("on"), lit(1))
        addL(st, FillLayer("dl-lane-fill", "dl-lane-bands").inSlot(MID)
            .fillColor(Expression.switchCase(isOn, col(onCol), col(offCol)))
            .fillOpacity(Expression.product(Expression.toNumber(Expression.get("a")), Expression.switchCase(isOn, lit(0.95), lit(if (night) 0.75 else 0.85))))
            .fillEmissiveStrength(1.0).minZoom(15.5))
        val lineCol = if (night) "#FFFFFF" else "#6A7080"
        addL(st, LineLayer("dl-lane-edge", "dl-lane-divs").inSlot(MID).filter(Expression.eq(Expression.get("s"), lit(1)))
            .lineColor(lineCol).lineOpacity(0.7).lineEmissiveStrength(1.0)
            .lineWidth(zoomed(2.0, 15 to 0.15, 20 to 2.5)).minZoom(15.5))
        addL(st, LineLayer("dl-lane-div", "dl-lane-divs").inSlot(MID).filter(Expression.eq(Expression.get("s"), lit(0)))
            .lineColor(lineCol).lineOpacity(0.75).lineEmissiveStrength(1.0).lineDasharray(listOf(3.0, 4.0))
            .lineWidth(zoomed(2.0, 15 to 0.15, 20 to 2.0)).minZoom(15.5))
        for ((id, on) in listOf("dl-lanes-off" to 0, "dl-lanes-on" to 1)) {
            val l = SymbolLayer(id, "dl-lanes").inSlot(MID).filter(Expression.eq(Expression.get("on"), lit(on)))
                .iconImage(Expression.get("img")).iconRotate(Expression.get("rot"))
                .iconRotationAlignment(IconRotationAlignment.MAP).iconPitchAlignment(IconPitchAlignment.MAP)
                .iconAllowOverlap(true).iconIgnorePlacement(true).iconEmissiveStrength(1.0)
                .iconSize(zoomed(2.0, 15 to 0.1, 20 to 2.6))
            addL(st, l)
            if (on == 1) lanesOnLayer = l
        }
        // street lamps (3D models, see StreetLamps below); and the road data used for lamps and road snapping
        installLamps(st)
        installRoadData(st)
        // traffic lights (icons stand on the road, above shop labels)
        addImg(st, "dl-signal", signalBitmap())
        addS(st, GeoJsonSource.Builder("dl-signals").featureCollection(empty()).build())
        addL(st, SymbolLayer("dl-signals", "dl-signals").inSlot(TOP)
            .iconImage("dl-signal").iconAllowOverlap(true).iconIgnorePlacement(true)
            .iconPitchAlignment(IconPitchAlignment.VIEWPORT).iconEmissiveStrength(1.0)
            .iconSize(zoomed(1.0, 8 to 0.28, 13 to 0.34, 15 to 0.38, 17 to 0.5, 19 to 0.64, 21 to 0.76))
            .iconAnchor(IconAnchor.BOTTOM))
        // speed cameras, roadworks and closures on the route
        for ((kind, nm) in hazardNames.withIndex()) addImg(st, nm, hazardBitmap(ctx, kind))
        // car parks with their free spaces (a blue P and the count), from city zoom in
        garageImgs.clear()
        addS(st, GeoJsonSource.Builder("dl-garages").featureCollection(empty()).build())
        addL(st, SymbolLayer("dl-garages", "dl-garages")
            .iconImage(Expression.get("img")).iconAllowOverlap(false).iconIgnorePlacement(false)
            .iconPitchAlignment(IconPitchAlignment.VIEWPORT).iconEmissiveStrength(1.0).iconAnchor(IconAnchor.BOTTOM)
            .iconSize(zoomed(1.0, 13 to 0.42, 15 to 0.55, 18 to 0.7)).minZoom(13.0))
        // every branch of a chain ("Alle Standorte"): its logo with the drive time, above everything else
        favImgs.clear()
        addS(st, GeoJsonSource.Builder("dl-fav").featureCollection(empty()).build())
        addL(st, SymbolLayer("dl-fav", "dl-fav").inSlot(TOP)
            .iconImage(Expression.get("img")).iconAllowOverlap(true).iconIgnorePlacement(true)
            .iconPitchAlignment(IconPitchAlignment.VIEWPORT).iconEmissiveStrength(1.0).iconAnchor(IconAnchor.BOTTOM).iconSize(0.6))
        pushFav()
        savedImgs.clear()
        addS(st, GeoJsonSource.Builder("dl-saved").featureCollection(empty()).build())
        addL(st, SymbolLayer("dl-saved", "dl-saved").inSlot(TOP)
            .iconImage(Expression.get("img")).iconAllowOverlap(true).iconIgnorePlacement(true)
            .iconPitchAlignment(IconPitchAlignment.VIEWPORT).iconEmissiveStrength(1.0).iconAnchor(IconAnchor.BOTTOM).iconSize(1.0))
        pushSaved()
        chainImgs.clear()
        addS(st, GeoJsonSource.Builder("dl-chain").featureCollection(empty()).build())
        addL(st, SymbolLayer("dl-chain", "dl-chain").inSlot(TOP)
            .iconImage(Expression.get("img")).iconAllowOverlap(true).iconIgnorePlacement(true).symbolSortKey(Expression.get("z"))
            .iconPitchAlignment(IconPitchAlignment.VIEWPORT).iconEmissiveStrength(1.0).iconAnchor(IconAnchor.BOTTOM).iconSize(0.5))
        pushChain()
        // every speed camera around you, route or not: a small camera badge on the road (the route's own ones sit on top, bigger)
        addS(st, GeoJsonSource.Builder("dl-alpr").featureCollection(empty()).build())
        addL(st, CircleLayer("dl-alpr-l", "dl-alpr")
            .circleColor("#7C4DFF").circleStrokeColor("#FFFFFF").circleStrokeWidth(1.5)
            .circleRadius(zoomed(1.0, 10 to 4.0, 14 to 6.0, 18 to 9.0)).circleEmissiveStrength(1.0))
        addS(st, GeoJsonSource.Builder("dl-cams").featureCollection(empty()).build())
        addL(st, SymbolLayer("dl-cams", "dl-cams")
            .iconImage("dl-hz-cam").iconAllowOverlap(true).iconIgnorePlacement(true)
            .iconPitchAlignment(IconPitchAlignment.VIEWPORT).iconEmissiveStrength(1.0)
            .iconSize(zoomed(1.0, 8 to 0.36, 12 to 0.44, 15 to 0.68, 18 to 0.9)))
        addL(st, SymbolLayer("dl-hazards", "dl-hazards")
            .iconImage(Expression.get("img")).iconAllowOverlap(true).iconIgnorePlacement(true).iconEmissiveStrength(1.0)
            .iconSize((prefs.i(PreferenceKeys.HAZARD_SIGN_SIZE, 115).coerceIn(50, 250) / 100.0).let { k -> zoomed(1.0, 11 to 0.45 * k, 15 to 0.75 * k, 18 to 1.0 * k) }))
        // speed limit signs on the route where the limit changes (Settings > On the map > Limit signs on the route)
        addS(st, GeoJsonSource.Builder("dl-limsigns").featureCollection(empty()).build())
        addL(st, SymbolLayer("dl-limsigns", "dl-limsigns")
            .iconImage(Expression.get("img")).iconAllowOverlap(true).iconIgnorePlacement(true).iconEmissiveStrength(1.0).minZoom(12.0)
            .iconSize((prefs.i(PreferenceKeys.ROUTE_LIMIT_SIGN_SIZE, 100).coerceIn(40, 250) / 100.0).let { k -> zoomed(1.0, 12 to 0.22 * k, 16 to 0.36 * k, 19 to 0.5 * k) }))
        // where you parked (on the phone, after Android Auto disconnects)
        addImg(st, "dl-car-pin", parkedBitmap())
        addS(st, GeoJsonSource.Builder("dl-parked").featureCollection(FeatureCollection.fromFeatures(
            parkedAt?.let { listOf(Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat))) } ?: emptyList())).build())
        signBmp?.let { addImg(st, "dl-lanesign", it) }
        addS(st, GeoJsonSource.Builder("dl-lanesign").featureCollection(FeatureCollection.fromFeatures(
            signAt?.let { listOf(Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat))) } ?: emptyList())).build())
        addL(st, SymbolLayer("dl-lanesign-l", "dl-lanesign").iconImage("dl-lanesign").iconAnchor(IconAnchor.BOTTOM)
            .iconAllowOverlap(true).iconIgnorePlacement(true).iconPitchAlignment(IconPitchAlignment.VIEWPORT).iconEmissiveStrength(1.0)
            .iconSize(zoomed(1.0, 13 to 0.45, 16 to 0.7, 18 to 0.9)))
        bubbleBmp?.let { addImg(st, "dl-lanebubble", it) }
        addS(st, GeoJsonSource.Builder("dl-lanebubble").featureCollection(FeatureCollection.fromFeatures(
            bubbleAt?.let { listOf(Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat))) } ?: emptyList())).build())
        addL(st, SymbolLayer("dl-lanebubble-l", "dl-lanebubble").iconImage("dl-lanebubble").iconAnchor(IconAnchor.BOTTOM)
            .iconAllowOverlap(true).iconIgnorePlacement(true).iconPitchAlignment(IconPitchAlignment.VIEWPORT).iconEmissiveStrength(1.0)
            .iconSize(zoomed(1.0, 13 to 0.3, 16 to 0.46, 18 to 0.6)))
        // the way on foot to the car: a dotted line under the car's pin
        addS(st, GeoJsonSource.Builder("dl-walk").featureCollection(if (walkPts.size >= 2) FeatureCollection.fromFeatures(listOf(line(walkPts))) else empty()).build())
        addL(st, LineLayer("dl-walk-l", "dl-walk").lineColor("#2F7BF6").lineEmissiveStrength(1.0).lineCap(LineCap.ROUND)
            .lineWidth(zoomed(2.0, 12 to 2.5, 16 to 5.0, 20 to 9.0)).lineDasharray(listOf(0.1, 1.9)))
        // arrival: the walk from the parking to the entrance (white dots), and the entrance itself (a green ring)
        addS(st, GeoJsonSource.Builder("dl-arrwalk").featureCollection(if (arrWalk.size >= 2) FeatureCollection.fromFeatures(listOf(line(arrWalk))) else empty()).build())
        addL(st, LineLayer("dl-arrwalk-l", "dl-arrwalk").inSlot(TOP).lineColor("#FFFFFF").lineEmissiveStrength(1.0).lineCap(LineCap.ROUND)
            .lineWidth(zoomed(2.0, 14 to 2.5, 17 to 5.0, 20 to 9.0)).lineDasharray(listOf(0.1, 2.0)))
        addS(st, GeoJsonSource.Builder("dl-entrance").featureCollection(FeatureCollection.fromFeatures(entranceAt?.let { listOf(Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat))) } ?: emptyList())).build())
        addL(st, CircleLayer("dl-entrance-l", "dl-entrance").inSlot(TOP).circleColor("#2EC27E").circleOpacity(0.35).circleEmissiveStrength(1.0)
            .circleRadius(zoomed(1.5, 15 to 7.0, 18 to 14.0, 20 to 22.0)).circleStrokeColor("#FFFFFF").circleStrokeWidth(2.5).circlePitchAlignment(CirclePitchAlignment.MAP))
        addL(st, SymbolLayer("dl-parked-l", "dl-parked").iconImage("dl-car-pin").iconAllowOverlap(true).iconIgnorePlacement(true)
            .iconPitchAlignment(IconPitchAlignment.VIEWPORT).iconEmissiveStrength(1.0).iconSize(zoomed(1.0, 10 to 0.4, 15 to 0.55, 18 to 0.7)))
        // the stops on the way (under the destination, so the pin stays on top where they meet). Pictures in both looks:
        // a charging stop has to read as one at a glance, which a plain dot can't do
        chgImgIds.clear()
        addS(st, GeoJsonSource.Builder("dl-chargers").featureCollection(empty()).build())
        addL(st, SymbolLayer("dl-chargers-l", "dl-chargers")
            .iconImage(Expression.get("img")).iconEmissiveStrength(1.0)
            .iconAllowOverlap(true).iconIgnorePlacement(true).iconAnchor(IconAnchor.BOTTOM)
            .iconSize(zoomed(1.0, 10 to 0.55, 13 to 0.8, 16 to 1.05, 18 to 1.2)))
        addImg(st, "dl-via-ev", viaEvBitmap())
        addImg(st, "dl-via-stop", viaStopBitmap())
        // the queues ahead: a small pill with the minutes where each starts (see NavEngine.queuesFor)
        queueImgs.clear()
        addS(st, GeoJsonSource.Builder("dl-queues").featureCollection(empty()).build())
        addL(st, SymbolLayer("dl-queues-l", "dl-queues")
            .iconImage(Expression.get("img")).iconEmissiveStrength(1.0)
            .iconAllowOverlap(true).iconIgnorePlacement(true).iconAnchor(IconAnchor.BOTTOM)
            .iconSize(zoomed(1.0, 10 to 0.6, 13 to 0.8, 16 to 1.0, 18 to 1.1)))
        addL(st, SymbolLayer("dl-via-l", "dl-via")
            .iconImage(Expression.get("img")).iconEmissiveStrength(1.0)
            .iconAllowOverlap(true).iconIgnorePlacement(true).iconPitchAlignment(IconPitchAlignment.VIEWPORT)
            .iconSize(zoomed(1.0, 8 to 0.4, 13 to 0.5, 16 to 0.62, 18 to 0.72)))
        if (im || neonMap) {
            addImg(st, "dl-pin", pinBitmap())
            addL(st, SymbolLayer("dl-dest-dot", "dl-dest")
                .iconImage("dl-pin").iconAnchor(IconAnchor.BOTTOM).iconEmissiveStrength(1.0)
                .iconAllowOverlap(true).iconIgnorePlacement(true).iconSize(0.6))
            // the neon look: a cyan selection frame round the marker
            if (neonMap) {
                addImg(st, "dl-pin-frame", neonFrameBitmap())
                addL(st, SymbolLayer("dl-dest-frame", "dl-dest")
                    .iconImage("dl-pin-frame").iconAnchor(IconAnchor.BOTTOM).iconEmissiveStrength(1.0)
                    .iconAllowOverlap(true).iconIgnorePlacement(true).iconSize(0.6))
            }
        } else addL(st, CircleLayer("dl-dest-dot", "dl-dest")
            .circleRadius(10.0).circleColor("#ff3b30").circleStrokeColor("#ffffff").circleStrokeWidth(3.0).circleEmissiveStrength(1.0))
        if (!im) {
            addL(st, CircleLayer("dl-me-halo", "dl-me")
                .circleRadius(15.0 * 0.625 * arrowScale).circleColor("#2e8bff").circleOpacity(0.25).circleEmissiveStrength(1.0)
                .circlePitchAlignment(CirclePitchAlignment.MAP).circlePitchScale(CirclePitchScale.MAP))
        }
        // the "+4 min" / "−2 min" bubbles on the alternatives (on top of everything, so they're always readable)
        addL(st, SymbolLayer("dl-live-bub", "dl-live-bub")
            .iconImage(Expression.get("img")).iconAnchor(IconAnchor.BOTTOM)
            .iconAllowOverlap(true).iconIgnorePlacement(true).iconEmissiveStrength(1.0)
            .iconPitchAlignment(IconPitchAlignment.VIEWPORT)
            // (about 30% smaller than before: the other routes' time cards no longer pile up)
            .iconSize(zoomed(1.0, 9 to 0.42, 14 to 0.48, 18 to 0.53)))
        flatArrow = neonArrowHex?.let { neonArrowBitmap(it) } ?: if (im) arrowBitmapImmersive() else arrowBitmap()
        addImg(st, "dl-arrow", flatArrow!!)
        addL(st, SymbolLayer("dl-me-arrow", "dl-me")
            .iconImage("dl-arrow").iconRotate(Expression.get("h"))
            .iconRotationAlignment(IconRotationAlignment.MAP).iconPitchAlignment(IconPitchAlignment.MAP)
            .iconAllowOverlap(true).iconIgnorePlacement(true).iconEmissiveStrength(1.0)
            .iconSize((if (im) 0.25 else 0.625) * arrowScale * flatIconK())
            .visibility(vis(locatorStyle == "flat")))
        // the real 3D model (Full 3D only); otherwise the 3D locator is drawn by locatorView over the map, see pushModel
        modelOn = false; modelAt = null
        if (modelWanted && !modelBroken && MapStylePolicy.supportsFullDetailFeatures(mapDetail)) installModel(st)
        // (the road-name pill is roadView, see placeLocator)
        spriteName = null
        placeLocator()
        pushAll()
        installExtras(st)
        if (neonMap && !satellite) try { installNeonCity(st) } catch (e: Throwable) { android.util.Log.w("MapBinding", "neon city: ${e.message}") }
        // the rain radar, if it's on, goes back under the route
        pushRadar()
    }

    fun setView(sat: Boolean, b3d: Boolean) {
        val satChanged = sat != satellite
        android.util.Log.w("MapBinding", "satellite: setView sat=$sat (was $satellite), style set=${wantedStyle != null}")
        satellite = sat
        val b3dChanged = buildings3d != b3d
        buildings3d = b3d
        // satellite is Mapbox's Standard Satellite style (a style of its own, so the map reloads); 3D buildings is one of Standard's settings
        if (satChanged && wantedStyle != null) { val r = onRestyle; if (r != null) r() else setStyleUrl(baseStyle()) }
        else if (!sat && slotsOn) {
            setBase("show3dObjects", Value.valueOf(b3dStd() && MapStylePolicy.supportsFullDetailFeatures(mapDetail)))
            // the building fronts and landmark buildings belong to the 3D buildings switch too: they stand only while it is on
            setBase("show3dFacades", Value.valueOf(facadesOn && b3dStd() && MapStylePolicy.supportsFullDetailFeatures(mapDetail)))
            setBase("show3dLandmarks", Value.valueOf(landmarksOn && !neonMap && MapStylePolicy.supportsFullDetailFeatures(mapDetail)))
            // the trees follow their own switch, however often the view is set (Android Auto sets it on every change)
            setBase("show3dTrees", Value.valueOf(treesOn))
            // the neon look's own blocks are drawn again (solid or flat)
            if (neonMap && b3dChanged && style != null) reinstall()
        }
        else if (!sat) style?.let { st ->
            // the Horizon colours' own 3D buildings
            try {
                if (st.styleLayerExists("dl-building-3d")) st.setStyleLayerProperty("dl-building-3d", "visibility", Value.valueOf(if (b3d) "visible" else "none"))
                else if (b3d && colours == "horizon") recolourClassic(st)
            } catch (e: Throwable) { softFail(e) }
        }
    }

    /**
     * Which stored photo sources the map shows now, for fetching a route's photos ahead: (state photos, EOX), or null when
     * satellite is off. Before the map has loaded with satellite, it's worked out the same way the map will.
     */
    fun satPrefetchPlan(): Pair<Boolean, Boolean>? {
        if (!satellite) return null
        val chain = satChain()
        val choice = satChoice()
        val step = if (satStep >= 0) satStep else SatelliteSources.startIndex(choice, chain, prefs.s(PreferenceKeys.SAT_REMEMBERED, ""),
            prefs.s(PreferenceKeys.SAT_REMEMBERED_AT, "0").toLongOrNull() ?: 0L, System.currentTimeMillis())
        val prov = chain[step.coerceIn(0, chain.size - 1)]
        return SatelliteSources.withStates(choice, prov) to (prov == SatProvider.EOX)
    }

    /**
     * The satellite settings changed (source, keys or how strong the roads show): with satellite on, the map loads again so the
     * new photos and road look apply at once. [sig] is everything those settings say, so the same settings again do nothing.
     */
    fun setSatSource(sig: String) {
        val first = satSig == null
        if (sig == satSig) return
        satSig = sig
        if (first) return
        satStep = -1; satFails = 0
        // loaded again even though it's the same style address (setStyleUrl alone would skip it)
        if (satellite && isMapboxStyle(wantedStyle)) { loadedStyle = null; setStyleUrl(baseStyle()) }
    }

    /** The style the map should show: Standard, or the streets style with the photos under it when satellite is on. */
    fun baseStyle(): String = when (MapStylePolicy.choose(satellite, mapDetail, nightStyle)) {
        BaseMapStyle.DARK_STREETS -> Style.DARK
        BaseMapStyle.STREETS -> Style.MAPBOX_STREETS
        BaseMapStyle.STANDARD -> Style.STANDARD
    }

    private val NAV_DAY = "mapbox://styles/mapbox/navigation-day-v1"
    private val NAV_NIGHT = "mapbox://styles/mapbox/navigation-night-v1"

    /** While a route is on the map, traffic colours on every other road can be hidden so the route line stands out (the route itself shows its own traffic). */
    fun setHideOtherTraffic(b: Boolean) {
        if (hideTraffic == b) return
        hideTraffic = b
        applyTraffic()
    }

    /** Live traffic colours on the roads on or off (Settings > Map > Live traffic). */
    fun setTraffic(on: Boolean) {
        if (trafficOn == on) return
        trafficOn = on
        applyTraffic()
    }

    /** Free-flowing roads drawn green as well as the slow ones (Settings > Map > Light traffic in green). */
    private var trafficGreen = false
    // (green only on the main roads (motorways, A roads and the bigger B roads): on every side street it painted the whole map green)
    private fun trafficFilter(): Expression = Expression.fromRaw(
        if (trafficGreen) "[\"any\",[\"match\",[\"get\",\"congestion\"],[\"moderate\",\"heavy\",\"severe\"],true,false]," +
            "[\"all\",[\"==\",[\"get\",\"congestion\"],\"low\"],[\"match\",[\"get\",\"class\"],[\"motorway\",\"trunk\",\"primary\",\"secondary\"],true,false]]]"
        else "[\"match\",[\"get\",\"congestion\"],[\"moderate\",\"heavy\",\"severe\"],true,false]")
    fun setTrafficGreen(on: Boolean) {
        if (on == trafficGreen) return
        trafficGreen = on
        main0.post { try { (layer("dl-traffic") as? LineLayer)?.filter(trafficFilter()) } catch (e: Throwable) { softFail(e) } }
    }

    private fun applyTraffic() {
        try { layer("dl-traffic")?.visibility(vis(trafficOn && !(routing && hideTraffic))) } catch (e: Throwable) { softFail(e) }
        // driving with no route: the traffic lines are wider and solid, so slow and stopped traffic stands out
        try {
            (layer("dl-traffic") as? LineLayer)?.let { l ->
                l.lineWidth(if (routing) zoomed(1.5, 8 to 1.0, 13 to 2.5, 16 to 4.5, 19 to 9.0) else zoomed(1.5, 8 to 1.6, 13 to 4.0, 16 to 7.0, 19 to 13.0))
                l.lineOpacity(if (routing) 0.85 else 1.0)
            }
        } catch (e: Throwable) { softFail(e) }
    }

    /** Light map: no points of interest and no route glow, so there is less to draw and place every frame. */
    private var poiMoving = false

    /** Set by the engine as you start and stop moving, so the full map can rest its shop labels while driving. */
    fun setMoving(m: Boolean) {
        if (m == poiMoving) return
        poiMoving = m
        if (!lite) setBase("showPointOfInterestLabels", Value.valueOf(!poiMoving))
    }


    /**
     * The Horizon colours, exactly as the original app painted its map: soft, light tones by day and a deep blue-grey at night, matched
     * to layers by name (land, water, parks, roads, buildings), green only for the shapes whose class is green, buildings shaded by
     * height, road fills a touch darker by day so they separate from the land, and the original label colours. Used for the Horizon
     * colours on Mapbox's map and for TomTom's map. Also adds 3D buildings when the map has none (as the original did).
     */
    private fun recolourClassic(st: Style) {
        val dark = nightStyle
        val land = if (dark) "#2D3358" else "#F4F2EE"
        val water = if (dark) "#2A6682" else "#AADAF5"
        val park = if (dark) "#2E5C5E" else "#CDEBC4"
        val bld = if (dark) "#454C78" else "#E7E5E1"
        // (by day the original made road fills 20% darker after painting them white: #CCCCCC)
        val roadFill = if (dark) "#464E7C" else "#CCCCCC"
        val roadCase = if (dark) "#262B4B" else "#DCDAD5"
        val hwyFill = if (dark) "#5E67A0" else "#CCCCCC"
        val hwyCase = if (dark) "#2B3052" else "#CFCCC6"
        val rail = if (dark) "#454C70" else "#D2D0CB"
        val text = if (dark) "#C9CEE8" else "#5F6368"
        val halo = if (dark) "#2D3358" else "#FFFFFF"
        val roadText = if (dark) "#D3D8F0" else "#3E4459"
        val roadHalo = if (dark) "#252A48" else "#F4F6FB"
        val parkWords = listOf("park", "green", "forest", "wood", "grass", "vegetation", "nature", "garden", "golf", "cemetery", "recreation", "pitch", "meadow", "scrub", "farm")
        val roadWords = listOf("road", "street", "motorway", "trunk", "highway", "primary", "secondary", "tertiary", "residential", "service", "link", "ramp", "track", "bridge", "tunnel", "path", "pedestrian", "transportation")
        val hwyWords = listOf("motorway", "trunk", "highway", "freeway", "expressway")
        val greenKinds = "[\"grass\",\"wood\",\"forest\",\"park\",\"garden\",\"meadow\",\"scrub\",\"heath\",\"farmland\",\"farm\",\"orchard\",\"vineyard\",\"recreation_ground\",\"recreation\",\"pitch\",\"golf_course\",\"golf\",\"cemetery\",\"nature_reserve\",\"village_green\",\"allotments\",\"common\",\"grassland\",\"tree\",\"trees\",\"wetland\",\"playground\",\"sports_centre\",\"green\"]"
        val greenByClass = Expression.fromRaw("[\"match\",[\"downcase\",[\"to-string\",[\"coalesce\",[\"get\",\"class\"],[\"get\",\"subclass\"],[\"get\",\"kind\"],[\"get\",\"type\"],\"\"]]],$greenKinds,\"$park\",\"$land\"]")
        val cols = if (dark) listOf("#3C4370", "#454C7E", "#51598F", "#5F68A0") else listOf("#F1EFEB", "#E9E7E3", "#E0DED9", "#D6D4CF")
        val extColour = Expression.fromRaw("[\"interpolate\",[\"linear\"],[\"case\",[\"has\",\"render_height\"],[\"to-number\",[\"get\",\"render_height\"]],[\"has\",\"height\"],[\"to-number\",[\"get\",\"height\"]],10],0,\"${cols[0]}\",20,\"${cols[1]}\",60,\"${cols[2]}\",150,\"${cols[3]}\"]")
        fun set(id: String, prop: String, v: Value) { try { st.setStyleLayerProperty(id, prop, v) } catch (e: Throwable) { softFail(e) } }
        fun set(id: String, prop: String, hex: String) = set(id, prop, Value.valueOf(hex))
        var has3d = false
        var buildingSrc: String? = null
        var firstSymbol: String? = null
        for (info in st.styleLayers) {
            val id = info.id
            if (id.startsWith("dl-")) continue
            val srcLayer = try { st.getStyleLayerProperty(id, "source-layer").value.contents as? String ?: "" } catch (e: Throwable) { "" }
            val key = (id + " " + srcLayer).lowercase()
            // the map's own traffic lines are hidden: the app draws live traffic itself (and hides it round your route)
            if (key.contains("traffic")) { set(id, "visibility", "none"); continue }
            if (key.contains("flow") || key.contains("incident")) continue
            when (info.type) {
                "background" -> set(id, "background-color", land)
                "fill" -> when {
                    key.contains("water") -> set(id, "fill-color", water)
                    parkWords.any { key.contains(it) } -> set(id, "fill-color", park)
                    key.contains("building") -> {
                        set(id, "fill-color", bld)
                        if (srcLayer == "building") buildingSrc = try { st.getStyleLayerProperty(id, "source").value.contents as? String } catch (e: Throwable) { null }
                    }
                    // generic land-cover / land-use layers hold grass, woods and parks together with plain ground: tint by each shape's class
                    listOf("landcover", "land cover", "landuse", "land use", "vegetation", "natural", "leisure", "green").any { key.contains(it) } -> set(id, "fill-color", greenByClass)
                    listOf("land", "background", "urban", "residential", "commercial", "industrial").any { key.contains(it) } -> set(id, "fill-color", land)
                }
                "fill-extrusion" -> {
                    has3d = true
                    set(id, "fill-extrusion-color", extColour)
                    set(id, "fill-extrusion-opacity", Value.valueOf(if (dark) 0.9 else 0.96))
                    set(id, "fill-extrusion-vertical-gradient", Value.valueOf(true))
                }
                "line" -> {
                    val casing = key.contains("casing") || key.contains("case") || key.contains("outline") || key.contains("stroke")
                    val hwy = hwyWords.any { key.contains(it) }
                    when {
                        key.contains("water") || key.contains("river") || key.contains("stream") || key.contains("canal") -> set(id, "line-color", water)
                        key.contains("rail") || key.contains("subway") || key.contains("transit") -> set(id, "line-color", rail)
                        roadWords.any { key.contains(it) } -> set(id, "line-color", if (casing) (if (hwy) hwyCase else roadCase) else (if (hwy) hwyFill else roadFill))
                    }
                }
                "symbol" -> {
                    if (firstSymbol == null) firstSymbol = id
                    val roadLabel = roadWords.any { key.contains(it) }
                    set(id, "text-color", if (roadLabel) roadText else text)
                    set(id, "text-halo-color", if (roadLabel) roadHalo else halo)
                    set(id, "text-halo-width", Value.valueOf(1.4))
                }
            }
        }
        // 3D buildings, as the original added to Mapbox's map (when 3D buildings are on), under the labels
        val src = buildingSrc ?: (if (st.styleSourceExists("composite")) "composite" else null)
        if (!has3d && src != null && buildings3d && !st.styleLayerExists("dl-building-3d")) {
            val l = FillExtrusionLayer("dl-building-3d", src).sourceLayer("building").minZoom(15.0)
                .filter(Expression.fromRaw("[\"all\",[\"!=\",[\"get\",\"extrude\"],\"false\"],[\"!=\",[\"get\",\"underground\"],\"true\"]]"))
                .fillExtrusionColor(extColour)
                .fillExtrusionHeight(Expression.fromRaw("[\"interpolate\",[\"linear\"],[\"zoom\"],15,0,15.6,[\"to-number\",[\"get\",\"height\"],8]]"))
                .fillExtrusionBase(Expression.fromRaw("[\"interpolate\",[\"linear\"],[\"zoom\"],15,0,15.6,[\"to-number\",[\"get\",\"min_height\"],0]]"))
                .fillExtrusionOpacity(if (dark) 0.9 else 0.95).fillExtrusionVerticalGradient(true)
            addL(st, l, firstSymbol)
        }
    }

    // ---- trees: Standard draws its own 3D trees ----
    private var treesOn = true

    fun treesEnabled(on: Boolean) {
        if (treesOn == on) return
        treesOn = on
        if (!satellite) setBase("show3dTrees", Value.valueOf(on))
    }


    /** Line width that grows with zoom the way road widths do: clearly wider than the road under it, but always well under the locator arrow's width. */
    private fun routeW(scale: Float): Expression {
        // everything route-related (line, casing, glow, driven part, turn arrow) is sized from this, so they all stay in step
        val k = scale * ROUTE_K
        return zoomed(1.7, 10 to 3f * k, 13 to 6.5f * k, 15 to 10f * k, 17 to 16f * k, 19 to 24f * k)
    }

    /** How thick the route is drawn overall (1 = the standard width). */
    /** Makes the route line (and everything sized with it) thicker: the Android Auto map sets this a little above 1, as the
     *  line looked thin there against the buildings. Set straight after creating the map, before its style is built. */
    var routeBoost = 1f
    private val ROUTE_K: Float get() = routeBoost

    private fun line(p: List<LL>): Feature =
        Feature.fromGeometry(LineString.fromLngLats(p.map { Point.fromLngLat(it.lon, it.lat) }))

    private fun lineSev(p: List<LL>, level: Double): Feature {
        val f = line(p)
        f.addNumberProperty("sev", level)
        return f
    }

    private var mainSev: IntArray? = null
    private var mainOff = 0

    /** Route colour: blue (a deeper blue by day, like the progress bar), amber where traffic is slow, red where it's heavy. */
    /** The neon look's yellow as the route's colour (the route line is drawn in it where traffic flows), else null. */
    private fun neonRoute(): String? = if (colours == "neon") "#FCEE0A" else null

    // (interpolated, so the in-between levels where traffic changes blend the colours, see RouteBlend; a whole level keeps its colour)
    private fun sevColor(im: Boolean = false, day: Boolean = false): Expression = Expression.interpolate(
        Expression.linear(), Expression.get("sev"),
        lit(0), col(neonRoute() ?: ownRoute() ?: if (day) "#2A7BE4" else if (im) "#2FC4F5" else "#4CC9FF"),
        lit(1), col(if (im) "#F9AB00" else "#FFB020"),
        lit(2), col(if (im) "#D93025" else "#FF4D4D"),
    )

    /** Settings > Map > Own design: the route colour picked there (empty = the usual blue). */
    private fun ownRoute(): String? = prefs.s(PreferenceKeys.ROUTE_COLOUR, "").takeIf { h -> h.isNotBlank() && try { android.graphics.Color.parseColor(h); true } catch (e: Exception) { false } }

    private fun pushAll() {
        pushLive(); pushRadar(); pushTurn()
        pushRoute(); pushDest(); pushVia(); pushChargers(); pushMe(); pushHazards(); pushCams(); pushAlpr(); lights.push(); pushGarages()
    }

    // ---- car parks with their free spaces ----
    /** Tapped a car park on the map. */
    var onGarageTap: ((Garage) -> Unit)? = null
    private var garages: List<Garage> = emptyList()
    private val garageImgs = HashSet<String>()

    fun setGarages(list: List<Garage>) { main0.post { if (list != garages) { garages = list; pushGarages() } } }

    // ---- every branch of a chain ----
    class ChainPin(val pos: LL, val label: String, val logo: Bitmap?, val selected: Boolean)
    /** Tapped the map where there is nothing to tap (no place, charger, car park or route). */
    var onFreeTap: ((LL) -> Unit)? = null
    private var poiTapAt = 0L
    /** Tapped a branch on the map (its index in the list given to [setChainPins]). */
    var onChainTap: ((Int) -> Unit)? = null
    private var chainPins: List<ChainPin> = emptyList()
    private val chainImgs = HashSet<String>()

    fun setChainPins(list: List<ChainPin>) { main0.post { chainPins = list; pushChain() } }

    // ---- favourites: a yellow star with the name (or without it, while navigating) ----
    var onFavTap: ((Int) -> Unit)? = null
    private var favPins: List<Place> = emptyList()
    private var favNames = true
    private val favImgs = HashSet<String>()

    fun setFavourites(list: List<Place>, withNames: Boolean) { main0.post { favPins = list; favNames = withNames; pushFav() } }

    private fun favImg(p: Place) = "dl-fav-" + (if (favNames) "n" else "s") + "-" + p.name.hashCode()

    // ---- Home, Work and starred places: a house on blue, a briefcase on amber, a star; a tap opens the place ----
    enum class SavedKind { HOME, WORK, STAR }
    class SavedPin(val pos: LL, val kind: SavedKind)
    var onSavedTap: ((Int) -> Unit)? = null
    private var savedPins: List<SavedPin> = emptyList()
    private var savedScale = 1.0
    private val savedImgs = HashSet<String>()

    /** [scale] is the size of the symbols as a share of the usual (1.0 = 100 %). */
    fun setSavedPins(list: List<SavedPin>, scale: Double) { main0.post { savedPins = list; savedScale = scale; pushSaved() } }

    private fun savedImg(k: SavedKind) = "dl-saved-" + k.name + "-" + Math.round(savedScale * 100)

    private fun pushSaved() {
        val st = style ?: return
        val s = src("dl-saved") ?: return
        try {
            for (k in savedPins) { val id = savedImg(k.kind); if (savedImgs.add(id)) addImg(st, id, savedBitmap(k.kind, savedScale.toFloat())) }
            s.featureCollection(FeatureCollection.fromFeatures(savedPins.map { p ->
                Feature.fromGeometry(Point.fromLngLat(p.pos.lon, p.pos.lat)).also { it.addStringProperty("img", savedImg(p.kind)) }
            }))
        } catch (e: Throwable) { softFail(e) }
    }


    /** The saved symbol nearest the tap (within about a finger's width). */
    private fun savedAt(sp: android.graphics.PointF): Int? {
        val ps = savedPins
        if (ps.isEmpty()) return null
        val dp = mapView.resources.displayMetrics.density
        val px = pixels(ps.map { it.pos })
        if (px.size != ps.size) return null
        var best: Int? = null; var bd = 30.0 * dp
        for (k in ps.indices) {
            val d = Math.hypot(px[k].x - sp.x, px[k].y - 16 * dp - sp.y)
            if (d < bd) { bd = d; best = k }
        }
        return best
    }

    private fun pushFav() {
        val st = style ?: return
        val s = src("dl-fav") ?: return
        try {
            for (p in favPins) { val id = favImg(p); if (favImgs.add(id)) addImg(st, id, favBitmap(p.name, favNames)) }
            s.featureCollection(FeatureCollection.fromFeatures(favPins.map { p ->
                Feature.fromGeometry(Point.fromLngLat(p.pos.lon, p.pos.lat)).also { it.addStringProperty("img", favImg(p)) }
            }))
        } catch (e: Throwable) { softFail(e) }
    }


    /** The favourite star nearest the tap (within about a finger's width). */
    private fun favAt(sp: android.graphics.PointF): Int? {
        val ps = favPins
        if (ps.isEmpty()) return null
        val dp = mapView.resources.displayMetrics.density
        val px = pixels(ps.map { it.pos })
        if (px.size != ps.size) return null
        var best: Int? = null; var bd = 30.0 * dp
        for (k in ps.indices) {
            val d = Math.hypot(px[k].x - sp.x, px[k].y - 16 * dp - sp.y)
            if (d < bd) { bd = d; best = k }
        }
        return best
    }

    private fun chainImg(p: ChainPin) = "dl-ch-" + (p.logo?.let { System.identityHashCode(it) } ?: 0) + "-" + p.label.replace(Regex("[^A-Za-z0-9]"), "_") + (if (p.selected) "-s" else "")

    private fun pushChain() {
        val st = style ?: return
        val s = src("dl-chain") ?: return
        try {
            for (p in chainPins) { val id = chainImg(p); if (chainImgs.add(id)) addImg(st, id, chainBitmap(p)) }
            s.featureCollection(FeatureCollection.fromFeatures(chainPins.map { p ->
                Feature.fromGeometry(Point.fromLngLat(p.pos.lon, p.pos.lat)).also { it.addStringProperty("img", chainImg(p)); it.addNumberProperty("z", if (p.selected) 1 else 0) }
            }))
        } catch (e: Throwable) { softFail(e) }
    }


    private fun chainAt(sp: android.graphics.PointF): Int? {
        val ps = chainPins
        if (ps.isEmpty()) return null
        val dp = mapView.resources.displayMetrics.density
        val px = pixels(ps.map { it.pos })
        if (px.size != ps.size) return null
        var best: Int? = null; var bd = 34.0 * dp
        for (k in ps.indices) {
            val d = Math.hypot(px[k].x - sp.x, px[k].y - 30 * dp - sp.y)
            if (d < bd) { bd = d; best = k }
        }
        return best
    }

    fun zoomNow(): Double = try { mbMap.cameraState.zoom } catch (e: Throwable) { 0.0 }

    private fun garageImg(g: Garage) = "dl-gar-${g.level()}-${g.badge()}"

    private fun pushGarages() {
        val st = style ?: return
        val s = src("dl-garages") ?: return
        try {
            for (g in garages) { val id = garageImg(g); if (garageImgs.add(id)) addImg(st, id, garageBitmap(g)) }
            s.featureCollection(FeatureCollection.fromFeatures(garages.map { g ->
                Feature.fromGeometry(Point.fromLngLat(g.pos.lon, g.pos.lat)).also { it.addStringProperty("img", garageImg(g)) }
            }))
        } catch (e: Throwable) { softFail(e) }
    }


    /** A tap near a car park's badge (about 30 dp), from city zoom in. */
    private fun garageAt(sp: android.graphics.PointF): Garage? {
        val gs = garages
        if (gs.isEmpty() || zoomNow() < 13.0) return null
        val dp = mapView.resources.displayMetrics.density
        val px = pixels(gs.map { it.pos })
        if (px.size != gs.size) return null
        var best: Garage? = null; var bd = 30.0 * dp
        for (k in gs.indices) {
            // the badge stands above its point
            val d = Math.hypot(px[k].x - sp.x, px[k].y - 16 * dp - sp.y)
            if (d < bd) { bd = d; best = gs[k] }
        }
        return best
    }

    private var cams: List<LL> = emptyList()
    private var alpr: List<LL> = emptyList()

    /** The speed cameras around you, shown on the map whether or not you're on a route. */
    fun setCameras(list: List<LL>) {
        main0.post { if (list != cams) { cams = list; pushCams() } }
    }

    /** Automatic number-plate reader cameras on the route ahead, drawn as violet dots. */
    fun setAlprCameras(list: List<LL>) {
        main0.post { if (list != alpr) { alpr = list; pushAlpr() } }
    }

    private fun pushAlpr() {
        val s = src("dl-alpr") ?: return
        try { s.featureCollection(FeatureCollection.fromFeatures(alpr.map { Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat)) })) } catch (e: Exception) { softFail(e) }
    }

    private fun pushCams() {
        val s = src("dl-cams") ?: return
        try { s.featureCollection(FeatureCollection.fromFeatures(cams.map { Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat)) })) } catch (e: Exception) { softFail(e) }
        // the 3D cameras face you (turned towards where you are now), standing at the left kerb
        val m = src("dl-cams-m") ?: return
        try {
            val here = me
            m.featureCollection(FeatureCollection.fromFeatures(cams.take(200).map { c ->
                val b = if (here != null) Geo.bearing(here, c) else 0.0
                val at = Furniture.offset(c, b - 90.0, 4.0)
                Furniture.point(at.lat, at.lon, b)
            }))
        } catch (e: Exception) { softFail(e) }
    }

    // (never throws: asking a style that's being replaced by a newer one is an error in the map engine, and these are called from
    // posted jobs that can run just as the map reloads; that was a crash in the old map engine)
    // asked only when the source is there: Mapbox logs a warning for every missing one (the clear-view area is asked for
    // twice a second, and isn't on the map in satellite mode, so the log filled with "dl-clip is not requested type")
    private fun src(id: String): GeoJsonSource? = try { style?.takeIf { it.styleSourceExists(id) }?.getSourceAs<GeoJsonSource>(id) } catch (e: Throwable) { null }

    /** The route line's stretch moved into the lane you need before a junction (from the road's line, back onto it), or empty. */
    private var routeShift: List<LL> = emptyList()

    fun setRouteShift(pts: List<LL>) {
        main0.post {
            if (pts.isEmpty() && routeShift.isEmpty()) return@post
            routeShift = pts
            try { setLine(main) } catch (e: Throwable) { softFail(e) }
        }
    }

    /** [p] moved onto the lane stretch when it's alongside it (within 10 m), else as it is. */
    private fun onShift(p: LL): LL = MapRouteGeometry.shiftPoint(p, routeShift)

    /**
     * [pts] with the lane stretch spliced in (where its ends sit on the line), and for each point the index of the route point it
     * stands for (so the traffic colours stay in the right places). Lane points already behind you are left out.
     */
    private fun withShift(pts: List<LL>): Pair<List<LL>, IntArray?> = MapRouteGeometry.spliceShift(pts, routeShift)

    private fun setLine(pts0: List<LL>) {
        val s = src("dl-route") ?: return
        val (pts, origIdx) = withShift(pts0)
        try { src("dl-flow")?.featureCollection(if (pts.size < 2) FeatureCollection.fromFeatures(ArrayList<Feature>()) else FeatureCollection.fromFeatures(arrayListOf(line(pts)))) } catch (e: Exception) { softFail(e) }
        if (pts.size < 2) { s.featureCollection(empty()); return }
        val sev = mainSev
        fun level(j0: Int): Int { val j = origIdx?.getOrNull(j0) ?: j0; return if (sev != null && mainOff + j in sev.indices) sev[mainOff + j] else 0 }
        // where the turn arrow is drawn, the route line underneath is left out (cut at the exact points where the arrow starts and ends,
        // not at the nearest route point), so none of it shows round the arrow's edges
        // ...but only while the arrow is drawn (zoom 13 and in): zoomed further out the arrow isn't shown, and the cut left a gap
        val cut = if (cutZoomOk()) turnCut else null
        cutActive = cut != null
        val feats = ArrayList<Feature>()
        // one piece per traffic level; where it changes the colours run into each other (Settings › On the map › Blend traffic colours)
        val blend = prefs.b(PreferenceKeys.TRAFFIC_BLEND, true)
        fun addRun(seg: List<LL>, firstIdx: Int) {
            for (p in RouteBlend.pieces(seg, { j -> level(firstIdx + j) }, blend)) feats += lineSev(p.pts, p.level)
        }
        val pa = cut?.firstOrNull()?.let { MapRouteGeometry.projectOn(pts, it) }
        val pb = cut?.lastOrNull()?.let { MapRouteGeometry.projectOn(pts, it) }
        if (pa != null && pb != null && pb.first > pa.first) {
            // before the arrow: up to the arrow's start point
            addRun(pts.subList(0, pa.first + 1) + pa.second, 0)
            // after the arrow: from its end point on
            addRun(listOf(pb.second) + pts.subList(pb.first + 1, pts.size), pb.first)
        } else addRun(pts, 0)
        s.featureCollection(FeatureCollection.fromFeatures(feats))
    }


    private fun pushRoute() {
        setLine(main)
        try { pushLimitSigns() } catch (e: Throwable) { softFail(e) }
        src("dl-alts")?.featureCollection(FeatureCollection.fromFeatures(ArrayList(alts.filter { it.size >= 2 }.map { line(it) })))
        pushPick()
    }

    /** The route picked once while choosing (yellow on the map), or null. */
    private var previewLine: List<LL>? = null
    fun setPreviewPick(p: List<LL>?) { previewLine = p?.takeIf { it.size >= 2 }; pushPick() }
    private fun pushPick() {
        src("dl-pick")?.featureCollection(FeatureCollection.fromFeatures(listOfNotNull(previewLine?.let { line(it) })))
    }

    /** Called with the index of an alternative line on the map while a route is being chosen (see tapAt). */
    var onPreviewTap: ((Int) -> Boolean)? = null

    private var hazards: List<Pair<LL, Int>> = emptyList()
    /** Map icons by kind: camera, roadworks, closure, police, accident, hazard, breakdown, mobile camera, traffic jam. */
    private val hazardNames = arrayOf("dl-hz-cam", "dl-hz-work", "dl-hz-closed", "dl-hz-police", "dl-hz-accident", "dl-hz-hazard", "dl-hz-breakdown", "dl-hz-mobilecam", "dl-hz-jam")

    /** Icons for speed cameras (0), roadworks (1) and closures (2) along the route. */
    fun setHazards(list: List<Pair<LL, Int>>) { hazards = list; pushHazards() }

    private fun pushHazards() {
        val s = src("dl-hazards") ?: return
        val fs = hazards.map { (p, k) ->
            Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat)).also { it.addStringProperty("img", hazardNames[k.coerceIn(0, hazardNames.size - 1)]) }
        }
        s.featureCollection(FeatureCollection.fromFeatures(fs))
    }
    // ---- lane arrows painted on the road ----
    class LaneMark(val pt: LL, val bearing: Double, val dir: String, val on: Boolean)
    private val laneImgs = HashSet<String>()
    private var lanePulse: android.animation.ValueAnimator? = null

    class LaneBand(val ring: List<LL>, val on: Boolean, val a: Double = 1.0)
    class LaneEdge(val pts: List<LL>, val solid: Boolean)

    /** Draws the lane strips and dividers (empty lists clear them). */
    fun setLaneBands(bands: List<LaneBand>, divs: List<LaneEdge>) {
        main0.post {
            val st = style ?: return@post
            try {
                val bf = ArrayList<Feature>()
                for (b in bands) {
                    if (b.ring.size < 4) continue
                    val pts = (b.ring + b.ring[0]).map { Point.fromLngLat(it.lon, it.lat) }
                    val f = Feature.fromGeometry(Polygon.fromLngLats(listOf(pts)))
                    f.addNumberProperty("on", if (b.on) 1 else 0)
                    f.addNumberProperty("a", b.a)
                    bf.add(f)
                }
                src("dl-lane-bands")?.featureCollection(FeatureCollection.fromFeatures(bf))
                val df = ArrayList<Feature>()
                for (d in divs) if (d.pts.size >= 2) df.add(Feature.fromGeometry(LineString.fromLngLats(d.pts.map { Point.fromLngLat(it.lon, it.lat) })).also { it.addNumberProperty("s", if (d.solid) 1 else 0) })
                src("dl-lane-divs")?.featureCollection(FeatureCollection.fromFeatures(df))
            } catch (e: Throwable) { softFail(e) }
        }
    }

    fun setLanes(marks: List<LaneMark>) {
        if (marks.isEmpty()) setLaneBands(emptyList(), emptyList())
        main0.post {
            val st = style ?: return@post
            val feats = ArrayList<Feature>()
            for (m in marks) {
                val name = "ln-${m.dir}-${if (m.on) 1 else 0}"
                if (laneImgs.add(name)) addImg(st, name, laneBitmap(m.dir, m.on))
                val f = Feature.fromGeometry(Point.fromLngLat(m.pt.lon, m.pt.lat))
                f.addStringProperty("img", name)
                f.addNumberProperty("rot", m.bearing)
                f.addNumberProperty("on", if (m.on) 1 else 0)
                feats.add(f)
            }
            src("dl-lanes")?.featureCollection(FeatureCollection.fromFeatures(feats))
            val layer = lanesOnLayer
            lanePulse?.cancel(); lanePulse = null
            if (feats.isEmpty()) { try { layer?.iconOpacity(1.0) } catch (e: Throwable) { softFail(e) }; return@post }
            lanePulse = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 600; repeatMode = android.animation.ValueAnimator.REVERSE; repeatCount = android.animation.ValueAnimator.INFINITE
                // only four brightness steps, so the map is not asked to redraw on every screen refresh just for a blink
                var lastStep = -1
                addUpdateListener {
                    val step = Math.round((it.animatedValue as Float) * 3f)
                    if (step != lastStep) { lastStep = step; try { layer?.iconOpacity(0.7 + 0.3 * step / 3.0) } catch (e: Throwable) { softFail(e) } }
                }
                start()
            }
        }
    }

    // ---- data from the engine ----
    fun setRoute(mainPts: List<LL>, altPts: List<List<LL>>, sev: IntArray? = null) {
        main = mainPts; alts = altPts; mainSev = sev; mainOff = 0; pushRoute(); lights.push()
        if (mainPts.isEmpty()) { setAvgZones(emptyList(), emptyList()); setTunnels(emptyList()); setSharpBends(emptyList()); setCrossings(emptyList()); setStopSigns(emptyList()) }
        if (mainPts.size >= 2) lights.start(mainPts) else lights.clearRoute()
        val on = mainPts.isNotEmpty()
        if (on != routing) { routing = on; applyTraffic(); applyNearLook() }
        syncFlow()
    }

    // ---- live alternatives while driving ----
    private var liveLines: List<List<LL>> = emptyList()
    private var liveBubs: List<LL> = emptyList()
    private var liveDiffs: List<Int> = emptyList()
    private var liveTraffic: List<Int> = emptyList()
    private val bubImgs = HashSet<String>()

    /** A rounded time bubble with a little tail: green when it is quicker, dark when it is slower. Drawn at 2x and shown at half size. */
    /**
     * The card on another route, like Google Maps: "4 min slower" or "3 min faster" in bold, and under it how its traffic compares
     * with yours. Faster ones are green, the rest dark. A small point underneath sits on the route.
     */
    private fun bubbleBitmap(diff: Int, traffic: Int = 0): Bitmap {
        val d = mapView.resources.displayMetrics.density * 2f
        val top = if (diff < 0) Lang.t("${-diff} Min. schneller", "${-diff} min faster") else if (diff > 0) Lang.t("$diff Min. langsamer", "$diff min slower") else Lang.t("Gleiche Zeit", "Same time")
        val sub = when (traffic) { -1 -> Lang.t("Weniger Verkehr", "Less traffic"); 1 -> Lang.t("Mehr Verkehr", "More traffic"); else -> Lang.t("Ähnlicher Verkehr", "Similar traffic") }
        // day: a light card with dark writing, like the rest of the day look; night: the dark card. A faster way is green in both
        val day = !nightStyle
        val green = diff < 0
        val lightCard = day && !green
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (lightCard) 0xFF14171C.toInt() else android.graphics.Color.WHITE; textSize = 16f * d; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (lightCard) 0xFF59616D.toInt() else 0xE6FFFFFF.toInt(); textSize = 12.5f * d }
        val tw = maxOf(tp.measureText(top), sp.measureText(sub))
        val w = (tw + 26f * d).toInt(); val bh = 50f * d; val tail = 9f * d
        val b = Bitmap.createBitmap(w + (6 * d).toInt(), (bh + tail + 6 * d).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val ox = 3f * d; val oy = 3f * d
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (green) 0xFF1E9E5A.toInt() else if (day) 0xFFF7F8FB.toInt() else 0xFF2B3142.toInt() }
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (lightCard) 0x40141A24 else 0xE6FFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = (if (lightCard) 1.4f else 2.2f) * d }
        val path = Path().apply {
            addRoundRect(RectF(ox, oy, ox + w, oy + bh), 14f * d, 14f * d, Path.Direction.CW)
            moveTo(ox + w / 2f - tail, oy + bh - 1); lineTo(ox + w / 2f, oy + bh + tail); lineTo(ox + w / 2f + tail, oy + bh - 1); close()
        }
        // a soft shadow just under it
        c.save(); c.translate(0f, 1.5f * d)
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (day) 0x30000000 else 0x40000000 })
        c.restore()
        c.drawPath(path, fill); c.drawPath(path, edge)
        c.drawText(top, ox + (w - tp.measureText(top)) / 2f, oy + 22f * d, tp)
        // the traffic line, with a little coloured dot: green less, amber similar, red more
        val dotCol = if (lightCard) when (traffic) { -1 -> 0xFF1E9E5A.toInt(); 1 -> 0xFFE0453A.toInt(); else -> 0xFFE09A00.toInt() }
            else when (traffic) { -1 -> 0xFF5BE38F.toInt(); 1 -> 0xFFFF6B5E.toInt(); else -> 0xFFFFC94D.toInt() }
        val sw = sp.measureText(sub); val dotR = 3.6f * d
        val sx = ox + (w - (sw + dotR * 2 + 5f * d)) / 2f
        c.drawCircle(sx + dotR, oy + 38f * d, dotR, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dotCol })
        c.drawText(sub, sx + dotR * 2 + 5f * d, oy + 42.5f * d, sp)
        return b
    }


    /** The other ways to go, in grey, with a bubble on each saying how it compares. Empty lists clear them. */
    fun setLiveAlts(lines: List<List<LL>>, bubbles: List<LL>, diffs: List<Int>, traffic: List<Int> = emptyList()) {
        liveLines = lines; liveBubs = bubbles; liveDiffs = diffs; liveTraffic = traffic; cardSpots = emptyList()
        main0.post { pushLive(); placeAltCards(true) }
    }

    // where each alternative's card is shown: moved along its line so it's always on screen (null when its line is off screen)
    private var cardSpots: List<LL?> = emptyList()
    private var cardsAt = 0L

    /** Screen positions of many places at once (one call to the map rather than one each). */
    private fun pixels(pts: List<LL>): List<ScreenCoordinate> =
        try { mbMap.pixelsForCoordinates(pts.map { Point.fromLngLat(it.lon, it.lat) }) } catch (e: Throwable) { emptyList() }

    /**
     * Keeps every alternative's card on screen: the card goes on the first stretch of its line that's in the clear part of the map
     * (not under the cards down the side, the buttons or the edges), as near the turn-off as possible. Checked a few times a second
     * while the map moves and once when it stops.
     */
    private fun placeAltCards(now: Boolean) {
        if (liveLines.isEmpty()) return
        val t = android.os.SystemClock.uptimeMillis()
        if (!now && t - cardsAt < 350L) return
        cardsAt = t
        val w = mapView.width.toFloat(); val h = mapView.height.toFloat()
        if (w < 50 || h < 50) return
        val dp = mapView.resources.displayMetrics.density
        // the card sits above its point, about 190 x 65 dp: keep the whole card on screen, clear of the round buttons down the side
        val halfW = 70f * dp; val cardH = 48f * dp; val btn = 84f * dp
        // first choice: the clear part of the map (not under the cards down the side or the top bar); else anywhere on screen
        val side = w * 0.40f
        val strict = floatArrayOf(
            if (cardsMirrored) btn + halfW else side + halfW, h * 0.14f + cardH,
            if (cardsMirrored) w - side - halfW else w - btn - halfW, h * 0.84f)
        val loose = floatArrayOf(halfW + 12f * dp, cardH + 12f * dp, w - halfW - 12f * dp, h - 24f * dp)
        val spots = liveLines.map { line ->
            if (line.size < 2) return@map null
            // look along the line (about 150 points at most), from the turn-off outwards
            val step = maxOf(1, line.size / 150)
            val sample = ArrayList<LL>()
            // a little way along the route itself (about 150 m from the turn-off), so cards of routes leaving the same point don't pile up
            var acc = 0.0; var i0 = 0
            while (i0 < line.size - 1 && acc < 150.0) { acc += Geo.dist(line[i0], line[i0 + 1]); i0++ }
            var i = minOf(maxOf(2, i0), line.size - 1)
            while (i < line.size) { sample += line[i]; i += step }
            val px = pixels(sample)
            if (px.size != sample.size) return@map null
            fun first(b: FloatArray): LL? {
                for (k in sample.indices) { val p = px[k]; if (p.x >= b[0] && p.x <= b[2] && p.y >= b[1] && p.y <= b[3]) return sample[k] }
                return null
            }
            first(strict) ?: first(loose)
        }
        val zoom = try { mbMap.cameraState.zoom } catch (e: Throwable) { 15.0 }
        val moved = spots.size != cardSpots.size || spots.indices.any { k ->
            val a = spots[k]; val b = cardSpots.getOrNull(k)
            if (a == null || b == null) a != b else Geo.dist(a, b) > (if (zoom > 14) 25.0 else 150.0)
        }
        if (moved) { cardSpots = spots; pushCards() }
    }

    /** Set by the screen: the cards are on the right (left-hand drive), so the clear part of the map is on the left. */
    var cardsMirrored = false

    private fun pushLive() {
        if (style == null) return
        try {
            val fl = ArrayList<Feature>()
            liveLines.forEachIndexed { i, l -> if (l.size >= 2) fl += line(l).also { it.addNumberProperty("i", i) } }
            src("dl-live-alts")?.featureCollection(FeatureCollection.fromFeatures(fl))
        } catch (e: Exception) { softFail(e) }
        pushCards()
    }

    /** Where each card is on the map right now (for taps), by alternative number. */
    private var shownCards: List<Pair<Int, LL>> = emptyList()

    /** Just the cards (moved often as the map moves; the lines themselves are only sent when they change). */
    private fun pushCards() {
        val st = style ?: return
        try {
            val fb = ArrayList<Feature>()
            val shown = ArrayList<Pair<Int, LL>>()
            liveBubs.forEachIndexed { i, p0 ->
                // the on-screen spot if it's been worked out, else the starting one; nothing when the whole line is off screen
                val p = if (i < cardSpots.size) (cardSpots[i] ?: return@forEachIndexed) else p0
                val diff = liveDiffs.getOrElse(i) { 0 }
                val tr = liveTraffic.getOrElse(i) { 0 }
                val nm = "dl-bub-$diff-$tr-${if (nightStyle) "n" else "d"}"
                if (bubImgs.add(nm)) addImg(st, nm, bubbleBitmap(diff, tr))
                fb += Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat)).also { it.addNumberProperty("i", i); it.addStringProperty("img", nm) }
                shown += i to p
            }
            shownCards = shown
            src("dl-live-bub")?.featureCollection(FeatureCollection.fromFeatures(fb))
        } catch (e: Exception) { softFail(e) }
    }

    // ---- rain radar ----
    private var radarUrl: String? = null

    /** Shows the live rain radar tiles under the route ([url] with {z}/{x}/{y}), or hides it with null. */
    fun setRadar(url: String?) {
        radarUrl = url
        main0.post { pushRadar() }
    }

    private fun pushRadar() {
        val st = style ?: return
        try {
            if (st.styleLayerExists("dl-radar")) st.removeStyleLayer("dl-radar")
            if (st.styleSourceExists("dl-radar")) st.removeStyleSource("dl-radar")
            val u = radarUrl ?: return
            addS(st, RasterSource.Builder("dl-radar").tiles(listOf(u)).tileSize(256).minzoom(0).maxzoom(7).build())
            addL(st, RasterLayer("dl-radar", "dl-radar").inSlot(MID).rasterOpacity(0.55).rasterFadeDuration(0.0).rasterEmissiveStrength(1.0), "dl-alts-line")
        } catch (e: Exception) { softFail(e) }
    }

    // ---- cinematic route preview ----
    private var flyToken = 0
    private var flyDone: (() -> Unit)? = null

    /**
     * A short fly-through of a new route: the camera swoops low along it in 3D (about 3 s), then [done] runs (the overview).
     * Touching the map stops it straight away and goes to the overview.
     */
    fun flyRoute(pts: List<LL>, cum: DoubleArray, len: Double, tilt: Double = 60.0, full: Boolean = false, done: () -> Unit) {
        if (pts.size < 2) { done(); return }
        val token = ++flyToken
        flyDone = done
        val zoom = if (full) (if (len < 30_000) 15.2 else if (len < 120_000) 13.8 else 12.6)
            else when { len < 5_000 -> 15.0; len < 30_000 -> 13.0; len < 120_000 -> 11.2; len < 300_000 -> 10.0; else -> 9.0 }
        fun at(f: Double): Pair<LL, Double> {
            val d = len * f
            var i = cum.indexOfFirst { it >= d }; if (i < 1) i = 1
            val a = pts[i - 1]; val b = pts[min(pts.size - 1, i + 3)]
            return pts[i] to Geo.bearing(a, b)
        }
        // the full fly-over ("Fly over route"): about 20 to 40 seconds along the whole drive, closer in than the quick one
        val frames = if (!full) listOf(0.04 to 900, 0.45 to 1100, 0.9 to 1000) else {
            val n = 16
            val total = (len / 1000.0 * 1300.0).coerceIn(18_000.0, 40_000.0)
            (0 until n).map { i -> (0.02 + 0.96 * i / (n - 1)) to (total / n).toInt() }
        }
        fun step(n: Int) {
            if (token != flyToken) return
            if (n >= frames.size) { flyDone = null; done(); return }
            val (f, ms) = frames[n]
            val (c, br) = at(f)
            tiltNow = tilt
            val cam = CameraOptions.Builder().center(Point.fromLngLat(c.lon, c.lat)).zoom(zoom).pitch(tilt).bearing(br)
                .padding(EdgeInsets(0.0, 0.0, 0.0, 0.0)).build()
            var cancelled = false
            try {
                mbMap.easeTo(cam, MapAnimationOptions.mapAnimationOptions { duration(ms.toLong()) }, object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationCancel(animation: android.animation.Animator) {
                        cancelled = true
                        if (token == flyToken) { flyToken++; flyDone = null; done() }
                    }
                    override fun onAnimationEnd(animation: android.animation.Animator) { if (!cancelled) step(n + 1) }
                })
            } catch (e: Throwable) { if (token == flyToken) { flyToken++; flyDone = null; done() } }
        }
        step(0)
    }

    /** Stops a fly-through without running its ending. */
    fun stopFly() { flyToken++; flyDone = null }

    /** Ends a route fly-through as a tap on the map does: its animation stops and its "done" runs (back to the overview). */
    fun cancelFly() {
        flyDone?.let { d -> flyDone = null; flyToken++; try { mapView.camera.cancelAllAnimators() } catch (e: Throwable) { softFail(e) }; d() }
    }

    /** The width (dp) of a route panel down the left of the screen (a phone held sideways), or 0 when the panel is along the bottom: [fit] leaves that part free. */
    @Volatile var fitSideDp = 0.0

    // ---- flowing light along the route ----
    private var flowOn = false
    private var flowWanted = true
    private var flowAnim: android.animation.ValueAnimator? = null
    private var flowLastPush = 0L

    /** A soft band of white light that brightens towards the middle; [p] is where it is along the line (0..1). */
    private fun flowGradient(p: Float): Expression {
        val c = p.coerceIn(0.07f, 0.93f).toDouble()
        fun w(a: Double) = Expression.rgba(255.0, 255.0, 255.0, a)
        return Expression.interpolate(
            Expression.linear(), Expression.lineProgress(),
            lit(0.0), w(0.0),
            lit(c - 0.06), w(0.0),
            lit(c), w(0.7),
            lit(c + 0.06), w(0.0),
            lit(1.0), w(0.0),
        )
    }

    /** Switches the flowing light on or off (it only runs while there is a route). */
    fun setFlow(on: Boolean) { flowWanted = on; syncFlow() }

    private fun syncFlow() {
        val want = flowWanted && routing
        if (want == flowOn) return
        flowOn = want
        main0.post {
            flowAnim?.cancel(); flowAnim = null
            try { flowLayer?.visibility(vis(flowOn)) } catch (e: Exception) { softFail(e) }
        }
    }

    /**
     * Moves the flowing light on a step. Called by the engine on the same beat as the camera, so the light and the camera change in
     * the same drawn frame (on its own timer the map was redrawn up to twice as often as needed, which a slow head unit can't keep up with).
     */
    fun flowFrame() {
        if (!flowOn) return
        val now = android.os.SystemClock.uptimeMillis()
        if (now - flowLastPush < 30) return
        flowLastPush = now
        try { flowLayer?.lineGradient(flowGradient((now % 3200L) / 3200f)) } catch (e: Exception) { softFail(e) }
    }

    private var pulseAnim: android.animation.ValueAnimator? = null

    /** Pulses the glow under the route line (used when a turn is close; the Modern look keeps the glow steady). */
    fun setTurnPulse(on: Boolean) {
        if (theme == "immersive") return
        if (on == (pulseAnim != null)) return
        main0.post {
            pulseAnim?.cancel(); pulseAnim = null
            val layer = glowLayer
            if (!on) {
                try { layer?.lineOpacity(0.4); layer?.lineWidth(routeW(1.7f)) } catch (e: Throwable) { softFail(e) }
            } else {
                pulseAnim = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 650; repeatMode = android.animation.ValueAnimator.REVERSE; repeatCount = android.animation.ValueAnimator.INFINITE
                    addUpdateListener {
                        val v = (it.animatedValue as Float).toDouble()
                        try { layer?.lineOpacity(0.35 + 0.55 * v); layer?.lineWidth(18.0 + 16.0 * v) } catch (e: Throwable) { softFail(e) }
                    }
                    start()
                }
            }
        }
    }

    /** Size of the locator arrow (and its glow) relative to normal: 1.0 is the base size. */
    var arrowScale = 2.4f
        private set

    // ---- 3D locator ----
    /** "arrow" (3D arrow), "car" (3D car) or "flat" (the flat picture). */
    var locatorStyle = "arrow"
        private set
    private var locatorColor = "#F2F2F6"
    private var modelZoom = -1.0
    private var spriteName: String? = null
    private var spriteDrawing: String? = null
    private var spriteYaw = 0.0
    private var spriteRot = 0.0
    private var spriteWanted: String? = null

    fun setLocator(styleName: String, color: String) {
        // the real 3D model: [color] is "<arrow|car>|<colour>"; when it can't be shown (Lite, Lightest, satellite) the picture of the
        // same shape and colour is used
        val wantModel = styleName == "model"
        val oldModel = modelKey()
        val oldNeon = neonArrowHex
        neonArrowHex = if (styleName == "flat" && color.startsWith("neon:")) color.removePrefix("neon:") else null
        if (wantModel) {
            val parts = color.split('|')
            modelGlow = parts.getOrNull(2) != "0"
            // your own model when there is one that can be read (else the built-in car stands in)
            modelShape = when (parts.getOrNull(0)) { "own" -> if (OwnModel.factor(ctx) != null) "own" else "car"; "car" -> "car"; else -> "arrow" }
            ownSize = parts.getOrNull(3)?.toIntOrNull()?.coerceIn(30, 300) ?: 100
            ownTurn = parts.getOrNull(4)?.toIntOrNull() ?: 0
            modelColour = parts.getOrNull(1)?.takeIf { it.startsWith("#") } ?: (if (modelCarLike()) "#F4F5F7" else "#F2F2F6")
        }
        modelWanted = wantModel && !modelBroken
        // your own picture, but none saved (or it was deleted): the 3D arrow
        val noPic = styleName == "custom" && !java.io.File(ctx.filesDir, "locator.png").exists()
        locatorStyle = when { wantModel -> modelShape; noPic -> "arrow"; else -> styleName }
        locatorColor = when { wantModel -> modelColour; noPic -> "#F2F2F6"; else -> color }
        if (style == null) return
        // the model's layers are put in (or taken out, or recoloured) with the rest of the app's layers
        if (oldModel != modelKey() || oldNeon != neonArrowHex) reinstall() else applyLocatorVisibility()
        spriteName = null
        pushModel(true)
    }

    // ---- the real 3D model locator (Full 3D only) ----
    private var modelWanted = false
    /** The 3D model failed to load once: it isn't tried again this run. */
    private var modelBroken = false
    private var modelShape = "arrow"
    /** Your own model: its size in per cent of the car's, and the turn (degrees) that points it the way you drive. */
    private var ownSize = 100
    private var ownTurn = 0
    private fun modelKey() = Triple(modelWanted, modelShape, modelColour + modelGlow + "|$ownSize|$ownTurn|" + OwnModel.stamp(ctx))
    /** The car's proportions: the built-in car, or your own model (which is sized to the car). */
    private fun modelCarLike() = modelShape == "car" || modelShape == "own"
    /** Your own model is sized to the car: its own bounds set the factor, then the size chosen. */
    private fun ownFactor(): Double = if (modelShape == "own") (OwnModel.factor(ctx) ?: 1.0) * ownSize / 100.0 else 1.0
    private fun bodyUri(): String = if (modelShape == "own") (OwnModel.url(ctx) ?: carModelUri("body", carModel)) else if (modelCarLike()) carModelUri("body", carModel) else "asset://models/arrow.glb"
    private fun partsUri(): String = if (modelCarLike() && modelShape != "own") carModelUri("parts", carModel) else "asset://models/disc.glb"
    private fun headingOut(): Double = meHeading + (if (modelShape == "own") ownTurn.toDouble() else 0.0)
    private var modelColour = "#F2F2F6"
    /** Settings > Your arrow > Glow: a soft blue glow on the ground round the model, and the model fully lit. */
    private var modelGlow = true
    /** The 3D model is on the map now (it's then drawn instead of the picture). */
    private var modelOn = false
    private var modelAt: LL? = null
    private var modelHead = -999.0

    /** Screen pixels per model unit (the model is built in the same units as the picture's shape). */
    private fun modelPxPerUnit(): Double = arrowScale * (if (modelCarLike()) 300.0 / 4.4 * 0.6 * 0.28 else 300.0 * 0.42 * 0.31) * ownFactor()

    /**
     * How big the car is on screen, in the same measure as [modelPxPerUnit] for the built-in car: for your own model without its
     * unit factor (a model drawn in millimetres has a factor of about 0.001, one in odd small units of 100 or more), only with the size
     * you set. The glow and the road name under the model go by this, so they fit round it as round the built-in car.
     */
    private fun modelPxCar(): Double = arrowScale * (if (modelCarLike()) 300.0 / 4.4 * 0.6 * 0.28 else 300.0 * 0.42 * 0.31) *
        (if (modelShape == "own") ownSize / 100.0 else 1.0)

    /** The glow's radius round the model (see [modelPxCar]). */
    private fun glowRadius(): Double = modelPxCar() / max(0.1, pixelRatioUsed.toDouble()) * (if (modelCarLike()) 2.2 else 1.45)

    /**
     * The model's size: the same size on screen at every zoom, exactly like the picture locator. Mapbox's location-indicator models
     * are sized in screen pixels per model unit ("viewport" scale mode), in the map's own pixels (screen pixels / pixel ratio).
     */
    private fun modelScale(): List<Double> {
        val s = modelPxPerUnit() / max(0.1, pixelRatioUsed.toDouble())
        return listOf(s, s, s)
    }



    private fun installModel(st: Style) {
        val car = modelCarLike()
        val p = me
        val uriA = bodyUri()
        val uriB = partsUri()
        try {
            for ((id, uri) in listOf("dl-me-ma" to uriA, "dl-me-mb" to uriB)) {
                val props = HashMap<String, Value>()
                props["type"] = Value("model")
                props["models"] = modelSourceValue(uri, p, headingOut())
                st.addStyleSource(id, Value(props))
            }
            val scale = modelScale()
            // the glow: a soft, blurred blue circle lying on the ground round the disc (under the model, same size on screen at every zoom)
            if (modelGlow) {
                addS(st, GeoJsonSource.Builder("dl-me-glow").featureCollection(FeatureCollection.fromFeatures(
                    if (p != null) listOf(Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat))) else emptyList())).build())
                addL(st, CircleLayer("dl-me-glow-l", "dl-me-glow").inSlot(TOP)
                    .circleRadius(glowRadius()).circleBlur(0.85).circleColor("#4C9BE6").circleOpacity(0.55)
                    .circlePitchAlignment(CirclePitchAlignment.MAP).circleEmissiveStrength(1.0))
            }
            // the untinted part first (the blue disc under the arrow, or the car's glass, lights and tyres), then the coloured part
            // set up the way Mapbox's own location marker is (a "location indicator" model, no shadows): the tested path
            addL(st, ModelLayer("dl-me-model-b", "dl-me-mb").modelType(ModelType.LOCATION_INDICATOR).modelScaleMode(ModelScaleMode.VIEWPORT).modelScale(scale)
                .modelCastShadows(false).modelReceiveShadows(false).modelEmissiveStrength(if (car) (if (modelGlow) 0.85 else 0.6) else 1.0)
                .visibility(vis(p != null)))
            // the arrow / car in your colour. In the default white it keeps the model's own shading (a white top over a grey-blue
            // edge, matched to the picture locator); a chosen colour tints it all. Mostly self-lit, so night doesn't grey it out
            addL(st, ModelLayer("dl-me-model-a", "dl-me-ma").modelType(ModelType.LOCATION_INDICATOR).modelScaleMode(ModelScaleMode.VIEWPORT).modelScale(scale)
                .modelColor(modelColour).modelColorMixIntensity(if (modelColour.equals(if (car) "#F4F5F7" else "#F2F2F6", true)) 0.0 else 1.0)
                .modelCastShadows(false).modelReceiveShadows(false).modelEmissiveStrength(if (modelGlow) 1.0 else if (car) 0.75 else 0.9)
                .visibility(vis(p != null)))
            modelOn = st.styleLayerExists("dl-me-model-a")
            modelAt = p; modelHead = meHeading
        } catch (e: Throwable) { modelOn = false; android.util.Log.w("MapBinding", "3D model: ${e.message}") }
    }

    /** Moves the 3D model to where the arrow is now (cheap: only its position and heading change, nothing is re-read). */
    private fun moveModel() {
        val st = style ?: return
        val p = me ?: return
        val prev = modelAt
        if (prev != null && Math.abs(prev.lat - p.lat) < 1e-8 && Math.abs(prev.lon - p.lon) < 1e-8 && Math.abs(modelHead - meHeading) < 0.2) return
        val first = prev == null
        modelAt = p; modelHead = meHeading
        val car = modelCarLike()
        try {
            st.setStyleSourceProperty("dl-me-ma", "models", modelSourceValue(bodyUri(), p, headingOut()))
            st.setStyleSourceProperty("dl-me-mb", "models", modelSourceValue(partsUri(), p, headingOut()))
            if (modelGlow && (first || prev == null || Math.abs(prev.lat - p.lat) > 1e-8 || Math.abs(prev.lon - p.lon) > 1e-8))
                src("dl-me-glow")?.featureCollection(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat)))))
            if (first) { layer("dl-me-model-a")?.visibility(vis(true)); layer("dl-me-model-b")?.visibility(vis(true)) }
        } catch (e: Throwable) { softFail(e) }
    }

    /** Sprites are drawn at 2.4x; this brings them to the same on-screen size the flat arrow had (the car a little longer). */
    private fun spriteSize(): Float = arrowScale * 1.22f * (if (locatorStyle == "car") 0.28f else 0.31f)


    /** A shape with soft corners: every corner of [pts] is replaced by a small curve of radius about [r] (in the same units). */




    /** Which car the "Car" locator is: saloon, hatch, estate, suv, pickup, van or sports (Settings > Your arrow). */
    @Volatile var carModel = "saloon"
        private set
    fun setCarModel(m: String) {
        val v = if (m in CAR_MODELS) m else "saloon"
        if (v == carModel) return
        carModel = v
        main0.post {
            spriteBmps.clear(); spriteName = null
            // the real 3D car: point it at the new car type's model files (only the two model sources change, nothing else is redrawn)
            try {
                val st = style
                if (st != null && modelOn && modelShape == "car") {
                    st.setStyleSourceProperty("dl-me-ma", "models", modelSourceValue(carModelUri("body", carModel), me ?: modelAt, meHeading))
                    st.setStyleSourceProperty("dl-me-mb", "models", modelSourceValue(carModelUri("parts", carModel), me ?: modelAt, meHeading))
                }
            } catch (e: Throwable) { softFail(e) }
            try { pushModel(true) } catch (e: Throwable) { softFail(e) }
        }
    }

    /** A small top-down picture of a car model in a colour, for choosing one in Settings. */
    fun carPreview(model: String, col: String): Bitmap = renderSprite(carParts(col, model), 300.0 / 5.4, 0.0, 0.0, true)




    /**
     * The 3D locator is an Android view laid over the map, not a picture inside it. A new picture is needed every 8 degrees of
     * turn and 5 of tilt, and adding a picture to the map makes it lay out every label and icon on screen again: going round a
     * roundabout (a full turn in a few seconds) did that dozens of times and the map dropped to a few frames a second. Swapping
     * the picture of a view costs nothing, and the view is moved to the car's spot on screen after every camera move.
     */
    val locatorView: android.widget.ImageView by lazy {
        android.widget.ImageView(ctx).apply {
            isClickable = false; isFocusable = false
            scaleType = android.widget.ImageView.ScaleType.FIT_XY
            visibility = android.view.View.GONE
        }
    }
    /** The road-name pill under the locator, also a view over the map. */
    val roadView: android.widget.ImageView by lazy {
        android.widget.ImageView(ctx).apply {
            isClickable = false; isFocusable = false
            scaleType = android.widget.ImageView.ScaleType.FIT_XY
            visibility = android.view.View.GONE
        }
    }
    private val spriteBmps = LinkedHashMap<String, Bitmap>()
    private var spriteBmp: Bitmap? = null

    /** Picks (and if needed draws, in the background) the picture for the current heading, map bearing and tilt. */
    private fun pushModel(force: Boolean) {
        if (locatorStyle == "flat" || modelOn) { placeLocator(); return }
        val cam = try { mbMap.cameraState } catch (e: Throwable) { return }
        modelZoom = cam.zoom
        val rel = ((meHeading - cam.bearing) % 360.0 + 360.0) % 360.0
        // pictures every 8 degrees of turn; the small difference in between is made up by turning the view itself, so the arrow/car
        // rotates perfectly smoothly instead of clicking from one picture to the next
        // your own picture: "<version>|<turns with you 1/0>|<circle 1/0>"
        val custom = locatorStyle == "custom"
        val cOpts = if (custom) locatorColor.split('|') else emptyList()
        val cTurns = cOpts.getOrNull(1) != "0"
        val cCircle = cOpts.getOrNull(2) == "1"
        val upright = custom && !cTurns
        val yawB = if (upright) 0L else (Math.round(rel / 8.0) * 8) % 360
        val pitB = if (upright) 0L else Math.round(cam.pitch / 5.0) * 5
        val key = "spr-$locatorStyle-${if (locatorStyle == "car") carModel else ""}-${locatorColor.removePrefix("#")}-$yawB-$pitB"
        if (key != spriteName || force) {
            val have = spriteBmps[key]
            if (have != null) {
                spriteBmps.remove(key); spriteBmps[key] = have      // most recently used last
                spriteName = key; spriteYaw = yawB.toDouble()
                if (spriteBmp !== have) { spriteBmp = have; locatorView.setImageBitmap(have) }
            } else if (spriteDrawing == null) {
                spriteDrawing = key
                val car = locatorStyle == "car"; val col = locatorColor
                Thread {
                    val bmp = try {
                        if (custom) customImage(cOpts.getOrNull(0) ?: "")?.let { renderCustom(it, yawB.toDouble(), pitB.toDouble(), !upright, cCircle) }
                        else if (car) renderSprite(carParts(col, carModel), 300.0 / 4.4 * 0.6, yawB.toDouble(), pitB.toDouble(), true)
                        else renderSprite(arrowParts(col), 300.0 * 0.42, yawB.toDouble(), pitB.toDouble(), false)
                    } catch (e: Throwable) { null }
                    main0.post {
                        spriteDrawing = null
                        if (bmp != null) {
                            spriteBmps[key] = bmp
                            // the last 48 pictures are kept (a full turn at one tilt is 45)
                            while (spriteBmps.size > 48) spriteBmps.remove(spriteBmps.keys.first())
                            pushModel(false)
                        } else spriteName = key      // couldn't draw it: don't keep retrying the same picture
                    }
                }.start()
            }
            // (while a picture is being drawn, the current one stays up, turned to the right angle)
        }
        spriteRot = if (upright) 0.0 else ((rel - spriteYaw + 540.0) % 360.0) - 180.0
        placeLocator()
    }

    @Volatile private var customBase: Bitmap? = null
    @Volatile private var customVer: String? = null

    /** Your locator picture (saved small by the settings page), loaded once per version. */
    private fun customImage(ver: String): Bitmap? {
        // (the white-background switch is part of the key, so turning it on or off shows at once)
        val keepWhite = Prefs(ctx).b(PreferenceKeys.LOCATOR_REMOVE_WHITE, true)
        val key = "$ver|$keepWhite"
        if (customVer == key) return customBase
        val f = java.io.File(ctx.filesDir, "locator.png")
        val b = try { if (f.exists()) android.graphics.BitmapFactory.decodeFile(f.path) else null } catch (e: Throwable) { null }
        val out = if (b != null && keepWhite) try { stripWhiteEdge(b) } catch (e: Throwable) { b } else b
        customBase = out; customVer = key
        return out
    }


    /** The flat arrow's picture (for spacing the road name under it). */
    private var flatArrow: Bitmap? = null


    /** Puts the locator view on the car's spot on screen (called after every camera move and every move of the car). */
    fun placeLocator() {
        val v = locatorView
        val p = me
        val bmp = spriteBmp
        fun gone(x: android.view.View) { if (x.visibility != android.view.View.GONE) x.visibility = android.view.View.GONE }
        if (p == null || style == null) { gone(v); gone(roadView); return }
        val pt = try { mbMap.pixelForCoordinate(Point.fromLngLat(p.lon, p.lat)).let { android.graphics.PointF(it.x.toFloat(), it.y.toFloat()) } } catch (e: Throwable) { return }
        // the road name: drawn at 2x and shown at half, its top a little below the locator (lower when the view is flatter)
        val rb = (roadView.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        if (roadName.isBlank() || rb == null || roadNameHidden) gone(roadView) else {
            // (times the road name size from Settings > Display)
            val rw = (rb.width / 2 * roadNameScale).toInt(); val rh = (rb.height / 2 * roadNameScale).toInt()
            val rlp = roadView.layoutParams
            if (rlp == null || rlp.width != rw || rlp.height != rh) roadView.layoutParams = android.widget.FrameLayout.LayoutParams(rw, rh)
            roadView.translationX = pt.x - rw / 2f
            // just below whichever reaches lower: the blue disc under the arrow (flattened by the tilt) or the arrow / car picture
            val dens = mapView.resources.displayMetrics.density
            val tilt = try { mbMap.cameraState.pitch } catch (e: Throwable) { tiltNow }
            // the Classic look has a flat blue ring on the map under the arrow (the Modern look draws its ring in the picture itself)
            val discHalf = if (theme == "immersive") 0f else 15f * 0.625f * arrowScale * dens * Math.cos(Math.toRadians(tilt)).toFloat()
            // the picture has empty space round the arrow / car: measure where its drawing really ends at the bottom
            val below = if (modelOn) (modelPxCar() * (if (modelCarLike()) 2.3 else 0.85) * Math.cos(Math.toRadians(tilt))).toFloat()
                        else if (locatorStyle != "flat" && bmp != null) drawnBelowCentre(bmp) * spriteSize()
                        else flatArrow?.let { drawnBelowCentre(it) * (if (theme == "immersive") 0.25f else 0.625f) * arrowScale * flatIconK().toFloat() * Math.cos(Math.toRadians(tilt)).toFloat() } ?: 0f
            roadView.translationY = pt.y + maxOf(discHalf, below) + 3f * dens
            if (roadView.visibility != android.view.View.VISIBLE) roadView.visibility = android.view.View.VISIBLE
        }
        if (modelOn) { gone(v); moveModel(); return }
        if (locatorStyle == "flat" || bmp == null) { gone(v); return }
        // the same size the map gave the picture before: its pixels times the icon size
        val k = spriteSize()
        val w = Math.round(bmp.width * k); val h = Math.round(bmp.height * k)
        val lp = v.layoutParams
        if (lp == null || lp.width != w || lp.height != h) v.layoutParams = android.widget.FrameLayout.LayoutParams(w, h)
        v.translationX = pt.x - w / 2f
        v.translationY = pt.y - h / 2f
        v.rotation = spriteRot.toFloat()
        if (v.visibility != android.view.View.VISIBLE) v.visibility = android.view.View.VISIBLE
    }

    /** Icon size for the arrow head at a zoom where the route line is [w] map pixels wide: about 2.4 times the shaft's width. */
    private fun headSize(w: Float): Float = 2.4f * 1.28f * w * ROUTE_K / 196f


    private var chevOn = true
    /** Set by the engine: whether the turn arrow is switched on (so the route line is only cut when the arrow is really there). */
    var turnArrowsShown = true


    /** Chevrons along the route on or off (the turn arrow has its own setting, checked in the engine). */
    fun setRouteArrows(on: Boolean) {
        chevOn = on
        try { layer("dl-route-chev")?.visibility(vis(on)) } catch (e: Exception) { softFail(e) }
    }

    fun setArrowScale(f: Float) {
        arrowScale = f
        val im = theme == "immersive"
        try {
            (layer("dl-me-arrow") as? SymbolLayer)?.iconSize((if (im) 0.25 else 0.625) * f)
            (layer("dl-me-halo") as? CircleLayer)?.circleRadius(15.0 * 0.625 * f)
            if (modelOn) { val sc = modelScale(); (layer("dl-me-model-a") as? ModelLayer)?.modelScale(sc); (layer("dl-me-model-b") as? ModelLayer)?.modelScale(sc)
                (layer("dl-me-glow-l") as? CircleLayer)?.circleRadius(glowRadius()) }
        } catch (e: Exception) { softFail(e) }
        placeLocator()
    }

    /** The arrow at the next turn, drawn along the route through the junction; an empty list clears it. */
    private var turnCut: List<LL>? = null
    private var cutActive = false
    private fun cutZoomOk(): Boolean = (try { mbMap.cameraState.zoom } catch (e: Throwable) { 16.0 }) >= 13.0

    private var turnPath: List<LL> = emptyList()

    /** Puts the remembered turn arrow back (after the map style reloads, e.g. day to night, its layers start empty). */
    private fun pushTurn() {
        val path = turnPath
        try {
            if (path.size < 2) { src("dl-turn")?.featureCollection(empty()); src("dl-turn-head")?.featureCollection(empty()); return }
            src("dl-turn")?.feature(line(path))
            val a = path[path.size - 2]; val b = path.last()
            src("dl-turn-head")?.feature(Feature.fromGeometry(Point.fromLngLat(b.lon, b.lat)).also { it.addNumberProperty("b", Geo.bearing(a, b)) })
        } catch (e: Exception) { softFail(e) }
    }

    fun setTurnArrow(path: List<LL>) {
        main0.post {
            turnPath = path
            turnCut = if (path.size >= 2 && turnArrowsShown) path else null
            try { setLine(main) } catch (e: Exception) { softFail(e) }
            try {
                if (path.size < 2) { src("dl-turn")?.featureCollection(empty()); src("dl-turn-head")?.featureCollection(empty()); return@post }
                // the shaft runs right to the end, where the head's flat base sits on it
                src("dl-turn")?.feature(line(path))
                val a = path[path.size - 2]; val b = path.last()
                src("dl-turn-head")?.feature(Feature.fromGeometry(Point.fromLngLat(b.lon, b.lat)).also { it.addNumberProperty("b", Geo.bearing(a, b)) })
            } catch (e: Exception) { softFail(e) }
        }
    }
    private var roadName = ""
    private var roadOffTilt = -99.0


    /** Size of the road name pill under the locator (1 = normal), from Settings > Display. */
    private var roadNameScale = 1f
    fun setRoadNameScale(s: Float) {
        val v = s.coerceIn(0.5f, 2.5f)
        if (v == roadNameScale) return
        roadNameScale = v
        try { roadView.scaleType = android.widget.ImageView.ScaleType.FIT_XY } catch (e: Throwable) { softFail(e) }
        placeLocator()
    }

    /**
     * Hides the road name pill under the locator while the screen shows the road elsewhere (the "road · Heading NW" pill at the
     * bottom when driving with no route): the two used to sit on top of each other.
     */
    @Volatile private var roadNameHidden = false
    fun setRoadNameHidden(h: Boolean) {
        if (h == roadNameHidden) return
        roadNameHidden = h
        main0.post { try { placeLocator() } catch (e: Throwable) { softFail(e) } }
    }

    /** Shows the name of the road you are on in a small pill under the locator (empty hides it). */
    fun setRoadName(name: String) {
        if (name == roadName) return
        roadName = name
        // a view over the map: a new name used to be a new map picture, which made the map lay out all its labels again
        try { if (name.isNotBlank()) roadView.setImageBitmap(roadBitmap(name)) } catch (e: Exception) { softFail(e) }
        placeLocator()
    }

    /** Recolours the route line straight away when its colour is changed in Settings > Map > Own design. */
    fun setRouteColour(@Suppress("UNUSED_PARAMETER") hex: String) {
        main0.post {
            val st = style ?: return@post
            val im = theme == "immersive"; val day = !nightStyle
            for (id in listOf("dl-route-line", "dl-route-glow")) try {
                if (st.styleLayerExists(id)) st.setStyleLayerProperty(id, "line-color", sevColor(im, day))
            } catch (e: Throwable) { softFail(e) }
        }
    }

    /** Recolours the other-route lines straight away when the colour is changed in Settings (the style itself is only built once). */
    fun setAltColour(hex: String) {
        val c = try { android.graphics.Color.parseColor(hex); hex } catch (e: Exception) { return }
        main0.post {
            val st = style ?: return@post
            for (id in listOf("dl-alts-line", "dl-live-alts-line")) try {
                if (st.styleLayerExists(id)) st.setStyleLayerProperty(id, "line-color", Value.valueOf(c))
            } catch (e: Throwable) { softFail(e) }
        }
    }

    /**
     * The map only redraws when something on it changes, and while following you that's the camera, which the engine moves on a steady
     * beat set by Settings > Display > Frame rate. The map itself is left free to draw each of those updates the moment it arrives: capping
     * it at the same rate made it drop the odd update (its timer and the screen's never line up exactly), which looked jumpy.
     */
    fun setMaxFps(n: Int) { try { mapView.setMaximumFps(60) } catch (e: Exception) { softFail(e) } } // (deliberately not [n]: see above)

    /** A hard cap on how often the map draws: only for Cool running on the car screen (30), and back to 60 when it's off. */
    fun capFps(n: Int) { try { mapView.setMaximumFps(n.coerceIn(15, 60)) } catch (e: Exception) { softFail(e) } }

    private var doneEmpty = true

    /** The driven stretch of the route (a short trail behind the car), drawn in grey. */
    fun setDone(pts: List<LL>) {
        val s = src("dl-done") ?: return
        if (pts.size < 2) { if (!doneEmpty) { s.featureCollection(empty()); doneEmpty = true }; return }
        doneEmpty = false
        s.feature(line(pts))
    }

    fun setMainOnly(mainPts: List<LL>, sev: IntArray? = null, firstSeg: Int = 0) {
        main = mainPts; mainSev = sev; mainOff = firstSeg; setLine(main)
        // the lights are picked against the route: as the drawn stretch of route moves on, pick them again (they used to stay as
        // they were for the first stretch, so lights further along never appeared)
        lights.push()
    }
    fun setDest(p: LL?) { dest = p; pushDest(); pushFlags() }
    /** The stops on the way, in order; true = a charging stop. Kept, so they're drawn again after a style change. */
    /** [labels] (same order as [stops]) name a charging stop's power on the map: "150 kW", or "?" when it isn't known. */
    fun setVia(stops: List<Pair<LL, Boolean>>, labels: List<String?> = emptyList(), names: List<String?> = emptyList()) { via = stops; viaLabels = labels; viaNames = names; pushVia() }
    private var meAnim: android.animation.ValueAnimator? = null
    private var lastMePush = 0L

    /** Moves the arrow smoothly from where it is now to [p] over [durationMs] (one GPS interval). */
    /**
     * Opens the map where you were last time (saved every 15 s while driving), facing the same way, with your arrow already there,
     * instead of over London until the GPS answers.
     */
    fun startAt(p: LL, bearing: Double?) {
        try {
            val b = CameraOptions.Builder().center(Point.fromLngLat(p.lon, p.lat)).zoom(16.5).pitch(55.0)
            if (bearing != null) b.bearing(bearing)
            mbMap.setCamera(b.build())
        } catch (e: Throwable) { softFail(e) }
        try { setMe(p, bearing ?: 0.0, 0) } catch (e: Throwable) { softFail(e) }
    }

    fun setMe(p0: LL?, heading: Double = 0.0, durationMs: Long = 1000) {
        // in the lane stretch before a junction your arrow rides in the lane with the route line, not on the road's middle line
        val p = p0?.let { onShift(it) }
        meAnim?.cancel()
        if (durationMs <= 0 && p != null) {
            val o = me
            if (o != null && Math.abs(o.lat - p.lat) < 1e-8 && Math.abs(o.lon - p.lon) < 1e-8 && Math.abs(meHeading - heading) < 0.2) return
        }
        val from = me
        if (p == null || from == null || durationMs <= 0 || Geo.dist(from, p) > 300) {
            me = p; meHeading = heading; pushMe(); return
        }
        val fromH = meHeading
        val dH = ((heading - fromH + 540.0) % 360.0) - 180.0
        meAnim = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { a ->
                val t = a.animatedFraction.toDouble()
                me = LL(from.lat + (p.lat - from.lat) * t, from.lon + (p.lon - from.lon) * t)
                meHeading = (fromH + dH * t + 360.0) % 360.0
                val now = android.os.SystemClock.uptimeMillis()
                if (now - lastMePush >= 66 || t >= 1.0) { lastMePush = now; pushMe() }
            }
            start()
        }
    }

    private fun pushDest() {
        val s = src("dl-dest") ?: return
        val pts = listOfNotNull(dest, pick)
        s.featureCollection(FeatureCollection.fromFeatures(pts.map { Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat)) }))
    }

    private val queueImgs = HashSet<String>()

    /** The queue pills on the map (see QueueMark); the picture of each minute count is made once per style. */
    fun setQueues(list: List<QueueMark>) {
        main0.post {
            val st = style ?: return@post
            val feats = list.map { q ->
                val name = "q-${if (q.heavy) "h" else "s"}-${if (q.dim) "d" else "n"}-${q.min}"
                if (queueImgs.add(name)) addImg(st, name, queueBitmap(q))
                Feature.fromGeometry(Point.fromLngLat(q.pos.lon, q.pos.lat)).also { it.addStringProperty("img", name) }
            }
            src("dl-queues")?.featureCollection(FeatureCollection.fromFeatures(feats))
        }
    }


    /** A spot picked by tapping the map in the car (shown with the destination pin until it's chosen or dropped). */
    private var pick: LL? = null
    fun setPick(p: LL?) { main0.post { pick = p; pushDest() } }

    /**
     * The map point under a tap at [sp] (screen pixels), with the name of the place drawn there (a shop, a station) when the tap
     * landed on one. [done] runs on the main thread; the place's own position is given when there is one.
     */
    fun pickAt(sp: android.graphics.PointF, done: (LL, String?) -> Unit) {
        val at = try { mbMap.coordinateForPixel(ScreenCoordinate(sp.x.toDouble(), sp.y.toDouble())) } catch (e: Throwable) { return }
        val ll = LL(at.latitude(), at.longitude())
        var answered = false
        fun answer(p: LL, n: String?) { if (answered) return; answered = true; done(p, n) }
        try {
            val r = 22.0 * mapView.resources.displayMetrics.density
            val box = RenderedQueryGeometry(ScreenBox(ScreenCoordinate(sp.x - r, sp.y - r), ScreenCoordinate(sp.x + r, sp.y + r)))
            mbMap.queryRenderedFeatures(com.mapbox.maps.interactions.standard.generated.StandardPoi(), box) { list ->
                val f = list.firstOrNull { !it.name.isNullOrBlank() }
                main0.post { if (f != null) answer(LL(f.geometry.latitude(), f.geometry.longitude()), f.name) else answer(ll, null) }
            }
            // a style without Standard's places (the offline map) never answers: the plain spot then
            main0.postDelayed({ answer(ll, null) }, 700L)
        } catch (e: Throwable) { answer(ll, null) }
    }

    /** The 3D flags: a thin white pole with a block for the flag on top, red at the destination and amber at each stop. */
    private fun flagFeats(t: Double = flagPhase): List<Feature> {
        if (!prefs.b(PreferenceKeys.FLAGS_3D, true)) return emptyList()
        val out = ArrayList<Feature>()
        fun poly(pts: List<LL>, base: Double, top: Double, colour: String) {
            val ring = (pts + pts.first()).map { Point.fromLngLat(it.lon, it.lat) }
            out += Feature.fromGeometry(Polygon.fromLngLats(listOf(ring))).also {
                it.addNumberProperty("b", base); it.addNumberProperty("h", top); it.addStringProperty("c", colour)
            }
        }
        fun at(p: LL, east: Double, north: Double) = Furniture.offset(Furniture.offset(p, 90.0, east), 0.0, north)
        fun pole(p: LL, half: Double, top: Double) = poly(listOf(at(p, half, half), at(p, -half, half), at(p, -half, -half), at(p, half, -half)), 0.0, top, "#F5F5F5")
        /**
         * The cloth as a grid of thin cells off the pole, each column pushed sideways by a wave that runs out along the flag and grows
         * towards its free end, so it waves in the wind ([t] moves the wave on). [colour] picks each cell's colour by column and row.
         */
        fun cloth(p: LL, len: Double, base: Double, top: Double, cols: Int, rows: Int, phase: Double, colour: (Int, Int) -> String) {
            val dx = len / cols; val dz = (top - base) / rows; val th = 0.12
            for (ci in 0 until cols) {
                val x0 = 0.25 + ci * dx; val x1 = x0 + dx
                val amp = 0.55 * (ci + 0.5) / cols
                val y0 = amp * Math.sin(t - ci * 0.9 + phase); val y1 = 0.55 * (ci + 1.5) / cols * Math.sin(t - (ci + 1) * 0.9 + phase)
                val quad = listOf(at(p, x0, y0 + th), at(p, x1, y1 + th), at(p, x1, y1 - th), at(p, x0, y0 - th))
                for (ri in 0 until rows) {
                    // the free end also droops a little as it swings
                    val sag = 0.25 * (ci + 0.5) / cols * (1 + Math.cos(t - ci * 0.9 + phase)) * 0.5
                    poly(quad, base + ri * dz - sag, base + (ri + 1) * dz - sag, colour(ci, ri))
                }
            }
        }
        // the destination: a chequered finish flag; the stops: yellow
        dest?.let { d -> pole(d, 0.3, 26.0); cloth(d, 4.2, 19.0, 25.5, 6, 4, 0.0) { c, r -> if ((c + r) % 2 == 0) "#111111" else "#F5F5F5" } }
        via.forEachIndexed { k, (p, _) -> pole(p, 0.25, 15.0); cloth(p, 2.8, 10.5, 14.5, 4, 1, k * 1.3) { _, _ -> "#FFB300" } }
        return out
    }
    private fun pushFlags() {
        try { src("dl-flags")?.featureCollection(FeatureCollection.fromFeatures(flagFeats())) } catch (e: Throwable) { softFail(e) }
        startFlagWave()
    }

    // ---- the flags wave: moved on a few times a second, only while one is on the map and the map is zoomed in close (14+) ----
    private var flagPhase = 0.0
    private var flagWaving = false
    private val flagTick = object : Runnable {
        override fun run() {
            if (destroyed || (dest == null && via.isEmpty()) || !prefs.b(PreferenceKeys.FLAGS_3D, true) || !prefs.b(PreferenceKeys.FLAGS_WAVE, true)) { flagWaving = false; return }
            // (the phone's map hidden behind another app or the settings: nothing to animate; the car's map is never "shown" as a view)
            if (!flushBranding && !mapView.isShown) { main0.postDelayed(this, 1000L); return }
            if (zoomNow() >= 14.0) {
                flagPhase = (flagPhase + 0.45) % (2 * Math.PI * 1000)
                try { src("dl-flags")?.featureCollection(FeatureCollection.fromFeatures(flagFeats())) } catch (e: Throwable) { softFail(e) }
            }
            main0.postDelayed(this, if (zoomNow() >= 14.0) 160L else 1000L)
        }
    }
    /** Settings > 3D flags or Waving flags changed: drawn again straight away. */
    fun refreshFlags() { main0.post { pushFlags() } }

    private fun startFlagWave() {
        if (flagWaving || (dest == null && via.isEmpty())) return
        flagWaving = true
        main0.postDelayed(flagTick, 160L)
    }

    /** The speed limit signs along the route: where each is, and the limit it shows (km/h, or mph when the units are miles). */
    private var limitSigns: List<Pair<LL, Int>> = emptyList()

    fun setLimitSigns(signs: List<Pair<LL, Int>>) { main0.post { limitSigns = signs; pushLimitSigns() } }

    private fun pushLimitSigns() {
        val s = src("dl-limsigns") ?: return
        val st = style ?: return
        for (v in limitSigns.map { it.second }.distinct()) try { addImg(st, "dl-lim-$v", limitSignBitmap(v)) } catch (e: Throwable) { softFail(e) }
        s.featureCollection(FeatureCollection.fromFeatures(limitSigns.map { (p, v) ->
            Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat)).also { it.addStringProperty("img", "dl-lim-$v") }
        }))
    }

    /** A round limit sign: white, with a red ring and the number. */
    private fun limitSignBitmap(v: Int): Bitmap {
        val bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawCircle(48f, 50f, 44f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55000000 })
        c.drawCircle(48f, 48f, 44f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
        c.drawCircle(48f, 48f, 38f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 10f; color = 0xFFD32F2F.toInt() })
        val text = v.toString()
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF202124.toInt(); textSize = if (text.length >= 3) 32f else 42f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
        c.drawText(text, 48f, 48f + tp.textSize * 0.35f, tp)
        return bmp
    }

    private fun pushVia() {
        val s = src("dl-via") ?: return
        val st = style
        s.featureCollection(FeatureCollection.fromFeatures(via.mapIndexed { i, (p, ev) ->
            val label = viaLabels.getOrNull(i)
            val img = when {
                !ev && st != null -> viaNumImage(st, i + 1, viaNames.getOrNull(i))
                !ev -> "dl-via-stop"
                label != null && st != null -> evLabelImage(st, label)
                else -> "dl-via-ev"
            }
            Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat)).also { it.addStringProperty("img", img) }
        }))
        pushFlags()
    }

    /** The charge points in view: [sites] are drawn with their power ("150 kW", or "?") and their free count ("2/5", or "5"). */
    fun setChargers(sites: List<ChargerSite>) { chargers = sites; pushChargers() }

    private fun pushChargers() {
        val s = src("dl-chargers") ?: return
        val st = style ?: return
        s.featureCollection(FeatureCollection.fromFeatures(chargers.map { c ->
            val kwLabel = c.kwLabel ?: c.kw?.let { "$it kW" } ?: "?"
            val countLabel = when {
                c.total == null -> ""
                c.free != null -> "${c.free}/${c.total}"
                else -> "${c.total}"
            }
            val id = "dl-chg-" + kwLabel.replace(Regex("[^A-Za-z0-9]"), "_") + "-" + countLabel.replace(Regex("[^A-Za-z0-9]"), "_")
            if (chgImgIds.add(id)) try { addImg(st, id, chargerBitmap(kwLabel, countLabel)) } catch (e: Throwable) { chgImgIds.remove(id) }
            Feature.fromGeometry(Point.fromLngLat(c.pos.lon, c.pos.lat)).also { it.addStringProperty("img", id) }
        }))
    }


    /** An orange numbered stop pin with the stop's name beside it, seen at every zoom (the number sits on the pin's middle). */
    private fun viaNumImage(st: Style, n: Int, name: String?): String {
        val label = name?.trim().orEmpty().take(24)
        val id = "dl-via-n-$n-" + label.hashCode()
        if (viaNumIds.add(id)) try { addImg(st, id, viaNumberedBitmap(n, label)) } catch (e: Throwable) { viaNumIds.remove(id); return "dl-via-stop" }
        return id
    }

    private fun viaNumberedBitmap(n: Int, label: String): Bitmap {
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF202124.toInt(); textSize = 26f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        val tw = if (label.isEmpty()) 0f else text.measureText(label)
        // the pin is in the middle of the picture (so it sits on the stop); the name goes on its right, and the left is left empty to match
        val half = (32f + (if (label.isEmpty()) 0f else 12f + tw + 18f)).toInt()
        val bmp = Bitmap.createBitmap(half * 2, 64, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cx = half.toFloat()
        if (label.isNotEmpty()) {
            c.drawRoundRect(RectF(cx + 14f, 15f, cx + 14f + tw + 24f, 49f), 17f, 17f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xEEFFFFFF.toInt() })
            c.drawText(label, cx + 26f, 41f, text)
        }
        c.drawCircle(cx, 34f, 27f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55000000 })
        c.drawCircle(cx, 32f, 27f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
        c.drawCircle(cx, 32f, 22f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFB8C00.toInt() })
        val num = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 28f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
        c.drawText(n.toString(), cx, 42f, num)
        return bmp
    }

    /** The charging-stop picture with its power under it. Registered again on each push (replaces the same name, cheap). */
    private fun evLabelImage(st: Style, label: String): String {
        val id = "dl-via-ev-" + label.replace(Regex("[^A-Za-z0-9]"), "_")
        try { addImg(st, id, viaEvLabelBitmap(label)) } catch (e: Throwable) { return "dl-via-ev" }
        return id
    }

    private fun viaEvLabelBitmap(label: String): Bitmap {
        val icon = viaEvBitmap()
        val w = 140; val h = 130
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawBitmap(icon, (w - icon.width) / 2f, 0f, null)
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 24f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
        val half = tp.measureText(label) / 2f + 8f
        c.drawRoundRect(android.graphics.RectF(w / 2f - half, 100f, w / 2f + half, 128f), 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xE6202124.toInt() })
        c.drawText(label, w / 2f, 122f, tp)
        return bmp
    }

    private var meShownFlat = false

    private fun pushMe() {
        pushModel(false)
        pushBeam()
        updateClear(false)
        // the map's own (flat) arrow only needs moving when it's the one showing; the 3D one is a view over the map
        val flat = locatorStyle == "flat"
        if (!flat && !meShownFlat) return
        val s = src("dl-me") ?: return
        val p = me
        if (p != null && flat) {
            val f = Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat))
            f.addNumberProperty("h", meHeading)
            s.feature(f)
            meShownFlat = true
        } else { s.featureCollection(empty()); meShownFlat = false }
    }

    private fun applyLocatorVisibility() {
        try { layer("dl-me-arrow")?.visibility(vis(locatorStyle == "flat")) } catch (e: Exception) { softFail(e) }
        pushMe()
        placeLocator()
    }

    // ---- camera ----
    /** The visible area as [minLat, minLon, maxLat, maxLon, zoom] (a little larger than the screen), or null. */
    fun viewBox(): DoubleArray? = try {
        val cs = mbMap.cameraState
        val c = cs.center
        val w = mapView.width.toDouble().coerceAtLeast(1.0); val h = mapView.height.toDouble().coerceAtLeast(1.0)
        val pxPerDeg = Math.pow(2.0, cs.zoom) * 512.0 / 360.0
        val dLon = w / 2.0 / pxPerDeg * 1.1
        val dLat = h / 2.0 * Math.cos(Math.toRadians(c.latitude())).coerceAtLeast(0.05) / pxPerDeg * 1.1
        doubleArrayOf(c.latitude() - dLat, c.longitude() - dLon, c.latitude() + dLat, c.longitude() + dLon, cs.zoom)
    } catch (e: Throwable) { null }

    fun center(): LL? = try { mbMap.cameraState.center.let { LL(it.latitude(), it.longitude()) } } catch (e: Throwable) { null }

    /**
     * The part of the map that can actually be seen, as insets from the view's edges in pixels (on Android Auto its own cards and
     * buttons cover some of the car screen; null means the whole view). The locator and the follow camera are placed within it.
     */
    @Volatile var viewInset: android.graphics.Rect? = null
    /** How far down the locator may sit (as the top padding fraction): kept higher on tall, narrow car screens. */
    @Volatile var maxTopFrac = 1.0
    /** Pixels at the bottom of the visible part covered by the app's own cards (tall car screens), kept clear of the locator. */
    @Volatile var coverTarget = 0.0
    // eased towards coverTarget a little each frame, so the locator glides rather than jumps when a card comes or goes
    private var coverNow = 0.0
    private fun cover(): Double {
        val t = coverTarget
        coverNow += (t - coverNow) * 0.08
        if (abs(t - coverNow) < 1.0) coverNow = t
        return coverNow
    }

    /** Camera padding (left, top, right, bottom) for a locator place given as fractions, within [viewInset] (see CameraPadding). */
    private fun pads(topF: Double, leftF: Double): DoubleArray =
        CameraPadding.pads(mapView.width.toDouble(), mapView.height.toDouble(), viewInset, maxTopFrac, topF, leftF) { cover() }

    /** The camera padding as Mapbox's insets. */
    private fun edge(pd: DoubleArray): EdgeInsets = CameraPadding.edge(pd, mapView.width.toDouble(), mapView.height.toDouble())

    fun cameraTo(c: LL, bearing: Double?, tilt: Double, zoom: Double, ms: Int, topPadFraction: Double, leftPadFraction: Double = 0.0) {
        val pd = pads(topPadFraction, leftPadFraction)
        val b = CameraOptions.Builder().center(Point.fromLngLat(c.lon, c.lat)).pitch(tilt).zoom(zoom).padding(edge(pd))
        if (bearing != null) b.bearing(bearing)
        tiltNow = tilt
        try { mbMap.easeTo(b.build(), MapAnimationOptions.mapAnimationOptions { duration(max(1, ms).toLong()) }) } catch (e: Throwable) { softFail(e) }
    }

    /** Moves the camera instantly (no animation). Called every frame by the glide loop so motion never stalls between GPS fixes. */
    fun cameraSet(c: LL, bearing: Double, tilt: Double, zoom: Double, topPadFraction: Double, leftPadFraction: Double) {
        // a negative side fraction pads from the right instead (the mirrored, left-hand drive layout)
        val pd = pads(topPadFraction, leftPadFraction)
        tiltNow = tilt
        try {
            // an eased move still running would fight this one (the old map stopped it the same way)
            mapView.camera.cancelAllAnimators()
            mbMap.setCamera(CameraOptions.Builder().center(Point.fromLngLat(c.lon, c.lat)).pitch(tilt).zoom(zoom).bearing(bearing).padding(edge(pd)).build())
        } catch (e: Throwable) { softFail(e) }
        placeLocator()
    }

    // roads on screen from the last lamp search (night), for snapping your position while off-route
    @Volatile private var roadSegs: List<List<Point>> = emptyList()

    /** A road line from Mapbox's road data: its points, its kind ("motorway", "primary", "street"...) and whether it's one-way. */
    class RoadPiece(val pts: List<Point>, val cls: String, val oneway: Boolean?)
    @Volatile private var roadInfo: List<RoadPiece> = emptyList()

    /**
     * The road at [p] running about the way [heading] goes (within 15 m), from the roads last read off the map: its kind and whether
     * it's one-way (null when the map hasn't shown that road yet).
     */
    fun roadAt(p: LL, heading: Double, maxDistance: Double = 15.0, headingTolerance: Double = 40.0): RoadPiece? {
        val list = roadInfo
        if (list.isEmpty() || android.os.SystemClock.uptimeMillis() - roadSegsAt > 30_000L) return null
        val k = Math.cos(Math.toRadians(p.lat)) * 111320.0; val k2 = 110574.0
        var best: RoadPiece? = null; var bd = maxDistance.coerceIn(1.0, 30.0)
        val tolerance = headingTolerance.coerceIn(5.0, 45.0)
        for (rp in list) {
            val pts = rp.pts
            for (i in 0 until pts.size - 1) {
                val ax = (pts[i].longitude() - p.lon) * k; val ay = (pts[i].latitude() - p.lat) * k2
                val bx = (pts[i + 1].longitude() - p.lon) * k; val by = (pts[i + 1].latitude() - p.lat) * k2
                if ((ax > bd && bx > bd) || (ax < -bd && bx < -bd) || (ay > bd && by > bd) || (ay < -bd && by < -bd)) continue
                val dx = bx - ax; val dy = by - ay
                val l2 = dx * dx + dy * dy
                if (l2 < 0.01) continue
                val t = (-(ax * dx + ay * dy) / l2).coerceIn(0.0, 1.0)
                val d = Math.hypot(ax + dx * t, ay + dy * t)
                if (d >= bd) continue
                // the same direction of travel (either way along the line: a two-way road's line may be drawn against you)
                val segB = Math.toDegrees(Math.atan2(dx, dy))
                val diff = Math.abs(((segB - heading + 540.0) % 360.0) - 180.0)
                if (diff > tolerance && diff < 180.0 - tolerance) continue
                bd = d; best = rp
            }
        }
        return best
    }
    @Volatile private var roadSegsAt = 0L

    private fun rememberRoads(feats: List<Feature>) {
        val out = ArrayList<List<Point>>()
        for (f in feats) when (val g = f.geometry()) {
            is LineString -> out += g.coordinates()
            is MultiLineString -> out.addAll(g.coordinates())
            else -> { }
        }
        roadSegs = out; roadSegsAt = android.os.SystemClock.uptimeMillis()
        // the same roads with what kind they are and whether they're one-way, for lining the painted lane guidance up with the road
        val info = ArrayList<RoadPiece>()
        for (f in feats) {
            val cls = try { f.getStringProperty("class") } catch (e: Throwable) { null } ?: ""
            val ow = try { f.getStringProperty("oneway") } catch (e: Throwable) { null }
            val one = when (ow) { "true" -> true; "false" -> false; else -> null }
            when (val g = f.geometry()) {
                is LineString -> info += RoadPiece(g.coordinates(), cls, one)
                is MultiLineString -> for (c in g.coordinates()) info += RoadPiece(c, cls, one)
                else -> { }
            }
        }
        roadInfo = info
        // the streets lamps stand along: not motorways or trunk roads, links, service roads or tunnels
        val streets = ArrayList<List<Point>>()
        val lampClasses = setOf("primary", "secondary", "tertiary", "street", "street_limited", "primary_link", "secondary_link", "tertiary_link")
        for (f in feats) {
            val cls = try { f.getStringProperty("class") } catch (e: Throwable) { null } ?: continue
            val tunnel = try { f.getStringProperty("structure") == "tunnel" } catch (e: Throwable) { false }
            if (tunnel || cls !in lampClasses) continue
            when (val g = f.geometry()) {
                is LineString -> streets += g.coordinates()
                is MultiLineString -> streets.addAll(g.coordinates())
                else -> { }
            }
        }
        if (streets.size != lampRoads.size || streets.isNotEmpty() && lampRoads.isEmpty()) lampRoadsNew = true
        lampRoads = streets
        if (lampRoadsNew) main0.post { refreshLamps() }
    }

    /**
     * Moves a GPS position onto the nearest drawn road that runs the way you are heading, or returns null when none is close enough.
     * It never waits on the map: it uses the roads the map last reported (the engine tries the speed-limit roads first anyway).
     */
    fun snapToRoad(p: LL, heading: Double): LL? {
        // (the same roads as roadSegs, with whether each is one-way: a one-way road run against you is the other carriageway)
        val segs = roadInfo
        if (segs.isEmpty() || android.os.SystemClock.uptimeMillis() - roadSegsAt > 20_000L) return null
        return try {
            val k = Math.cos(Math.toRadians(p.lat)) * 111320.0
            val k2 = 110574.0
            var best: LL? = null
            var bd = 24.0
            for (rp in segs) {
                val pts = rp.pts
                val oneWay = rp.oneway == true
                for (i in 0 until pts.size - 1) {
                    val ax = (pts[i].longitude() - p.lon) * k; val ay = (pts[i].latitude() - p.lat) * k2
                    val bx = (pts[i + 1].longitude() - p.lon) * k; val by = (pts[i + 1].latitude() - p.lat) * k2
                    if ((ax > bd && bx > bd) || (ax < -bd && bx < -bd) || (ay > bd && by > bd) || (ay < -bd && by < -bd)) continue
                    val dx = bx - ax; val dy = by - ay
                    val l2 = dx * dx + dy * dy
                    if (l2 < 0.01) continue
                    val t = (-(ax * dx + ay * dy) / l2).coerceIn(0.0, 1.0)
                    val d = Math.hypot(ax + dx * t, ay + dy * t)
                    if (d >= bd) continue
                    // only roads that run the way you are heading (either direction), so a parallel street or a crossing road is not chosen
                    val segB = (Math.toDegrees(Math.atan2(dx, dy)) + 360.0) % 360.0
                    var diff = Math.abs(segB - heading) % 360.0
                    if (diff > 180.0) diff = 360.0 - diff
                    if (diff > 35.0 && diff < 145.0) continue
                    // a one-way road is drawn the way its traffic goes: against it is the other carriageway, not yours
                    if (oneWay && diff >= 145.0) continue
                    bd = d
                    best = LL(p.lat + (ay + dy * t) / k2, p.lon + (ax + dx * t) / k)
                }
            }
            best
        } catch (e: Exception) { null }
    }

    /**
     * A tap on the map at screen point [sp] (also used for taps on the Android Auto screen): skips the route fly-through, or picks an
     * alternative route by its card or line. True when it did something. Worked out from where the cards and lines are on screen,
     * so it never has to wait for the map.
     */
    fun tapAt(sp: android.graphics.PointF): Boolean {
        // a tap during the route fly-through skips straight to the overview
        flyDone?.let { d -> flyDone = null; flyToken++; try { mapView.camera.cancelAllAnimators() } catch (e: Throwable) { softFail(e) }; d(); return true }
        // a branch of a chain ("Alle Standorte")
        chainAt(sp)?.let { onChainTap?.invoke(it); return true }
        // a favourite's star
        favAt(sp)?.let { onFavTap?.invoke(it); return true }
        savedAt(sp)?.let { onSavedTap?.invoke(it); return true }
        // a charge point on the map (drawn above the route, so it comes first)
        nearestCharger(sp)?.let { onChargerTap?.invoke(it); return true }
        // choosing a route: a tap on one of the other routes' lines (see onPreviewTap)
        if (liveLines.isEmpty() && alts.isNotEmpty() && onPreviewTap != null) run {
            val hit = try {
                val dp = mapView.resources.displayMetrics.density
                var best = -1; var bd = 30.0 * dp
                alts.forEachIndexed { i, line ->
                    if (line.size < 2) return@forEachIndexed
                    val px = pixels(line)
                    for (n in 0 until px.size - 1) {
                        val ax = px[n].x - sp.x; val ay = px[n].y - sp.y
                        val dx = px[n + 1].x - px[n].x; val dy = px[n + 1].y - px[n].y
                        val l2 = dx * dx + dy * dy
                        val t = if (l2 > 0) (-(ax * dx + ay * dy) / l2).coerceIn(0.0, 1.0) else 0.0
                        val d = Math.hypot(ax + dx * t, ay + dy * t)
                        if (d < bd) { bd = d; best = i }
                    }
                }
                best
            } catch (e: Exception) { -1 }
            if (hit >= 0 && onPreviewTap?.invoke(hit) == true) return true
        }
        if (liveLines.isEmpty()) { val g = garageAt(sp) ?: return false; onGarageTap?.invoke(g); return true }
        return try {
            val dp = mapView.resources.displayMetrics.density
            // the cards first (about 190 x 65 dp, standing on their point): easy to hit while driving
            val cards = shownCards
            val cpx = pixels(cards.map { it.second })
            if (cpx.size == cards.size) for (k in cards.indices) {
                val p = cpx[k]
                if (sp.x >= p.x - 105 * dp && sp.x <= p.x + 105 * dp && sp.y >= p.y - 80 * dp && sp.y <= p.y + 12 * dp) { onAltTap?.invoke(cards[k].first); return true }
            }
            // then the lines: the nearest one within about 30 dp of the tap
            val r = 30.0 * dp
            var best = -1; var bd = r
            liveLines.forEachIndexed { i, line ->
                if (line.size < 2) return@forEachIndexed
                val step = maxOf(1, line.size / 300)
                val sample = ArrayList<LL>()
                var j = 0
                while (j < line.size) { sample += line[j]; j += step }
                if (sample.last() !== line.last()) sample += line.last()
                val px = pixels(sample)
                for (n in 0 until px.size - 1) {
                    val ax = px[n].x - sp.x; val ay = px[n].y - sp.y
                    val bx = px[n + 1].x - sp.x; val by = px[n + 1].y - sp.y
                    val dx = bx - ax; val dy = by - ay
                    val l2 = dx * dx + dy * dy
                    val t = if (l2 > 0) (-(ax * dx + ay * dy) / l2).coerceIn(0.0, 1.0) else 0.0
                    val d = Math.hypot(ax + dx * t, ay + dy * t)
                    if (d < bd) { bd = d; best = i }
                }
            }
            if (best >= 0) { onAltTap?.invoke(best); true } else garageAt(sp)?.let { g -> onGarageTap?.invoke(g); true } ?: false
        } catch (e: Exception) { false }
    }

    /** The charge point whose picture is under the tap [sp]: the picture stands above its point, so the tap may be above the point. */
    private fun nearestCharger(sp: android.graphics.PointF): ChargerSite? = try {
        if (chargers.isEmpty()) null else {
            val dp = mapView.resources.displayMetrics.density
            val px = pixels(chargers.map { it.pos })
            var best: ChargerSite? = null; var bd = Double.MAX_VALUE
            for (i in chargers.indices) {
                if (i >= px.size) break
                val dx = px[i].x - sp.x; val dy = px[i].y - sp.y
                // the picture reaches about 80 dp above its point and 12 dp below, and about 36 dp to each side
                if (abs(dx) > 36 * dp || dy > 12 * dp || dy < -80 * dp) continue
                val d = Math.hypot(dx, dy + 40 * dp)
                if (d < bd) { bd = d; best = chargers[i] }
            }
            best
        }
    } catch (e: Exception) { null }

    /** Current camera, as (lat, lon, bearing, tilt, zoom, topPadFraction, leftPadFraction). */
    fun cameraNow(): DoubleArray? {
        val c = try { mbMap.cameraState } catch (e: Throwable) { return null }
        val t = c.center
        val pd = c.padding
        val w = max(1, mapView.width).toDouble(); val h = max(1, mapView.height).toDouble()
        val i = viewInset
        if (i != null) {
            // back from padding within the visible part to the fractions the camera code works in
            val bot = pd.bottom
            val vw = max(1.0, w - i.left - i.right); val vh = max(1.0, h - i.top - bot)
            val l = pd.left - i.left; val r = pd.right - i.right
            return doubleArrayOf(t.latitude(), t.longitude(), c.bearing, c.pitch, c.zoom, (pd.top - i.top) / vh, (max(0.0, l) - max(0.0, r)) / vw)
        }
        return doubleArrayOf(t.latitude(), t.longitude(), c.bearing, c.pitch, c.zoom, pd.top / h, (pd.left - pd.right) / w)
    }

    fun setTilt(tilt: Double) {
        tiltNow = tilt
        try { mbMap.easeTo(CameraOptions.Builder().pitch(tilt).build(), MapAnimationOptions.mapAnimationOptions { duration(500L) }) } catch (e: Throwable) { softFail(e) }
    }

    fun zoomBy(delta: Double) {
        try {
            val z = mbMap.cameraState.zoom
            mbMap.easeTo(CameraOptions.Builder().zoom(z + delta).build(), MapAnimationOptions.mapAnimationOptions { duration(250L) })
        } catch (e: Throwable) { softFail(e) }
    }

    fun resetNorth() {
        try { mbMap.easeTo(CameraOptions.Builder().bearing(0.0).build(), MapAnimationOptions.mapAnimationOptions { duration(400L) }) } catch (e: Throwable) { softFail(e) }
    }

    /** Drags the map with the user's finger (the car screen's scroll gesture). Android Auto reports
     * finger displacement, while cameraForDrag expects the inverse camera displacement; negate it so
     * dragging right moves the map content right, and dragging down moves it down. */
    fun panBy(dx: Float, dy: Float) {
        try {
            val w = mapView.width / 2.0; val h = mapView.height / 2.0
            mbMap.setCamera(mbMap.cameraForDrag(ScreenCoordinate(w, h), ScreenCoordinate(w - dx, h - dy)))
        } catch (e: Throwable) { softFail(e) }
    }

    /** Zooms by [dz] levels keeping the point under ([x], [y]) where it is (the car screen's pinch and double tap). */
    fun zoomAround(dz: Double, x: Float, y: Float) {
        try {
            val z = mbMap.cameraState.zoom
            mbMap.setCamera(CameraOptions.Builder().zoom((z + dz).coerceIn(1.0, 21.0)).anchor(ScreenCoordinate(x.toDouble(), y.toDouble())).build())
        } catch (e: Throwable) { softFail(e) }
    }

    /** Smoothly zooms out (flat, north-up) so every point sits in the area above the route-preview panel. */
    /** [panel]: leave the bottom of the screen for the phone's preview panel; off for the car's overview, which has no panel there. */
    fun fit(points: List<LL>, panel: Boolean = true) {
        if (points.size < 2) return
        val d = mapView.resources.displayMetrics.density.toDouble()
        val w = mapView.width / d
        val h = mapView.height / d
        if (w < 50 || h < 50) return
        fun mx(lon: Double) = (lon + 180.0) / 360.0
        fun my(lat: Double): Double {
            val r = Math.toRadians(lat.coerceIn(-85.0, 85.0))
            return (1.0 - Math.log(Math.tan(r) + 1.0 / Math.cos(r)) / Math.PI) / 2.0
        }
        val xs = points.map { mx(it.lon) }; val ys = points.map { my(it.lat) }
        val x0 = xs.min(); val x1 = xs.max(); val y0 = ys.min(); val y1 = ys.max()
        // visible area (dp): the bottom half is covered by the preview panel. On Android Auto only the part of the screen it leaves
        // the map counts (another app beside it, its own buttons), so the routes aren't drawn behind them
        val vi = viewInset
        val il = (vi?.left ?: 0) / d; val ir = (vi?.right ?: 0) / d; val it0 = (vi?.top ?: 0) / d; val ib = (vi?.bottom ?: 0) / d
        val vw = max(50.0, w - il - ir); val vh = max(50.0, h - it0 - ib)
        val side = if (panel) fitSideDp.coerceIn(0.0, vw * 0.6) else 0.0
        val pl = il + side + (vw - side) * 0.07; val pr = ir + (vw - side) * 0.07; val pt = it0 + vh * 0.10; val pb = ib + vh * (if (panel && side <= 0.0 && vi == null) 0.60 else 0.10)
        val aw = w - pl - pr; val ah = h - pt - pb
        val fx = max(x1 - x0, 1e-7); val fy = max(y1 - y0, 1e-7)
        // (zoom in this formula is for a map drawn at the screen's own density; a lower render scale shows more map, so it's corrected)
        val prK = d / max(0.1, mapPixelRatio())
        val z = (min(Math.log(aw * prK / (512.0 * fx)), Math.log(ah * prK / (512.0 * fy))) / Math.log(2.0))
        val zoom = z.coerceIn(2.0, 17.0)
        val world = 512.0 * Math.pow(2.0, zoom) / prK
        // bounds centre should land in the middle of the visible area, not the middle of the map
        val cx = (x0 + x1) / 2 + (w / 2 - (pl + aw / 2)) / world
        val cy = (y0 + y1) / 2 + (h / 2 - (pt + ah / 2)) / world
        val lon = cx * 360.0 - 180.0
        val lat = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * cy))))
        tiltNow = 0.0
        val cp = CameraOptions.Builder().center(Point.fromLngLat(lon, lat)).zoom(zoom).pitch(0.0).bearing(0.0)
            .padding(EdgeInsets(0.0, 0.0, 0.0, 0.0)).build()
        try { mbMap.easeTo(cp, MapAnimationOptions.mapAnimationOptions { duration(1600L) }) } catch (e: Throwable) { softFail(e) }
    }

    private fun mapPixelRatio(): Double = pixelRatioUsed.toDouble()

    companion object {
        /** Stands in for the access token until one is entered (the map can't even be created without some token). */
        const val NO_TOKEN = "pk.none"
        /** The car shapes for the "Car" locator, in the order they're offered. */
        val CAR_MODELS = listOf("saloon", "hatch", "estate", "suv", "pickup", "van", "sports")
    }
}
