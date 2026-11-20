package com.headunit.launcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * At the start of a route: a short note when today or tomorrow is a public holiday in the country you're in (more traffic on the
 * roads, and the holiday weekend's traffic jams). Shown for a few seconds; nothing is spoken.
 */
internal fun NavEngine.checkHolidayHint() {
    if (!prefs.b(PreferenceKeys.HOLIDAY_HINT, true)) return
    val cc = countryCode()
    scope.launch {
        val found = withContext(Dispatchers.IO) {
            val today = LocalDate.now()
            PublicHolidays.on(cc, today)?.let { true to it } ?: PublicHolidays.on(cc, today.plusDays(1))?.let { false to it }
        } ?: return@launch
        val (isToday, name) = found
        val day = if (isToday) Lang.t("heute", "today") else Lang.t("morgen", "tomorrow")
        showNotice(NavEngine.Notice("holiday", Lang.t("Feiertag $day", "Public holiday $day"),
            Lang.t("$name: mit mehr Verkehr auf der Strecke rechnen", "$name: expect more traffic on the road"), 1), 15_000L)
    }
}
