package com.headunit.launcher

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Shops and other places near you, for the search box: a chain's name ("dm", "Edeka", "Aldi") finds its branches nearest you,
 * and a kind of place ("Supermarkt", "Drogerie", "Apotheke") finds the nearest places of that kind rather than places that are
 * merely called that. The general place search ranks by how well the name matches anywhere, so on its own it often missed them.
 */
object PoiSearch {

    /** A kind of place: its OpenStreetMap filter (for Overpass) and the word TomTom's category search understands. */
    class Kind(val osm: String, val tomtom: String, val label: String)

    private fun k(osm: String, tomtom: String, de: String, en: String) = Kind(osm, tomtom, Lang.t(de, en))

    /** The kind of place a search means, when it is just a kind's name ("Supermarkt", "Apotheken", "pharmacy"), else null. */
    fun kindOf(q: String): Kind? {
        val w = q.trim().lowercase().removeSuffix(".").replace('é', 'e')
        if (w.isEmpty() || w.contains(' ') && w !in MULTI) return null
        return when (w) {
            "supermarkt", "supermärkte", "supermarket", "supermarkets", "lebensmittel", "einkaufen", "discounter" ->
                k("[\"shop\"=\"supermarket\"]", "supermarket", "Supermarkt", "Supermarket")
            "drogerie", "drogerien", "drogeriemarkt", "drugstore", "chemist" -> k("[\"shop\"=\"chemist\"]", "drugstore", "Drogerie", "Chemist")
            "apotheke", "apotheken", "pharmacy", "pharmacies" -> k("[\"amenity\"=\"pharmacy\"]", "pharmacy", "Apotheke", "Pharmacy")
            "bäcker", "bäckerei", "bäckereien", "baecker", "baeckerei", "bakery" -> k("[\"shop\"=\"bakery\"]", "bakery", "Bäckerei", "Bakery")
            "metzger", "metzgerei", "fleischerei", "butcher" -> k("[\"shop\"=\"butcher\"]", "butcher", "Metzgerei", "Butcher")
            "baumarkt", "baumärkte", "hardware store", "diy" -> k("[\"shop\"~\"^(doityourself|hardware)$\"]", "hardware store", "Baumarkt", "DIY store")
            "getränkemarkt", "getränke", "getraenkemarkt" -> k("[\"shop\"=\"beverages\"]", "drinks shop", "Getränkemarkt", "Drinks shop")
            "kiosk", "späti" -> k("[\"shop\"~\"^(kiosk|convenience)$\"]", "convenience store", "Kiosk", "Kiosk")
            "tankstelle", "tankstellen", "tanken", "petrol station", "gas station", "fuel" -> k("[\"amenity\"=\"fuel\"]", "petrol station", "Tankstelle", "Fuel station")
            "ladesäule", "ladesäulen", "ladestation", "charging station", "ev charging" -> k("[\"amenity\"=\"charging_station\"]", "electric vehicle station", "Ladesäule", "Charging point")
            "parkplatz", "parkhaus", "parken", "tiefgarage", "car park", "parking" -> k("[\"amenity\"=\"parking\"]", "parking", "Parkplatz", "Car park")
            "restaurant", "restaurants" -> k("[\"amenity\"=\"restaurant\"]", "restaurant", "Restaurant", "Restaurant")
            "imbiss", "fast food", "fastfood" -> k("[\"amenity\"=\"fast_food\"]", "fast food", "Imbiss", "Fast food")
            "cafe", "café", "kaffee", "coffee" -> k("[\"amenity\"=\"cafe\"]", "cafe", "Café", "Cafe")
            "geldautomat", "bankautomat", "atm", "bargeld" -> k("[\"amenity\"=\"atm\"]", "atm", "Geldautomat", "Cash machine")
            "bank", "banken" -> k("[\"amenity\"=\"bank\"]", "bank", "Bank", "Bank")
            "krankenhaus", "klinik", "notaufnahme", "hospital" -> k("[\"amenity\"=\"hospital\"]", "hospital", "Krankenhaus", "Hospital")
            "arzt", "ärzte", "hausarzt", "doctor" -> k("[\"amenity\"=\"doctors\"]", "doctor", "Arzt", "Doctor")
            "zahnarzt", "dentist" -> k("[\"amenity\"=\"dentist\"]", "dentist", "Zahnarzt", "Dentist")
            "toilette", "toiletten", "wc", "toilet", "toilets" -> k("[\"amenity\"=\"toilets\"]", "public toilet", "Toilette", "Toilets")
            "hotel", "hotels", "übernachtung" -> k("[\"tourism\"~\"^(hotel|motel|guest_house)$\"]", "hotel", "Hotel", "Hotel")
            "post", "postfiliale", "post office" -> k("[\"amenity\"=\"post_office\"]", "post office", "Post", "Post office")
            "packstation", "paketstation" -> k("[\"amenity\"=\"parcel_locker\"]", "parcel locker", "Packstation", "Parcel locker")
            "friseur", "frisör", "hairdresser" -> k("[\"shop\"=\"hairdresser\"]", "hairdresser", "Friseur", "Hairdresser")
            "waschanlage", "autowäsche", "car wash" -> k("[\"amenity\"=\"car_wash\"]", "car wash", "Waschanlage", "Car wash")
            "werkstatt", "autowerkstatt", "car repair" -> k("[\"shop\"=\"car_repair\"]", "repair facility", "Werkstatt", "Car repair")
            "polizei", "police" -> k("[\"amenity\"=\"police\"]", "police station", "Polizei", "Police")
            "kino", "cinema" -> k("[\"amenity\"=\"cinema\"]", "cinema", "Kino", "Cinema")
            "einkaufszentrum", "mall", "shopping centre" -> k("[\"shop\"=\"mall\"]", "shopping center", "Einkaufszentrum", "Shopping centre")
            "elektronik", "elektromarkt", "electronics" -> k("[\"shop\"=\"electronics\"]", "electronics", "Elektronik", "Electronics")
            "spielplatz", "playground" -> k("[\"leisure\"=\"playground\"]", "playground", "Spielplatz", "Playground")
            "schwimmbad", "hallenbad", "freibad", "swimming pool" -> k("[\"leisure\"~\"^(swimming_pool|water_park)$\"][\"access\"!=\"private\"]", "swimming pool", "Schwimmbad", "Swimming pool")
            else -> null
        }
    }

