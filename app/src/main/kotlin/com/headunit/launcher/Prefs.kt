package com.headunit.launcher

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.json.JSONObject

/** Stable SharedPreferences names. Keep these values unchanged to preserve existing user settings. */
internal object PreferenceKeys {
    const val API_KEY = "apikey"
    /** Your own HERE key: speed limits from HERE first (see HereRest). */
    const val HERE_KEY = "herekey"
    const val MAP_STYLE = "mapstyle"
    const val MAP_DETAIL = "mapdetail"
    const val MAP_LITE = "maplite"
    const val GOOGLE_API_KEY = "gkey"
    const val MAPBOX_PROMPT = "mbprompt"
    const val MAPBOX_PROMPTED = "mbprompted"
    const val MAPBOX_MIGRATED = "mbmigrated"
    const val LOCATOR = "locator"
    const val LOCATOR_IMAGE = "locimg"
    const val LOCATOR_IMAGE_TURNS = "locimgturn"
    const val LOCATOR_IMAGE_CIRCLE = "locimgcircle"
    const val MODEL_SHAPE = "modelshape"
    const val MODEL_GLOW = "modelglow"
    const val CAR_COLOUR = "carcolour"
    const val ARROW_COLOUR = "arrowcolour"
    /** own design: the main route colour, the app's accent colour (empty = usual) and the map lighter/darker (-50..50) */
    const val ROUTE_COLOUR = "routecolour"
    const val ACCENT_COLOUR = "accentcolour"
    const val MAP_LIGHT = "maplight"
    /** The map's brightness by day (-50 … 50, 0 = as it is). The night one is MAP_LIGHT (Settings › Map › Karte heller/dunkler). */
    const val MAP_LIGHT_DAY = "maplightday"
    const val TRAFFIC = "traffic"
    const val SATELLITE = "satellite"
    const val BUILDINGS_3D = "buildings3d"
    const val SIGNALS = "signals"
    const val HAZARD_ICONS = "hazardicons"
    /** 3D flags on the map: a tall red one at the destination, amber ones at the stops. */
    const val FLAGS_3D = "flags3d"
    const val FLAGS_WAVE = "flagswave"
    /** Routes bring you to the kerb side of the destination (Mapbox approaches=curb). */
    const val CURB_ARRIVAL = "curbarrival"
    const val LAMPS = "lamps"
    const val TREES = "trees"
    const val PATHS = "paths"
    const val RAIN_RADAR = "radar"
    const val RAIN_EFFECTS = "rainfx"
    /** Shows the HD lane data diagnostic message (only in Demo mode). Off by default. */
    const val HD_DIAG = "hddiag"
    /** Favourites on the map as yellow stars with their names (names only when not navigating). */
    const val FAV_ON_MAP = "favmap"
    /** How the favourites are listed: "az" (alphabetical, the default) or "near" (nearest first). */
    const val FAV_SORT = "favsort"
    /** The size of the symbols of Home, Work and starred places on the map, in percent. */
    const val SAVED_ICON_SIZE = "savedicon"
    /** Rain, snow and fog on the map, each on its own (all on by default; the master switch is RAIN_EFFECTS). */
    const val FX_RAIN = "fxrain"
    const val FX_SNOW = "fxsnow"
    const val FX_FOG = "fxfog"
    const val MAP_FPS = "mapfps"
    const val UI_SCALE = "uiscale"
    const val UI_AUTO_SCALE = "uiauto"
    const val SOUND_MODE = "soundmode"
    const val FUEL_TYPE = "fueltype"
    const val PACK_REGION = "packregion"
    const val ETA_NUMBER = "etanum"
    const val ETA_NAME = "etaname"
    const val TRIP_NAME = "tripname"
    const val LANE_STYLE = "lanestyle"
    const val LANE_STRIP = "lanestrip"
    const val THEN_MODE = "thenmode"
    const val THEN_STYLE = "thenstyle"
    const val MAIN_CARD_SIZE = "maincardsize"
    const val MAIN_TEXT_SIZE = "maintextsize"
    const val MAIN_SPEED_SIZE = "mainspeedsize"
    /** which side the phone / head unit shows the speed on: "right" (the default), "left", or "auto" (left when mirrored for left-hand drive) */
    const val MAIN_SPEED_SIDE = "mainspeedside"
    const val MAIN_BUTTON_SIZE = "mainbuttonsize"
    const val MAIN_ARROW_SIZE = "mainarrowsize"
    /** Set once the old Screen & sizes arrow factor was folded into the arrow size (see mergeArrowSize). */
    const val ARROW_MERGED = "arrowmerged"
    const val CAR_CARD_SIZE = "carcardsize"
    const val CAR_TEXT_SIZE = "cartextsize"
    const val CAR_SPEED_SIZE = "carspeedsize"
    /** "normal" (the big speed ring or block) or "compact" (a small dark pill with the limit sign and speed) on the car screen */
    const val CAR_SPEED_STYLE = "carspeedstyle"
    /** where the car screen's speed sits: "bottom" (bottom left corner), "turn" (under the turn card) or "top" (top right) */
    const val CAR_SPEED_POS = "carspeedpos"
    const val CAR_BUTTON_SIZE = "carbuttonsize"
    const val CAR_LIMIT_SIGN_SIZE = "carlimitsignsize"
    const val LOCATOR_REMOVE_WHITE = "locatorRemoveWhite"
    /** The new portrait layout (on by default): with no route on an upright screen, the map buttons sit half-way down the clear map. */
    const val PORTRAIT_LAYOUT = "portraitLayout"
    const val CAR_ARROW_SIZE = "cararrowsize"
    const val MAP_THEME = "maptheme"
    const val MAP_COLOURS = "mapcolours"
    const val MAP_RESOLUTION = "mapres"
    const val MAPBOX_TOKEN = "mbtoken"
    const val ESRI_KEY = "esrikey"
    const val MAPTILER_KEY = "maptilerkey"
    /** Mapillary client token, for speed limit signs recognised on street photos (Settings > Kartenkonten). */
    const val MAPILLARY_KEY = "mapillarykey"
    const val SAT_SOURCE = "satsource"
    const val VEHICLE = "vehicle"
    const val TRIP_BUSINESS = "tripbusiness"
    const val SAT_HYBRID = "sathybrid"
    const val SAT_PREFETCH = "satprefetch"
    const val SAT_REMEMBERED = "satremembered"
    const val SAT_REMEMBERED_AT = "satrememberedat"
    const val MAPBOX_AUTO_NETWORK = "mbautonet"
    const val MAPBOX_LOGO = "mblogo"
    const val ARROW_PERCENT = "arrowpct"
    const val SPEED_SIZE = "speedsize"
    const val THEME = "theme"
    const val NIGHT_FROM = "nightfrom"
    const val DAY_FROM = "dayfrom"
    const val SETUP_DONE = "setupdone"
    const val OFF_MAP = "offmap"
    const val LIVE_ALTERNATIVES = "livealts"
    const val ALT_ROUTE_COLOUR = "altroutecolour"
    const val FASTER_ROUTE = "fasterroute"
    const val AUTO_REROUTE = "autoreroute"
    const val AUTO_REROUTE_MIN = "autoreroutemin"
    const val CAMERA_WARNINGS = "cameras"
    // the speed cameras drawn on the map: on by default, also where camera warnings are banned (they're only drawn, never announced)
    const val CAMERA_MAP = "cameramap"
    const val GARAGES_MAP = "garagesmap"
    const val VOICE = "voice"
    const val TRIPS = "trips"
    const val PLACES_CUSTOM = "places_custom"
    const val PLACES_FAVOURITES = "places_fav"
    /** favourites' tags of your own ("Kunden", "Urlaub"), by position */
    const val FAV_TAGS = "favtags"
    const val PLACES_RECENT = "places_recent"
    const val PLACES_SEEN = "places_seen"
    const val PLACE_PARKED = "place_parked"
    const val DRIVE = "drive"
    const val DISTANCE_UNIT = "distunit"
    const val TEMPERATURE_UNIT = "tempunit"
    const val TRAFFIC_WARNING = "warntol"
    const val RIBBON = "ribbon"
    const val LANE_VIEW = "laneview"
    const val JUNCTION_VIEW = "junctionview"
    const val PERFORMANCE_METER = "perfmeter"
    const val UPDATE_LAST_CHECK = "updlast"
    const val WHATS_NEW_SEEN_VERSION = "whatsnew_seen_version"
    /** The bullets of the what's-new card already seen (joined by a unit separator, the newest last, at most 300). */
    const val WHATS_NEW_SEEN_LINES = "whatsnew_seen_lines"
    const val AUTO_START_ROUTE = "autostartroute"
    const val AUTO_START_SECONDS = "autostartseconds"
    // Additional legacy keys centralised without changing their persisted names.
    const val AMBMAP = "ambmap"
    const val AMBREACT = "ambreact"
    const val ARRIVALFX = "arrivalfx"
    const val AVGZONES = "avgzones"
    const val AVOID_FERRY = "avoid_ferry"
    /** Keep routes off trains that carry cars (still used when one is the only way). */
    const val AVOID_CARTRAIN = "avoid_cartrain"
    const val AVOID_MOTORWAY = "avoid_motorway"
    const val AVOID_TOLL = "avoid_toll"
    const val AVOID_VIGNETTE = "avoid_vignette"
    const val VIGNETTE_WARN = "vignettewarn"
    const val WRONGWAY_REPORTS = "wrongwayreports"
    /** Parkuhr: "end ms,lat,lon" (where the car is), or "" when no parking time runs. */
    const val PARK_UNTIL = "parkuntil"
    const val AVOID_UNPAVED = "avoid_unpaved"
    const val AVOIDZONES = "avoidzones"
    const val BREAKREMIND = "breakremind"
    const val BRIGHT = "bright"
    const val BUMPS = "bumps"
    const val CAMBANOBEY = "cambanobey"
    /** The countries (codes, comma-separated) told that camera warnings are off there: each is said only once. */
    const val CAMBAN_NOTED = "cambannoted"
    const val CARSCALE = "carscale"
    const val KEEP_ARROW_SIDE = "keeparrowside"
    const val CHIME_VOL = "chimevol"
    const val CARTHEME = "cartheme"
    /** Android Auto: day and night follow sunrise and sunset (for cars that go dark whenever the headlights are on) */
    const val CARSUN = "carsun"
    const val CINEMATIC = "cinematic"
    const val CLEARVIEW = "clearview"
    const val COUNTRY = "country"
    const val COUNTRYDETECTED = "countrydetected"
    const val CRUISE = "cruise"
    const val DATAPROV = "dataprov"
    const val DATASNOOZE = "datasnooze"
    const val DEMO = "demo"
    const val DIMNIGHT = "dimnight"
    const val DRIVELOCK = "drivelock"
    const val DRIVESIMPLE = "drivesimple"
    const val EVMPK = "evmpk"
    /** The steady base consumption (kWh/km at 80 km/h) taken from the car's own range figure. */
    const val EV_BASE_GOM = "ev_base_gom"
    const val EVPPK = "evppk"
    const val FACADES = "facades"
    const val FFID = "ffid"
    const val FFSECRET = "ffsecret"
    const val FLYOVER = "flyover"
    const val FORCESETUP = "forcesetup"
    const val FUELMPG = "fuelmpg"
    /** Settings › Fuel & charging › Live fuel cost (off by default). */
    const val LIVE_FUEL_COST = "liveFuelCost"
    /** A petrol or diesel car: the tank's size in litres, and the fill level and range the car last sent (with when). */
    const val FUEL_TANK_L = "fueltankl"
    const val FUEL_NOW_PCT = "fuelnowpct"
    const val FUEL_NOW_KM = "fuelnowkm"
    const val FUEL_NOW_AT = "fuelnowat"
    // the battery level from the car maker's cloud (Settings > Fuel > Your electric car): own account and own keys of the user
    const val CLOUD = "cloudprov"
    const val CLOUD_ID = "cloudid"
    const val CLOUD_SECRET = "cloudsecret"
    const val CLOUD_VCC = "cloudvcc"
    const val CLOUD_ACCESS = "cloudaccess"
    const val CLOUD_REFRESH = "cloudrefresh"
    const val CLOUD_EXPIRES = "cloudexp"
    const val CLOUD_VEHICLE = "cloudveh"
    /** Smartcar v3: the user ID from the Connect redirect, and the connection ID (to remove it on sign-out). */
    const val CLOUD_SC_USER = "cloudscuser"
    const val CLOUD_SC_CONN = "cloudscconn"
    const val CLOUD_VERIFIER = "cloudverifier"
    const val CLOUD_CONTAINER = "cloudcontainer"
    const val CLOUD_CALLS = "cloudcalls"
    /** How often the cloud is read (minutes); the provider's name is added ("cloudratebmw"). */
    const val CLOUD_RATE = "cloudrate"
    const val CLOUD_STATE = "cloudstate"
    const val CLOUD_CHARGING = "cloudcharging"
    const val CLOUD_CHARGE_MIN = "cloudchargemin"
    const val CLOUD_AT = "cloudat"
    const val FUELPPL = "fuelppl"
    const val FURNITURE3D = "furniture3d"
    const val GLASS = "glass"
    const val HABITS = "habits"
    const val HAZARDRADAR = "hazardradar"
    const val INCIDENTS = "incidents"
    const val ROADWORKS_WARN = "roadworksWarn"
    const val KEEPON = "keepon"
    const val LAMPGAP = "lampgap"
    const val LANDMARKS3D = "landmarks3d"
    const val LANEMAP = "lanemap"
    const val LANES_MAPBOX = "lanesmb"
    const val LASTLAT = "lastlat"
    const val LASTLON = "lastlon"
    const val LIMITCHANGE = "limitchange"
    /** How a speed limit change is told: "both" (gong, then the voice), "voice" or "chime". */
    const val LIMIT_HOW = "limithow"
    /** Street lamps from OpenStreetMap online (see LampsOnline). */
    const val LAMPS_ONLINE = "lampsonline"
    const val LIMITEST = "limitest"
    const val LIVECARD = "livecard"
    const val MAPFAST = "mapfast"
    const val MAPSPEED = "mapspeed"
    const val MBAUTOROUTE = "mbautoroute"
    const val MBHOMEKM = "mbhomekm"
    const val MBLOOK = "mblook"
    const val MBREGIONS = "mbregions"
    const val NIGHTSKY = "nightsky"
    const val PACKINSTALLED = "packinstalled"
    const val PACKNAG = "packnag"
    /** Car parks suggested shortly before arriving. Off unless switched on (a new key: it used to be on for everyone). */
    const val PARKPROMPT = "parkprompt2"
    const val PLACE_HOME = "place_home"
    const val PLANNED = "planned"
    const val POLICEALERTS = "policealerts"
    const val POLICEREP = "policerep"
    const val PREDICT = "predict"
    /** Your usual trips: how long they took, by place and time of day (kept on the phone; see LearnedTrips). */
    const val LEARNED_TRIPS = "learned_trips"
    /** What ended the last five routes, for problem reports (never the place itself). */
    const val ROUTE_ENDS = "route_ends"
    const val QUICKLIST = "quicklist"
    /** Tap the battery widget's refresh button now and then while Android Auto is connected. */
    const val EV_AUTOREFRESH = "evautorefresh"
    /** ...every this many minutes (1 to 30). */
    const val EV_REFRESH_MIN = "evrefreshmin"
    /** Minutes between widget refreshes when no route is being driven (EV_REFRESH_MIN is the one while navigating). */
    const val EV_REFRESH_IDLE_MIN = "evrefreshidlemin"
    /** Saved & recent on the phone folded down to its header line (tap the header to fold and unfold). */
    const val QUICKFOLD = "quickfold"
    /** The map in 3D (true) or flat 2D: remembered between drives, and shared by the phone / head unit and Android Auto. */
    const val VIEW_3D = "view3d"
    /** 3D tilt in degrees (30 to 65) for a wide screen, a tall (portrait) screen and Android Auto; 0 = automatic. */
    const val TILT_WIDE = "tilt_wide"
    const val TILT_TALL = "tilt_tall"
    const val TILT_CAR = "tilt_car"
    /** North up in Android Auto, remembered for the car (the phone's own setting is not stored). */
    const val NORTHUP_CAR = "northup_car"
    /** Your own normal zoom when not on a route, in zoom levels from the standard one (-2 to +2; 0 = standard). */
    const val DEFAULT_ZOOM = "defzoom"
    /** Off: the map keeps one steady zoom (+ and − still change it) instead of following your speed and the junctions. */
    const val AUTO_ZOOM = "autozoom"
    /** With no route, the search bar and its buttons fold into one round search button in the corner. */
    const val SEARCH_FOLD = "search_fold"
    /** The media bar (back, play / pause, next) on the driving screen while music plays. */
    const val MEDIA_CONTROLS = "media_controls"
    /** The direction ("Heading E") after the road name when driving with no route. */
    const val HEADING_WORD = "heading_word"
    /** The row under the search bar: a button for your saved places, and one for your GPX tracks (nearest first). */
    const val SC_PLACES = "sc_places"
    const val SC_TRACKS = "sc_tracks"
    /** The + / - zoom last used when not on a route, kept for next time: on the phone / head unit, and on Android Auto. */
    const val FREE_ZOOM = "freezoom"
    const val FREE_ZOOM_CAR = "freezoomcar"
    /** Stopped for 30 s: the map pulls back to a top-down view of the traffic round you. */
    const val STOPPED_OVERVIEW = "stopview"
    /** Where you left the car, "lat,lon,time" (saved on the phone when Android Auto disconnects). */
    const val PARKED_SPOT = "parkedspot"
    const val PARK_BLUETOOTH = "parkbt"
    const val CAR_BT_DEVICES = "carbtdevices"
    const val CAR_BT_LAST = "carbtlast"
    /** Phone-to-car link: the pairing code, this device's part ("car" receives, "phone" sends), and the last message taken. */
    const val LINK_CODE = "linkcode"
    const val LINK_ROLE = "linkrole"
    const val LINK_LAST = "linklast"
    const val RAILFORD = "railford"
    const val SHARPBENDS = "sharpbends"
    const val HEADLIGHT_BEAM = "headlightbeam"
    /** HD roads: "auto" (on in the UK only), "on" or "off". A new key, so every install starts from the standard. */
    const val HD_ROADS2 = "hdroads2"
    const val LIGHT_DEST = "lightdest"
    const val REPORTS = "reports"
    const val RESUME = "resume"
    const val ROADFAM = "roadfam"
    const val ROUTE_ECO = "route_eco"
    /** Start on a much simpler route (far fewer turns) when it's only about a minute slower; off = always the fastest. */
    const val SIMPLER_ROUTE = "simpler_route"
    const val ROUTEBACK = "routeback"
    const val ROUTEBAR = "routebar"
    const val ROUTECHEVRONS = "routechevrons"
    const val ROUTEFLOW = "routeflow"
    const val ROUTETRAFFICONLY = "routetrafficonly"
    const val ROUTEWX = "routewx"
    const val SCHOOLS = "schools"
    const val SEARCHWITH = "searchwith"
    const val SERVICESCARD = "servicescard"
    const val SKYWEATHER = "skyweather"
    const val SMARTFUEL = "smartfuel"
    const val SMARTVOICE = "smartvoice"
    const val SNAPROAD = "snaproad"
    const val SPEEDRING = "speedring"
    const val SPEEDTILT = "speedtilt"
    const val SPEEDWARN = "speedwarn"
    const val STARTPOS = "startpos"
    const val SYSDARKSEEN = "sysdarkseen"
    const val LIGHTSREVERSED = "lightsreversed"
    const val VENDORLIGHT = "vendorlight"
    const val TAPSELECT = "tapselect"
    const val TERRAIN = "terrain"
    const val TUNNELDIM = "tunneldim"
    const val TURN2D = "turn2d"
    const val TURNARROWS = "turnarrows"
    const val UKSIGNS = "uksigns"
    const val UPDCHECK = "updcheck"
    const val UPDSNOOZE = "updsnooze"
    const val WRONGWAY = "wrongway"
    const val WRONGWAY_RESET = "wrongwayreset"
    const val WXADVICE = "wxadvice"
    /** Official DWD weather warnings (markant and worse) on the route ahead or where you are. */
    const val DWD_WARN = "dwdwarn"
    /** A note at the start of a route when today or tomorrow is a public holiday (more traffic). */
    const val HOLIDAY_HINT = "holidayhint"
    /** The rescue lane notice in a jam on the motorway. */
    const val RESCUE_LANE = "rescuelane"
    const val ZEBRAS = "zebras"
    const val ZONEWARN = "zonewarn"
    const val TT_MAP_FIX = "ttmapfix"
    const val VOICE_CHANNEL = "voicechan"
    /** Where the voice and chimes come out: "auto" (as the phone routes them) or "speaker" (the phone's own speaker). */
    const val VOICE_OUT = "voiceOut"
    /** The app plays the voice itself, the way it plays the chimes (for head units that only lower the music for those). */
    const val VOICE_AS_CHIME = "voiceAsChime"
    /** Voice boost in dB (0 to 18): the app's own voice player gets louder; needs VOICE_AS_CHIME (see the slider). */
    const val VOICE_BOOST = "voiceBoost"
    /** The system speech voice chosen in Settings (its name); empty = the best installed one. */
    const val TTS_VOICE = "ttsvoice"
    const val TANKERKOENIG_KEY = "tkkey"
    const val VOICE_ROADS = "voiceroads"
    const val LANDMARKS = "landmarks"
    const val AVOID_TOLL_COUNTRIES = "avoid_toll_cc"
    const val ROAD_NAME_SIZE = "roadnamesize"
    const val CAR_ROAD_NAME_SIZE = "carroadnamesize"
    /** Buttons on the Android Auto trip card (on/off each) and their size: "normal", "large" or "xl". */
    const val TRIP_BTN_OVERVIEW = "tripbtn_overview"
    /** The search button on the trip card (Android Auto). */
    const val TRIP_BTN_SEARCH = "tripbtn_search"
    const val TRIP_BTN_OTHERS = "tripbtn_others"
    /** Home and Work under the search bar, each on its own (the app and Android Auto alike) */
    const val HOME_TILE = "home_tile"; const val WORK_TILE = "work_tile"
    /** Starred places of Your places show in the row under the search bar (off hides them all; they stay in search). */
    const val STARRED_ROW = "starred_row"
    /** A small clock under the speedometer, for head units whose status bar is covered. */
    const val CLOCK_ON_MAP = "clock_on_map"
    /** The folder the daily backup is written to (a folder picker URI), and when it was last written (ms). Never backed up. */
    const val AUTO_BACKUP_FOLDER = "auto_backup_folder"
    const val AUTO_BACKUP_LAST = "auto_backup_last"
    /** The speed limit sign big, where the speedometer was, and your speed small beside it. */
    const val SPEED_SIGN_BIG = "speed_sign_big"
    /** Android Auto: the search bar, Home and Work, the shortcut buttons and the suggestion card left off the map when not navigating (rotary-knob cars). */
    const val CAR_HIDE_SEARCH = "car_hide_search"
    /** Android Auto: seconds without a touch before the search bar and places go (0 = never). */
    const val CAR_HIDE_AFTER = "carHideAfter"
    /** Android Auto › Cool running: "off", "auto" (when the phone gets hot) or "always". */
    const val CAR_COOL = "carCool"
    /** Settings › On the map › Blend traffic colours on the route line and the route bar (on by default). */
    const val TRAFFIC_BLEND = "trafficBlend"
    /** Settings › While driving › Picture in picture on phones (on by default). */
    const val PIP = "pip"
    /** Settings › Alerts: the speed limit's own beeps and voice (over the limit, and when it changes); off leaves other alerts. */
    const val LIMIT_SOUNDS = "limitSounds"
    /** Settings › Alerts: the pop-up when the limit changes (the sign by the speedometer pulses either way). */
    const val LIMIT_POPUP = "limitPopup"
    /** My routes: the ways you saved, as JSON (see MyRoutes). */
    const val MY_ROUTES = "myRoutes"
    /** Settings › Alerts: say "Slow down" after the over-the-limit beep (off by default). */
    const val SAY_SLOW = "saySlow"
    /** Settings › Alerts › Top speed alert: your own top speed in km/h or mph (0 = off). */
    const val TOP_SPEED = "topSpeed"
    /** The turn card's arrow, width and height in percent (100 = as designed), on the phone and on the car screen. */
    const val TURN_ARROW_SIZE = "turnArrowSize"
    const val TURN_CARD_WIDTH = "turnCardWidth"
    const val TURN_CARD_HEIGHT = "turnCardHeight"
    const val CAR_TURN_ARROW_SIZE = "carTurnArrowSize"
    const val CAR_TURN_CARD_WIDTH = "carTurnCardWidth"
    const val CAR_TURN_CARD_HEIGHT = "carTurnCardHeight"
    /** Android Auto: the sound button stays on the map when not navigating (off: only while navigating, for more room). */
    const val CAR_SOUND_IDLE = "car_sound_idle"
    const val TRIP_BTN_SOUND = "tripbtn_sound"
    const val TRIP_BTN_END = "tripbtn_end"
    const val TRIP_BTN_QUICK = "tripbtn_quick"
    const val TRIP_BTN_SIZE = "tripbtn_size"
    const val CAR_MODEL = "carmodel"
    /** Your own 3D model (a .glb you pick): its size in per cent, the turn in degrees, and the time it was picked (see OwnModel). */
    const val OWN_MODEL_SIZE = "ownmodelsize"
    const val OWN_MODEL_TURN = "ownmodelturn"
    const val OWN_MODEL_STAMP = "ownmodelstamp"
    const val OWN_MODEL_NAME = "ownmodelname"
    const val TRAFFIC_GREEN = "trafficgreen"
    const val STOP_SIGNS = "stopsigns"
    // "Vorfahrt gewähren" signs on the way: off by default
    const val GIVE_WAY = "giveway"
    const val EV_BATTERY_KWH = "evbatt"
    const val EV_CHARGE_PCT = "evcharge"
    /** What the battery level showed against the estimate, per kind of road (town, country, motorway): "1.0,1.1,1.2". */
    const val EV_LEARN = "evlearn"
    /** How briskly you accelerate (average of the accelerations while speeding up, m/s²), learned from your trips; "" = not learned yet. */
    const val EV_ACCEL = "evaccel"
    /** How fast you usually drive against the posted limit (1.0 = at the limit), and on roads with no limit (km/h). */
    const val EV_STYLE = "evstyle"
    const val EV_FREE_KMH = "evfreekmh"
    /** Of the energy going downhill, the share that comes back (learned). */
    const val EV_REGEN = "evregen"
    /** The learning state kept between drives: the reference reading and what was driven since it (see NavEngine.evSave). */
    const val EV_STATE = "evstate"
    /** The look of the app: "standard", or "neon" (dark city, glowing lines, cut corners). */
    const val LOOK = "look"
    /** The neon look's colour: "yellow", "cyan" or "magenta". */
    const val NEON_ACCENT = "neonaccent"
    /** Your driving time against the route's own, on stretches without traffic (learned, 1.0 = the same). */
    const val ETA_PACE = "etapace"
    /** The car's battery level now (from the widget or Android Auto). Separate from the charge you want to arrive with. */
    const val EV_NOW_PCT = "evnow"
    const val EV_MIN_ARRIVE = "evminarr"
    /** The range the widget reported, in km. */
    const val EV_NOW_KM = "evnowkm"
    /** The least range to arrive at the destination with, in km. */
    const val EV_DEST_KM = "evdestkm"
    /** The least range to arrive at a charger with, in km (so a busy one can be left for the next). */
    const val EV_CHARGER_KM = "evchargerkm"
    /** The chosen electric car's label, "" when none (see [EvCars]). */
    const val EV_CAR = "evcar"
    /** The chosen car's usable battery in kWh, as a string. */
    const val EV_CAR_KWH = "evcarkwh"
    /** The chosen car's max DC charging power in kW, as a string ("" if unknown). */
    const val EV_CAR_KW = "evcarkw"
    /** The chosen car's DC charging curve as "pct:kw,pct:kw,..." ("" if unknown). */
    const val EV_CAR_CURVE = "evcarcurve"
    /** Chargers used for charging stops: 0 = all, 50, 150 or 300 kW and faster; default 50. */
    const val EV_MIN_KW = "evminkw"
    /** The map's charger filter: only charge points of at least this many kW are shown (0 = all). */
    const val CHARGER_FILTER_KW = "chgfilterkw"
    /** Charge points in France (national base, monthly file + live availability): only when switched on in Settings (default off). */
    const val FR_CHARGERS = "frchargers"
    /** Preferred charging operators: comma-separated ids from ChargeOperators (empty = none). */
    const val EV_PREF_OPS = "evprefops"
    /** Extra minutes a preferred operator's charging stops may cost over the quickest plan (default 10). */
    const val EV_PREF_MIN = "evprefmin"
    /** Open Charge Map API key (optional): looks up the power and operator of chargers outside Germany's register. */
    const val OCM_KEY = "ocmkey"
    /** Shared reports: send your own reports to the reports server (default on) and show other drivers' reports. */
    const val REPORTS_SHARE = "reportsshare"
    /** The reports server's address, overriding the one built into the app ("" = the built-in one). */
    const val REPORTS_URL = "reportsurl"
    /** A random id made once on this device, sent with every request to the reports server (anonymous, nothing else). */
    const val REPORTS_DEVICE = "reportsdevice"

