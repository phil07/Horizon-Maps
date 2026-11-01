package com.headunit.launcher

/** A charging network the user can prefer. [aliases] are lowercase words matched in a charger's operator and name. */
data class ChargeOperator(val id: String, val label: String, val aliases: List<String>)

/**
 * The common fast-charging networks in Germany and around it, for "preferred operators" (Settings > Fuel > Your electric car).
 * A charger's operator comes as a company name ("EnBW mobility+ AG und Co.KG", "Tesla Germany GmbH") from TomTom, OpenStreetMap
 * or the Bundesnetzagentur register, and its name often carries the brand too ("IONITY Aichstetten"), so both are searched.
 */
object ChargeOperators {
    val all = listOf(
        ChargeOperator("ionity", "IONITY", listOf("ionity")),
        ChargeOperator("enbw", "EnBW", listOf("enbw")),
        ChargeOperator("tesla", "Tesla Supercharger", listOf("tesla", "supercharger")),
        ChargeOperator("aral", "Aral pulse", listOf("aral")),
        ChargeOperator("bp", "BP pulse", listOf("bp", "bp pulse")),
        ChargeOperator("shell", "Shell Recharge", listOf("shell", "newmotion")),
        ChargeOperator("ewe", "EWE Go", listOf("ewe go", "ewe")),
        ChargeOperator("allego", "Allego", listOf("allego")),
        ChargeOperator("fastned", "Fastned", listOf("fastned")),
        ChargeOperator("eon", "E.ON Drive", listOf("e.on", "eon drive", "e-on")),
        ChargeOperator("mer", "Mer", listOf("mer", "mer germany")),
        ChargeOperator("total", "TotalEnergies", listOf("totalenergies", "total")),
        ChargeOperator("electra", "Electra", listOf("electra")),
        ChargeOperator("atlante", "Atlante", listOf("atlante")),
        ChargeOperator("pfalzwerke", "Pfalzwerke", listOf("pfalzwerke")),
        ChargeOperator("vattenfall", "Vattenfall InCharge", listOf("vattenfall", "incharge")),
        ChargeOperator("elli", "Elli / Volkswagen", listOf("elli", "volkswagen group charging", "volkswagen charging")),
        ChargeOperator("mercedes", "Mercedes-Benz HPC", listOf("mercedes-benz hpc", "mercedes-benz charging", "mercedes hpc", "mercedes-benz high-power")),
        ChargeOperator("porsche", "Porsche", listOf("porsche")),
        ChargeOperator("lidl", "Lidl", listOf("lidl")),
        ChargeOperator("kaufland", "Kaufland", listOf("kaufland")),
        ChargeOperator("aldi", "ALDI", listOf("aldi")),
        ChargeOperator("eam", "EAM", listOf("eam")),
        ChargeOperator("comfortcharge", "Comfortcharge", listOf("comfortcharge", "comfort charge")),
        ChargeOperator("maingau", "Maingau", listOf("maingau")),
        ChargeOperator("ladenetz", "Ladenetz", listOf("ladenetz")),
        ChargeOperator("stadtwerke", "Stadtwerke", listOf("stadtwerke")),
    )

    // whole words only: a bare "mer" must not hit "Mercedes", nor "bp" a word that merely contains it
    private const val WORD = "a-z0-9äöüß"
    private val patterns: Map<String, List<Regex>> = all.associate { o -> o.id to o.aliases.map { Regex("(?<![$WORD])" + Regex.escape(it) + "(?![$WORD])") } }

    /** The ids picked in Settings, in list order. */
    fun selected(prefs: Prefs): Set<String> {
        val ids = prefs.s(PreferenceKeys.EV_PREF_OPS, "").split(',').map { it.trim() }.toSet()
        return all.map { it.id }.filter { it in ids }.toCollection(LinkedHashSet())
    }

    fun setSelected(prefs: Prefs, ids: Set<String>) = prefs.put(PreferenceKeys.EV_PREF_OPS, all.map { it.id }.filter { it in ids }.joinToString(","))

    private fun find(text: String?, ids: Collection<String>? = null): ChargeOperator? {
        if (text.isNullOrBlank()) return null
        val t = text.lowercase()
        return all.firstOrNull { o -> (ids == null || o.id in ids) && patterns[o.id].orEmpty().any { it.containsMatchIn(t) } }
    }

    /** Whether [n] is run by (or branded as) one of the operators [ids]. */
    fun matches(n: Nearby, ids: Set<String>): Boolean =
        ids.isNotEmpty() && (find(n.op, ids) != null || find(n.place.name, ids) != null)

    // company-form words left off a raw operator name, so it fits the narrow stop list
    private val legalForm = Regex("(?i)[\\s,]+(gmbh|mbh|ag|se|kg|ohg|ug|b\\.v\\.|bv|ltd\\.?|s\\.a\\.|sas|s\\.r\\.l\\.|aktiengesellschaft|und co\\.?\\s*kg|& co\\.?\\s*kg|co\\.?\\s*kg)(?=\\W|$).*$")

    /** The network to show for [n]: a known operator's label, else its operator as given (without "GmbH" etc.), else null. */
    fun label(n: Nearby): String? {
        val raw = n.op?.trim().orEmpty()
        val clean = raw.replace(legalForm, "").trim().ifEmpty { raw }.take(30)
        val o = find(n.op) ?: find(n.place.name)
        // "Stadtwerke" alone says little: the town in the operator's name ("Stadtwerke München") is kept
        if (o != null && !(o.id == "stadtwerke" && clean.isNotEmpty())) return o.label
        return clean.ifEmpty { null }
    }
}
