package com.headunit.launcher

import org.json.JSONArray
import org.json.JSONObject

/** A place you saved yourself under your own name ("Mum's", "Gym"). */
/** One of your own places: [starred] ones sit in the row under the search bar; [folder] (blank = none) groups it in the shortcut lists. */
data class LabeledPlace(val label: String, val place: Place, val starred: Boolean = false, val folder: String = "")

/** Saved places (home, work, your own labelled places, favourites, recents), stored as JSON in [Prefs]. */
class PlaceStore(private val prefs: Prefs) {
    /** Where you are now, for "nearest first" (set by the engine; null until a fix). */
    var here: () -> LL? = { null }
    private fun enc(p: Place) = JSONObject().put("n", p.name).put("s", p.sub).put("la", p.pos.lat).put("lo", p.pos.lon)
    private fun dec(o: JSONObject) = Place(o.optString("n"), o.optString("s"), LL(o.getDouble("la"), o.getDouble("lo")))

    private fun readOne(key: String): Place? {
        val s = prefs.s(key, "")
        if (s.isBlank()) return null
        return try { dec(JSONObject(s)) } catch (e: Exception) { null }
    }

    private fun readList(key: String): List<Place> {
        val s = prefs.s(key, "")
        if (s.isBlank()) return emptyList()
        return try {
            val a = JSONArray(s)
            (0 until a.length()).mapNotNull { try { dec(a.getJSONObject(it)) } catch (e: Exception) { null } }
        } catch (e: Exception) { emptyList() }
    }

    private fun writeList(key: String, list: List<Place>) {
        val a = JSONArray()
        list.forEach { a.put(enc(it)) }
        prefs.put(key, a.toString())
    }

    private fun same(a: Place, b: Place) = Geo.within(a.pos, b.pos, 60.0)

    fun slot(name: String): Place? = readOne("place_$name")
    fun setSlot(name: String, p: Place) { prefs.put("place_$name", enc(p).toString()); onSavedChanged?.invoke() }


    /** Your own labelled places, in the order you added them (shown as pills after Home and Work). */
    fun customs(): List<LabeledPlace> {
        val s = prefs.s(PreferenceKeys.PLACES_CUSTOM, "")
        if (s.isBlank()) return emptyList()
        return try {
            val a = JSONArray(s)
            (0 until a.length()).mapNotNull { i -> try { val o = a.getJSONObject(i); LabeledPlace(o.optString("l"), dec(o), o.optBoolean("s", false), o.optString("f", "")) } catch (e: Exception) { null } }
        } catch (e: Exception) { emptyList() }
    }

    private fun writeCustoms(list: List<LabeledPlace>) {
        onSavedChanged?.invoke()
        val a = JSONArray()
        list.forEach { a.put(enc(it.place).put("l", it.label).put("s", it.starred).put("f", it.folder)) }
        prefs.put(PreferenceKeys.PLACES_CUSTOM, a.toString())
    }

    /** Saves [p] as [label]; a place already saved there (or a label already used) is replaced. */
    fun addCustom(label: String, p: Place) {
        val l = label.trim().take(24).ifBlank { p.name.take(24) }
        writeCustoms(customs().filterNot { it.label.equals(l, true) || same(it.place, p) } + LabeledPlace(l, p))
    }

    fun removeCustom(label: String) = writeCustoms(customs().filterNot { it.label == label })
    fun renameCustom(old: String, new: String) {
        val n = new.trim().take(24); if (n.isBlank()) return
        writeCustoms(customs().map { if (it.label == old) it.copy(label = n) else it })
    }
    /** A star puts the place in the row under the search bar (next to Home and Work). */
    fun setStar(label: String, on: Boolean) = writeCustoms(customs().map { if (it.label == label) it.copy(starred = on) else it })
    /** Puts the place in the folder [folder] of Your places (blank takes it out of its folder). */
    fun setFolder(label: String, folder: String) = writeCustoms(customs().map { if (it.label == label) it.copy(folder = folder.trim().take(24)) else it })
    fun moveCustom(label: String, by: Int) {
        val l = customs().toMutableList(); val i = l.indexOfFirst { it.label == label }; val j = i + by
        if (i < 0 || j !in l.indices) return
        val t = l[i]; l[i] = l[j]; l[j] = t; writeCustoms(l)
    }
    fun customAt(p: Place): LabeledPlace? = customs().firstOrNull { same(it.place, p) }

