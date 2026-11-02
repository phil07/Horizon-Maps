package com.headunit.launcher

import org.json.JSONObject

/**
 * The order of the search results. A name exactly as typed comes before one that only starts with it, and towns and cities come
 * before streets: "berlin" finds Berlin first, then the Berliner Straße nearby. When what you typed looks like a street
 * ("Berliner Str", "Hauptstraße 5"), the nearest matching street comes first instead.
 */
internal object SearchRank {

    /** Mapbox's kinds of result that are a town, city or region. */
    val MAPBOX_TOWNS = setOf("place", "locality", "district", "region", "country")

    /** OpenStreetMap place kinds (Photon's osm_value) that are a town or city. */
    val OSM_TOWNS = setOf("city", "town", "village", "municipality", "borough")

    /** The offline pack's kinds (places.tsv) that are a town or city. */
    val PACK_TOWNS = setOf("City", "Town", "Village", "Municipality", "Borough")

    /** Added to a result found in the downloaded lists; shown only when searching with no signal (see [online]). */
    const val OFFLINE = " · offline"

    /** A downloaded result as shown in a search with signal: without the "offline" note (it's mixed in with the online ones). */
    fun online(p: Place): Place = if (p.sub.endsWith(OFFLINE)) Place(p.name, p.sub.removeSuffix(OFFLINE), p.pos).town(p.town) else p

    /** The offline pack's kinds (OpenStreetMap's words: "Street", "Fuel", "Car wash") in the app's language. */
    fun kindName(kind: String): String {
        if (!Lang.de) return kind
        return KINDS_DE[kind.lowercase()] ?: kind
    }

    private val KINDS_DE = mapOf(
        "street" to "Straße", "city" to "Stadt", "town" to "Stadt", "village" to "Dorf", "municipality" to "Gemeinde",
        "borough" to "Stadtbezirk", "suburb" to "Stadtteil", "quarter" to "Stadtviertel", "neighbourhood" to "Wohngebiet",
        "hamlet" to "Weiler", "isolated dwelling" to "Einzelsiedlung", "locality" to "Ort", "farm" to "Hof", "square" to "Platz",
        "island" to "Insel", "islet" to "Insel",
        "fuel" to "Tankstelle", "charging station" to "Ladestation", "parking" to "Parkplatz", "parking space" to "Parkplatz",
        "car wash" to "Waschanlage", "restaurant" to "Restaurant", "fast food" to "Schnellimbiss", "cafe" to "Café", "bar" to "Bar",
        "pub" to "Kneipe", "biergarten" to "Biergarten", "ice cream" to "Eisdiele", "bank" to "Bank", "atm" to "Geldautomat",
        "pharmacy" to "Apotheke", "hospital" to "Krankenhaus", "clinic" to "Klinik", "doctors" to "Arztpraxis", "dentist" to "Zahnarzt",
        "school" to "Schule", "kindergarten" to "Kindergarten", "university" to "Universität", "college" to "Hochschule",
        "library" to "Bücherei", "townhall" to "Rathaus", "police" to "Polizei", "fire station" to "Feuerwehr",
        "post office" to "Post", "place of worship" to "Kirche", "theatre" to "Theater", "cinema" to "Kino", "toilets" to "Toilette",
        "supermarket" to "Supermarkt", "bakery" to "Bäckerei", "butcher" to "Metzgerei", "convenience" to "Laden",
        "kiosk" to "Kiosk", "clothes" to "Bekleidung", "shoes" to "Schuhe", "hairdresser" to "Friseur", "car" to "Autohaus",
        "car repair" to "Autowerkstatt", "doityourself" to "Baumarkt", "hardware" to "Eisenwaren", "furniture" to "Möbel",
        "electronics" to "Elektronik", "chemist" to "Drogerie", "florist" to "Blumen", "beverages" to "Getränkemarkt",
        "department store" to "Kaufhaus", "mall" to "Einkaufszentrum", "garden centre" to "Gartencenter", "optician" to "Optiker",
        "hotel" to "Hotel", "motel" to "Motel", "guest house" to "Pension", "hostel" to "Herberge", "camp site" to "Campingplatz",
        "caravan site" to "Wohnmobilstellplatz", "attraction" to "Sehenswürdigkeit", "museum" to "Museum", "viewpoint" to "Aussichtspunkt",
        "information" to "Information", "zoo" to "Zoo", "theme park" to "Freizeitpark", "picnic site" to "Rastplatz",
        "park" to "Park", "playground" to "Spielplatz", "sports centre" to "Sportzentrum", "stadium" to "Stadion",
        "swimming pool" to "Schwimmbad", "pitch" to "Sportplatz", "golf course" to "Golfplatz", "fitness centre" to "Fitnessstudio",
        "marina" to "Hafen", "station" to "Bahnhof", "halt" to "Haltepunkt", "tram stop" to "Straßenbahnhaltestelle",
        "subway entrance" to "U-Bahn-Eingang", "platform" to "Bahnsteig", "office" to "Büro", "company" to "Firma",
        "government" to "Behörde", "insurance" to "Versicherung", "estate agent" to "Makler", "lawyer" to "Anwalt",
    )