    // Fahrzeugprofile: Typ und Abmessungen für Höhen-, Breiten- und Gewichtswarnungen
    /** Fahrzeugtyp: CAR, RV, TRAILER, LORRY. */
    const val VEHICLE_TYPE = "vehicletype"
    /** Fahrzeughöhe in Metern. */
    const val VEHICLE_HEIGHT = "vehicleheight"
    /** Fahrzeugbreite in Metern. */
    const val VEHICLE_WIDTH = "vehiclewidth"
    /** Fahrzeuglänge in Metern. */
    const val VEHICLE_LENGTH = "vehiclelength"
    /** Fahrzeuggewicht in Tonnen. */
    const val VEHICLE_WEIGHT = "vehicleweight"
    /** Achslast in Tonnen. */
    const val VEHICLE_AXLE_WEIGHT = "vehicleaxleweight"
}

/**
 * Tiny observable key/value store (Compose-aware).
 * Every key has its own state, so changing one setting only redraws the screens that actually read that setting
 * (a single shared map would redraw everything that read any setting).
 */
class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("drive_launcher", Context.MODE_PRIVATE)
    // one set of values for the whole app: the phone's screen, the Android Auto screen and the services all see a change at once
    private val raw: HashMap<String, String> = synchronized(LOCK) {
        RAW ?: HashMap<String, String>().apply {
            val rewrite = sp.edit()
            var migrate = false
            sp.all.forEach { (k, v) ->
                val s = v.toString()
                if (k !in SECRET_KEYS) { put(k, s); return@forEach }
                put(k, SecretBox.open(s))
                // a secret saved before encryption: stored encrypted from now on
                if (s.isNotEmpty() && !s.startsWith(SecretBox.PREFIX)) { rewrite.putString(k, SecretBox.seal(s)); migrate = true }
            }
            if (migrate) rewrite.apply()
        }.also { RAW = it }
    }
    private val states get() = STATES

    private companion object {
        val LOCK = Any()
        var RAW: HashMap<String, String>? = null
        val STATES = HashMap<String, MutableState<String?>>()
        /**
         * Stored encrypted on the phone (Android Keystore): the API keys the driver entered, the cloud account's client secret, key and tokens.
         * (The values are read as plain text in the app; only the stored copy is encrypted.)
         */
        val SECRET_KEYS = setOf(
            PreferenceKeys.API_KEY, PreferenceKeys.HERE_KEY, PreferenceKeys.GOOGLE_API_KEY, PreferenceKeys.MAPBOX_TOKEN, PreferenceKeys.ESRI_KEY,
            PreferenceKeys.MAPTILER_KEY, PreferenceKeys.MAPILLARY_KEY, PreferenceKeys.TANKERKOENIG_KEY, PreferenceKeys.OCM_KEY,
            PreferenceKeys.CLOUD_SECRET, PreferenceKeys.CLOUD_VCC, PreferenceKeys.CLOUD_ACCESS, PreferenceKeys.CLOUD_REFRESH, PreferenceKeys.CLOUD_VERIFIER, PreferenceKeys.CLOUD_SC_USER, PreferenceKeys.CLOUD_SC_CONN,
        )
    }

    private fun state(k: String): MutableState<String?> = synchronized(LOCK) { states.getOrPut(k) { mutableStateOf(raw[k]) } }

    fun s(k: String, d: String) = state(k).value ?: d
    fun b(k: String, d: Boolean) = state(k).value?.toBooleanStrictOrNull() ?: d
    fun i(k: String, d: Int) = state(k).value?.toIntOrNull() ?: d
    fun f(k: String, d: Float) = state(k).value?.toFloatOrNull() ?: d

    /** Keys that never go into a backup or come out of one: API keys, the reports device id, the cloud account's keys, tokens and vehicle number (VIN). */
    private val NEVER_BACKED_UP = setOf(PreferenceKeys.API_KEY, PreferenceKeys.REPORTS_DEVICE, PreferenceKeys.AUTO_BACKUP_FOLDER, PreferenceKeys.AUTO_BACKUP_LAST, PreferenceKeys.CLOUD_SECRET, PreferenceKeys.CLOUD_VCC,
        PreferenceKeys.CLOUD_ACCESS, PreferenceKeys.CLOUD_REFRESH, PreferenceKeys.CLOUD_VERIFIER, PreferenceKeys.CLOUD_STATE, PreferenceKeys.CLOUD_VEHICLE,
        PreferenceKeys.CLOUD_SC_USER, PreferenceKeys.CLOUD_SC_CONN)

    /** Everything except the API key, as JSON text (settings, saved places, logs...). */
    fun exportJson(): String = synchronized(LOCK) {
        val o = JSONObject()
        // (nor the shared-reports device id: a backup restored on another phone must not make it the same "device")
        raw.forEach { (k, v) -> if (k !in NEVER_BACKED_UP) o.put(k, v) }
        o.toString(2)
    }

    /** Restores values from [exportJson] output; validates the complete file before changing settings. */
    fun importJson(text: String): Int {
        require(text.isNotBlank() && text.length <= 2 * 1024 * 1024) { Lang.t("Die Sicherung ist leer oder zu groß", "Backup is empty or too large") }
        val o = JSONObject(text)
        // Parse and validate everything first, so a malformed backup can never leave half the settings restored.
        val pending = LinkedHashMap<String, String>()
        val keys = o.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            require(k.isNotBlank() && k.length <= 200) { "Invalid setting name" }
            if (k in NEVER_BACKED_UP) continue // API keys (and the reports device id) are intentionally never imported from backups.
            val value = o.get(k)
            require(value !is JSONObject && value !is org.json.JSONArray) { "Invalid setting value" }
            pending[k] = if (value == JSONObject.NULL) "" else value.toString()
        }
        pending.forEach { (k, v) -> put(k, v) }
        return pending.size
    }

    /** The names of the API keys and account secrets (for the password-protected key backup, see KeyBackup). */
    fun secretKeys(): Set<String> = SECRET_KEYS

    fun put(k: String, v: Any) {
        val s = v.toString()
        // nothing changed: no disk write and nothing on screen is redrawn
        synchronized(LOCK) { if (raw[k] == s) return; raw[k] = s }
        state(k).value = s
        sp.edit().putString(k, if (k in SECRET_KEYS) SecretBox.seal(s) else s).apply()
    }
}

