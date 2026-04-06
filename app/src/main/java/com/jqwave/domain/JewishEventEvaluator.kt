package com.jqwave.domain

import com.jqwave.data.EventKind
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.LocalDate
import java.util.Calendar

object JewishEventEvaluator {

    fun applies(kind: EventKind, jc: JewishCalendar): Boolean = when (kind) {
        EventKind.ROSH_HODESH -> jc.isRoshChodesh
        EventKind.SFIRAT_HAOMER -> jc.getDayOfOmer() >= 1
        EventKind.SHABBAT -> false
    }

    /**
     * KosherJava [JewishCalendar.setGregorianDate] expects a Java [Calendar]-style month (0 = January),
     * not [LocalDate.getMonthValue] (1–12).
     */
    fun setGregorianFromLocalDate(jc: JewishCalendar, day: LocalDate) {
        jc.setGregorianDate(day.year, day.monthValue - 1, day.dayOfMonth)
    }

    /**
     * Omer count for [triggerMillis] on civil [day]. After local sunset the Hebrew date advances, so day 1
     * (16 Nissan) is recognized for triggers that evening even though at civil noon the calendar is still 15 Nissan.
     *
     * @param sunsetMillis sunset on that civil day in the same clock as [triggerMillis], or null if unavailable
     */
    fun dayOfOmerAtTrigger(
        inIsrael: Boolean,
        day: LocalDate,
        triggerMillis: Long,
        sunsetMillis: Long?,
    ): Int {
        val jc = JewishCalendar().apply {
            setInIsrael(inIsrael)
            setGregorianFromLocalDate(this, day)
        }
        if (sunsetMillis != null && triggerMillis >= sunsetMillis) {
            jc.forward(Calendar.DATE, 1)
        }
        return jc.getDayOfOmer()
    }

    fun isOmerDayAtTrigger(
        inIsrael: Boolean,
        day: LocalDate,
        triggerMillis: Long,
        sunsetMillis: Long?,
    ): Boolean = dayOfOmerAtTrigger(inIsrael, day, triggerMillis, sunsetMillis) >= 1
}
