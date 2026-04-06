package com.jqwave.domain

import com.jqwave.data.UserLocation
import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.TimeZone

/** Formats upcoming candle / havdalah wall times for test notifications. */
object ShabbatPreview {

    fun upcomingStartEndTimeLabels(location: UserLocation): Pair<String, String>? {
        val zone = runCatching { ZoneId.of(location.timeZoneId) }.getOrNull() ?: ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val friday = fridayForCurrentShabbatContext(today)
        val saturday = friday.plusDays(1)
        val geo = GeoLocation(
            "user",
            location.latitude,
            location.longitude,
            0.0,
            TimeZone.getTimeZone(location.timeZoneId),
        )
        val start = formatSunsetWithOffset(friday, geo, zone, -18) ?: return null
        val end = formatTzaitWithOffset(saturday, geo, zone, 0) ?: return null
        return start to end
    }

    private fun fridayForCurrentShabbatContext(today: LocalDate): LocalDate = when (today.dayOfWeek) {
        DayOfWeek.SATURDAY -> today.minusDays(1)
        DayOfWeek.FRIDAY -> today
        else -> {
            var d = today
            while (d.dayOfWeek != DayOfWeek.FRIDAY) d = d.plusDays(1)
            d
        }
    }

    private fun zmanimForNoon(day: LocalDate, geo: GeoLocation): ComplexZmanimCalendar {
        val zcal = ComplexZmanimCalendar(geo)
        val cal = zcal.getCalendar()
        cal.set(Calendar.YEAR, day.year)
        cal.set(Calendar.MONTH, day.monthValue - 1)
        cal.set(Calendar.DAY_OF_MONTH, day.dayOfMonth)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return zcal
    }

    private fun formatSunsetWithOffset(day: LocalDate, geo: GeoLocation, zone: ZoneId, offsetMinutes: Int): String? {
        val zcal = zmanimForNoon(day, geo)
        val base = zcal.sunset?.time ?: return null
        return formatMillis(base + offsetMinutes * 60_000L, zone)
    }

    private fun formatTzaitWithOffset(day: LocalDate, geo: GeoLocation, zone: ZoneId, offsetMinutes: Int): String? {
        val zcal = zmanimForNoon(day, geo)
        val base = zcal.getTzaisGeonim7Point083Degrees()?.time ?: return null
        return formatMillis(base + offsetMinutes * 60_000L, zone)
    }

    private fun formatMillis(epochMillis: Long, zone: ZoneId): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withZone(zone)
            .format(Instant.ofEpochMilli(epochMillis))
}