    /** The favourites as they were saved (newest first). */
    private fun savedFavourites(): List<Place> = readList("places_fav")

    /** The favourites in the chosen order: alphabetical by default, or nearest first (Settings › Places & trips). */
    fun favourites(): List<Place> {
        val all = savedFavourites()
        return if (prefs.s(PreferenceKeys.FAV_SORT, "az") == "near") {
            val h = here()
            if (h == null) all else all.sortedBy { Geo.dist(it.pos, h) }
        } else all.sortedWith(compareBy({ it.name.lowercase() }, { it.name }))
    }
    /** The saved favourite nearest [p] within [r] metres (a place is often saved a little way from where it shows on the map). */
    fun favouriteNear(p: Place, r: Double = 150.0): Place? =
        savedFavourites().filter { Geo.dist(it.pos, p.pos) < r }.minByOrNull { Geo.dist(it.pos, p.pos) }
    fun isFavourite(p: Place) = favouriteNear(p) != null
    /** The favourites that aren't already in Your places ([customs]), so a place shows once. */
    fun favouritesNotIn(customs: List<LabeledPlace>): List<Place> =
        favourites().filter { f -> customs.none { Geo.within(it.place.pos, f.pos, 60.0) } }
    /** Called when the saved places change: favourites, Your places, Home and Work (the map's symbols follow it). */
    var onSavedChanged: (() -> Unit)? = null

    fun toggleFavourite(p: Place): Boolean {
        val cur = savedFavourites()
        val near = favouriteNear(p)
        val on = if (near != null) {
            writeList("places_fav", cur.filterNot { it == near }); false
        } else {
            writeList("places_fav", (listOf(p) + cur).take(60)); true
        }
        onSavedChanged?.invoke()
        return on
    }


    // ---- tags for favourites ("Kunden", "Urlaub"): kept beside the list, by position, so the saved favourites stay as they are ----
    private fun tagKey(p: Place) = String.format(java.util.Locale.US, "%.4f,%.4f", p.pos.lat, p.pos.lon)
    private fun tagMap(): org.json.JSONObject = try { org.json.JSONObject(prefs.s(PreferenceKeys.FAV_TAGS, "").ifBlank { "{}" }) } catch (e: Exception) { org.json.JSONObject() }
    fun favTag(p: Place): String = tagMap().optString(tagKey(p))
    /** Sets the favourite's tag (blank takes it off). */
    fun setFavTag(p: Place, tag: String) {
        val m = tagMap(); val t = tag.trim().take(20)
        if (t.isEmpty()) m.remove(tagKey(p)) else m.put(tagKey(p), t)
        prefs.put(PreferenceKeys.FAV_TAGS, m.toString())
    }
    /** The tags in use, in alphabetical order. */
    fun favTags(): List<String> = favourites().map { favTag(it) }.filter { it.isNotBlank() }.distinct().sortedBy { it.lowercase() }

    /** Moves a favourite into Your places (into [folder] when given), so it no longer sits in the favourites. */
    fun moveFavouriteToPlaces(p: Place, folder: String = "") {
        val near = favouriteNear(p) ?: return
        writeList("places_fav", savedFavourites().filterNot { it == near })
        addCustom(near.name, near)
        if (folder.isNotBlank()) setFolder(near.name.trim().take(24).ifBlank { near.name.take(24) }, folder)
        onSavedChanged?.invoke()
    }

    /** Moves every favourite into Your places, in a "Favoriten" folder there. */
    fun moveAllFavouritesToPlaces() {
        for (f in savedFavourites()) {
            addCustom(f.name, f)
            setFolder(f.name.trim().take(24).ifBlank { f.name.take(24) }, "Favoriten")
        }
        writeList("places_fav", emptyList())
        onSavedChanged?.invoke()
    }

