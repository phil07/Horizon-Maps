package com.headunit.launcher

import org.json.JSONArray
import org.json.JSONObject

/**
 * Several cars: each car (its Bluetooth device) keeps its own fuel type, electric car data and the values learned from its trips.
 * When a car connects over Bluetooth, its settings are put back; the settings of the car that was active are kept with it first.
 * A car the app doesn't know yet is put to the question in Settings (Android Auto > Fahrzeuge): which car it is, or a new one.
 */
object VehicleProfiles {
    private const val KEY_PROFILES = "vehprofiles"
    private const val KEY_ACTIVE = "vehactive"
    /** A Bluetooth device that connected and isn't assigned to a car yet: "address|name", or "" when there is none. */
    const val KEY_PENDING = "vehpending"

    /** The settings that describe a car (kept per car). */
    private val KEYS = listOf(
        PreferenceKeys.FUEL_TYPE, PreferenceKeys.VEHICLE, PreferenceKeys.VEHICLE_TYPE, PreferenceKeys.VEHICLE_HEIGHT, PreferenceKeys.VEHICLE_WIDTH,
        PreferenceKeys.VEHICLE_LENGTH, PreferenceKeys.VEHICLE_WEIGHT, PreferenceKeys.VEHICLE_AXLE_WEIGHT,
        PreferenceKeys.EV_BATTERY_KWH, PreferenceKeys.EV_CHARGE_PCT, PreferenceKeys.EV_BASE_GOM, PreferenceKeys.EV_ACCEL, PreferenceKeys.EV_STYLE,
        PreferenceKeys.EV_FREE_KMH, PreferenceKeys.EV_REGEN, PreferenceKeys.EV_LEARN, PreferenceKeys.EV_CAR, PreferenceKeys.EV_CAR_KWH,
        PreferenceKeys.EV_CAR_KW, PreferenceKeys.EV_CAR_CURVE,
    )

    data class Car(val id: String, val name: String, val bt: List<String>, val data: Map<String, String>)

    private fun load(prefs: Prefs): MutableList<Car> = try {
        val a = JSONArray(prefs.s(KEY_PROFILES, "[]"))
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            val bt = o.optJSONArray("bt") ?: JSONArray()
            val d = o.optJSONObject("data") ?: JSONObject()
            Car(o.getString("id"), o.getString("name"), (0 until bt.length()).map { bt.getString(it) }, d.keys().asSequence().associateWith { d.getString(it) })
        }.toMutableList()
    } catch (e: Exception) { mutableListOf() }

    private fun save(prefs: Prefs, cars: List<Car>) {
        val a = JSONArray()
        for (c in cars) a.put(JSONObject().put("id", c.id).put("name", c.name).put("bt", JSONArray(c.bt)).put("data", JSONObject(c.data)))
        prefs.put(KEY_PROFILES, a.toString())
    }

    /** The car settings as they are now (the empty ones left out). */
    /** The vehicle kinds' own sizes and speeds ("veh_camper_h" and so on), kept per car as well. */
    private val KIND_KEYS = listOf("car", "camper", "lorry").flatMap { k -> listOf("max", "h", "w", "l", "kg", "axlekg").map { Vehicle.key(k, it) } }

    private fun snapshot(prefs: Prefs): Map<String, String> = (KEYS + KIND_KEYS).associateWith { prefs.s(it, "") }.filterValues { it.isNotEmpty() }

    private fun apply(prefs: Prefs, data: Map<String, String>) { for ((k, v) in data) if (prefs.s(k, "") != v) prefs.put(k, v) }

    fun cars(prefs: Prefs): List<Car> = load(prefs)
    fun activeId(prefs: Prefs): String = prefs.s(KEY_ACTIVE, "")

    /** Whether a Bluetooth device belongs to a known car. */
    fun knows(prefs: Prefs, addr: String): Boolean = load(prefs).any { addr in it.bt }

    /** Keeps the active car's settings with it (before another car takes over). */
    private fun keepActive(prefs: Prefs, cars: MutableList<Car>) {
        val i = cars.indexOfFirst { it.id == activeId(prefs) }
        if (i >= 0) cars[i] = cars[i].copy(data = snapshot(prefs))
    }

    /** Makes car [id] the active one: its settings go back in place. */
    @Synchronized fun switchTo(prefs: Prefs, id: String) {
        val cars = load(prefs)
        val target = cars.firstOrNull { it.id == id } ?: return
        if (activeId(prefs) == id) return
        keepActive(prefs, cars)
        apply(prefs, target.data)
        prefs.put(KEY_ACTIVE, id)
        save(prefs, cars)
        android.util.Log.w("VehicleProfiles", "car switched")
    }

    /** A new car from the settings as they are now, with [bt] as its Bluetooth device (if any); it becomes the active one. */
    @Synchronized fun create(prefs: Prefs, name: String, bt: String?): String {
        val cars = load(prefs)
        keepActive(prefs, cars)
        val id = "car" + System.currentTimeMillis()
        cars += Car(id, name.ifBlank { "Fahrzeug ${cars.size + 1}" }, listOfNotNull(bt), snapshot(prefs))
        prefs.put(KEY_ACTIVE, id)
        save(prefs, cars)
        return id
    }

    @Synchronized fun rename(prefs: Prefs, id: String, name: String) {
        val n = name.trim().take(40)
        if (n.isEmpty()) return
        save(prefs, load(prefs).map { if (it.id == id) it.copy(name = n) else it }.toMutableList())
    }

    /** Puts a Bluetooth device with [addr] to car [id] (taking it away from any other car). */
    @Synchronized fun assign(prefs: Prefs, id: String, addr: String) {
        val cars = load(prefs).map { c -> c.copy(bt = c.bt.filter { it != addr } + (if (c.id == id) listOf(addr) else emptyList())) }.toMutableList()
        save(prefs, cars)
    }

    @Synchronized fun delete(prefs: Prefs, id: String) {
        val cars = load(prefs).filter { it.id != id }.toMutableList()
        save(prefs, cars)
        if (activeId(prefs) == id) prefs.put(KEY_ACTIVE, "")
    }

    /**
     * A Bluetooth device connected: a car it's assigned to becomes the active one (its settings go back in place). A car-class
     * device the app doesn't know is left for the question in Settings.
     */
    fun onConnected(prefs: Prefs, addr: String, name: String, carClass: Boolean) {
        val car = load(prefs).firstOrNull { addr in it.bt }
        if (car != null) { if (prefs.s(KEY_PENDING, "").isNotBlank()) prefs.put(KEY_PENDING, ""); switchTo(prefs, car.id) }
        else if (carClass) prefs.put(KEY_PENDING, "$addr|$name")
    }

    /** The unknown device waiting for an answer: its address and name, or null. */
    fun pending(prefs: Prefs): Pair<String, String>? = prefs.s(KEY_PENDING, "").split('|', limit = 2).takeIf { it.size == 2 && it[0].isNotBlank() }?.let { it[0] to it[1] }
    fun clearPending(prefs: Prefs) = prefs.put(KEY_PENDING, "")
}
