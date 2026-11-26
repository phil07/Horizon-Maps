package com.headunit.launcher

/**
 * The kinds of road report a driver can make. [key] is what is stored and shared, so it never changes. Phone, car and the
 * engine all read this one list; each screen only adds its own icon.
 */
enum class ReportKind(
    val key: String,
    private val de: String,
    private val en: String,
    /** How long a report stays on the map, in milliseconds. */
    val lifeMs: Long,
    /** The number of the icon on the route map. */
    val mapIcon: Int,
    /** The colour, as ARGB, on the phone and in the car. */
    val argb: Long,
    /** Also offered on the car's quick panel. */
    val quick: Boolean,
) {
    POLICE("police", "Polizei", "Police", 3 * HOUR, 3, 0xFF1E66F5, true),
    ACCIDENT("accident", "Unfall", "Crash", 2 * HOUR, 4, 0xFFFF4D4D, true),
    HAZARD("hazard", "Gefahr", "Hazard", HOUR, 5, 0xFFFFB020, true),
    BREAKDOWN("breakdown", "Panne", "Broken down", 2 * HOUR, 6, 0xFFFB8C00, true),
    MOBILECAM("mobilecam", "Mobiler Blitzer", "Mobile cam", 4 * HOUR, 7, 0xFF1E66F5, true),
    JAM("jam", "Stau", "Traffic jam", 30 * MINUTE, 8, 0xFFF4511E, true),
    ROADWORKS("roadworks", "Baustelle", "Roadworks", 12 * HOUR, 1, 0xFFFBC02D, false),
    CLOSED("closed", "Straße gesperrt", "Road closed", 3 * HOUR, 2, 0xFFFF4D4D, false);

    val label: String get() = Lang.t(de, en)

    companion object {
        private const val MINUTE = 60_000L
        private const val HOUR = 60 * MINUTE

        /** The kind stored under [key], or null for a kind this version does not know. */
        fun of(key: String): ReportKind? = entries.firstOrNull { it.key == key }

        /** The kind stored under [key]; an unknown one is shown as a police report, as before. */
        fun orPolice(key: String): ReportKind = of(key) ?: POLICE
    }
}