    private val MULTI = setOf("hardware store", "petrol station", "gas station", "charging station", "ev charging", "car park",
        "fast food", "post office", "car wash", "car repair", "shopping centre", "swimming pool")

    /** OpenStreetMap's keys for a shop, a service or a sight: a Photon result with one of these is a place you can go to. */
    private val POI_KEYS = setOf("shop", "amenity", "tourism", "leisure", "craft", "office", "healthcare", "club", "historic")

    private fun get(url: String, connectMs: Int, readMs: Int): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = connectMs; c.readTimeout = readMs
        c.setRequestProperty("User-Agent", "HorizonNav/1.0 (Android navigation app)")
        val code = c.responseCode
        val body = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText() ?: ""
        if (code !in 200..299) throw IOException("HTTP $code")
        return body
    }

    /**
     * Shops and places whose name or brand matches [q] within [radius] metres of [near], from OpenStreetMap (Photon, free).
     * Photon is asked to weigh nearness strongly, and what it returns further away than [radius] is dropped.
     */
    fun osmByName(q: String, near: LL, radius: Int): List<Place> {
        val enc = URLEncoder.encode(q.trim(), "UTF-8").replace("+", "%20")
        val lang = java.util.Locale.getDefault().language.takeIf { it in setOf("en", "de", "fr", "it") } ?: "en"
        val url = String.format(java.util.Locale.US, "https://photon.komoot.io/api/?q=%s&limit=30&lang=%s&lat=%.5f&lon=%.5f&zoom=14&location_bias_scale=0.1",
            enc, lang, near.lat, near.lon)
        val arr = JSONObject(get(url, 8000, 9000)).optJSONArray("features") ?: JSONArray()
        val out = ArrayList<Place>()
        for (i in 0 until arr.length()) {
            val f = arr.optJSONObject(i) ?: continue
            val pr = f.optJSONObject("properties") ?: continue
            if (pr.optString("osm_key") !in POI_KEYS) continue
            // a car park or a bus stop named after the shop is not the shop
            if (pr.optString("osm_value") in setOf("parking", "parking_entrance", "bus_stop", "platform")) continue
            val co = f.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
            val pos = LL(co.getDouble(1), co.getDouble(0))
            if (Geo.dist(near, pos) > radius) continue
            val name = pr.optString("name")
            if (name.isBlank()) continue
            val street = listOf(pr.optString("street"), pr.optString("housenumber")).filter { it.isNotBlank() }.joinToString(" ")
            val kind = SearchRank.kindName(pr.optString("osm_value").replace('_', ' ').replaceFirstChar { it.uppercase() })
            val sub = listOf(kind, street, pr.optString("city").ifBlank { pr.optString("district") }).filter { it.isNotBlank() }.joinToString(", ")
            out += Place(name, sub, pos)
        }
        return out
    }

    /** The places of [kind] within [radius] metres of [near], nearest first, from OpenStreetMap (Overpass). */
    fun osmByKind(kind: Kind, near: LL, radius: Int): List<Place> {
        val f = kind.osm
        val q = "[out:json][timeout:10];(node(around:$radius,${near.lat},${near.lon})$f;way(around:$radius,${near.lat},${near.lon})$f;);out center tags 60;"
        val els = Overpass.query(q, 5000, 9000, 2)
        val out = ArrayList<Place>()
        for (i in 0 until els.length()) {
            val e = els.optJSONObject(i) ?: continue
            val t = e.optJSONObject("tags") ?: JSONObject()
            val pos = if (e.has("lat")) LL(e.getDouble("lat"), e.getDouble("lon"))
                else e.optJSONObject("center")?.let { LL(it.getDouble("lat"), it.getDouble("lon")) } ?: continue
            val name = t.optString("name").ifBlank { t.optString("brand") }.ifBlank { t.optString("operator") }.ifBlank { kind.label }
            val street = listOf(t.optString("addr:street"), t.optString("addr:housenumber")).filter { it.isNotBlank() }.joinToString(" ")
            val sub = listOf(if (name != kind.label) kind.label else "", street, t.optString("addr:city")).filter { it.isNotBlank() }.joinToString(", ")
            out += Place(name, sub, pos)
        }
        return out.sortedBy { Geo.dist(near, it.pos) }
    }

    /** Drops the second of two results for the same place (the same spot, or the same name within a few doors). */
    fun distinct(list: List<Place>): List<Place> {
        val out = ArrayList<Place>()
        for (p in list) if (out.none { Geo.dist(it.pos, p.pos) < 25 || (it.name.equals(p.name, true) && Geo.dist(it.pos, p.pos) < 120) }) out += p
        return out
    }
}
