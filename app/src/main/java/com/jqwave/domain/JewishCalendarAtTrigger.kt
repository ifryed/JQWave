package com.jqwave.domain

import com.jqwave.data.UserLocation
import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone

/**
 * Hebrew calendar for “now” (or [triggerMillis]) in [location]: after local sunset the date rolls forward,
 * matching [JewishEventEvaluator.dayOfOmerAtTrigger].
 */
fun jewishCalendarAtTrigger(location: UserLocation, triggerMillis: Long): JewishCalendar {
    val zone = runCatching { ZoneId.of(location.timeZoneId) }.getOrNull() ?: ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val geo = GeoLocation(
        "user",
        location.latitude,
        location.longitude,
        0.0,
        TimeZone.getTimeZone(location.timeZoneId),
    )
    val zcal = ComplexZmanimCalendar(geo)
    val cal = zcal.getCalendar()
    cal.set(Calendar.YEAR, today.year)
    cal.set(Calendar.MONTH, today.monthValue - 1)
    cal.set(Calendar.DAY_OF_MONTH, today.dayOfMonth)
    cal.set(Calendar.HOUR_OF_DAY, 12)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val jc = JewishCalendar().apply {
        setInIsrael(location.inIsrael)
        JewishEventEvaluator.setGregorianFromLocalDate(this, today)
    }
    val sunset = zcal.sunset?.time
    if (sunset != null && triggerMillis >= sunset) {
        jc.forward(Calendar.DATE, 1)
    }
    return jc
}
