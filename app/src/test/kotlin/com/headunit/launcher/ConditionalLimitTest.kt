package com.headunit.launcher

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConditionalLimitTest {
    private fun at(dow: Int, h: Int, m: Int = 0): Calendar = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 2, h, m, 0)   // a Monday
        set(Calendar.DAY_OF_WEEK, dow)
    }
    private fun kmh(v: Double?) = v?.let { Math.round(it * 3.6).toInt() }

    @Test fun nightOverMidnight() {
        assertEquals(30, kmh(ConditionalLimit.at("30 @ (22:00-06:00)", "DE", at(Calendar.MONDAY, 23))))
        assertEquals(30, kmh(ConditionalLimit.at("30 @ (22:00-06:00)", "DE", at(Calendar.TUESDAY, 5, 59))))
        assertNull(ConditionalLimit.at("30 @ (22:00-06:00)", "DE", at(Calendar.MONDAY, 12)))
    }

    @Test fun weekdaysWithSchoolHolidaysOff() {
        val c = "30 @ (Mo-Fr 07:00-17:00; SH off)"
        assertEquals(30, kmh(ConditionalLimit.at(c, "DE", at(Calendar.WEDNESDAY, 8))))
        assertNull(ConditionalLimit.at(c, "DE", at(Calendar.SATURDAY, 8)))
        assertNull(ConditionalLimit.at(c, "DE", at(Calendar.WEDNESDAY, 18)))
    }

    @Test fun notForCarsOrWeather() {
        assertNull(ConditionalLimit.at("60 @ (weight>7.5)", "DE", at(Calendar.MONDAY, 12)))
        assertNull(ConditionalLimit.at("80 @ wet", "DE", at(Calendar.MONDAY, 12)))
        assertEquals(80, kmh(ConditionalLimit.at("80 @ wet", "DE", at(Calendar.MONDAY, 12), wet = true)))
    }

    @Test fun severalRulesAndNone() {
        val c = "60 @ (weight>3.5); 120 @ (06:00-20:00); none @ (20:00-06:00)"
        assertEquals(120, kmh(ConditionalLimit.at(c, "DE", at(Calendar.MONDAY, 12))))
        assertEquals(-1.0, ConditionalLimit.at(c, "DE", at(Calendar.MONDAY, 22))!!, 0.0)
    }
}