/**
 * The map source: whatever was picked in Settings; until something is picked, TomTom once a TomTom key has been entered
 * (otherwise this build's default).
 */
/**
 * One arrow size: the phone's "Screen & sizes" factor (Bildschirm) used to multiply the arrow's own percentage (Pfeil), so turning
 * both up made the arrow huge. It is folded into the percentage once, so the arrow stays the size it was; the factor is then 100.
 */
fun Prefs.mergeArrowSize() {
    if (b(PreferenceKeys.ARROW_MERGED, false)) return
    val factor = i(PreferenceKeys.MAIN_ARROW_SIZE, 100).coerceIn(60, 180)
    if (factor != 100) {
        val pct = s(PreferenceKeys.ARROW_PERCENT, "240").toIntOrNull() ?: 240
        put(PreferenceKeys.ARROW_PERCENT, (pct * factor / 100).toString())
    }
    put(PreferenceKeys.MAIN_ARROW_SIZE, 100)
    put(PreferenceKeys.ARROW_MERGED, true)
}

fun Prefs.mapSource(): String = s(PreferenceKeys.MAP_STYLE, BuildConfig.DEFAULT_MAP).let { if (it == "google") "mapbox" else it }

/**
 * Once, on the first start after updating from a Horizon Nav from before the Mapbox map: switch the map to Mapbox (the old
 * TomTom / OpenStreetMap / Google choice was for the old map engine) and drop the Google key, which nothing uses now. Your places,
 * keys, settings and downloaded packs are all kept.
 */