    /** Gives a favourite a name of your own (it stays in the same place in the list). */
    fun renameFavourite(p: Place, name: String) {
        val n = name.trim().take(40)
        if (n.isEmpty()) return
        writeList("places_fav", savedFavourites().map { if (same(it, p)) it.copy(name = n) else it })
        onSavedChanged?.invoke()
    }

    /** Recents saved while the habit suggestion still put "Meist gegen 12:02" in the subtitle: that text is no address, so it is dropped. */
    fun recents(): List<Place> = readList("places_recent").map { p ->
        if (p.sub.startsWith("Meist gegen") || p.sub.startsWith("Usually around")) Place(p.name, "", p.pos) else p
    }
    /** Takes one place off the recent list (it's still remembered for searching without signal). */
    fun removeRecent(p: Place) = writeList("places_recent", recents().filterNot { same(it, p) })
    fun clearSlot(name: String) { prefs.put("place_$name", ""); onSavedChanged?.invoke() }
    fun clearParked() = prefs.put(PreferenceKeys.PLACE_PARKED, "")
    fun clearFavourites() = prefs.put(PreferenceKeys.PLACES_FAVOURITES, "")
    /** Forgets recent destinations and the places remembered for searching without signal. */
    fun clearHistory() { prefs.put(PreferenceKeys.PLACES_RECENT, ""); prefs.put(PreferenceKeys.PLACES_SEEN, "") }
    fun addRecent(p: Place) {
        writeList("places_recent", (listOf(p) + recents().filterNot { same(it, p) }).take(8))
        // everywhere you have routed to is also kept for searching without signal
        writeList("places_seen", (listOf(p) + readList("places_seen").filterNot { same(it, p) }).take(600))
    }

    /** Search over saved, favourite and previously used places only: works without any signal. */
    fun searchLocal(q: String, near: LL?): List<Place> {
        val toks = q.lowercase().split(' ', ',').filter { it.isNotBlank() }
        if (toks.isEmpty()) return emptyList()
        val pool = ArrayList<Place>()
        slot("home")?.let { pool += Place("Home", it.name, it.pos) }
        slot("work")?.let { pool += Place("Work", it.name, it.pos) }
        for (c in customs()) pool += Place(c.label, c.place.name, c.place.pos)
        pool += favourites(); pool += recents(); pool += readList("places_seen")
        val uniq = ArrayList<Place>()
        for (p in pool) if (uniq.none { same(it, p) }) uniq += p
        return uniq.filter { p -> val h = (p.name + " " + p.sub).lowercase(); toks.all { h.contains(it) } }
            .sortedBy { p -> (if (p.name.lowercase().startsWith(toks[0])) 0.0 else 1e7) + (if (near != null) Geo.dist(near, p.pos) else 0.0) }
            .take(10)
    }
}

// ------------------------------------------------------------------ trips

class Trip(val start: Long, val end: Long, val dist: Double, val moving: Long, val maxSpeed: Double, val to: String)

class TripStore(private val prefs: Prefs) {
    fun all(): List<Trip> {
        val s = prefs.s(PreferenceKeys.TRIPS, "")
        if (s.isBlank()) return emptyList()
        return try {
            val a = JSONArray(s)
            (0 until a.length()).mapNotNull {
                try {
                    val o = a.getJSONObject(it)
                    Trip(o.getLong("s"), o.getLong("e"), o.getDouble("d"), o.getLong("m"), o.getDouble("v"), o.optString("t"))
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    fun add(t: Trip) {
        val a = JSONArray()
        (listOf(t) + all()).take(300).forEach {
            a.put(JSONObject().put("s", it.start).put("e", it.end).put("d", it.dist).put("m", it.moving).put("v", it.maxSpeed).put("t", it.to))
        }
        prefs.put(PreferenceKeys.TRIPS, a.toString())
    }

    fun clear() = prefs.put(PreferenceKeys.TRIPS, "")
}
