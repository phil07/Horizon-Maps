package com.headunit.launcher

/**
 * Road numbers as they are signed in the country you drive in. A German motorway carries both its national number and a
 * European one ("A 4" and "E 40"), but only the national one is on the signs people follow, so that is the one shown and
 * spoken. In Germany the letter and the number are written apart ("A 4", "B 27"), as on the blue and yellow signs; British
 * numbers ("M4", "A34") are left as they are.
 */
object RoadRefs {
    /** ISO country you are in (set by the engine from the GPS); blank until known. */
    @Volatile var country: String = ""

    /** The country to format a route's numbers for, from where it starts. */
    fun countryFor(start: LL?): String {
        if (start != null && Overpass.countryAt(start) == "GB") return "GB"
        return country.ifBlank { if (start != null) Overpass.countryAt(start) else java.util.Locale.getDefault().country }.uppercase()
    }

    private val euro = Regex("^E\\s?\\d{1,3}$", RegexOption.IGNORE_CASE)
    private val refLike = Regex("^[A-Za-z]{1,3}\\s?\\d{1,4}[a-zA-Z]?(\\(M\\))?$")
    private val deRef = Regex("^(A|B|L|K|S|St|Kr)\\s*(\\d{1,4}[a-z]?)$")

    /** A European E-road number ("E40", "E 40"). */
    fun isEuro(s: String): Boolean = euro.matches(s.trim())

    /** Looks like a road number rather than a name ("A4", "B 27", "M25", "A34(M)", "E 40"). */
    fun isRef(s: String): Boolean = refLike.matches(s.trim())

    /** One road number written the way the country signs it: "A4" becomes "A 4" in Germany. */
    fun format(ref: String, cc: String = country): String {
        val t = ref.trim()
        if (cc.uppercase() != "DE") return t
        val m = deRef.matchEntire(t) ?: return t
        return m.groupValues[1] + " " + m.groupValues[2]
    }

    /** "A4/E40", "A 4; E 40", "A4 (E40)" into their numbers. */
    fun split(s: String): List<String> =
        s.replace(Regex("\\((?!M\\))"), ";").replace(Regex("(?<!\\(M)\\)"), "")
            .split(';', '/', ',').map { it.trim() }.filter { it.isNotEmpty() }

    /** The number to show for a road with these numbers: the national one before a European one. Blank when there are none. */
    fun best(refs: List<String>, cc: String = country): String {
        val parts = refs.flatMap { split(it) }
        val pick = parts.firstOrNull { !isEuro(it) } ?: parts.firstOrNull() ?: return ""
        return format(pick, cc)
    }

    /**
     * The road for a route step: its [name] when it has a real one, otherwise its number (the national one, also looked for in
     * [refs], the other numbers the routing service gives for the same road).
     */
    fun street(name: String, refs: List<String>, cc: String = country): String {
        val n = name.trim()
        if (n.isEmpty()) return best(refs, cc)
        val parts = split(n)
        if (parts.isEmpty() || !parts.all { isRef(it) }) return n
        // only numbers (often just the E-number on a German motorway): the national one, from the name or the other numbers
        return best(parts.filter { !isEuro(it) } + refs + parts.filter { isEuro(it) }, cc)
    }
}