    /** Whether a Nominatim (OpenStreetMap) result is a town, city or region. */
    fun nominatimTown(x: JSONObject): Boolean {
        val cat = x.optString("category"); val type = x.optString("type")
        return (cat == "place" && type in OSM_TOWNS) || (cat == "boundary" && type == "administrative")
    }

    private val STREET_ENDINGS = listOf("straße", "strasse", "str", "str.", "weg", "allee", "platz", "gasse", "damm", "ufer",
        "chaussee", "pfad", "steig", "street", "road", "lane", "avenue", "rd", "st")

    /** Whether what was typed looks like a street or an address: a street word ("Str", "…weg", "Allee") or a house number. */
    fun streety(q: String): Boolean {
        val words = q.trim().lowercase().split(' ', ',').filter { it.isNotBlank() }
        if (words.any { w -> w.any { it.isDigit() } }) return true
        // "Berliner Str": a street word after the name
        if (words.drop(1).any { it in STREET_ENDINGS }) return true
        // "Hauptstraße", "Lindenweg": a street word at the end of one word (not the short ones, "st" ends too many names)
        return words.any { w -> STREET_ENDINGS.any { e -> e.length >= 3 && w.length > e.length + 2 && w.endsWith(e) } }
    }

    /**
     * [all]: each result with its place in the list it came from (online results 0, 1, 2 …, offline ones from 1000), so ties
     * keep the search service's order. [dist]: how far a result is from you.
     */
    fun <T> rank(q: String, all: List<Pair<T, Int>>, name: (T) -> String, town: (T) -> Boolean, dist: (T) -> Double): List<T> {
        val full = q.trim().lowercase()
        fun exact(t: T) = name(t).lowercase().let { it == full || it.substringBefore(',').trim() == full }
        fun prefix(t: T) = name(t).lowercase().startsWith(full)
        val byDist = compareBy<Pair<T, Int>> { dist(it.first) }
        val byOrder = compareBy<Pair<T, Int>> { it.second }
        val ex = all.filter { exact(it.first) }
        val pre = all.filter { !exact(it.first) && prefix(it.first) }
        val rest = all.filter { !exact(it.first) && !prefix(it.first) }.sortedWith(byOrder)
        val ordered = if (streety(q)) {
            ex.sortedWith(byDist) + pre.sortedWith(byDist)
        } else {
            // towns of exactly that name nearest first (the nearest Neustadt); towns that only start with it in the service's order
            // (Frankfurt before Frankenberg); other places nearest first
            ex.filter { town(it.first) }.sortedWith(byDist) + ex.filter { !town(it.first) }.sortedWith(byDist) +
                pre.filter { town(it.first) }.sortedWith(byOrder) + pre.filter { !town(it.first) }.sortedWith(byDist)
        }
        return (ordered + rest).map { it.first }
    }
}
