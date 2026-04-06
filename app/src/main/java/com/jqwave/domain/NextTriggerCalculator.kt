package com.jqwave.domain

import com.jqwave.data.EventKind
import com.jqwave.data.NotificationRule
import com.jqwave.data.ShabbatSegment
import com.jqwave.data.TimeAnchor
import com.jqwave.data.UserLocation
import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

object NextTriggerCalculator {

    private const val MAX_DAYS_AHEAD = 500

    fun nextTriggerMillis(
        kind: EventKind,
        rule: NotificationRule,
        location: UserLocation,
        fromMillis: Long = System.currentTimeMillis(),
    ): Long? {
        val zone = runCatching { ZoneId.of(location.timeZoneId) }.getOrNull() ?: ZoneId.systemDefault()
        val geo = GeoLocation(
            "user",
            location.latitude,
            location.longitude,
            0.0,
            TimeZone.getTimeZone(location.timeZoneId),
        )
        val zcal = ComplexZmanimCalendar(geo)
        val jc = JewishCalendar().apply { setInIsrael(location.inIsrael) }

        var day = LocalDate.ofInstant(Instant.ofEpochMilli(fromMillis), zone)
        repeat(MAX_DAYS_AHEAD) {
            val trigger = triggerOnGregorianDay(day, rule, zcal, jc, kind, zone)
            if (trigger != null && trigger > fromMillis) return trigger
            day = day.plusDays(1)
        }
        return null
    }

    private fun triggerOnGregorianDay(
        day: LocalDate,
        rule: NotificationRule,
        zcal: ComplexZmanimCalendar,
        jc: JewishCalendar,
        kind: EventKind,
        zone: ZoneId,
    ): Long? {
        val cal = zcal.getCalendar()
        cal.set(Calendar.YEAR, day.year)
        cal.set(Calendar.MONTH, day.monthValue - 1)
        cal.set(Calendar.DAY_OF_MONTH, day.dayOfMonth)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val triggerMillis = when (rule.anchor) {
            TimeAnchor.CLOCK -> {
                ZonedDateTime.of(day, LocalTime.of(rule.hour, rule.minute), zone)
                    .toInstant()
                    .toEpochMilli()
            }
            TimeAnchor.SUNRISE -> {
                val base = zcal.sunrise ?: return null
                base.time + rule.offsetMinutes * 60_000L
            }
            TimeAnchor.SUNSET -> {
                val base = zcal.sunset ?: return null
                base.time + rule.offsetMinutes * 60_000L
            }
            TimeAnchor.TZAIT -> {
                val base = zcal.tzait7083 ?: return null
                base.time + rule.offsetMinutes * 60_000L
            }
        }

        if (!eventAppliesAtTrigger(kind, rule, jc, day, triggerMillis, zcal, zone)) return null
        return triggerMillis
    }

    private fun eventAppliesAtTrigger(
        kind: EventKind,
        rule: NotificationRule,
        templateJc: JewishCalendar,
        day: LocalDate,
        triggerMillis: Long,
        zcal: ComplexZmanimCalendar,
        zone: ZoneId,
    ): Boolean = when (kind) {
        EventKind.SFIRAT_HAOMER -> {
            JewishEventEvaluator.isOmerDayAtTrigger(
                templateJc.getInIsrael(),
                day,
                triggerMillis,
                zcal.sunset?.time,
            )
        }
        EventKind.ROSH_HODESH -> {
            JewishEventEvaluator.setGregorianFromLocalDate(templateJc, day)
            JewishEventEvaluator.applies(kind, templateJc)
        }
        EventKind.SHABBAT -> {
            val seg = rule.shabbatSegment ?: return false
            val dow = Instant.ofEpochMilli(triggerMillis).atZone(zone).dayOfWeek
            when (seg) {
                ShabbatSegment.START ->
                    dow == DayOfWeek.FRIDAY && rule.anchor != TimeAnchor.TZAIT
                ShabbatSegment.END ->
                    dow == DayOfWeek.SATURDAY &&
                        (rule.anchor == TimeAnchor.TZAIT || rule.anchor == TimeAnchor.CLOCK)
            }
        }
    }

    private val ComplexZmanimCalendar.sunrise: Date?
        get() = getSunrise()

    private val ComplexZmanimCalendar.sunset: Date?
        get() = getSunset()

    private val ComplexZmanimCalendar.tzait7083: Date?
        get() = getTzaisGeonim7Point083Degrees()
}