fun Prefs.migrateToMapbox() {
    // a short prompt for the Mapbox key shows once after this update (new installs get it in setup instead, which clears it)
    if (!b(PreferenceKeys.MAPBOX_PROMPTED, false)) { put(PreferenceKeys.MAPBOX_PROMPT, true); put(PreferenceKeys.MAPBOX_PROMPTED, true) }
    if (!b(PreferenceKeys.MAPBOX_MIGRATED, false)) {
        put(PreferenceKeys.MAP_STYLE, "mapbox")
        put(PreferenceKeys.GOOGLE_API_KEY, "")
        put(PreferenceKeys.MAPBOX_MIGRATED, true)
    }
    // Once: adding a TomTom key used to switch the map to TomTom's flat map by itself, so the 3D buildings, models and lane
    // markings all vanished the moment people added their keys. Put those maps back to Mapbox (TomTom's map can still be picked
    // in Settings > Map; from now on the key is only used for search, routes and traffic).
    if (!b(PreferenceKeys.TT_MAP_FIX, false)) {
        if (s(PreferenceKeys.MAP_STYLE, "") == "tomtom") put(PreferenceKeys.MAP_STYLE, "mapbox")
        put(PreferenceKeys.TT_MAP_FIX, true)
    }
}

/** Saves a TomTom key. (It's used for search, routes and traffic; it no longer switches the map to TomTom's.) */
fun Prefs.setApiKey(k: String) {
    put(PreferenceKeys.API_KEY, k.trim())
}

/** The locator setting as the map takes it: its style and colour ("model" takes "<arrow|car>|<colour>"). */
fun Prefs.locatorSpec(): Pair<String, String> {
    val loc = s(PreferenceKeys.LOCATOR, "arrow")
    // the neon look: the flat arrow is replaced by the glowing neon chevron in the accent colour ("neon:<hex>"), on the phone and in the car
    if (s(PreferenceKeys.LOOK, "standard") == "neon" && (loc == "arrow" || loc == "flat")) {
        val hex = when (s(PreferenceKeys.NEON_ACCENT, "yellow")) { "cyan" -> "#19F0FF"; "magenta" -> "#FF3CCB"; else -> "#FCEE0A" }
        return "flat" to "neon:$hex"
    }
    return when (loc) {
        "custom" -> loc to (s(PreferenceKeys.LOCATOR_IMAGE, "0") + "|" + (if (b(PreferenceKeys.LOCATOR_IMAGE_TURNS, true)) "1" else "0") + "|" + (if (b(PreferenceKeys.LOCATOR_IMAGE_CIRCLE, false)) "1" else "0"))
        "model" -> {
            val shape = s(PreferenceKeys.MODEL_SHAPE, "arrow")
            val carLike = shape == "car" || shape == "own"
            loc to (shape + "|" + s(if (carLike) PreferenceKeys.CAR_COLOUR else PreferenceKeys.ARROW_COLOUR, if (carLike) "#F4F5F7" else "#F2F2F6") +
                "|" + (if (b(PreferenceKeys.MODEL_GLOW, true)) "1" else "0") +
                "|" + i(PreferenceKeys.OWN_MODEL_SIZE, 100) + "|" + i(PreferenceKeys.OWN_MODEL_TURN, 0) + "|" + s(PreferenceKeys.OWN_MODEL_STAMP, ""))
        }
        "car" -> loc to s(PreferenceKeys.CAR_COLOUR, "#F4F5F7")
        else -> loc to s(PreferenceKeys.ARROW_COLOUR, "#F2F2F6")
    }
}

/** Settings > Map > Map detail: "full", "lite" or "lightest" (the old "Light map" switch counts as lite). */
fun Prefs.mapDetail(): String = s(PreferenceKeys.MAP_DETAIL, if (b(PreferenceKeys.MAP_LITE, false)) "lite" else "full")
